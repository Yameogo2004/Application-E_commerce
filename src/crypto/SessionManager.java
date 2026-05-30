package crypto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    
    private static SessionManager instance;
    private final Map<String, SecureSession> sessions = new ConcurrentHashMap<>();
    
    private SessionManager() {}
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public SecureSession createSession() throws Exception {
        SecureSession session = new SecureSession();
        sessions.put(session.getSessionId(), session);
        cleanExpiredSessions();
        return session;
    }
    
    public SecureSession getSession(String sessionId) {
        SecureSession session = sessions.get(sessionId);
        if (session != null && session.isExpired()) {
            sessions.remove(sessionId);
            return null;
        }
        if (session != null) {
            session.refresh();
        }
        return session;
    }
    
    public void invalidateSession(String sessionId) {
        SecureSession session = sessions.remove(sessionId);
        if (session != null) {
            session.invalidate();
        }
    }
    
    private void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    public int getActiveSessionCount() {
        cleanExpiredSessions();
        return sessions.size();
    }
}