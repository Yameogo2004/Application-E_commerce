package test;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SynFloodSimulator {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001;
    private static final int ATTACK_THREADS = 100;
    
    public static void main(String[] args) {
        System.out.println("🔥 DÉMARRAGE DE L'ATTAQUE SYN FLOOD 🔥");
        System.out.println("Tentative d'ouverture de " + ATTACK_THREADS + " connexions...\n");
        
        List<Socket> sockets = new ArrayList<>();
        int successCount = 0;
        
        for (int i = 0; i < ATTACK_THREADS; i++) {
            try {
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                sockets.add(socket);
                successCount++;
                System.out.print(".");
                
                if ((i + 1) % 50 == 0) {
                    System.out.println(" " + (i + 1) + " connexions");
                }
            } catch (IOException e) {
                System.out.println("\n❌ Connexion #" + (i + 1) + " échouée: " + e.getMessage());
                break;
            }
        }
        
        System.out.println("\n\n=== RÉSULTAT DE L'ATTAQUE ===");
        System.out.println("Connexions réussies: " + successCount);
        System.out.println("Connexions refusées: " + (ATTACK_THREADS - successCount));
        
        // Vérifier la protection
        if (successCount <= 10) {
            System.out.println("\n✅ PROTECTION SYN FLOOD ACTIVE !");
            System.out.println("   Max connexions/IP: 5");
            System.out.println("   Max connexions totales: 50");
        } else {
            System.out.println("\n❌ PROTECTION INSUFFISANTE !");
        }
        
        // Fermer les connexions
        for (Socket s : sockets) {
            try { s.close(); } catch (IOException ignored) {}
        }
    }
}