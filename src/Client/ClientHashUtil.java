package Client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire de hashage côté client (SHA-256).
 * Le serveur appliquera PBKDF2 sur ce hash.
 */
public class ClientHashUtil {

    /**
     * Hache le mot de passe en SHA-256 et retourne la représentation hexadécimale.
     * @param password mot de passe en clair
     * @return hash hexadécimal
     */
    public static String hashPasswordClient(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur SHA-256 : " + e.getMessage());
            return null;
        }
    }
}