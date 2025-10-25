package nsu.kochanov.server;

import nsu.kochanov.crypto.KeyStoreService;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerApp {
    private static final Logger LOG = Logger.getLogger(ServerApp.class.getName());
    public static void main(String[] args) throws Exception {
        // Простой парсинг аргументов: --port --threads --issuer --ca-key
        int port = 9000;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        String issuer = "CN=KeyServer CA";
        Path caKey = null;
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--threads" -> threads = Integer.parseInt(args[++i]);
                case "--issuer" -> issuer = args[++i];
                case "--ca-key" -> caKey = Path.of(args[++i]);
                default -> {}
            }
        }
        if (caKey == null) {
            throw new IllegalArgumentException("--ca-key path is required");
        }
        LOG.info("Starting KeyServer on port=" + port + ", threads=" + threads + ", issuer='" + issuer + "'");
        var ks = KeyStoreService.load(caKey, issuer);
        var server = new KeyServer(port, threads, ks);
        try {
            server.start();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Server stopped with error", t);
            throw t;
        }
    }
}


