package crypto;

import javax.crypto.Cipher;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.Base64;

public class RSAUtil {
    
    private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";
    
    /**
     * Chiffre un message avec la clé publique RSA
     */
    public static String encrypt(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    /**
     * Déchiffre un message avec la clé privée RSA
     */
    public static String decrypt(String ciphertext, PrivateKey privateKey) throws Exception {
        byte[] encrypted = Base64.getDecoder().decode(ciphertext);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }
    
    /**
     * Vérifie si les clés sont compatibles
     */
    public static boolean canEncryptDecrypt(String message, PublicKey publicKey, PrivateKey privateKey) {
        try {
            String encrypted = encrypt(message, publicKey);
            String decrypted = decrypt(encrypted, privateKey);
            return message.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }


	public static PublicKey decodePublicKey(String publicKeyBase64) {
		// TODO Auto-generated method stub
		return null;
	}
}