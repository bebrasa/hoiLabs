package nsu.kochanov.tools;

import nsu.kochanov.crypto.PemUtils;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

public final class GenCaKey {
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "ca.key");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(8192, new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();
        PemUtils.writePem(out, kp.getPrivate());
        System.out.println("CA private key written to " + out);
    }
}




