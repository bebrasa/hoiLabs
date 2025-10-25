package nsu.kochanov.crypto;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public final class PemUtils {
    private PemUtils() {}

    public static Object readPemObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path); PEMParser parser = new PEMParser(reader)) {
            return parser.readObject();
        }
    }

    public static KeyPair readKeyPair(Path path) throws IOException {
        Object obj = readPemObject(path);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        if (obj instanceof PEMKeyPair pemKeyPair) {
            return converter.getKeyPair(pemKeyPair);
        }
        if (obj instanceof PrivateKeyInfo privInfo) {
            PrivateKey privateKey = converter.getPrivateKey(privInfo);
            SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(privInfo.parsePrivateKey());
            PublicKey publicKey = converter.getPublicKey(spki);
            return new KeyPair(publicKey, privateKey);
        }
        throw new IOException("Unsupported PEM object type: " + (obj == null ? "null" : obj.getClass()));
    }

    public static PrivateKey readPrivateKey(Path path) throws IOException {
        Object obj = readPemObject(path);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        if (obj instanceof PrivateKeyInfo privInfo) {
            return converter.getPrivateKey(privInfo);
        }
        if (obj instanceof PEMKeyPair pemKeyPair) {
            return converter.getKeyPair(pemKeyPair).getPrivate();
        }
        throw new IOException("Unsupported PEM object type: " + (obj == null ? "null" : obj.getClass()));
    }

    public static String toPem(Object obj) throws IOException {
        try (StringWriter sw = new StringWriter(); JcaPEMWriter writer = new JcaPEMWriter(sw)) {
            writer.writeObject(obj);
            writer.flush();
            return sw.toString();
        }
    }

    public static void writePem(Path path, Object obj) throws IOException {
        String pem = toPem(obj);
        Files.writeString(path, pem);
    }
}


