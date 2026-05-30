package server;

import security.UDPRateLimiter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPServer {
    
    private static final Logger logger = LogManager.getLogger(UDPServer.class);
    private static final int UDP_PORT = 5002;
    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_PACKET_SIZE = 512;  // Taille max autorisée
    
    private final UDPRateLimiter rateLimiter = new UDPRateLimiter();
    private volatile boolean running = true;
    
    public void start() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
                logger.info("🛡️ Serveur UDP lancé sur le port {} (protection anti-UDP Flood activée)", UDP_PORT);
                logger.info("   - Max paquets/seconde: 100");
                logger.info("   - Max paquets par IP: 50");
                logger.info("   - Taille max paquet: {} octets", MAX_PACKET_SIZE);
                
                byte[] buffer = new byte[BUFFER_SIZE];
                
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String clientIp = packet.getAddress().getHostAddress();
                    int packetSize = packet.getLength();
                    
                    // 🔐 FILTRAGE : Taille du paquet
                    if (packetSize > MAX_PACKET_SIZE) {
                        logger.warn("⛔ PAQUET TROP VOLUMINEUX - IP: {}, Taille: {} octets (max: {})", 
                            clientIp, packetSize, MAX_PACKET_SIZE);
                        continue;
                    }
                    
                    // 🔐 FILTRAGE : Rate limiting
                    if (!rateLimiter.isAllowed(clientIp)) {
                        logger.warn("⛔ RATE LIMITING - IP bloquée temporairement: {}", clientIp);
                        continue;
                    }
                    
                    // Traiter le message
                    String message = new String(packet.getData(), 0, packet.getLength());
                    logger.debug("📨 Message UDP reçu de {}: {}", clientIp, message);
                    
                    // Optionnel : répondre au client
                    String response = "ACK: " + message;
                    byte[] responseData = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseData, responseData.length, 
                        packet.getAddress(), packet.getPort()
                    );
                    socket.send(responsePacket);
                }
                
            } catch (SocketException e) {
                logger.error("❌ Erreur socket UDP: {}", e.getMessage());
            } catch (IOException e) {
                logger.error("❌ Erreur UDP: {}", e.getMessage());
            }
        }).start();
    }
    
    public void stop() {
        running = false;
        logger.info("🛑 Serveur UDP arrêté");
    }
    
    public static void main(String[] args) {
        UDPServer server = new UDPServer();
        server.start();
    }
}