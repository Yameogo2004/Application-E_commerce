package security;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Scanner;
import java.security.cert.CertificateFactory;

public class PKCS12GeneratorUtil {

    private static final int KEY_SIZE = 2048;
    private static final String KEYS_DIR = "keys/";

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("📧 Entrez l'email de l'admin : ");
            String adminEmail = scanner.nextLine().trim();
            System.out.print("🔒 Mot de passe pour protéger le PKCS#12 : ");
            String password = scanner.nextLine().trim();
            scanner.close();

            if (adminEmail.isEmpty() || password.isEmpty()) {
                System.out.println("❌ Email et mot de passe requis !");
                return;
            }

            // Générer la paire de clés RSA
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // Créer le KeyStore PKCS#12
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            
            // Pour PKCS#12, il faut un certificat. On utilise un certificat temporaire auto-signé
            X509Certificate cert = createTemporaryCertificate(adminEmail, keyPair);
            ks.setKeyEntry("admin", privateKey, password.toCharArray(), new Certificate[]{cert});

            // Sauvegarder le KeyStore
            File dir = new File(KEYS_DIR);
            if (!dir.exists()) dir.mkdirs();
            
            String keystoreFile = KEYS_DIR + adminEmail + ".p12";
            try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
                ks.store(fos, password.toCharArray());
            }

            // Sauvegarder la clé publique
            String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String publicKeyFile = KEYS_DIR + adminEmail + "_public.key";
            try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
                fos.write(publicKeyBase64.getBytes());
            }

            System.out.println("\n=== PKCS#12 GÉNÉRÉ AVEC SUCCÈS ===");
            System.out.println("📧 Email: " + adminEmail);
            System.out.println("📁 Fichier PKCS#12: " + keystoreFile);
            System.out.println("🔑 Fichier clé publique: " + publicKeyFile);
            System.out.println("\n🔑 Clé PUBLIQUE (à stocker dans la BDD):");
            System.out.println(publicKeyBase64);

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static X509Certificate createTemporaryCertificate(String email, KeyPair keyPair) throws Exception {
        // Alternative: générer un certificat simple
        java.security.cert.CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // Pour un vrai certificat, il faudrait une librairie. 
        // On retourne null en attendant - keytool reste la meilleure solution
        throw new Exception("Veuillez utiliser keytool en ligne de commande pour générer le certificat");
    }
}