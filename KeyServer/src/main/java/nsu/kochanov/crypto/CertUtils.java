package nsu.kochanov.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public final class CertUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CertUtils() {}

    public static X509Certificate issueCertificate(String issuerDn, String subjectDn, KeyPair subjectKeyPair, PrivateKey caPrivateKey) {
        try {
            X500Name issuer = new X500Name(issuerDn);
            X500Name subject = new X500Name(subjectDn);
            Instant now = Instant.now();
            Date notBefore = Date.from(now.minus(1, ChronoUnit.MINUTES));
            Date notAfter = Date.from(now.plus(3650, ChronoUnit.DAYS)); // ~10 лет
            BigInteger serial = new BigInteger(160, SECURE_RANDOM);

            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                subjectKeyPair.getPublic()
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA512withRSA").build(caPrivateKey);
            X509CertificateHolder holder = builder.build(signer);
            Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(new BouncyCastleProvider())
                    .getCertificate(holder);
            return (X509Certificate) cert;
        } catch (Exception e) {
            throw new RuntimeException("Failed to issue certificate", e);
        }
    }
}


