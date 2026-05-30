package crypto;

import javax.crypto.SecretKey;

public class DatabaseEncryption {
    
    // 🔑 NOUVELLE CLÉ MAÎTRE (valide)
    private static final String MASTER_KEY_BASE64 = "kBeW9D71IS9qj4Y9HlcRxzi4yJKA2tMphHJ1SYorstc=";
    private static SecretKey masterKey;
    
    static {
        try {
            masterKey = AESUtil.decodeKey(MASTER_KEY_BASE64);
            System.out.println("✅ Clé maître chargée avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement clé maître: " + e.getMessage());
            throw new RuntimeException("Erreur initialisation clé maître", e);
        }
    }
    
    public static String encryptSensitive(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return "";
        try {
            return AESUtil.encrypt(plaintext, masterKey);
        } catch (Exception e) {
            System.err.println("Erreur chiffrement: " + e.getMessage());
            return plaintext;
        }
    }
    
    public static String decryptSensitive(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) return "";
        try {
            return AESUtil.decrypt(ciphertext, masterKey);
        } catch (Exception e) {
            System.err.println("Erreur déchiffrement: " + e.getMessage());
            return ciphertext;
        }
    }
}