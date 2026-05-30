package server;

import dao.CategoryDAO;
import dao.OrderDAO;
import dao.UserDAO;
import model.Cart;
import model.CartItem;
import model.Category;
import model.Order;
import model.OrderItem;
import model.Payment;
import model.Product;
import model.User;
import service.AuthService;
import service.CartService;
import service.OrderService;
import service.OtpService;
import service.PaymentService;
import service.ProductService;

import java.io.*;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import security.ChallengeGenerator;
import security.Verifier;
import crypto.AESUtil;
import crypto.RSAUtil;
import crypto.SecureSession;
import crypto.SessionManager;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ClientHandler extends Thread {

    // Loggers
    private static final Logger logger = LogManager.getLogger(ClientHandler.class);
    private static final Logger paymentLogger = LogManager.getLogger("com.chrionline.payment");
    private static final Logger authLogger = LogManager.getLogger("com.chrionline.auth");
    private static final Logger adminLogger = LogManager.getLogger("com.chrionline.admin");

    // ==================== PROTECTION CONTRE FORCE BRUTE ====================
    private final Map<String, List<Long>> loginAttempts = new HashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION = 300000;

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    // ==================== PROTECTION SYN FLOOD ====================
    private final String clientIp;
    private final Runnable onCloseCallback;

    private final CartService cartService;
    private final ProductService productService;
    private OrderService orderService;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final OtpService otpService;

    // Stockage des nonces utilisés pour anti-rejeu
    private final Set<String> usedNonces = new HashSet<>();
    private static final long MAX_TIMESTAMP_DIFF = 300000; // 5 minutes

    private final Map<String, ChallengeEntry> challengeStore = new HashMap<>();

    // ==================== GESTION DES SESSIONS ====================
    private String currentSessionToken = null;

    // ==================== CHIFFREMENT HYBRIDE ====================
    private final java.util.Map<String, javax.crypto.SecretKey> secureSessions = new java.util.concurrent.ConcurrentHashMap<>();

    // Liste des commandes qui peuvent contenir un token
    private static final Set<String> COMMANDS_WITH_TOKEN = new HashSet<>(Arrays.asList(
        "CART_ADD", "CART_REMOVE", "CART_REMOVE_BY_NAME", "CART_GET", "CART_CLEAR",
        "GET_PRODUCT", "CHECKOUT", "PAYMENT", "GET_PROFILE", "UPDATE_PROFILE",
        "ADMIN_ADD_PRODUCT", "ADMIN_UPDATE_PRODUCT", "ADMIN_DELETE_PRODUCT",
        "ADMIN_ADD_CATEGORY", "ADMIN_UPDATE_CATEGORY", "ADMIN_DELETE_CATEGORY",
        "ADMIN_UPDATE_ORDER_STATUS", "ADMIN_MARK_NOTIFICATION_READ", "ADMIN_ADJUST_STOCK",
        "ADMIN_SECURE_TEST"
    ));

    private static class ChallengeEntry {
        String challenge;
        long expiry;
        ChallengeEntry(String challenge, long expiry) {
            this.challenge = challenge;
            this.expiry = expiry;
        }
    }

    // Constructeur pour connexion normale (avec IP et callback)
    public ClientHandler(Socket socket, String clientIp, Runnable onCloseCallback) {
        this.clientSocket = socket;
        this.clientIp = clientIp;
        this.onCloseCallback = onCloseCallback;
        this.cartService = new CartService();
        this.productService = new ProductService();
        this.paymentService = new PaymentService();
        this.authService = new AuthService();
        this.otpService = new OtpService();

        try {
            this.orderService = new OrderService();
        } catch (Exception e) {
            logger.error("Impossible d'initialiser OrderService", e);
            this.orderService = null;
        }

        try {
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            logger.error("Erreur initialisation ClientHandler : {}", e.getMessage());
        }
    }

    // Constructeur pour compatibilité (sans IP et callback)
    public ClientHandler(Socket socket) {
        this(socket, socket.getInetAddress().getHostAddress(), null);
    }

    @Override
    public void run() {
        try {
            out.println("CONNECTED_TO_SERVER");
            logger.info("Client connecté : {}", clientIp);

            String request;
            while ((request = in.readLine()) != null) {
                logger.debug("Requête reçue : {}", request);
                out.println(handleRequest(request));
            }

        } catch (IOException e) {
            logger.warn("Client déconnecté : {}", clientIp);
        } finally {
            closeResources();
        }
    }

    /**
     * Extrait la commande et le token d'une requête
     */
    private String[] extractCommandAndToken(String request) {
        int firstColon = request.indexOf(':');
        if (firstColon == -1) {
            return new String[]{request, null, null};
        }

        String potentialCommand = request.substring(0, firstColon);
        String rest = request.substring(firstColon + 1);

        // Commandes sans token
        if (potentialCommand.equals("LOGIN") || potentialCommand.equals("REGISTER") ||
            potentialCommand.equals("SEND_OTP") || potentialCommand.equals("VERIFY_OTP") ||
            potentialCommand.equals("INIT_SECURE") ||
            potentialCommand.equals("SECURE") || potentialCommand.equals("LOGOUT") ||
            potentialCommand.equals("GET_PRODUCTS") || potentialCommand.equals("GET_CATEGORIES") ||
            potentialCommand.equals("ADMIN_GET_CATEGORIES") || potentialCommand.equals("ADMIN_GET_USERS") ||
            potentialCommand.equals("ADMIN_GET_ORDERS") || potentialCommand.equals("ADMIN_GET_DASHBOARD_SUMMARY") ||
            potentialCommand.equals("ADMIN_GET_NOTIFICATIONS") || potentialCommand.equals("ADMIN_GET_STOCK_ALERTS") ||
            potentialCommand.equals("ADMIN_GET_STOCK_HISTORY") || potentialCommand.equals("ADMIN_GET_NONCE") ||
            potentialCommand.startsWith("ADMIN_AUTH_CHALLENGE") || potentialCommand.startsWith("ADMIN_AUTH_VERIFY") ||
            potentialCommand.startsWith("ADMIN_REGISTER_PUBLIC_KEY")) {
            return new String[]{potentialCommand, null, rest};
        }

        // Commandes avec token
        if (COMMANDS_WITH_TOKEN.contains(potentialCommand)) {
            int secondColon = rest.indexOf(':');
            if (secondColon != -1) {
                String potentialToken = rest.substring(0, secondColon);
                // Vérifier si c'est un vrai token (contient '-' et longueur > 20)
                if (potentialToken.contains("-") && potentialToken.length() > 20) {
                    String token = potentialToken;
                    String data = rest.substring(secondColon + 1);
                    return new String[]{potentialCommand, token, data};
                } else {
                    // Pas de token, le reste est directement les données
                    return new String[]{potentialCommand, null, rest};
                }
            } else {
                return new String[]{potentialCommand, rest, null};
            }
        }

        return null;
    }

    private String handleRequest(String request) {
        logger.info("📨 REQUÊTE REÇUE: {}", request);
        try {
            if (request == null || request.trim().isEmpty()) {
                return "ERROR:EMPTY_REQUEST";
            }

            if (request.equalsIgnoreCase("PING")) return "PONG";

            String[] extracted = extractCommandAndToken(request);
            String command;
            String token;
            String data;

            if (extracted != null) {
                command = extracted[0];
                token = extracted[1];
                data = extracted[2];
                logger.debug("Commande: {}, Token: {}, Data: {}", command,
                    token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null",
                    data != null ? data.substring(0, Math.min(50, data.length())) : "null");
            } else {
                command = request;
                token = null;
                data = null;
            }

            // ==================== CHIFFREMENT PAR NONCES ====================
            if (command.equalsIgnoreCase("INIT_SECURE")) {
                return handleInitSecure(request);
            }
            if (command.equalsIgnoreCase("SECURE")) {
                return handleSecureRequest(request);
            }

            // ==================== LOGOUT ====================
            if (command.equalsIgnoreCase("LOGOUT")) {
                return handleLogout(request);
            }

            if (command.equalsIgnoreCase("LOGIN")) return handleLogin(request);
            if (command.equalsIgnoreCase("REGISTER")) return handleRegister(request);
            if (command.equalsIgnoreCase("SEND_OTP")) return handleSendOtp(request);
            if (command.equalsIgnoreCase("VERIFY_OTP")) return handleVerifyOtp(request);

            // ==================== COMMANDES PUBLIQUES (sans session) ====================
            if (command.equalsIgnoreCase("GET_PRODUCTS")) return handleGetProducts();
            if (command.equalsIgnoreCase("GET_CATEGORIES")) return handleGetCategories();

            // GET_PRODUCT est public (consultation catalogue)
            if (command.equalsIgnoreCase("GET_PRODUCT")) {
                return handleGetProduct(request);
            }

            // ==================== VÉRIFICATION DE SESSION ====================
            if (!validateRequestSession(request)) {
                logger.warn("⚠️ Session invalide pour la requête: {}",
                    request.substring(0, Math.min(80, request.length())));
                return "ERROR:INVALID_SESSION";
            }

            // ==================== PANIER ====================
            if (command.equalsIgnoreCase("CART_ADD"))          return handleCartAdd(request);
            if (command.equalsIgnoreCase("CART_REMOVE"))       return handleCartRemove(request);
            if (command.equalsIgnoreCase("CART_REMOVE_BY_NAME")) return handleCartRemoveByName(request);
            if (command.equalsIgnoreCase("CART_GET"))          return handleCartGet(request);
            if (command.equalsIgnoreCase("CART_CLEAR"))        return handleCartClear(request);

            // ==================== COMMANDES / PAIEMENT ====================
            if (command.equalsIgnoreCase("CHECKOUT")) return handleCheckout(request);
            if (command.equalsIgnoreCase("PAYMENT"))  return handlePayment(request);

            // ==================== PROFIL ====================
            if (command.equalsIgnoreCase("GET_PROFILE"))    return handleGetProfile(request);
            if (command.equalsIgnoreCase("UPDATE_PROFILE")) return handleUpdateProfile(request);

            // ==================== ADMIN PRODUITS ====================
            if (command.equalsIgnoreCase("ADMIN_ADD_PRODUCT"))    return handleAdminAddProduct(request);
            if (command.equalsIgnoreCase("ADMIN_UPDATE_PRODUCT")) return handleAdminUpdateProduct(request);
            if (command.equalsIgnoreCase("ADMIN_DELETE_PRODUCT")) return handleAdminDeleteProduct(request);

            // ==================== ADMIN CATEGORIES ====================
            if (command.equalsIgnoreCase("ADMIN_GET_CATEGORIES"))    return handleAdminGetCategories();
            if (command.equalsIgnoreCase("ADMIN_ADD_CATEGORY"))      return handleAdminAddCategory(request);
            if (command.equalsIgnoreCase("ADMIN_UPDATE_CATEGORY"))   return handleAdminUpdateCategory(request);
            if (command.equalsIgnoreCase("ADMIN_DELETE_CATEGORY"))   return handleAdminDeleteCategory(request);

            // ==================== ADMIN USERS / ORDERS ====================
            if (command.equalsIgnoreCase("ADMIN_GET_USERS"))          return handleAdminGetUsers();
            if (command.equalsIgnoreCase("ADMIN_GET_ORDERS"))         return handleAdminGetOrders();
            if (command.equalsIgnoreCase("ADMIN_UPDATE_ORDER_STATUS")) return handleAdminUpdateOrderStatus(request);

            // ==================== ADMIN RSA ====================
            if (command.equalsIgnoreCase("ADMIN_AUTH_CHALLENGE"))      return handleAdminAuthChallenge(request);
            if (command.equalsIgnoreCase("ADMIN_AUTH_VERIFY"))         return handleAdminAuthVerify(request);
            if (command.equalsIgnoreCase("ADMIN_REGISTER_PUBLIC_KEY")) return handleAdminRegisterPublicKey(request);

            // ==================== ADMIN DASHBOARD ====================
            if (command.equalsIgnoreCase("ADMIN_GET_DASHBOARD_SUMMARY"))   return handleAdminGetDashboardSummary();
            if (command.equalsIgnoreCase("ADMIN_GET_NOTIFICATIONS"))        return handleAdminGetNotifications();
            if (command.equalsIgnoreCase("ADMIN_MARK_NOTIFICATION_READ"))   return handleAdminMarkNotificationRead(request);
            if (command.equalsIgnoreCase("ADMIN_GET_STOCK_ALERTS"))         return handleAdminGetStockAlerts();
            if (command.equalsIgnoreCase("ADMIN_GET_STOCK_HISTORY"))        return handleAdminGetStockHistory();
            if (command.equalsIgnoreCase("ADMIN_ADJUST_STOCK"))             return handleAdminAdjustStock(request);

            // ==================== ANTI-REPLAY ====================
            if (command.equalsIgnoreCase("ADMIN_GET_NONCE"))   return handleAdminGetNonce();
            if (command.equalsIgnoreCase("ADMIN_SECURE_TEST")) return handleAdminSecureTest(request);

            logger.warn("Commande inconnue : {}", request);
            return "ERROR:UNKNOWN_COMMAND";

        } catch (Exception e) {
            logger.error("Exception lors du traitement de la requête : {}", request, e);
            return "ERROR:EXCEPTION_OCCURRED";
        }
    }

    // ==================== CHIFFREMENT PAR NONCES ====================

    private String generateNonce() {
        byte[] nonce = new byte[32];
        new SecureRandom().nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce);
    }

    private SecretKey generateKeyFromNonces(String clientNonce, String serverNonce) throws Exception {
        String combined = clientNonce + serverNonce;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String handleInitSecure(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:INIT_FORMAT";

            String clientSessionId = parts[1];
            String clientNonce = parts[2];
            String serverNonce = generateNonce();

            SecretKey sharedKey = generateKeyFromNonces(clientNonce, serverNonce);
            secureSessions.put(clientSessionId, sharedKey);

            logger.info("🔐 Session sécurisée créée - ID: {}", clientSessionId);
            return "NONCE_RESPONSE:" + clientSessionId + ":" + serverNonce;
        } catch (Exception e) {
            logger.error("Erreur init secure", e);
            return "ERROR:INIT_SECURE_FAILED";
        }
    }

    private String handleSecureRequest(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:SECURE_FORMAT";

            String sessionId = parts[1];
            String encryptedData = parts[2];

            SecretKey aesKey = secureSessions.get(sessionId);
            if (aesKey == null) {
                logger.warn("Session sécurisée invalide: {}", sessionId);
                return "ERROR:INVALID_SECURE_SESSION";
            }

            String decryptedRequest = AESUtil.decrypt(encryptedData, aesKey);
            logger.debug("Requête déchiffrée: {}", decryptedRequest);

            String response = processSecureCommand(decryptedRequest, aesKey);
            String encryptedResponse = AESUtil.encrypt(response, aesKey);
            return "SECURE_RESPONSE:" + encryptedResponse;

        } catch (Exception e) {
            logger.error("Erreur requête sécurisée", e);
            return "ERROR:SECURE_REQUEST_FAILED";
        }
    }

    private String processSecureCommand(String command, SecretKey aesKey) {
        try {
            logger.info("🔧 Traitement commande sécurisée: {}", command);

            // Les commandes sécurisées arrivent SANS token (déjà authentifiées par la session AES)
            // On les passe directement aux handlers qui prennent le dernier élément

            if (command.startsWith("LOGIN:")) {
                String[] parts = command.split(":", 3);
                if (parts.length >= 3) return handleLoginSecure(parts[1], parts[2]);

            } else if (command.startsWith("GET_PROFILE:"))        return handleGetProfile(command);
            else if (command.startsWith("UPDATE_PROFILE:"))       return handleUpdateProfile(command);

            else if (command.startsWith("CHECKOUT:"))             return handleCheckout(command);
            else if (command.startsWith("PAYMENT:"))              return handlePayment(command);

            else if (command.startsWith("GET_PRODUCTS"))          return handleGetProducts();
            else if (command.startsWith("GET_CATEGORIES"))        return handleGetCategories();
            else if (command.startsWith("GET_PRODUCT:"))          return handleGetProduct(command);

            else if (command.startsWith("CART_GET:"))             return handleCartGet(command);
            else if (command.startsWith("CART_ADD:"))             return handleCartAdd(command);
            else if (command.startsWith("CART_REMOVE_BY_NAME:"))  return handleCartRemoveByName(command);
            else if (command.startsWith("CART_REMOVE:"))          return handleCartRemove(command);
            else if (command.startsWith("CART_CLEAR:"))           return handleCartClear(command);

            else if (command.startsWith("ADMIN_ADD_PRODUCT:"))        return handleAdminAddProduct(command);
            else if (command.startsWith("ADMIN_UPDATE_PRODUCT:"))     return handleAdminUpdateProduct(command);
            else if (command.startsWith("ADMIN_DELETE_PRODUCT:"))     return handleAdminDeleteProduct(command);
            else if (command.startsWith("ADMIN_GET_CATEGORIES"))      return handleAdminGetCategories();
            else if (command.startsWith("ADMIN_ADD_CATEGORY:"))       return handleAdminAddCategory(command);
            else if (command.startsWith("ADMIN_UPDATE_CATEGORY:"))    return handleAdminUpdateCategory(command);
            else if (command.startsWith("ADMIN_DELETE_CATEGORY:"))    return handleAdminDeleteCategory(command);
            else if (command.startsWith("ADMIN_GET_USERS"))           return handleAdminGetUsers();
            else if (command.startsWith("ADMIN_GET_ORDERS"))          return handleAdminGetOrders();
            else if (command.startsWith("ADMIN_UPDATE_ORDER_STATUS:"))return handleAdminUpdateOrderStatus(command);
            else if (command.startsWith("ADMIN_GET_DASHBOARD_SUMMARY"))return handleAdminGetDashboardSummary();
            else if (command.startsWith("ADMIN_GET_NOTIFICATIONS"))   return handleAdminGetNotifications();
            else if (command.startsWith("ADMIN_MARK_NOTIFICATION_READ:"))return handleAdminMarkNotificationRead(command);
            else if (command.startsWith("ADMIN_GET_STOCK_ALERTS"))    return handleAdminGetStockAlerts();
            else if (command.startsWith("ADMIN_GET_STOCK_HISTORY"))   return handleAdminGetStockHistory();
            else if (command.startsWith("ADMIN_ADJUST_STOCK:"))       return handleAdminAdjustStock(command);
            else if (command.startsWith("ADMIN_SECURE_TEST:"))        return handleAdminSecureTest(command);
            else if (command.startsWith("ADMIN_GET_NONCE"))           return handleAdminGetNonce();

            logger.warn("Commande sécurisée inconnue: {}", command);
            return "ERROR:UNKNOWN_SECURE_COMMAND:" + command;
        } catch (Exception e) {
            logger.error("Erreur traitement commande sécurisée", e);
            return "ERROR:SECURE_COMMAND_FAILED";
        }
    }

    private String handleLoginSecure(String email, String password) {
        try {
            if (isBlocked(email, clientIp)) return "ERROR:TOO_MANY_ATTEMPTS";
            User user = authService.login(email, password);

            if (user != null) {
                loginAttempts.remove(email + ":" + clientIp);
                String sessionToken = authService.createSession(user.getId(), user.getRole(), clientIp);
                authLogger.info("✅ Connexion sécurisée réussie - email: {}", email);
                return "LOGIN_SUCCESS:" + user.getId() + ":" + user.getRole() + ":" + sessionToken;
            }

            if (authService.emailExists(email) && !authService.isAccountActive(email)) {
                return "ERROR:ACCOUNT_NOT_ACTIVE";
            }

            recordFailedAttempt(email, clientIp);
            int remainingAttempts = MAX_ATTEMPTS - getAttemptCount(email, clientIp);
            return "ERROR:LOGIN_FAILED:" + remainingAttempts;
        } catch (Exception e) {
            return "ERROR:LOGIN_EXCEPTION";
        }
    }

    // ==================== GESTION DES SESSIONS ====================

    private String extractSessionToken(String request) {
        String[] parts = request.split(":");
        if (parts.length >= 2) {
            String potentialToken = parts[1];
            if (potentialToken.contains("-") && potentialToken.length() > 20) {
                return potentialToken;
            }
        }
        return null;
    }

    private boolean validateRequestSession(String request) {
        // Endpoints publics sans session
        if (request.startsWith("LOGIN:") || request.startsWith("REGISTER:") ||
            request.startsWith("SEND_OTP:") || request.startsWith("VERIFY_OTP:") ||
            request.startsWith("INIT_SECURE:") ||
            request.startsWith("ADMIN_AUTH_CHALLENGE:") || request.startsWith("ADMIN_AUTH_VERIFY:") ||
            request.startsWith("ADMIN_REGISTER_PUBLIC_KEY:") ||
            request.equalsIgnoreCase("GET_PRODUCTS") ||
            request.equalsIgnoreCase("GET_CATEGORIES") ||
            request.startsWith("GET_PRODUCT:")) {
            return true;
        }

        String token = extractSessionToken(request);
        if (token == null) {
            logger.warn("⚠️ Requête sans token de session: {}",
                request.substring(0, Math.min(50, request.length())));
            return false;
        }

        AuthService.SessionInfo sessionInfo = authService.validateSession(token, clientIp);
        if (sessionInfo == null) {
            logger.warn("⚠️ Session invalide ou expirée pour token: {}...",
                token.substring(0, Math.min(20, token.length())));
            return false;
        }

        currentSessionToken = token;
        return true;
    }

    private String handleLogout(String request) {
        String[] parts = request.split(":");
        if (parts.length >= 2) {
            String token = parts[1];
            authService.invalidateSession(token);
            authLogger.info("🔓 Déconnexion - IP: {}", clientIp);
        }
        return "LOGOUT_SUCCESS";
    }

    // ==================== PROTECTION CONTRE FORCE BRUTE ====================

    private boolean isBlocked(String email, String ip) {
        String key = email + ":" + ip;
        List<Long> attempts = loginAttempts.get(key);
        if (attempts == null) return false;
        long now = System.currentTimeMillis();
        attempts.removeIf(time -> (now - time) > BLOCK_DURATION);
        return attempts.size() >= MAX_ATTEMPTS;
    }

    private void recordFailedAttempt(String email, String ip) {
        String key = email + ":" + ip;
        loginAttempts.computeIfAbsent(key, k -> new ArrayList<>()).add(System.currentTimeMillis());
    }

    private int getAttemptCount(String email, String ip) {
        String key = email + ":" + ip;
        List<Long> attempts = loginAttempts.get(key);
        if (attempts == null) return 0;
        long now = System.currentTimeMillis();
        attempts.removeIf(time -> (now - time) > BLOCK_DURATION);
        return attempts.size();
    }

    // ==================== GESTION DES CHALLENGES RSA ====================

    private void storeChallenge(String email, String challenge) {
        challengeStore.put(email, new ChallengeEntry(challenge, System.currentTimeMillis() + 300000));
    }

    private String getStoredChallenge(String email) {
        ChallengeEntry entry = challengeStore.get(email);
        if (entry == null || entry.expiry < System.currentTimeMillis()) {
            challengeStore.remove(email);
            return null;
        }
        return entry.challenge;
    }

    private void clearChallenge(String email) {
        challengeStore.remove(email);
    }

    // ==================== AUTHENTIFICATION RSA (admin) ====================

    private String handleAdminAuthChallenge(String request) {
        logger.info("📨 ADMIN_AUTH_CHALLENGE reçu: {}", request);
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:CHALLENGE_FORMAT";

            String adminEmail = parts[1];
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(adminEmail);

            if (user == null) return "ERROR:USER_NOT_FOUND";
            if (!"admin".equalsIgnoreCase(user.getRole())) return "ERROR:NOT_ADMIN";
            if (user.getPublicKey() == null || user.getPublicKey().isEmpty()) return "ERROR:NO_PUBLIC_KEY";

            String challenge = ChallengeGenerator.generateChallenge();
            storeChallenge(adminEmail, challenge);
            logger.info("✅ Challenge généré pour {}", adminEmail);
            return "CHALLENGE:" + challenge;

        } catch (Exception e) {
            logger.error("Erreur dans handleAdminAuthChallenge", e);
            return "ERROR:CHALLENGE_FAILED";
        }
    }

    private String handleAdminAuthVerify(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:VERIFY_FORMAT";

            String adminEmail = parts[1];
            String signatureBase64 = parts[2];
            String challenge = getStoredChallenge(adminEmail);
            if (challenge == null) return "ERROR:NO_CHALLENGE_OR_EXPIRED";

            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(adminEmail);
            if (user == null) return "ERROR:USER_NOT_FOUND";

            String publicKeyBase64 = user.getPublicKey();
            if (publicKeyBase64 == null || publicKeyBase64.isEmpty()) return "ERROR:PUBLIC_KEY_NOT_FOUND";

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
            boolean valid = Verifier.verify(challenge, signatureBase64, publicKey);

            if (valid) {
                clearChallenge(adminEmail);
                String sessionToken = authService.createSession(user.getId(), user.getRole(), clientIp);
                authLogger.info("✅ Session créée pour admin RSA - userId: {}", user.getId());
                return "AUTH_SUCCESS:" + user.getId() + ":" + sessionToken;
            } else {
                return "ERROR:INVALID_SIGNATURE";
            }
        } catch (Exception e) {
            logger.error("Erreur dans handleAdminAuthVerify", e);
            return "ERROR:VERIFY_FAILED";
        }
    }

    private String handleAdminRegisterPublicKey(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:REGISTER_FORMAT";

            String adminEmail = parts[1];
            String publicKeyBase64 = parts[2];
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(adminEmail);
            if (user == null) return "ERROR:USER_NOT_FOUND";
            if (!"admin".equalsIgnoreCase(user.getRole())) return "ERROR:NOT_ADMIN";

            boolean saved = userDAO.savePublicKey(user.getId(), publicKeyBase64);
            return saved ? "PUBLIC_KEY_REGISTERED" : "ERROR:REGISTER_FAILED";
        } catch (Exception e) {
            return "ERROR:REGISTER_FAILED";
        }
    }

    // ==================== LOGIN ====================

    private String handleLogin(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 3) return "ERROR:LOGIN_FORMAT";

            String email = parts[1];
            String password = parts[2];

            if (isBlocked(email, clientIp)) return "ERROR:TOO_MANY_ATTEMPTS";
            User user = authService.login(email, password);

            if (user != null) {
                loginAttempts.remove(email + ":" + clientIp);
                String sessionToken = authService.createSession(user.getId(), user.getRole(), clientIp);
                authLogger.info("✅ Connexion réussie - email: {}, rôle: {}", email, user.getRole());
                return "LOGIN_SUCCESS:" + user.getId() + ":" + user.getRole() + ":" + sessionToken;
            }

            if (authService.emailExists(email) && !authService.isAccountActive(email)) {
                return "ERROR:ACCOUNT_NOT_ACTIVE";
            }

            recordFailedAttempt(email, clientIp);
            int remainingAttempts = MAX_ATTEMPTS - getAttemptCount(email, clientIp);
            return "ERROR:LOGIN_FAILED:" + remainingAttempts;
        } catch (Exception e) {
            return "ERROR:LOGIN_EXCEPTION";
        }
    }

    // ==================== ADMIN DASHBOARD ====================

    private String handleAdminGetDashboardSummary() {
        try {
            List<Product> allProducts = productService.getAllProducts();
            int totalProducts = allProducts.size();
            int lowStock = (int) allProducts.stream().filter(p -> p.getStock() <= 5 && p.getStock() > 0).count();
            int outOfStock = (int) allProducts.stream().filter(p -> p.getStock() == 0).count();

            List<User> allUsers = new UserDAO().findAll();
            int totalUsers = allUsers.size();

            List<Order> allOrders = new OrderDAO().findAll();
            int totalOrders = allOrders.size();
            int pendingOrders = (int) allOrders.stream().filter(o -> "pending".equalsIgnoreCase(o.getStatus())).count();
            int paidOrders = (int) allOrders.stream().filter(o -> "paid".equalsIgnoreCase(o.getStatus())).count();

            double todayRevenue = allOrders.stream()
                .filter(o -> "paid".equalsIgnoreCase(o.getStatus()))
                .filter(o -> o.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .mapToDouble(Order::getTotalPrice).sum();

            double monthRevenue = allOrders.stream()
                .filter(o -> "paid".equalsIgnoreCase(o.getStatus()))
                .filter(o -> o.getCreatedAt().getMonth() == LocalDate.now().getMonth()
                          && o.getCreatedAt().getYear() == LocalDate.now().getYear())
                .mapToDouble(Order::getTotalPrice).sum();

            return String.format("DASHBOARD_SUMMARY:%d;%d;%d;%d;%d;%d;%d;%.2f;%.2f;0",
                totalProducts, lowStock, outOfStock, totalUsers,
                totalOrders, pendingOrders, paidOrders, todayRevenue, monthRevenue);
        } catch (Exception e) {
            logger.error("Erreur dashboard summary", e);
            return "DASHBOARD_SUMMARY:0;0;0;0;0;0;0;0;0;0";
        }
    }

    private String handleAdminGetNotifications() {
        try {
            List<String> notifications = new ArrayList<>();
            String sql = "SELECT * FROM notifications ORDER BY created_at DESC LIMIT 20";
            try (PreparedStatement ps = database.DatabaseConnection.getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(String.format("%d;%s;%s;%s;%s;%b;%s;%d;%s",
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getString("level"),
                        rs.getBoolean("is_read"),
                        rs.getString("entity_type"),
                        rs.getInt("entity_id"),
                        rs.getTimestamp("created_at").toString()
                    ));
                }
            }
            if (notifications.isEmpty()) return "NO_NOTIFICATIONS";
            return String.join("|", notifications);
        } catch (Exception e) {
            logger.error("Erreur notifications", e);
            return "NO_NOTIFICATIONS";
        }
    }

    private String handleAdminMarkNotificationRead(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 2) return "ERROR:ADMIN_MARK_NOTIFICATION_READ_FORMAT";
            int notificationId = Integer.parseInt(parts[parts.length - 1]);
            String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
            try (PreparedStatement ps = database.DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, notificationId);
                ps.executeUpdate();
            }
            return "ADMIN_MARK_NOTIFICATION_READ_SUCCESS";
        } catch (Exception e) {
            logger.error("Erreur mark notification read", e);
            return "ERROR:ADMIN_MARK_NOTIFICATION_READ_EXCEPTION";
        }
    }

    private String handleAdminGetStockAlerts() {
        try {
            List<Product> products = productService.getAllProducts();
            List<String> alerts = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Product p : products) {
                if (p.getStock() <= 5) {
                    String level = p.getStock() == 0 ? "CRITIQUE" : (p.getStock() <= 2 ? "ÉLEVÉ" : "MOYEN");
                    String status = p.getStock() == 0 ? "en rupture" : "stock faible";
                    alerts.add(String.format("%d;%s;%d;5;%s;%s;%s",
                        p.getIdProduct(), p.getName(), p.getStock(), level, status,
                        LocalDateTime.now().format(formatter)));
                }
            }
            if (alerts.isEmpty()) return "NO_STOCK_ALERTS";
            return String.join("|", alerts);
        } catch (Exception e) {
            logger.error("Erreur stock alerts", e);
            return "NO_STOCK_ALERTS";
        }
    }

    private String handleAdminGetStockHistory() {
        try {
            List<String> movements = new ArrayList<>();
            String sql = "SELECT * FROM stock_movement ORDER BY created_at DESC LIMIT 100";
            try (PreparedStatement ps = database.DatabaseConnection.getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movements.add(String.format("%d;%d;%s;%s;%d;%d;%d;%s;%s;%s",
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("movement_type"),
                        rs.getInt("quantity"),
                        rs.getInt("previous_stock"),
                        rs.getInt("new_stock"),
                        rs.getString("reason"),
                        rs.getString("admin_user_id"),
                        rs.getTimestamp("created_at").toString()
                    ));
                }
            }
            if (movements.isEmpty()) return "NO_STOCK_HISTORY";
            return String.join("|", movements);
        } catch (Exception e) {
            logger.error("Erreur stock history", e);
            return "NO_STOCK_HISTORY";
        }
    }

    private String handleAdminAdjustStock(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 6) return "ERROR:ADMIN_ADJUST_STOCK_FORMAT";

            int productId    = Integer.parseInt(parts[parts.length - 5]);
            int quantity     = Integer.parseInt(parts[parts.length - 4]);
            String movementType = parts[parts.length - 3];
            String reason    = parts[parts.length - 2];
            int adminUserId  = Integer.parseInt(parts[parts.length - 1]);

            Product product = productService.getProductById(productId);
            if (product == null) return "ERROR:PRODUCT_NOT_FOUND";

            int oldStock = product.getStock();
            int newStock = movementType.equalsIgnoreCase("ADD")
                ? oldStock + quantity
                : oldStock - quantity;

            if (newStock < 0) return "ERROR:INSUFFICIENT_STOCK";

            product.setStock(newStock);
            boolean success = productService.updateProduct(product);

            if (success) {
                addStockMovement(productId, product.getName(), movementType,
                    quantity, oldStock, newStock, reason);
                checkAndCreateStockAlert(product);
                adminLogger.info("Stock ajusté - Produit: {}, Ancien: {}, Nouveau: {}",
                    product.getName(), oldStock, newStock);
            }
            return success ? "ADMIN_ADJUST_STOCK_SUCCESS" : "ERROR:ADMIN_ADJUST_STOCK_FAILED";
        } catch (Exception e) {
            logger.error("Erreur adjust stock", e);
            return "ERROR:ADMIN_ADJUST_STOCK_EXCEPTION";
        }
    }

    // ==================== ANTI-REPLAY ====================

    private String handleAdminGetNonce() {
        try {
            return "NONCE:" + UUID.randomUUID().toString();
        } catch (Exception e) {
            return "ERROR:ADMIN_GET_NONCE_EXCEPTION";
        }
    }

    private String handleAdminSecureTest(String request) {
        try {
            String[] parts = request.split(":", 3);
            if (parts.length != 3) return "ERROR:ADMIN_SECURE_TEST_FORMAT";
            String nonce = parts[1];
            if (usedNonces.contains(nonce)) {
                logger.warn("REPLAY ATTACK DETECTED - Nonce: {}", nonce);
                return "ERROR:REPLAY_ATTACK_DETECTED";
            }
            usedNonces.add(nonce);
            return "ADMIN_SECURE_TEST_SUCCESS";
        } catch (Exception e) {
            return "ERROR:ADMIN_SECURE_TEST_EXCEPTION";
        }
    }

    private boolean isTimestampValid(long timestamp) {
        return Math.abs(System.currentTimeMillis() - timestamp) <= MAX_TIMESTAMP_DIFF;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private void addStockMovement(int productId, String productName, String movementType,
                                   int quantity, int previousStock, int newStock, String reason) {
        try {
            String sql = "INSERT INTO stock_movement (product_id, product_name, movement_type, " +
                         "quantity, previous_stock, new_stock, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = database.DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, productId);
                ps.setString(2, productName);
                ps.setString(3, movementType);
                ps.setInt(4, quantity);
                ps.setInt(5, previousStock);
                ps.setInt(6, newStock);
                ps.setString(7, reason);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Erreur ajout stock mouvement", e);
        }
    }

    private void addNotification(String title, String message, String entityType, String level, int entityId) {
        try {
            String sql = "INSERT INTO notifications (title, message, type, level, entity_type, entity_id) " +
                         "VALUES (?, ?, 'SYSTEM', ?, ?, ?)";
            try (PreparedStatement ps = database.DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, message);
                ps.setString(3, level);
                ps.setString(4, entityType);
                ps.setInt(5, entityId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Erreur ajout notification", e);
        }
    }

    private void checkAndCreateStockAlert(Product product) {
        if (product.getStock() <= 5) {
            String level = product.getStock() == 0 ? "DANGER" : "WARNING";
            String message = product.getStock() == 0
                ? "Le produit \"" + product.getName() + "\" est en rupture de stock !"
                : "Le produit \"" + product.getName() + "\" a un stock faible ("
                    + product.getStock() + " unités restantes).";
            addNotification("Alerte stock", message, "STOCK", level, product.getIdProduct());
            adminLogger.warn("Alerte stock - Produit: {}, Stock: {}", product.getName(), product.getStock());
        }
    }

    // ==================== MISE À JOUR STOCK LORS DU PAIEMENT ====================

    private boolean updateStockFromOrder(Order order) {
        try {
            List<OrderItem> items = order.getItems();
            if (items == null || items.isEmpty()) {
                logger.warn("Commande sans articles - UUID: {}", order.getOrderUUID());
                return true;
            }
            for (OrderItem item : items) {
                Product product = productService.getProductById(item.getProductId());
                if (product == null) {
                    logger.error("Produit non trouvé - ID: {}", item.getProductId());
                    return false;
                }
                int oldStock = product.getStock();
                int newStock = oldStock - item.getQuantity();
                if (newStock < 0) {
                    logger.error("Stock insuffisant - Produit: {}, Actuel: {}, Commandé: {}",
                        product.getName(), oldStock, item.getQuantity());
                    return false;
                }
                product.setStock(newStock);
                boolean updated = productService.updateProduct(product);
                if (updated) {
                    addStockMovement(product.getIdProduct(), product.getName(), "REMOVE",
                        item.getQuantity(), oldStock, newStock, "Commande #" + order.getId());
                    checkAndCreateStockAlert(product);
                } else {
                    logger.error("Échec mise à jour stock - Produit: {}", product.getName());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du stock", e);
            return false;
        }
    }

    // ==================== CHECKOUT AVEC ANTI-REJEU ====================

    private String handleCheckout(String request) {
        try {
            if (orderService == null) return "ERROR:ORDER_SERVICE_NOT_INITIALIZED";

            String[] parts = request.split(":");
            logger.info("📦 CHECKOUT - parts count: {}, content: {}", parts.length, Arrays.toString(parts));

            // Format normal avec token : CHECKOUT:TOKEN:clientId:nonce:timestamp  (parts.length=5)
            // Format sécurisé sans token: CHECKOUT:clientId:nonce:timestamp        (parts.length=4)
            // Le token est un UUID avec tirets, longueur > 30 et contient '-'
            int startIdx = 1;
            if (parts.length >= 2 && parts[1].contains("-") && parts[1].length() > 30
                    && !isNumeric(parts[1])) {
                startIdx = 2; // token présent
            }

            if (parts.length < startIdx + 3) {
                logger.warn("❌ CHECKOUT FORMAT invalide - startIdx: {}, parts: {}", startIdx, parts.length);
                return "ERROR:CHECKOUT_FORMAT";
            }

            int clientId;
            String nonce;
            long timestamp;

            try {
                clientId  = Integer.parseInt(parts[startIdx]);
                nonce     = parts[startIdx + 1];
                timestamp = Long.parseLong(parts[startIdx + 2]);
            } catch (NumberFormatException e) {
                logger.error("❌ CHECKOUT - parsing échoué: {}", e.getMessage());
                return "ERROR:CHECKOUT_FORMAT";
            }

            if (!isTimestampValid(timestamp)) {
                logger.warn("⏰ TIMESTAMP INVALIDE - checkout - clientId: {}", clientId);
                return "ERROR:INVALID_TIMESTAMP";
            }

            if (usedNonces.contains(nonce)) {
                logger.warn("🔄 REJEU DÉTECTÉ - Checkout - nonce: {}", nonce);
                return "ERROR:REPLAY_ATTACK_DETECTED";
            }
            usedNonces.add(nonce);

            paymentLogger.info("✅ Anti-rejeu OK - Checkout - clientId: {}", clientId);

            Cart cart = cartService.getCartByClient(clientId);
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                return "ERROR:CART_EMPTY";
            }

            Order order = orderService.createOrder(clientId, cart.getItems());
            cartService.clearCart(clientId);

            return "ORDER_CREATED;" + order.getOrderUUID() + ";" + order.getTotalPrice();

        } catch (Exception e) {
            logger.error("Erreur checkout", e);
            return "ERROR:CHECKOUT_EXCEPTION";
        }
    }

    private boolean isNumeric(String s) {
        try { Long.parseLong(s); return true; } catch (NumberFormatException e) { return false; }
    }

    // ==================== PAYMENT AVEC ANTI-REJEU ====================

    private String handlePayment(String request) {
        try {
            if (orderService == null) return "ERROR:ORDER_SERVICE_NOT_INITIALIZED";

            String[] parts = request.split(":");
            logger.info("📦 PAYMENT - parts count: {}, content: {}", parts.length, Arrays.toString(parts));

            // Format avec token    : PAYMENT:TOKEN:orderUUID:method:nonce:timestamp  (6 parts)
            // Format sans token    : PAYMENT:orderUUID:method:nonce:timestamp         (5 parts)
            // Le token est long (>30 chars) et contient '-', l'orderUUID aussi mais
            // on distingue : si parts[1] n'est pas un UUID standard (8-4-4-4-12),
            // c'est un token de session (format différent).
            // Stratégie simple : le token de session fait typiquement > 36 chars.
            int startIdx = 1;
            if (parts.length >= 2 && parts[1].contains("-") && parts[1].length() > 36) {
                startIdx = 2; // token de session présent
            }

            if (parts.length < startIdx + 4) {
                logger.warn("❌ PAYMENT FORMAT invalide - startIdx: {}, parts: {}", startIdx, parts.length);
                return "ERROR:PAYMENT_FORMAT";
            }

            String orderUUID = parts[startIdx];
            String method    = parts[startIdx + 1];
            String nonce     = parts[startIdx + 2];
            long timestamp;

            try {
                timestamp = Long.parseLong(parts[startIdx + 3]);
            } catch (NumberFormatException e) {
                logger.error("❌ PAYMENT - timestamp invalide: '{}'", parts[startIdx + 3]);
                return "ERROR:INVALID_TIMESTAMP_FORMAT";
            }

            if (!isTimestampValid(timestamp)) {
                paymentLogger.warn("⏰ TIMESTAMP INVALIDE - paiement - UUID: {}", orderUUID);
                return "ERROR:INVALID_TIMESTAMP";
            }

            if (usedNonces.contains(nonce)) {
                paymentLogger.warn("🔄 REJEU DÉTECTÉ - Paiement - nonce: {}", nonce);
                return "ERROR:REPLAY_ATTACK_DETECTED";
            }
            usedNonces.add(nonce);

            paymentLogger.info("✅ Anti-rejeu OK - Paiement - UUID: {}", orderUUID);

            Order order = orderService.getOrderByUUID(orderUUID);
            if (order == null) {
                paymentLogger.warn("⚠️ Commande non trouvée - UUID: {}", orderUUID);
                return "ERROR:ORDER_NOT_FOUND";
            }

            boolean stockUpdated = updateStockFromOrder(order);
            if (!stockUpdated) {
                paymentLogger.error("❌ Échec stock pour la commande: {}", orderUUID);
                return "ERROR:STOCK_UPDATE_FAILED";
            }

            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setMethod(method);
            payment.setAmount(order.getTotalPrice());
            payment.setStatus("pending");

            boolean success = paymentService.processPayment(payment);

            if (success) {
                orderService.updateStatus(order.getId(), "paid");
                paymentLogger.info("✅ Paiement réussi - UUID: {}, Montant: {} DH",
                    orderUUID, order.getTotalPrice());
                return "PAYMENT_SUCCESS;" + order.getOrderUUID();
            } else {
                paymentLogger.error("❌ Paiement échoué - UUID: {}", orderUUID);
                return "PAYMENT_FAILED;" + order.getOrderUUID();
            }

        } catch (Exception e) {
            paymentLogger.error("Erreur lors du paiement", e);
            return "ERROR:PAYMENT_EXCEPTION";
        }
    }

    // ==================== CATÉGORIES ====================

    private String handleGetCategories() {
        try {
            CategoryDAO categoryDAO = new CategoryDAO();
            List<Category> categories = categoryDAO.findAll();
            if (categories == null || categories.isEmpty()) return "NO_CATEGORIES";
            StringBuilder sb = new StringBuilder();
            for (Category c : categories) {
                sb.append(c.getId()).append(";")
                  .append(safe(c.getName())).append(";")
                  .append(safe(c.getDescription())).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return "ERROR:GET_CATEGORIES_EXCEPTION";
        }
    }

    // ==================== REGISTER / OTP ====================

    private String handleRegister(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 8) return "ERROR:REGISTER_FORMAT";
            String nom = parts[1], prenom = parts[2], email = parts[3], password = parts[4],
                   address = parts[5], phone = parts[6], ville = parts[7];
            if (authService.emailExists(email)) return "ERROR:EMAIL_ALREADY_EXISTS";
            boolean success = authService.registerPending(nom, prenom, email, password, address, phone, ville);
            if (!success) return "ERROR:REGISTER_FAILED";
            boolean otpSent = otpService.sendOtp(email);
            return otpSent ? "REGISTER_SUCCESS_OTP_SENT" : "REGISTER_SUCCESS_BUT_OTP_FAILED";
        } catch (Exception e) {
            return "ERROR:REGISTER_EXCEPTION";
        }
    }

    private String handleSendOtp(String request) {
        try {
            String[] parts = request.split(":", 2);
            if (parts.length != 2) return "ERROR:SEND_OTP_FORMAT";
            boolean sent = otpService.sendOtp(parts[1]);
            return sent ? "OTP_SENT" : "ERROR:OTP_SEND_FAILED";
        } catch (Exception e) {
            return "ERROR:SEND_OTP_EXCEPTION";
        }
    }

    private String handleVerifyOtp(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length != 3) return "ERROR:VERIFY_OTP_FORMAT";
            boolean verified = otpService.verifyOtp(parts[1], parts[2]);
            return verified ? "OTP_VERIFIED" : "ERROR:OTP_INVALID";
        } catch (Exception e) {
            return "ERROR:VERIFY_OTP_EXCEPTION";
        }
    }

    // ==================== PROFIL ====================

    private String handleGetProfile(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 2) return "ERROR:GET_PROFILE_FORMAT";
            int userId = Integer.parseInt(parts[parts.length - 1]);
            User user = new UserDAO().findById(userId);
            if (user == null) return "ERROR:PROFILE_NOT_FOUND";

            String fullName = (user.getPrenom() == null ? "" : user.getPrenom())
                + ((user.getNom() == null || user.getNom().isBlank()) ? "" : " " + user.getNom());
            String phone = "", address = "", city = "";
            if (user instanceof model.Client client) {
                phone   = client.getPhone()   == null ? "" : client.getPhone();
                address = client.getAddress() == null ? "" : client.getAddress();
                city    = client.getVille()   == null ? "" : client.getVille();
            }
            return "PROFILE_DATA:" + safe(fullName.trim()) + ";" + safe(user.getEmail()) + ";"
                 + safe(phone) + ";" + safe(address) + ";" + safe(city) + ";" + safe(user.getRole());
        } catch (Exception e) {
            return "ERROR:GET_PROFILE_EXCEPTION";
        }
    }

    private String handleUpdateProfile(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 7) return "ERROR:UPDATE_PROFILE_FORMAT";
            int userId = Integer.parseInt(parts[parts.length - 6]);
            boolean success = new UserDAO().updateProfile(userId,
                parts[parts.length - 5], parts[parts.length - 4],
                parts[parts.length - 3], parts[parts.length - 2], parts[parts.length - 1]);
            return success ? "UPDATE_PROFILE_SUCCESS" : "ERROR:UPDATE_PROFILE_FAILED";
        } catch (Exception e) {
            return "ERROR:UPDATE_PROFILE_EXCEPTION";
        }
    }

    // ==================== PANIER ====================

    private String handleCartAdd(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 4) return "ERROR:CART_ADD_FORMAT";
            int clientId  = Integer.parseInt(parts[parts.length - 3]);
            int productId = Integer.parseInt(parts[parts.length - 2]);
            int quantity  = Integer.parseInt(parts[parts.length - 1]);
            if (quantity <= 0) return "ERROR:INVALID_QUANTITY";
            Product product = productService.getProductById(productId);
            if (product == null) return "ERROR:PRODUCT_NOT_FOUND";
            if (product.getStock() < quantity) return "ERROR:INSUFFICIENT_STOCK";
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            boolean added = cartService.addItemToCart(clientId, item);
            return added ? "CART_ADD_SUCCESS" : "ERROR:CART_ADD_FAILED";
        } catch (NumberFormatException e) {
            return "ERROR:INVALID_NUMBER_FORMAT";
        } catch (Exception e) {
            return "ERROR:CART_ADD_EXCEPTION";
        }
    }

    private String handleCartRemove(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 3) return "ERROR:CART_REMOVE_FORMAT";
            int clientId  = Integer.parseInt(parts[parts.length - 2]);
            int productId = Integer.parseInt(parts[parts.length - 1]);
            boolean removed = cartService.removeItemFromCart(clientId, productId);
            return removed ? "CART_REMOVE_SUCCESS" : "ERROR:CART_REMOVE_FAILED";
        } catch (Exception e) {
            return "ERROR:CART_REMOVE_EXCEPTION";
        }
    }

    private String handleCartRemoveByName(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 3) return "ERROR:CART_REMOVE_BY_NAME_FORMAT";
            int clientId       = Integer.parseInt(parts[parts.length - 2]);
            String productName = parts[parts.length - 1];
            Product product = productService.getProductByName(productName);
            if (product == null) return "ERROR:PRODUCT_NOT_FOUND";
            boolean removed = cartService.removeItemFromCart(clientId, product.getIdProduct());
            return removed ? "CART_REMOVE_SUCCESS" : "ERROR:CART_REMOVE_FAILED";
        } catch (Exception e) {
            return "ERROR:CART_REMOVE_BY_NAME_EXCEPTION";
        }
    }

    private String handleCartGet(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 2) return "ERROR:CART_GET_FORMAT";
            int clientId = Integer.parseInt(parts[parts.length - 1]);
            Cart cart = cartService.getCartByClient(clientId);
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) return "CART_EMPTY";
            StringBuilder response = new StringBuilder();
            response.append("CART_DETAILS")
                .append("|CartID=").append(cart.getId())
                .append("|Items=").append(cart.getItems().size())
                .append("|Total=").append(cart.calculateTotal());
            for (CartItem item : cart.getItems()) {
                if (item.getProduct() != null) {
                    response.append("|ProductId=").append(item.getProduct().getIdProduct())
                        .append(",Product=").append(safe(item.getProduct().getName()))
                        .append(",Qty=").append(item.getQuantity())
                        .append(",Subtotal=").append(item.calculateSubtotal());
                }
            }
            return response.toString();
        } catch (Exception e) {
            return "ERROR:CART_GET_EXCEPTION";
        }
    }

    private String handleCartClear(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 2) return "ERROR:CART_CLEAR_FORMAT";
            int clientId = Integer.parseInt(parts[parts.length - 1]);
            boolean cleared = cartService.clearCart(clientId);
            return cleared ? "CART_CLEAR_SUCCESS" : "ERROR:CART_CLEAR_FAILED";
        } catch (Exception e) {
            return "ERROR:CART_CLEAR_EXCEPTION";
        }
    }

    // ==================== PRODUITS ====================

    private String handleGetProducts() {
        try {
            logger.info("🔍 GET_PRODUCTS appelé");
            List<Product> products = productService.getAllProducts();
            if (products == null || products.isEmpty()) {
                logger.warn("⚠️ Aucun produit trouvé en base");
                return "NO_PRODUCTS";
            }
            StringBuilder sb = new StringBuilder();
            for (Product p : products) {
                String categoryName = p.getCategory() != null && p.getCategory().getName() != null
                    ? p.getCategory().getName() : "Sans catégorie";
                sb.append(p.getIdProduct()).append(";")
                  .append(safe(p.getName())).append(";")
                  .append(p.getPrice()).append(";")
                  .append(safe(p.getImage())).append(";")
                  .append(safe(categoryName)).append(";")
                  .append(p.getStock()).append("|");
            }
            String result = sb.substring(0, sb.length() - 1);
            logger.info("✅ Envoi de {} produits", products.size());
            return result;
        } catch (Exception e) {
            logger.error("Erreur GET_PRODUCTS: ", e);
            return "ERROR:GET_PRODUCTS_FAILED";
        }
    }

    private String handleGetProduct(String request) {
        try {
            // Format: GET_PRODUCT:TOKEN:productId  ou  GET_PRODUCT:productId
            String[] parts = request.split(":");
            if (parts.length < 2) return "ERROR:GET_PRODUCT_FORMAT";

            // Le productId est TOUJOURS le dernier élément
            int productId;
            try {
                productId = Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                logger.error("❌ ID produit invalide dans: {}", request);
                return "ERROR:INVALID_PRODUCT_ID";
            }

            Product p = productService.getProductById(productId);
            if (p == null) return "ERROR:PRODUCT_NOT_FOUND";

            String categoryName = p.getCategory() != null && p.getCategory().getName() != null
                ? p.getCategory().getName() : "Sans catégorie";

            return p.getIdProduct() + ";" + safe(p.getName()) + ";" + p.getPrice() + ";"
                 + safe(p.getDescription()) + ";" + p.getStock() + ";"
                 + safe(p.getImage()) + ";" + safe(categoryName);
        } catch (Exception e) {
            logger.error("Erreur GET_PRODUCT", e);
            return "ERROR:GET_PRODUCT_EXCEPTION";
        }
    }

    // ==================== ADMIN PRODUITS ====================

    private String handleAdminAddProduct(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 7) return "ERROR:ADMIN_ADD_PRODUCT_FORMAT";
            String name        = parts[parts.length - 6];
            String description = parts[parts.length - 5];
            double price       = Double.parseDouble(parts[parts.length - 4]);
            int stock          = Integer.parseInt(parts[parts.length - 3]);
            String image       = parts[parts.length - 2];
            int categoryId     = Integer.parseInt(parts[parts.length - 1]);

            Product product = new Product(0, name, description, image, price, stock);
            product.setCategory(new Category(categoryId, "", ""));
            boolean success = productService.addProduct(product);

            if (success) {
                addStockMovement(product.getIdProduct(), name, "ADD", stock, 0, stock, "Création du produit");
                addNotification("Nouveau produit",
                    "Le produit \"" + name + "\" a été ajouté avec " + stock + " unités.",
                    "PRODUCT", "INFO", product.getIdProduct());
                checkAndCreateStockAlert(product);
                adminLogger.info("Produit ajouté - Nom: {}, Stock: {}", name, stock);
            }
            return success ? "ADMIN_ADD_PRODUCT_SUCCESS" : "ERROR:ADMIN_ADD_PRODUCT_FAILED";
        } catch (Exception e) {
            logger.error("Erreur addProduct", e);
            return "ERROR:ADMIN_ADD_PRODUCT_EXCEPTION";
        }
    }

    private String handleAdminUpdateProduct(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 8) return "ERROR:ADMIN_UPDATE_PRODUCT_FORMAT";
            int id             = Integer.parseInt(parts[parts.length - 7]);
            String name        = parts[parts.length - 6];
            String description = parts[parts.length - 5];
            double price       = Double.parseDouble(parts[parts.length - 4]);
            int newStock       = Integer.parseInt(parts[parts.length - 3]);
            String image       = parts[parts.length - 2];
            int categoryId     = Integer.parseInt(parts[parts.length - 1]);

            Product oldProduct = productService.getProductById(id);
            int oldStock = oldProduct != null ? oldProduct.getStock() : 0;

            Product product = new Product(id, name, description, image, price, newStock);
            product.setCategory(new Category(categoryId, "", ""));
            boolean success = productService.updateProduct(product);

            if (success && newStock != oldStock) {
                String movementType = newStock > oldStock ? "ADD" : "REMOVE";
                int diff = Math.abs(newStock - oldStock);
                addStockMovement(id, name, movementType, diff, oldStock, newStock, "Modification manuelle");
                checkAndCreateStockAlert(product);
                adminLogger.info("Produit modifié - ID: {}, Stock: {} → {}", id, oldStock, newStock);
            }
            return success ? "ADMIN_UPDATE_PRODUCT_SUCCESS" : "ERROR:ADMIN_UPDATE_PRODUCT_FAILED";
        } catch (Exception e) {
            logger.error("Erreur updateProduct", e);
            return "ERROR:ADMIN_UPDATE_PRODUCT_EXCEPTION";
        }
    }

    private String handleAdminDeleteProduct(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 2) return "ERROR:ADMIN_DELETE_PRODUCT_FORMAT";
            int id = Integer.parseInt(parts[parts.length - 1]);
            boolean success = productService.deleteProduct(id);
            return success ? "ADMIN_DELETE_PRODUCT_SUCCESS" : "ERROR:ADMIN_DELETE_PRODUCT_FAILED";
        } catch (Exception e) {
            return "ERROR:ADMIN_DELETE_PRODUCT_EXCEPTION";
        }
    }

    // ==================== ADMIN CATÉGORIES ====================

    private String handleAdminGetCategories() {
        try {
            List<Category> categories = new CategoryDAO().findAll();
            if (categories == null || categories.isEmpty()) return "NO_CATEGORIES";
            StringBuilder sb = new StringBuilder();
            for (Category c : categories) {
                sb.append(c.getId()).append(";")
                  .append(safe(c.getName())).append(";")
                  .append(safe(c.getDescription())).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return "ERROR:ADMIN_GET_CATEGORIES_EXCEPTION";
        }
    }

    private String handleAdminAddCategory(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 3) return "ERROR:ADMIN_ADD_CATEGORY_FORMAT";
            new CategoryDAO().save(new Category(0, parts[parts.length - 2], parts[parts.length - 1]));
            return "ADMIN_ADD_CATEGORY_SUCCESS";
        } catch (Exception e) {
            return "ERROR:ADMIN_ADD_CATEGORY_EXCEPTION";
        }
    }

    private String handleAdminUpdateCategory(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 4) return "ERROR:ADMIN_UPDATE_CATEGORY_FORMAT";
            int id = Integer.parseInt(parts[parts.length - 3]);
            new CategoryDAO().update(new Category(id, parts[parts.length - 2], parts[parts.length - 1]));
            return "ADMIN_UPDATE_CATEGORY_SUCCESS";
        } catch (Exception e) {
            return "ERROR:ADMIN_UPDATE_CATEGORY_EXCEPTION";
        }
    }

    private String handleAdminDeleteCategory(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 2) return "ERROR:ADMIN_DELETE_CATEGORY_FORMAT";
            new CategoryDAO().delete(Integer.parseInt(parts[parts.length - 1]));
            return "ADMIN_DELETE_CATEGORY_SUCCESS";
        } catch (Exception e) {
            return "ERROR:ADMIN_DELETE_CATEGORY_EXCEPTION";
        }
    }

    // ==================== ADMIN USERS ====================

    private String handleAdminGetUsers() {
        try {
            List<User> users = new UserDAO().findAll();
            if (users == null || users.isEmpty()) return "NO_USERS";
            StringBuilder sb = new StringBuilder();
            for (User user : users) {
                sb.append(user.getId()).append(";")
                  .append(safe(user.getNom())).append(";")
                  .append(safe(user.getPrenom())).append(";")
                  .append(safe(user.getEmail())).append(";")
                  .append(safe(user.getRole())).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return "ERROR:ADMIN_GET_USERS_EXCEPTION";
        }
    }

    // ==================== ADMIN COMMANDES ====================

    private String handleAdminGetOrders() {
        try {
            List<Order> orders = new OrderDAO().findAll();
            if (orders == null || orders.isEmpty()) return "NO_ORDERS";
            StringBuilder sb = new StringBuilder();
            for (Order order : orders) {
                sb.append(order.getId()).append(";")
                  .append(order.getOrderUUID()).append(";")
                  .append(safe(order.getClientFullName())).append(";")
                  .append(safe(order.getClientEmail())).append(";")
                  .append(order.getTotalPrice()).append(";")
                  .append(order.getStatus()).append(";")
                  .append(order.getCreatedAt()).append("|");
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            logger.error("Erreur adminGetOrders", e);
            return "NO_ORDERS";
        }
    }

    private String handleAdminUpdateOrderStatus(String request) {
        try {
            String[] parts = request.split(":");
            if (parts.length < 3) return "ERROR:ADMIN_UPDATE_ORDER_STATUS_FORMAT";
            int orderId  = Integer.parseInt(parts[parts.length - 2]);
            String status = parts[parts.length - 1];
            new OrderDAO().updateStatus(orderId, status);
            return "ADMIN_UPDATE_ORDER_STATUS_SUCCESS";
        } catch (Exception e) {
            return "ERROR:ADMIN_UPDATE_ORDER_STATUS_EXCEPTION";
        }
    }

    // ==================== UTILITAIRES ====================

    private String safe(String value) {
        if (value == null) return "";
        return value.replace(";", ",").replace("|", "/").replace(":", "-");
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (onCloseCallback != null) onCloseCallback.run();
            logger.info("Ressources fermées pour client : {}", clientIp);
        } catch (IOException e) {
            logger.error("Erreur fermeture ressources : {}", e.getMessage());
        }
    }
}