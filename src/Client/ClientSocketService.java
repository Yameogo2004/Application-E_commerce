package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import crypto.AESUtil;

public class ClientSocketService {

    private static final String SERVER_HOST = "localhost";
    private static final int SSL_PORT = 8443;
    private static final int PLAIN_PORT = 5001;

    private SSLSocket sslSocket;
    private java.net.Socket plainSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private boolean isSSL = false;

    // ==================== SESSION ====================
    private String sessionToken;
    private int currentUserId;

    // ==================== CHIFFREMENT ====================
    private String secureSessionId;
    private SecretKey aesKey;
    private boolean isSecureMode = false;
    private String clientNonce;

    private static final String KEYS_DIR = "keys/";

    // Commandes qui NE doivent PAS recevoir le token automatiquement
    private static final java.util.Set<String> NO_TOKEN_COMMANDS = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "LOGIN", "REGISTER", "SEND_OTP", "VERIFY_OTP",
            "INIT_SECURE", "GET_PRODUCTS", "GET_CATEGORIES",
            "GET_PRODUCT",  // public
            "ADMIN_AUTH_CHALLENGE", "ADMIN_AUTH_VERIFY", "ADMIN_REGISTER_PUBLIC_KEY",
            "LOGOUT"
        )
    );

    public ClientSocketService() {}

    // ==================== CONNEXION ====================

    public boolean connect() {
        try {
            if (connected && !isSSL) return true;
            close();
            plainSocket = new java.net.Socket(SERVER_HOST, PLAIN_PORT);
            in = new BufferedReader(new InputStreamReader(plainSocket.getInputStream()));
            out = new PrintWriter(plainSocket.getOutputStream(), true);
            String msg = in.readLine();
            connected = "CONNECTED_TO_SERVER".equals(msg);
            isSSL = false;
            System.out.println("✅ Connexion normale établie sur le port " + PLAIN_PORT);
            return connected;
        } catch (Exception e) {
            System.err.println("❌ Erreur connexion normale: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    public boolean connectSSL(String email, String keystorePassword, String truststorePassword) {
        try {
            if (connected && isSSL) return true;
            close();
            System.setProperty("javax.net.ssl.keyStore", KEYS_DIR + email + ".p12");
            System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStore", KEYS_DIR + "admin_truststore.p12");
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) factory.createSocket(SERVER_HOST, SSL_PORT);
            sslSocket.startHandshake();
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            out = new PrintWriter(sslSocket.getOutputStream(), true);
            connected = true;
            isSSL = true;
            System.out.println("✅ Connexion SSL établie pour " + email);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur connexion SSL: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    public boolean connectForAdmin() {
        return connect();
    }

    // ==================== ENVOI DE REQUÊTES ====================

    /**
     * Envoie une requête RAW exactement telle quelle, sans modifier le format.
     * Utilisé en interne quand le format est déjà complet.
     */
    private String sendRaw(String finalRequest) {
        try {
            if (!connected || in == null || out == null) {
                if (!connect()) return "ERROR:SERVER_UNREACHABLE";
            }

            System.out.println("📤 [SEND] " + finalRequest.substring(0, Math.min(120, finalRequest.length())));

            out.println(finalRequest);
            String response = in.readLine();

            System.out.println("📥 [RECV] " + (response != null ? response.substring(0, Math.min(150, response.length())) : "null"));

            extractSessionTokenFromResponse(response);

            return response == null ? "ERROR:NO_RESPONSE" : response;

        } catch (Exception e) {
            System.err.println("❌ Erreur sendRaw: " + e.getMessage());
            connected = false;
            return "ERROR:COMMUNICATION";
        }
    }

    /**
     * Envoie une requête en ajoutant automatiquement le token de session
     * si la commande en a besoin et qu'un token est disponible.
     *
     * Format produit : COMMANDE:TOKEN:reste_des_données
     */
    public String sendRequest(String request) {
        try {
            // Mode sécurisé (chiffrement AES)
            if (isSecureMode && aesKey != null && secureSessionId != null) {
                return sendSecureRequest(request);
            }

            // Extraire la commande (avant le premier ':')
            String command = request.contains(":") ? request.split(":", 2)[0] : request;

            // Commandes publiques : envoyer tel quel
            if (NO_TOKEN_COMMANDS.contains(command) || sessionToken == null || sessionToken.isEmpty()) {
                return sendRaw(request);
            }

            // Injecter le token : COMMANDE:TOKEN:reste
            String finalRequest;
            if (request.contains(":")) {
                String[] parts = request.split(":", 2);
                finalRequest = parts[0] + ":" + sessionToken + ":" + parts[1];
            } else {
                finalRequest = request + ":" + sessionToken;
            }

            return sendRaw(finalRequest);

        } catch (Exception e) {
            System.err.println("❌ Erreur sendRequest: " + e.getMessage());
            connected = false;
            return "ERROR:COMMUNICATION";
        }
    }

    /**
     * Envoie une requête chiffrée AES
     */
    private String sendSecureRequest(String plainRequest) throws Exception {
        String encrypted = AESUtil.encrypt(plainRequest, aesKey);
        String finalRequest = "SECURE:" + secureSessionId + ":" + encrypted;
        out.println(finalRequest);
        String response = in.readLine();
        if (response == null) return "ERROR:NO_RESPONSE";
        if (response.startsWith("SECURE_RESPONSE:")) {
            String encryptedResponse = response.substring("SECURE_RESPONSE:".length());
            return AESUtil.decrypt(encryptedResponse, aesKey);
        }
        return response;
    }

    /**
     * Extrait et stocke le token de session depuis la réponse LOGIN_SUCCESS
     */
    private void extractSessionTokenFromResponse(String response) {
        if (response == null) return;
        if (response.startsWith("LOGIN_SUCCESS:")) {
            String[] parts = response.split(":");
            if (parts.length >= 4) {
                currentUserId = Integer.parseInt(parts[1]);
                sessionToken = parts[3];
                System.out.println("🔐 Session token reçu (userId=" + currentUserId + ")");
            }
        } else if (response.startsWith("AUTH_SUCCESS:")) {
            // Auth RSA admin : AUTH_SUCCESS:userId:token
            String[] parts = response.split(":");
            if (parts.length >= 3) {
                currentUserId = Integer.parseInt(parts[1]);
                sessionToken = parts[2];
                System.out.println("🔐 Session admin RSA token reçu (userId=" + currentUserId + ")");
            }
        }
    }

    // ==================== SESSION ====================

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String token) { this.sessionToken = token; }
    public boolean hasActiveSession() { return sessionToken != null && !sessionToken.isEmpty(); }
    public int getCurrentUserId() { return currentUserId; }

    public String sendRequestWithToken(String request, String token) {
        String old = this.sessionToken;
        this.sessionToken = token;
        String response = sendRequest(request);
        this.sessionToken = old;
        return response;
    }

    public void invalidateSession() {
        if (sessionToken != null) sendRaw("LOGOUT:" + sessionToken);
        sessionToken = null;
        currentUserId = 0;
        isSecureMode = false;
        aesKey = null;
        secureSessionId = null;
        clientNonce = null;
    }

    // ==================== CHIFFREMENT PAR NONCES ====================

    private String generateNonce() {
        byte[] nonce = new byte[32];
        new SecureRandom().nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce);
    }

    private SecretKey generateKeyFromNonces(String cNonce, String sNonce) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest((cNonce + sNonce).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public void enableSecureMode() {
        try {
            if (!isSecureMode) {
                if (initSecureSession()) {
                    System.out.println("🔐 Mode sécurisé activé");
                } else {
                    System.err.println("⚠️ Échec activation mode sécurisé");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur activation mode sécurisé: " + e.getMessage());
        }
    }

    public boolean initSecureSession() throws Exception {
        String tempId = "client_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        clientNonce = generateNonce();

        // INIT_SECURE est dans NO_TOKEN_COMMANDS → envoyé sans token
        String response = sendRaw("INIT_SECURE:" + tempId + ":" + clientNonce);

        if (response != null && response.startsWith("NONCE_RESPONSE:")) {
            String[] parts = response.substring("NONCE_RESPONSE:".length()).split(":", 2);
            String serverSessionId = parts[0];
            String serverNonce = parts[1];
            this.aesKey = generateKeyFromNonces(clientNonce, serverNonce);
            this.secureSessionId = serverSessionId;
            this.isSecureMode = true;
            System.out.println("✅ Session sécurisée établie - ID: " + serverSessionId);
            return true;
        }
        return false;
    }

    public String sendSecureCommand(String command, String data) throws Exception {
        if (!isSecureMode || aesKey == null) return "ERROR:NO_SECURE_SESSION";
        return sendRequest(command + ":" + data);
    }

    public boolean isSecureMode() { return isSecureMode; }
    public String getSecureSessionId() { return secureSessionId; }

    // ==================== AUTH ====================

    public String login(String email, String password) {
        return sendRequest("LOGIN:" + safe(email) + ":" + safe(password));
    }

    public String register(String nom, String prenom, String email, String password,
                           String address, String phone, String ville) {
        return sendRequest("REGISTER:" + safe(nom) + ":" + safe(prenom) + ":" + safe(email) + ":" +
                safe(password) + ":" + safe(address) + ":" + safe(phone) + ":" + safe(ville));
    }

    public String sendOtp(String email) {
        return sendRequest("SEND_OTP:" + safe(email));
    }

    public String verifyOtp(String email, String code) {
        return sendRequest("VERIFY_OTP:" + safe(email) + ":" + safe(code));
    }

    // ==================== PRODUITS (publics) ====================

    public String getProducts() {
        return sendRaw("GET_PRODUCTS");
    }

    public String getProduct(int productId) {
        // GET_PRODUCT est public → pas de token
        return sendRaw("GET_PRODUCT:" + productId);
    }

    public String getCategories() {
        return sendRaw("GET_CATEGORIES");
    }

    // ==================== PANIER (avec token auto) ====================

    public String addToCart(int clientId, int productId, int quantity) {
        return sendRequest("CART_ADD:" + clientId + ":" + productId + ":" + quantity);
    }

    public String getCart(int clientId) {
        return sendRequest("CART_GET:" + clientId);
    }

    public String removeFromCart(int clientId, int productId) {
        return sendRequest("CART_REMOVE:" + clientId + ":" + productId);
    }

    public String removeFromCartByName(int clientId, String productName) {
        return sendRequest("CART_REMOVE_BY_NAME:" + clientId + ":" + safe(productName));
    }

    public String clearCart(int clientId) {
        return sendRequest("CART_CLEAR:" + clientId);
    }

    // ==================== CHECKOUT AVEC ANTI-REJEU ====================

    /**
     * Checkout avec nonce + timestamp anti-rejeu (méthode principale)
     * Format final envoyé : CHECKOUT:TOKEN:clientId:nonce:timestamp
     */
    public String checkout(int clientId) {
        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        // sendRequest va ajouter le token automatiquement
        // résultat : CHECKOUT:TOKEN:clientId:nonce:timestamp
        return sendRequest("CHECKOUT:" + clientId + ":" + nonce + ":" + timestamp);
    }

    /** Alias pour compatibilité */
    public String checkoutWithAntiReplay(int clientId, String nonce, long timestamp) {
        return sendRequest("CHECKOUT:" + clientId + ":" + nonce + ":" + timestamp);
    }

    // ==================== PAIEMENT AVEC ANTI-REJEU ====================

    /**
     * Paiement avec nonce + timestamp anti-rejeu (méthode principale)
     * Format final envoyé : PAYMENT:TOKEN:orderUUID:method:nonce:timestamp
     */
    public String pay(String uuid, String method) {
        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        return sendRequest("PAYMENT:" + safe(uuid) + ":" + safe(method) + ":" + nonce + ":" + timestamp);
    }

    /** Alias pour compatibilité */
    public String payWithAntiReplay(String uuid, String method, String nonce, long timestamp) {
        return sendRequest("PAYMENT:" + safe(uuid) + ":" + safe(method) + ":" + nonce + ":" + timestamp);
    }

    public String makePayment(String uuid, String method) {
        return pay(uuid, method);
    }

    // ==================== ADMIN PRODUITS ====================

    public String adminAddProduct(String name, String description, double price, int stock, String image, int categoryId) {
        return sendRequest("ADMIN_ADD_PRODUCT:" + safe(name) + ":" + safe(description) + ":" +
                price + ":" + stock + ":" + safe(image) + ":" + categoryId);
    }

    public String adminUpdateProduct(int productId, String name, String description, double price, int stock, String image, int categoryId) {
        return sendRequest("ADMIN_UPDATE_PRODUCT:" + productId + ":" + safe(name) + ":" +
                safe(description) + ":" + price + ":" + stock + ":" + safe(image) + ":" + categoryId);
    }

    public String adminDeleteProduct(int productId) {
        return sendRequest("ADMIN_DELETE_PRODUCT:" + productId);
    }

    // ==================== ADMIN CATÉGORIES ====================

    public String adminGetCategories() {
        return sendRequest("ADMIN_GET_CATEGORIES");
    }

    public String adminAddCategory(String name, String description) {
        return sendRequest("ADMIN_ADD_CATEGORY:" + safe(name) + ":" + safe(description));
    }

    public String adminUpdateCategory(int categoryId, String name, String description) {
        return sendRequest("ADMIN_UPDATE_CATEGORY:" + categoryId + ":" + safe(name) + ":" + safe(description));
    }

    public String adminDeleteCategory(int categoryId) {
        return sendRequest("ADMIN_DELETE_CATEGORY:" + categoryId);
    }

    // ==================== ADMIN USERS / ORDERS ====================

    public String adminGetUsers() {
        return sendRequest("ADMIN_GET_USERS");
    }

    public String adminGetOrders() {
        return sendRequest("ADMIN_GET_ORDERS");
    }

    public String adminUpdateOrderStatus(int orderId, String status) {
        return sendRequest("ADMIN_UPDATE_ORDER_STATUS:" + orderId + ":" + safe(status));
    }

    // ==================== PROFIL ====================

    public String getProfile(int userId) {
        return sendRequest("GET_PROFILE:" + userId);
    }

    public String updateProfile(int userId, String fullName, String email, String phone, String address, String city) {
        return sendRequest("UPDATE_PROFILE:" + userId + ":" + safe(fullName) + ":" +
                safe(email) + ":" + safe(phone) + ":" + safe(address) + ":" + safe(city));
    }

    // ==================== ADMIN DASHBOARD & STOCK ====================

    public String adminGetDashboardSummary() {
        return sendRequest("ADMIN_GET_DASHBOARD_SUMMARY");
    }

    public String adminGetNotifications() {
        return sendRequest("ADMIN_GET_NOTIFICATIONS");
    }

    public String adminMarkNotificationRead(int notificationId) {
        return sendRequest("ADMIN_MARK_NOTIFICATION_READ:" + notificationId);
    }

    public String adminGetStockAlerts() {
        return sendRequest("ADMIN_GET_STOCK_ALERTS");
    }

    public String adminGetStockHistory() {
        return sendRequest("ADMIN_GET_STOCK_HISTORY");
    }

    public String adminAdjustStock(int productId, int quantity, String movementType, String reason, int adminUserId) {
        return sendRequest("ADMIN_ADJUST_STOCK:" + productId + ":" + quantity + ":" +
                safe(movementType) + ":" + safe(reason) + ":" + adminUserId);
    }

    // ==================== ANTI-REPLAY ====================

    public String getNonce() {
        return sendRequest("ADMIN_GET_NONCE");
    }

    public String generateLocalNonce() {
        return UUID.randomUUID().toString();
    }

    public String adminSecureTest(String nonce, String message) {
        return sendRequest("ADMIN_SECURE_TEST:" + nonce + ":" + safe(message));
    }

    // ==================== RSA ADMIN AUTH ====================

    public String requestAdminChallenge(String email) {
        return sendRaw("ADMIN_AUTH_CHALLENGE:" + email);
    }

    public String verifyAdminSignature(String email, String signature) {
        return sendRaw("ADMIN_AUTH_VERIFY:" + email + ":" + signature);
    }

    public String registerAdminPublicKey(String email, String publicKeyBase64) {
        return sendRaw("ADMIN_REGISTER_PUBLIC_KEY:" + email + ":" + publicKeyBase64);
    }

    // ==================== LOGOUT ====================

    public String logout() {
        String response = sendRaw("LOGOUT:" + (sessionToken != null ? sessionToken : ""));
        sessionToken = null;
        currentUserId = 0;
        isSecureMode = false;
        aesKey = null;
        secureSessionId = null;
        clientNonce = null;
        return response;
    }

    // ==================== UTILITAIRES ====================

    private String safe(String value) {
        if (value == null) return "";
        return value.replace(":", "-").replace(";", ",").replace("|", "/");
    }

    public void close() {
        try {
            if (sessionToken != null) logout();
            connected = false;
            if (in != null) in.close();
            if (out != null) out.close();
            if (sslSocket != null && !sslSocket.isClosed()) sslSocket.close();
            if (plainSocket != null && !plainSocket.isClosed()) plainSocket.close();
        } catch (Exception ignored) {}
    }
}