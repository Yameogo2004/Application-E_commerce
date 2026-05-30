package test;

import java.io.*;
import java.net.Socket;

public class IPSpoofingSimulator {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001;
    
    // IP cible à usurper (celle d'un admin légitime)
    private static final String SPOOFED_IP = "192.168.1.100";
    
    public static void main(String[] args) {
        System.out.println("🔥 SIMULATION D'ATTAQUE IP SPOOFING 🔥");
        System.out.println("IP usurpée: " + SPOOFED_IP);
        System.out.println("====================================\n");
        
        // Note: Le spoofing IP pur n'est pas possible en Java standard
        // car le système d'exploitation gère la couche IP.
        // Cette simulation montre le principe théorique.
        
        System.out.println("⚠️ Le vrai spoofing IP nécessite :");
        System.out.println("   - Accès au niveau réseau (raw sockets)");
        System.out.println("   - Ou modification des en-têtes IP au niveau système");
        System.out.println("   - Ou utilisation d'un proxy/modificateur de paquets\n");
        
        System.out.println("✅ Dans notre application, la protection est assurée par :");
        System.out.println("   1. Authentification par email/mot de passe");
        System.out.println("   2. Authentification RSA pour admin");
        System.out.println("   3. Session token (non prédictible)");
        System.out.println("   4. L'IP est un critère supplémentaire, pas unique\n");
        
        // Tester une connexion normale
        testNormalConnection();
    }
    
    private static void testNormalConnection() {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            in.readLine(); // Welcome message
            
            // Tentative de connexion avec email valide mais IP différente
            out.println("LOGIN:admin@chri.com:admin123");
            String response = in.readLine();
            
            System.out.println("Résultat tentative avec IP réelle: " + response);
            
            socket.close();
            
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
}