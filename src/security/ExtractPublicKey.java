package security;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64;

public class ExtractPublicKey {
    public static void main(String[] args) throws Exception {
        String password = "ariyam";
        String keystorePath = "keys/yameogoariel0@gmail.com.p12";
        
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, password.toCharArray());
        }
        
        Certificate cert = ks.getCertificate("admin");
        PublicKey publicKey = cert.getPublicKey();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        
        System.out.println("🔑 Clé publique (à stocker dans la BDD) :");
        System.out.println(publicKeyBase64);
    }
}