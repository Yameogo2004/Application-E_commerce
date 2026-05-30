package Client;

public class AppSession {

    private int clientId;
    private String role;
    private String orderUUID;
    private double lastOrderTotal;
    private String sessionToken;      // 🔐 Token de session sécurisé (TP5)
    private long sessionExpiry;       // ⏰ Date d'expiration de la session
    private String ipAddress;         // 📱 IP du client (vérification anti-hijacking)
    private String userAgent;         // 🌐 Agent utilisateur (vérification supplémentaire)

    public AppSession() {
        this.sessionExpiry = 0;
    }

    // ==================== GETTERS / SETTERS EXISTANTS ====================
    
    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOrderUUID() {
        return orderUUID;
    }

    public void setOrderUUID(String orderUUID) {
        this.orderUUID = orderUUID;
    }

    public double getLastOrderTotal() {
        return lastOrderTotal;
    }

    public void setLastOrderTotal(double lastOrderTotal) {
        this.lastOrderTotal = lastOrderTotal;
    }

    public void clearOrderData() {
        this.orderUUID = null;
        this.lastOrderTotal = 0.0;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
    
    private String fullName;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getUserId() {
        return clientId;
    }

    // ==================== NOUVEAUX GETTERS / SETTERS POUR TP5 ====================
    
    /**
     * Récupère le token de session sécurisé
     */
    public String getSessionToken() {
        return sessionToken;
    }
    
    /**
     * Définit le token de session sécurisé
     */
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    /**
     * Récupère la date d'expiration de la session
     */
    public long getSessionExpiry() {
        return sessionExpiry;
    }
    
    /**
     * Définit la date d'expiration de la session (timestamp en millisecondes)
     * @param expiry Timestamp d'expiration (ex: System.currentTimeMillis() + 3600000)
     */
    public void setSessionExpiry(long expiry) {
        this.sessionExpiry = expiry;
    }
    
    /**
     * Vérifie si la session a expiré
     */
    public boolean isSessionExpired() {
        return sessionExpiry > 0 && System.currentTimeMillis() > sessionExpiry;
    }
    
    /**
     * Récupère l'adresse IP du client
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * Définit l'adresse IP du client
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Récupère le User-Agent du client
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Définit le User-Agent du client
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    // ==================== MÉTHODES UTILITAIRES ====================
    
    /**
     * Rafraîchit l'expiration de la session (prolonge de 1 heure)
     */
    public void refreshSession() {
        this.sessionExpiry = System.currentTimeMillis() + 3600000; // 1 heure
    }
    
    /**
     * Invalide la session (déconnexion)
     */
    public void invalidateSession() {
        this.sessionToken = null;
        this.sessionExpiry = 0;
        // On garde les infos utilisateur mais le token est invalide
    }
    
    /**
     * Vérifie si la session est active (token présent et non expiré)
     */
    public boolean isSessionActive() {
        return sessionToken != null && !sessionToken.isEmpty() && !isSessionExpired();
    }
    
    /**
     * Vérifie si l'adresse IP correspond à celle de la session
     * (protection contre session hijacking)
     */
    public boolean isIpMatching(String ip) {
        if (ipAddress == null || ip == null) return true;
        return ipAddress.equals(ip);
    }
    
    /**
     * Nettoie complètement la session (déconnexion totale)
     */
    public void clearSession() {
        this.clientId = 0;
        this.role = null;
        this.fullName = null;
        this.sessionToken = null;
        this.sessionExpiry = 0;
        this.ipAddress = null;
        this.userAgent = null;
        clearOrderData();
    }

    @Override
    public String toString() {
        return "AppSession{" +
                "clientId=" + clientId +
                ", role='" + role + '\'' +
                ", fullName='" + fullName + '\'' +
                ", sessionToken='" + (sessionToken != null ? sessionToken.substring(0, Math.min(20, sessionToken.length())) + "..." : "null") + '\'' +
                ", sessionExpiry=" + sessionExpiry +
                ", ipAddress='" + ipAddress + '\'' +
                ", isExpired=" + isSessionExpired() +
                '}';
    }
}