package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {

    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final int PLAIN_PORT = 5001;   // Port pour les connexions normales
    private static final int SSL_PORT = 8443;     // Port pour les connexions SSL (admins)
    private static final int UDP_PORT = 5002;     // Port pour le serveur UDP (TP4)
    
    // ==================== PROTECTION SYN FLOOD ====================
    private static final int MAX_CONNECTIONS_PER_IP = 5;      // Max connexions par IP
    private static final int MAX_TOTAL_CONNECTIONS = 50;     // Max connexions totales
    private static final Semaphore connectionSemaphore = new Semaphore(MAX_TOTAL_CONNECTIONS);
    private static final Map<String, Integer> ipConnections = new ConcurrentHashMap<>();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    
    // Serveur UDP pour TP4
    private static UDPServer udpServer;
    
    // Configuration SSL
    private static final String KEYSTORE_PATH = "keys/server.p12";
    private static final String KEYSTORE_PASSWORD = "serverpass";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String TRUSTSTORE_PATH = "keys/truststore.p12";
    private static final String TRUSTSTORE_PASSWORD = "trustpassword";
    private static final String TRUSTSTORE_TYPE = "PKCS12";

    public static void main(String[] args) {
        logger.info("🔐 Démarrage du serveur ChriOnline...");
        
        // Configurer SSL au démarrage
        configureSSL();
        
        // Démarrer le serveur normal (sans SSL)
        startPlainServer();
        
        // Démarrer le serveur SSL (pour les admins)
        startSSLServer();
        
        // Démarrer le serveur UDP pour TP4
        startUDPServer();
    }
    
    /**
     * Configure les propriétés SSL pour le serveur
     */
    private static void configureSSL() {
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_PATH);
        System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.keyStoreType", KEYSTORE_TYPE);
        
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", TRUSTSTORE_TYPE);
        
        logger.debug("Configuration SSL chargée - KeyStore: {}, TrustStore: {}", KEYSTORE_PATH, TRUSTSTORE_PATH);
    }
    
    /**
     * Libère les compteurs de connexion pour une IP donnée
     */
    private static void releaseConnection(String ip) {
        ipConnections.merge(ip, -1, Integer::sum);
        if (ipConnections.get(ip) <= 0) {
            ipConnections.remove(ip);
        }
        connectionSemaphore.release();
    }
    
    /**
     * Serveur normal (sans SSL) pour les clients avec protection SYN Flood
     */
    private static void startPlainServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PLAIN_PORT)) {
                logger.info("✅ Serveur normal lancé sur le port {}", PLAIN_PORT);
                logger.info("🛡️ Protection SYN Flood active - Max connexions/IP: {}, Max total: {}", 
                    MAX_CONNECTIONS_PER_IP, MAX_TOTAL_CONNECTIONS);
                
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    String ip = clientSocket.getInetAddress().getHostAddress();
                    
                    // ==================== PROTECTION SYN FLOOD ====================
                    int currentIpConnections = ipConnections.getOrDefault(ip, 0);
                    if (currentIpConnections >= MAX_CONNECTIONS_PER_IP) {
                        logger.warn("⛔ TROP DE CONNEXIONS depuis l'IP: {} (actuel: {}, max: {})", 
                            ip, currentIpConnections, MAX_CONNECTIONS_PER_IP);
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {}
                        continue;
                    }
                    
                    if (!connectionSemaphore.tryAcquire()) {
                        logger.warn("⛔ TROP DE CONNEXIONS TOTALES (max: {})", MAX_TOTAL_CONNECTIONS);
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {}
                        continue;
                    }
                    // =============================================================
                    
                    ipConnections.merge(ip, 1, Integer::sum);
                    logger.info("📱 Nouveau client connecté : {} (connexions IP: {}/{}, total: {}/{})", 
                        ip, currentIpConnections + 1, MAX_CONNECTIONS_PER_IP,
                        connectionSemaphore.availablePermits() == 0 ? MAX_TOTAL_CONNECTIONS : 
                        MAX_TOTAL_CONNECTIONS - connectionSemaphore.availablePermits(), MAX_TOTAL_CONNECTIONS);
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket, ip, 
                        () -> releaseConnection(ip));
                    threadPool.execute(clientHandler);
                }
                
            } catch (IOException e) {
                logger.error("❌ Erreur serveur normal : {}", e.getMessage(), e);
            }
        }).start();
    }
    
    /**
     * Serveur SSL pour les admins avec protection SYN Flood
     */
    private static void startSSLServer() {
        new Thread(() -> {
            try {
                SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(SSL_PORT);
                
                serverSocket.setNeedClientAuth(true);
                
                logger.info("🔒 Serveur SSL lancé sur le port {}", SSL_PORT);
                logger.info("   - Authentification client requise");
                logger.info("   - Chiffrement TLS activé");
                logger.info("🛡️ Protection SYN Flood active sur SSL - Max connexions/IP: {}, Max total: {}", 
                    MAX_CONNECTIONS_PER_IP, MAX_TOTAL_CONNECTIONS);
                
                while (true) {
                    javax.net.ssl.SSLSocket clientSocket = (javax.net.ssl.SSLSocket) serverSocket.accept();
                    String ip = clientSocket.getInetAddress().getHostAddress();
                    
                    int currentIpConnections = ipConnections.getOrDefault(ip, 0);
                    if (currentIpConnections >= MAX_CONNECTIONS_PER_IP) {
                        logger.warn("⛔ TROP DE CONNEXIONS SSL depuis l'IP: {} (actuel: {}, max: {})", 
                            ip, currentIpConnections, MAX_CONNECTIONS_PER_IP);
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {}
                        continue;
                    }
                    
                    if (!connectionSemaphore.tryAcquire()) {
                        logger.warn("⛔ TROP DE CONNEXIONS TOTALES (max: {})", MAX_TOTAL_CONNECTIONS);
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {}
                        continue;
                    }
                    
                    ipConnections.merge(ip, 1, Integer::sum);
                    logger.info("🔐 Admin connecté via SSL : {} (connexions IP: {}/{}, total: {}/{})", 
                        ip, currentIpConnections + 1, MAX_CONNECTIONS_PER_IP,
                        connectionSemaphore.availablePermits() == 0 ? MAX_TOTAL_CONNECTIONS : 
                        MAX_TOTAL_CONNECTIONS - connectionSemaphore.availablePermits(), MAX_TOTAL_CONNECTIONS);
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket, ip, 
                        () -> releaseConnection(ip));
                    threadPool.execute(clientHandler);
                }
                
            } catch (IOException e) {
                logger.error("❌ Erreur serveur SSL : {}", e.getMessage(), e);
            }
        }).start();
    }
    
    /**
     * Serveur UDP pour TP4 - Protection contre UDP Flood
     */
    private static void startUDPServer() {
        udpServer = new UDPServer();
        udpServer.start();
    }
}