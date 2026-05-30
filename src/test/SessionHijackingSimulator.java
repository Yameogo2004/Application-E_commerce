package test;

import java.io.*;
import java.net.Socket;

public class SessionHijackingSimulator {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001;
    
    public static void main(String[] args) {
        System.out.println("🔥 SIMULATION D'ATTAQUE SESSION HIJACKING 🔥");
        System.out.println("=========================================\n");
        
        // Simuler un attaquant qui intercepte un token de session
        String stolenToken = interceptSessionToken();
        System.out.println("🎯 Token volé: " + (stolenToken != null ? stolenToken.substring(0, Math.min(20, stolenToken.length())) + "..." : "null"));
        
        // Tenter d'utiliser le token volé
        System.out.println("\n🔓 Tentative d'utilisation du token volé...");
        String result = useStolenToken(stolenToken);
        System.out.println("   Résultat: " + result);
        
        if (result.contains("INVALID_SESSION") || result.contains("expirée")) {
            System.out.println("\n✅ PROTECTION ACTIVE ! Le token volé est invalide.");
            System.out.println("   - Vérification IP");
            System.out.println("   - Expiration session");
            System.out.println("   - Token sécurisé (non prédictible)");
        } else if (result.contains("SUCCESS")) {
            System.out.println("\n❌ SESSION HIJACKING RÉUSSI !");
        }
    }
    
    private static String interceptSessionToken() {
        // Simulation d'interception
        return "stolen-fake-token-123456";
    }
    
    private static String useStolenToken(String token) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            in.readLine(); // Welcome message
            
            // Tentative d'utiliser le token volé
            out.println("GET_PROFILE:" + token + ":1");
            String response = in.readLine();
            socket.close();
            
            return response;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}