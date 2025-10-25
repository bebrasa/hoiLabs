package nsu.kochanov.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KeyClient {
    private static final Logger LOG = Logger.getLogger(KeyClient.class.getName());
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 9000;
        String name = null;
        int delay = 0; // seconds
        boolean crash = false;
        Path outDir = Path.of(".");

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--name" -> name = args[++i];
                case "--delay" -> delay = Integer.parseInt(args[++i]);
                case "--crash" -> crash = Boolean.parseBoolean(args[++i]);
                case "--out" -> outDir = Path.of(args[++i]);
                default -> {}
            }
        }
        if (name == null) throw new IllegalArgumentException("--name required");
        Files.createDirectories(outDir);

        try (SocketChannel ch = SocketChannel.open()) {
            LOG.info("Connecting to " + host + ":" + port);
            ch.connect(new InetSocketAddress(host, port));
            ch.configureBlocking(true);
            byte[] nameBytes = name.getBytes();
            ByteBuffer send = ByteBuffer.allocate(nameBytes.length + 1);
            send.put(nameBytes).put((byte)0).flip();
            while (send.hasRemaining()) ch.write(send);
            LOG.info("Sent name='" + name + "'");

            if (crash) {
                LOG.warning("Exiting early to simulate crash");
                return; // имитация аварийного завершения до чтения
            }
            if (delay > 0) Thread.sleep(delay * 1000L);

            // читаем весь ответ до закрытия сервером
            ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
            while (true) {
                int r = ch.read(buf);
                if (r == -1) break;
                if (!buf.hasRemaining()) {
                    ByteBuffer bigger = ByteBuffer.allocate(buf.capacity() * 2);
                    buf.flip();
                    bigger.put(buf);
                    buf = bigger;
                }
            }
            buf.flip();
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            String payload = new String(data);

            // Отделим приватный ключ и сертификат по заголовкам PEM
            int keyStart = payload.indexOf("-----BEGIN");
            int certStart = payload.indexOf("-----BEGIN CERTIFICATE-----");
            if (keyStart == -1 || certStart == -1) throw new IOException("Invalid payload");
            String keyPem = payload.substring(keyStart, certStart);
            String certPem = payload.substring(certStart);

            Path keyFile = outDir.resolve(name + ".key");
            Path crtFile = outDir.resolve(name + ".crt");
            Files.writeString(keyFile, keyPem);
            Files.writeString(crtFile, certPem);
            LOG.info("Saved: " + keyFile + " and " + crtFile);
        }
    }
}



