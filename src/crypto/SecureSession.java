package crypto;

import javax.crypto.SecretKey;
import java.util.UUID;

public class SecureSession {
    
    private final String sessionId;
    private final SecretKey aesKey;
    private final long createdAt;
    private long lastActivity;
    private boolean isActive;
    
    public SecureSession() throws Exception {
        this.sessionId = UUID.randomUUID().toString();
        this.aesKey = AESUtil.generateKey();
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = createdAt;
        this.isActive = true;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public SecretKey getAesKey() {
        return aesKey;
    }
    
    public String getAesKeyBase64() {
        return AESUtil.encodeKey(aesKey);
    }
    
    public void refresh() {
        this.lastActivity = System.currentTimeMillis();
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - lastActivity > 3600000; // 1 heure
    }
    
    public void invalidate() {
        this.isActive = false;
    }
    
    public boolean isActive() {
        return isActive && !isExpired();
    }
    
    public String encrypt(String plaintext) throws Exception {
        return AESUtil.encrypt(plaintext, aesKey);
    }
    
    public String decrypt(String ciphertext) throws Exception {
        return AESUtil.decrypt(ciphertext, aesKey);
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getLastActivity() {
        return lastActivity;
    }
    
    @Override
    public String toString() {
        return "SecureSession{" +
                "sessionId='" + sessionId + '\'' +
                ", createdAt=" + createdAt +
                ", isActive=" + isActive +
                '}';
    }
}