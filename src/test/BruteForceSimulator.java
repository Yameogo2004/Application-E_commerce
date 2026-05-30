package test;

import java.io.*;
import java.net.Socket;

public class BruteForceSimulator {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001;
    private static final String TARGET_EMAIL = "admin@chri.com";
    
    private static final String[] TEST_PASSWORDS = {
        "123456", "password", "admin", "admin123", "root", 
        "123456789", "azerty", "qwerty", "motdepasse", "000000",
        "admin@123", "password123", "passer", "12345", "letmein"
    };
    
    public static void main(String[] args) {
        System.out.println("🔥 SIMULATION D'ATTAQUE PAR FORCE BRUTE 🔥");
        System.out.println("Cible: " + TARGET_EMAIL);
        System.out.println("Nombre de mots de passe à tester: " + TEST_PASSWORDS.length);
        System.out.println("================================================\n");
        
        int attempt = 0;
        for (String password : TEST_PASSWORDS) {
            attempt++;
            System.out.print("Test #" + attempt + " - Mot de passe: '" + password + "' → ");
            
            try {
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                in.readLine(); // Welcome message
                out.println("LOGIN:" + TARGET_EMAIL + ":" + password);
                String response = in.readLine();
                socket.close();
                
                if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                    System.out.println("✅✅✅ MOT DE PASSE TROUVÉ : " + password + " ✅✅✅");
                    System.out.println("\n🎯 ATTAQUE RÉUSSIE !");
                    return;
                } else if (response != null && response.contains("TOO_MANY_ATTEMPTS")) {
                    System.out.println("⛔ BLOCKÉ - Trop de tentatives !");
                    break;
                } else {
                    System.out.println("❌ Échec");
                }
                
                Thread.sleep(100);
                
            } catch (Exception e) {
                System.out.println("⚠️ Erreur: " + e.getMessage());
            }
        }
        
        System.out.println("\n❌ ATTAQUE ÉCHOUÉE - Aucun mot de passe trouvé");
    }
}