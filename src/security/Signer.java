package security;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

public class Signer {
    
    public static String sign(String challenge, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge.getBytes("UTF-8"));
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
}