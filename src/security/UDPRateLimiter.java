package security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UDPRateLimiter {
    
    private static final int MAX_PACKETS_PER_SECOND = 100;  // Max paquets par seconde
    private static final int MAX_PACKETS_PER_IP = 50;      // Max paquets par IP
    private static final int BLOCK_DURATION = 60000;       // 1 minute de blocage
    
    private final Map<String, AtomicInteger> ipPackets = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedIPs = new ConcurrentHashMap<>();
    private final AtomicInteger totalPackets = new AtomicInteger(0);
    private long lastReset = System.currentTimeMillis();
    
    /**
     * Vérifie si une requête est autorisée
     * @param ip Adresse IP du client
     * @return true si autorisé, false si bloqué
     */
    public boolean isAllowed(String ip) {
        // Nettoyer les IPs bloquées expirées
        cleanBlockedIPs();
        
        // Vérifier si l'IP est bloquée
        if (isBlocked(ip)) {
            return false;
        }
        
        // Réinitialiser les compteurs toutes les secondes
        resetCountersIfNeeded();
        
        // Vérifier le total de paquets
        if (totalPackets.get() >= MAX_PACKETS_PER_SECOND) {
            return false;
        }
        
        // Vérifier le nombre de paquets par IP
        int ipCount = ipPackets.computeIfAbsent(ip, k -> new AtomicInteger(0)).incrementAndGet();
        if (ipCount > MAX_PACKETS_PER_IP) {
            blockIP(ip);
            return false;
        }
        
        totalPackets.incrementAndGet();
        return true;
    }
    
    private void resetCountersIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastReset >= 1000) {
            totalPackets.set(0);
            ipPackets.clear();
            lastReset = now;
        }
    }
    
    private void blockIP(String ip) {
        blockedIPs.put(ip, System.currentTimeMillis() + BLOCK_DURATION);
        ipPackets.remove(ip);
    }
    
    private boolean isBlocked(String ip) {
        Long expiry = blockedIPs.get(ip);
        if (expiry == null) return false;
        if (expiry < System.currentTimeMillis()) {
            blockedIPs.remove(ip);
            return false;
        }
        return true;
    }
    
    private void cleanBlockedIPs() {
        long now = System.currentTimeMillis();
        blockedIPs.entrySet().removeIf(entry -> entry.getValue() < now);
    }
    
    public int getBlockedIPCount() {
        return blockedIPs.size();
    }
}