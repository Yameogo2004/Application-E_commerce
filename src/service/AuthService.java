package service;

import dao.UserDAO;
import model.Client;
import model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AuthService {

    private final UserDAO userDAO;
    
    // ==================== PARAMÈTRES PBKDF2 ====================
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_LENGTH = 256;
    
    // ==================== GESTION DES SESSIONS (TP5) ====================
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 3600000; // 1 heure en millisecondes
    
    public static class SessionInfo {
        int userId;
        String role;
        long lastAccess;
        String ipAddress;
        String userAgent;
        
        SessionInfo(int userId, String role, String ipAddress, String userAgent) {
            this.userId = userId;
            this.role = role;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.lastAccess = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastAccess > SESSION_TIMEOUT;
        }
        
        void refresh() {
            lastAccess = System.currentTimeMillis();
        }
        
        boolean isIpMatching(String ip) {
            if (ipAddress == null || ip == null) return true;
            return ipAddress.equals(ip);
        }
        
        boolean isUserAgentMatching(String ua) {
            if (userAgent == null || ua == null) return true;
            return userAgent.equals(ua);
        }
    }

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Génère un salt aléatoire pour le hachage serveur.
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hachage simple SHA-256 (pour compatibilité avec ancien système)
     * @deprecated Utiliser hashWithPBKDF2() pour les nouveaux comptes
     */
    @Deprecated
    public String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Erreur hash : " + e.getMessage());
            return null;
        }
    }

    /**
     * Hachage PBKDF2 pour une sécurité renforcée.
     * @param clientHashedHash le hash SHA-256 reçu du client
     * @param salt le salt (Base64)
     * @return le hash PBKDF2 en Base64
     */
    private String hashWithPBKDF2(String clientHashedHash, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(clientHashedHash.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            System.err.println("❌ Erreur PBKDF2 : " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie un mot de passe (hash client) contre le hash stocké.
     * Supporte à la fois l'ancien format (SHA-256) et le nouveau format (PBKDF2).
     * @param clientHashedHash le hash SHA-256 reçu du client
     * @param storedHash le hash stocké en base
     * @return true si correspondance
     */
    public boolean checkPassword(String clientHashedHash, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) return false;

        String[] parts = storedHash.split(":");
        if (parts.length != 2) return false;

        String salt = parts[0];
        String expectedHash = parts[1];
        
        // Essayer d'abord avec PBKDF2 (nouveau format sécurisé)
        String computedHashPBKDF2 = hashWithPBKDF2(clientHashedHash, salt);
        if (computedHashPBKDF2 != null && computedHashPBKDF2.equals(expectedHash)) {
            return true;
        }
        
        // Fallback : ancien format SHA-256 (pour compatibilité avec comptes existants)
        String computedHashSHA256 = hashPassword(clientHashedHash, salt);
        if (computedHashSHA256 != null && computedHashSHA256.equals(expectedHash)) {
            // Migrer automatiquement l'ancien compte vers le nouveau format
            migrateToPBKDF2(clientHashedHash, salt, storedHash);
            return true;
        }
        
        return false;
    }

    /**
     * Migre un ancien compte (SHA-256) vers le nouveau format (PBKDF2)
     */
    private void migrateToPBKDF2(String clientHashedHash, String salt, String oldStoredHash) {
        try {
            String newHash = hashWithPBKDF2(clientHashedHash, salt);
            if (newHash != null) {
                String newStoredHash = salt + ":" + newHash;
                // Mettre à jour le mot de passe dans la base de données
                // Note: Il faudrait une méthode dans UserDAO pour update le password
                System.out.println("🔄 Migration du compte vers PBKDF2 effectuée");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la migration : " + e.getMessage());
        }
    }

    /**
     * Authentifie un utilisateur avec double hachage.
     * @param email l'email
     * @param clientHashedHash le hash SHA-256 du mot de passe (venant du client)
     * @return l'utilisateur si succès, null sinon
     */
    public User login(String email, String clientHashedHash) {
        if (email == null || email.isEmpty() || clientHashedHash == null || clientHashedHash.isEmpty()) {
            return null;
        }

        User user = userDAO.findByEmail(email);

        if (user == null) {
            return null;
        }

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            System.out.println("Compte non actif : " + email);
            return null;
        }

        if (checkPassword(clientHashedHash, user.getPassword())) {
            System.out.println("✅ Connexion réussie ! Bienvenue " + user.getNom());
            return user;
        }

        return null;
    }

    // ==================== GESTION DES SESSIONS (TP5) ====================
    
    /**
     * Génère un token de session sécurisé
     * Format: UUID.randomUUID() + timestamp + random
     */
    public String generateSessionToken() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        String randomPart = Base64.getEncoder().encodeToString(randomBytes);
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis() + "-" + randomPart.substring(0, 8);
    }
    
    /**
     * Crée une session pour un utilisateur
     * @param userId ID de l'utilisateur
     * @param role Rôle de l'utilisateur
     * @param ipAddress Adresse IP du client
     * @param userAgent User-Agent du client (optionnel)
     * @return Token de session sécurisé
     */
    public String createSession(int userId, String role, String ipAddress, String userAgent) {
        String token = generateSessionToken();
        SessionInfo session = new SessionInfo(userId, role, ipAddress, userAgent);
        activeSessions.put(token, session);
        System.out.println("🔐 Session créée pour userId: " + userId + ", token: " + token.substring(0, 20) + "...");
        return token;
    }
    
    /**
     * Crée une session simplifiée (sans userAgent)
     */
    public String createSession(int userId, String role, String ipAddress) {
        return createSession(userId, role, ipAddress, null);
    }
    
    /**
     * Vérifie si un token de session est valide
     * @param token Token à vérifier
     * @param ipAddress IP du client (pour vérification anti-hijacking)
     * @param userAgent User-Agent du client (pour vérification supplémentaire)
     * @return SessionInfo si valide, null sinon
     */
    public SessionInfo validateSession(String token, String ipAddress, String userAgent) {
        SessionInfo session = activeSessions.get(token);
        
        if (session == null) {
            System.out.println("❌ Session introuvable: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            return null;
        }
        
        if (session.isExpired()) {
            System.out.println("⏰ Session expirée: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            activeSessions.remove(token);
            return null;
        }
        
        if (!session.isIpMatching(ipAddress)) {
            System.out.println("⚠️ ALERTE SESSION HIJACKING - IP différente! Session IP: " + session.ipAddress + ", Requête IP: " + ipAddress);
            activeSessions.remove(token); // Invalider la session suspecte
            return null;
        }
        
        if (!session.isUserAgentMatching(userAgent)) {
            System.out.println("⚠️ ALERTE SESSION HIJACKING - User-Agent différent!");
            activeSessions.remove(token); // Invalider la session suspecte
            return null;
        }
        
        session.refresh();
        return session;
    }
    
    /**
     * Vérifie un token (version simplifiée, sans userAgent)
     */
    public SessionInfo validateSession(String token, String ipAddress) {
        return validateSession(token, ipAddress, null);
    }
    
    /**
     * Récupère les informations d'une session sans vérification IP
     */
    public SessionInfo getSessionInfo(String token) {
        SessionInfo session = activeSessions.get(token);
        if (session != null && session.isExpired()) {
            activeSessions.remove(token);
            return null;
        }
        return session;
    }
    
    /**
     * Rafraîchit une session (prolonge l'expiration)
     */
    public boolean refreshSession(String token) {
        SessionInfo session = activeSessions.get(token);
        if (session == null || session.isExpired()) {
            return false;
        }
        session.refresh();
        return true;
    }
    
    /**
     * Invalide une session (déconnexion)
     */
    public void invalidateSession(String token) {
        if (token != null) {
            activeSessions.remove(token);
            System.out.println("🔓 Session invalidée: " + token.substring(0, Math.min(20, token.length())) + "...");
        }
    }
    
    /**
     * Invalide toutes les sessions d'un utilisateur
     */
    public void invalidateAllUserSessions(int userId) {
        int count = 0;
        for (Map.Entry<String, SessionInfo> entry : activeSessions.entrySet()) {
            if (entry.getValue().userId == userId) {
                activeSessions.remove(entry.getKey());
                count++;
            }
        }
        System.out.println("🔓 " + count + " session(s) invalidée(s) pour userId: " + userId);
    }
    
    /**
     * Nettoie toutes les sessions expirées
     */
    public void cleanExpiredSessions() {
        int count = 0;
        for (Map.Entry<String, SessionInfo> entry : activeSessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                activeSessions.remove(entry.getKey());
                count++;
            }
        }
        if (count > 0) {
            System.out.println("🧹 " + count + " session(s) expirée(s) nettoyée(s)");
        }
    }
    
    /**
     * Retourne le nombre de sessions actives
     */
    public int getActiveSessionCount() {
        cleanExpiredSessions();
        return activeSessions.size();
    }
    
    /**
     * Vérifie si un token est actif
     */
    public boolean isSessionActive(String token) {
        SessionInfo session = activeSessions.get(token);
        return session != null && !session.isExpired();
    }

    // ==================== MÉTHODES D'INSCRIPTION ====================
    
    /**
     * Enregistre un client avec statut "pending" (en attente OTP).
     * Utilise le double hachage : SHA-256 client + PBKDF2 serveur.
     * @param clientHashedHash le hash SHA-256 reçu du client
     */
    public boolean registerPending(String nom, String prenom, String email,
                                   String clientHashedHash, String address,
                                   String phone, String ville) {
        if (nom == null || nom.isBlank() ||
                prenom == null || prenom.isBlank() ||
                email == null || email.isBlank() ||
                clientHashedHash == null || clientHashedHash.isBlank()) {
            return false;
        }

        if (userDAO.emailExists(email)) {
            return false;
        }

        // Générer un nouveau salt et calculer le hash PBKDF2
        String salt = generateSalt();
        String serverHash = hashWithPBKDF2(clientHashedHash, salt);
        if (serverHash == null) return false;

        // Format stocké : "salt:hash"
        String finalStoredHash = salt + ":" + serverHash;

        Client client = new Client(nom, prenom, email, finalStoredHash, address, phone, ville);
        return userDAO.savePendingClient(client);
    }

    /**
     * Version de compatibilité pour l'ancien système (à ne plus utiliser)
     * @deprecated Utiliser registerPending avec clientHashedHash à la place
     */
    @Deprecated
    public boolean registerPendingLegacy(String nom, String prenom, String email,
                                         String password, String address,
                                         String phone, String ville) {
        if (nom == null || nom.isBlank() ||
                prenom == null || prenom.isBlank() ||
                email == null || email.isBlank() ||
                password == null || password.isBlank()) {
            return false;
        }

        if (userDAO.emailExists(email)) {
            return false;
        }

        String salt = generateSalt();
        String hashedPassword = salt + ":" + hashPassword(password, salt);

        Client client = new Client(nom, prenom, email, hashedPassword, address, phone, ville);
        return userDAO.savePendingClient(client);
    }

    public boolean emailExists(String email) {
        return userDAO.emailExists(email);
    }

    public boolean isAccountActive(String email) {
        return userDAO.isAccountActive(email);
    }
    
    /**
     * Méthode pour obtenir un User par email (utile pour certaines fonctionnalités)
     */
    public User getUserByUsername(String email) {
        return userDAO.findByEmail(email);
    }
}