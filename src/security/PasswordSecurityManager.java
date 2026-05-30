package security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Gestionnaire de hachage de mots de passe avec PBKDF2-HMAC-SHA256.
 * Conforme aux recommandations NIST SP 800-132.
 * - Salt aléatoire de 16 octets
 * - 65536 itérations
 * - Clé dérivée de 256 bits
 * - Comparaison en temps constant (anti-timing attack)
 */
public class PasswordSecurityManager {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;   // bits
    private static final int SALT_LENGTH = 16;   // bytes

    /**
     * Hache un mot de passe avec un salt aléatoire.
     * @param password mot de passe en clair (le tableau sera effacé après usage)
     * @return chaîne encodée en Base64 contenant salt+hash, ou null si erreur
     */
    public static String hashPassword(char[] password) {
        try {
            // Générer salt
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            // Dériver la clé
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(keySpec).getEncoded();

            // Combiner salt + hash
            byte[] saltAndHash = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, saltAndHash, 0, salt.length);
            System.arraycopy(hash, 0, saltAndHash, salt.length, hash.length);

            // Encodage Base64
            String hashedPassword = Base64.getEncoder().encodeToString(saltAndHash);

            // Nettoyer la mémoire
            Arrays.fill(password, '\0');
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(hash, (byte) 0);

            return hashedPassword;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("❌ Erreur PBKDF2 : " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie un mot de passe contre un hash stocké (format salt+hash).
     * @param password mot de passe à tester
     * @param storedHash hash stocké encodé en Base64 (salt + hash)
     * @return true si correspondance
     */
    public static boolean verifyPassword(char[] password, String storedHash) {
        try {
            byte[] saltAndHash = Base64.getDecoder().decode(storedHash);

            // Extraire le salt
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(saltAndHash, 0, salt, 0, SALT_LENGTH);

            // Dériver la clé avec le même salt
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] computedHash = factory.generateSecret(keySpec).getEncoded();

            // Extraire le hash stocké
            byte[] storedHashOnly = new byte[saltAndHash.length - SALT_LENGTH];
            System.arraycopy(saltAndHash, SALT_LENGTH, storedHashOnly, 0, storedHashOnly.length);

            // Comparaison en temps constant
            boolean isValid = constantTimeEquals(computedHash, storedHashOnly);

            Arrays.fill(password, '\0');
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(computedHash, (byte) 0);
            Arrays.fill(storedHashOnly, (byte) 0);

            return isValid;

        } catch (Exception e) {
            System.err.println("Erreur vérification mot de passe : " + e.getMessage());
            return false;
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Affiche les paramètres de sécurité (pour information).
     */
    public static void printSecurityInfo() {
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("🔐 CONFIGURATION SÉCURITÉ MOTS DE PASSE (PBKDF2)");
        System.out.println("Algorithme       : " + ALGORITHM);
        System.out.println("Itérations       : " + ITERATIONS);
        System.out.println("Longueur hash    : " + KEY_LENGTH + " bits");
        System.out.println("Longueur salt    : " + SALT_LENGTH + " bytes");
        System.out.println("══════════════════════════════════════════════════════");
    }
}