package security;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class Verifier {
    
    public static boolean verify(String challenge, String signatureBase64, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challenge.getBytes("UTF-8"));
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(signatureBytes);
    }
}