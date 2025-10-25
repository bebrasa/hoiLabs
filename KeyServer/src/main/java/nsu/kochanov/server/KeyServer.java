package nsu.kochanov.server;

import nsu.kochanov.crypto.CertUtils;
import nsu.kochanov.crypto.KeyStoreService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KeyServer {
    private static final Logger LOG = Logger.getLogger(KeyServer.class.getName());
    private static final int MAX_NAME_LEN = 4096;
    private static final byte ZERO = 0;

    private final int port;
    private final int generatorThreads;
    private final KeyStoreService keyStoreService;

    private final Map<String, CompletableFuture<Result>> nameToFuture = new ConcurrentHashMap<>();
    private final ExecutorService generatorPool;
    private final Queue<Runnable> ioTasks = new ConcurrentLinkedQueue<>();

    public KeyServer(int port, int generatorThreads, KeyStoreService keyStoreService) {
        this.port = port;
        this.generatorThreads = generatorThreads;
        this.keyStoreService = keyStoreService;
        this.generatorPool = Executors.newFixedThreadPool(generatorThreads, r -> {
            Thread t = new Thread(r, "keygen-thread");
            t.setDaemon(true);
            return t;
        });
    }

    private record Result(KeyPair keyPair, X509Certificate certificate) {}

    private static final class ConnState {
        final SocketChannel channel;
        final ByteBuffer readBuf = ByteBuffer.allocate(8192);
        ByteBuffer writeBuf; // заполняется PEMами для ответа
        boolean closed;
        String name; // когда получим 0-терминатор

        ConnState(SocketChannel channel) {
            this.channel = channel;
        }
    }

    public void start() throws IOException {
        try (Selector selector = Selector.open();
             ServerSocketChannel server = ServerSocketChannel.open()) {
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            LOG.info(() -> "Listening on tcp://0.0.0.0:" + port);

            while (true) {
                // Выполним накопленные IO-задачи (например, переключение на запись)
                Runnable task;
                while ((task = ioTasks.poll()) != null) task.run();

                selector.select(250);

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        handleAccept(server, selector);
                    } else if (key.isReadable()) {
                        handleRead(key, selector);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                }
            }
        }
    }

    private void handleAccept(ServerSocketChannel server, Selector selector) throws IOException {
        SocketChannel ch = server.accept();
        if (ch == null) return;
        ch.configureBlocking(false);
        SelectionKey key = ch.register(selector, SelectionKey.OP_READ);
        key.attach(new ConnState(ch));
        LOG.fine(() -> "Accepted connection from " + safeRemote(ch));
    }

    private void handleRead(SelectionKey key, Selector selector) {
        ConnState st = (ConnState) key.attachment();
        try {
            int read = st.channel.read(st.readBuf);
            if (read == -1) { closeKey(key); return; }
            st.readBuf.flip();
            // Ищем нулевой байт
            int zeroPos = -1;
            for (int i = st.readBuf.position(); i < st.readBuf.limit(); i++) {
                if (st.readBuf.get(i) == ZERO) { zeroPos = i; break; }
            }
            if (zeroPos == -1) {
                if (st.readBuf.remaining() > MAX_NAME_LEN) { closeKey(key); return; }
                st.readBuf.compact();
                return;
            }
            int len = zeroPos - st.readBuf.position();
            byte[] nameBytes = new byte[len];
            st.readBuf.get(nameBytes);
            st.readBuf.get(); // consume zero
            st.readBuf.compact();
            st.name = new String(nameBytes); // ASCII по условию
            LOG.info(() -> "Request received for name='" + st.name + "'");

            CompletableFuture<Result> fut = nameToFuture.computeIfAbsent(st.name, n -> {
                CompletableFuture<Result> f = new CompletableFuture<>();
                submitGeneration(n, f);
                return f;
            });

            fut.whenComplete((res, err) -> {
                if (err != null) { safeClose(st.channel); return; }
                try {
                    String pemPriv = nsu.kochanov.crypto.PemUtils.toPem(res.keyPair().getPrivate());
                    String pemCert = nsu.kochanov.crypto.PemUtils.toPem(res.certificate());
                    byte[] payload = (pemPriv + pemCert).getBytes();
                    st.writeBuf = ByteBuffer.wrap(payload);
                    schedule(selector, () -> key.interestOps(SelectionKey.OP_WRITE));
                    LOG.fine(() -> "Prepared response for name='" + st.name + "' (" + payload.length + " bytes)");
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to prepare PEM response", e);
                    safeClose(st.channel);
                }
            });
        } catch (IOException e) {
            LOG.log(Level.FINE, "IO error on read", e);
            closeKey(key);
        }
    }

    private void handleWrite(SelectionKey key) {
        ConnState st = (ConnState) key.attachment();
        try {
            if (st.writeBuf == null) { key.interestOps(SelectionKey.OP_READ); return; }
            st.channel.write(st.writeBuf);
            if (!st.writeBuf.hasRemaining()) {
                // Ответ отправлен — закрываем соединение
                LOG.fine(() -> "Response sent, closing " + safeRemote(st.channel));
                closeKey(key);
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "IO error on write", e);
            closeKey(key);
        }
    }

    private void submitGeneration(String name, CompletableFuture<Result> target) {
        generatorPool.submit(() -> {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(8192, new SecureRandom());
                KeyPair kp = kpg.generateKeyPair();
                X509Certificate cert = CertUtils.issueCertificate(
                        keyStoreService.getIssuerDn(),
                        "CN=" + name,
                        kp,
                        keyStoreService.getCaPrivateKey()
                );
                target.complete(new Result(kp, cert));
                LOG.info(() -> "Generated RSA8192 and certificate for '" + name + "'");
            } catch (Exception e) {
                target.completeExceptionally(e);
                LOG.log(Level.WARNING, "Generation failed for '" + name + "'", e);
            }
        });
    }

    private void schedule(Selector selector, Runnable r) {
        ioTasks.add(r);
        selector.wakeup();
    }

    private void closeKey(SelectionKey key) {
        try { key.channel().close(); } catch (IOException ignored) {}
        key.cancel();
    }

    private void safeClose(Channel ch) {
        try { ch.close(); } catch (IOException ignored) {}
    }

    private static String safeRemote(SocketChannel ch) {
        try { return String.valueOf(ch.getRemoteAddress()); } catch (IOException e) { return "?"; }
    }
}



