package nsu.kochanov.crypto;

import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;

public final class KeyStoreService {
    private final PrivateKey caPrivateKey;
    private final String issuerDn;

    private KeyStoreService(PrivateKey caPrivateKey, String issuerDn) {
        this.caPrivateKey = caPrivateKey;
        this.issuerDn = issuerDn;
    }

    public static KeyStoreService load(Path caKeyPath, String issuerDn) throws IOException {
        PrivateKey key = PemUtils.readPrivateKey(caKeyPath);
        return new KeyStoreService(key, issuerDn);
    }

    public PrivateKey getCaPrivateKey() {
        return caPrivateKey;
    }

    public String getIssuerDn() {
        return issuerDn;
    }
}




