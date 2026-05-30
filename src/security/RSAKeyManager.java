package security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public class RSAKeyManager {

    private static final String KEYS_DIR = "keys/";
    
    // Liste des noms de fichiers à ignorer (ce ne sont pas des keystores admin)
    private static final List<String> IGNORED_FILES = List.of(
        "server.p12", 
        "truststore.p12", 
        "admin_truststore.p12"
    );
    
    // Pour l'ancien système (fichiers .key) - gardé pour compatibilité
    private static final String ADMIN_PRIVATE_KEY_FILE = KEYS_DIR + "admin_private.key";
    private static final String ADMIN_PUBLIC_KEY_FILE = KEYS_DIR + "admin_public.key";

    // ==================== NOUVEAU : PKCS#12 ====================

    /**
     * Charge la clé privée depuis un PKCS#12 KeyStore
     * @param email Email de l'admin (correspond au nom du fichier .p12)
     * @param password Mot de passe du KeyStore
     */
    public static PrivateKeyData loadPrivateKeyFromPKCS12(String email, String password) throws Exception {
        String keystorePath = KEYS_DIR + email + ".p12";
        File keystoreFile = new File(keystorePath);
        
        if (!keystoreFile.exists()) {
            throw new Exception("PKCS#12 introuvable pour " + email + " : " + keystorePath);
        }
        
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, password.toCharArray());
        }
        
        // Trouver le premier alias
        Enumeration<String> aliases = ks.aliases();
        if (!aliases.hasMoreElements()) {
            throw new Exception("Aucun alias trouvé dans le KeyStore");
        }
        
        String alias = aliases.nextElement();
        
        // Récupérer la clé privée
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
        
        // Récupérer l'email depuis le certificat
        java.security.cert.Certificate cert = ks.getCertificate(alias);
        if (cert instanceof X509Certificate) {
            X509Certificate x509 = (X509Certificate) cert;
            String subjectDN = x509.getSubjectX500Principal().getName();
            String extractedEmail = extractEmailFromDN(subjectDN);
            if (extractedEmail != null && !extractedEmail.isEmpty()) {
                return new PrivateKeyData(extractedEmail, privateKey);
            }
        }
        
        // Fallback: utiliser l'email passé en paramètre
        return new PrivateKeyData(email, privateKey);
    }

    /**
     * Pour la connexion automatique (sans spécifier l'email)
     * Scan le dossier keys pour trouver les fichiers .p12
     * Si un seul fichier, demande le mot de passe et charge
     * Si plusieurs fichiers, retourne une exception pour que l'UI gère le choix
     */
    public static PrivateKeyData loadDefaultPKCS12() throws Exception {
        List<String> p12Files = getPKCS12Files();
        
        if (p12Files.isEmpty()) {
            throw new Exception("Aucun fichier PKCS#12 trouvé dans " + KEYS_DIR);
        }
        
        if (p12Files.size() == 1) {
            // Un seul fichier, on demande le mot de passe
            String email = p12Files.get(0);
            JPasswordField passwordField = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(null, passwordField,
                "🔒 Mot de passe pour " + email,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            
            if (option != JOptionPane.OK_OPTION) {
                throw new Exception("Authentification annulée");
            }
            
            String password = new String(passwordField.getPassword());
            return loadPrivateKeyFromPKCS12(email, password);
        }
        
        // Plusieurs fichiers, on laisse l'UI gérer
        throw new Exception("MULTIPLE_KEYS_FOUND");
    }

    /**
     * Récupère la liste de tous les emails admin disponibles (pour menu déroulant)
     */
    public static List<String> getAllPKCS12Emails() {
        return getPKCS12Files();
    }

    /**
     * Vérifie s'il y a plusieurs fichiers PKCS#12
     */
    public static boolean hasMultiplePKCS12() {
        return getPKCS12Files().size() > 1;
    }

    /**
     * Vérifie s'il existe au moins un fichier PKCS#12
     */
    public static boolean hasAnyPKCS12() {
        return !getPKCS12Files().isEmpty();
    }

    /**
     * Retourne la liste des fichiers .p12 valides pour les admins
     * (exclut les fichiers système comme server.p12, truststore.p12, etc.)
     */
    private static List<String> getPKCS12Files() {
        List<String> emails = new ArrayList<>();
        File keysDir = new File(KEYS_DIR);
        
        if (!keysDir.exists() || !keysDir.isDirectory()) {
            return emails;
        }
        
        File[] p12Files = keysDir.listFiles((dir, name) -> 
            name.endsWith(".p12") && !IGNORED_FILES.contains(name)
        );
        
        if (p12Files != null) {
            for (File f : p12Files) {
                String fileName = f.getName();
                String email = fileName.substring(0, fileName.length() - 4); // Enlever .p12
                // Vérifier que c'est bien un email (contient @)
                if (email.contains("@")) {
                    emails.add(email);
                }
            }
        }
        
        return emails;
    }

    private static String extractEmailFromDN(String dn) {
        // CN=admin@chri.com, OU=Admin, O=ChriOnline, C=MA
        for (String part : dn.split(",")) {
            part = part.trim();
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return null;
    }

    /**
     * Charge une clé publique depuis une chaîne Base64
     */
    public static PublicKey loadPublicKeyFromBase64(String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    // ==================== ANCIENNES MÉTHODES (garde pour compatibilité) ====================

    /**
     * Lit le fichier de clé privée et retourne l'email et la clé (ancien système)
     * @deprecated Utiliser loadPrivateKeyFromPKCS12() à la place
     */
    @Deprecated
    public static PrivateKeyData loadPrivateKeyWithEmail() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_PRIVATE_KEY_FILE))) {
            String email = reader.readLine().trim();
            String privateKeyBase64 = reader.readLine().trim();
            
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(spec);
            
            return new PrivateKeyData(email, privateKey);
        }
    }

    @Deprecated
    public static PrivateKey loadPrivateKeyFromFile(String filePath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // Lire et ignorer l'email
            String privateKeyBase64 = reader.readLine().trim();
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        }
    }

    @Deprecated
    public static PublicKey loadPublicKeyFromFile(String filePath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String publicKeyBase64 = reader.readLine().trim();
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        }
    }

    @Deprecated
    public static boolean adminPrivateKeyExists() {
        return new File(ADMIN_PRIVATE_KEY_FILE).exists();
    }

    @Deprecated
    public static String getAdminPrivateKeyPath() {
        return ADMIN_PRIVATE_KEY_FILE;
    }

    @Deprecated
    public static String getAdminPublicKeyPath() {
        return ADMIN_PUBLIC_KEY_FILE;
    }

    // ==================== CLASSE INTERNE ====================

    /**
     * Classe pour retourner email + clé privée
     */
    public static class PrivateKeyData {
        public final String email;
        public final PrivateKey privateKey;
        
        public PrivateKeyData(String email, PrivateKey privateKey) {
            this.email = email;
            this.privateKey = privateKey;
        }
    }
}