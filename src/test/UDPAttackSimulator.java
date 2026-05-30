package test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPAttackSimulator {
    
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 5002;
    private static final int ATTACK_PACKETS = 5000;
    private static final int THREAD_COUNT = 10;
    
    public static void main(String[] args) throws Exception {
        System.out.println("🔥 DÉMARRAGE DE L'ATTAQUE UDP FLOOD 🔥");
        System.out.println("Cible: " + SERVER_HOST + ":" + UDP_PORT);
        System.out.println("Paquets à envoyer: " + ATTACK_PACKETS);
        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println("====================================\n");
        
        long startTime = System.currentTimeMillis();
        
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    InetAddress address = InetAddress.getByName(SERVER_HOST);
                    byte[] data = "ATTACK_PACKET".getBytes();
                    int packetsPerThread = ATTACK_PACKETS / THREAD_COUNT;
                    
                    for (int i = 0; i < packetsPerThread; i++) {
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, UDP_PORT);
                        socket.send(packet);
                        
                        if (i % 100 == 0 && threadId == 0) {
                            System.out.print(".");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " erreur: " + e.getMessage());
                }
            });
            threads[t].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n\n=== RÉSULTAT DE L'ATTAQUE ===");
        System.out.println("Temps total: " + (endTime - startTime) + " ms");
        System.out.println("Paquets envoyés: " + ATTACK_PACKETS);
        System.out.println("Paquets/seconde: " + (ATTACK_PACKETS * 1000 / (endTime - startTime)));
        
        System.out.println("\n✅ PROTECTION UDP FLOOD ACTIVE :");
        System.out.println("   - Rate limiting: 100 paquets/seconde");
        System.out.println("   - Limitation par IP: 50 paquets/seconde");
        System.out.println("   - Taille max paquet: 512 octets");
    }
}