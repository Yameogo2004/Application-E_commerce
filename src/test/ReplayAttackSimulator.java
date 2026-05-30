package test;

import java.io.*;
import java.net.Socket;

public class ReplayAttackSimulator {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001;
    
    public static void main(String[] args) {
        System.out.println("🔥 SIMULATION D'ATTAQUE PAR REJEU 🔥");
        System.out.println("===================================\n");
        
        // Étape 1: Capturer une requête valide
        System.out.println("📡 1. Capture d'une requête valide...");
        String capturedRequest = captureValidRequest();
        System.out.println("   Requête capturée: " + capturedRequest);
        
        // Étape 2: Rejouer la même requête
        System.out.println("\n🔄 2. Rejeu de la même requête...");
        String replayResult = sendRequest(capturedRequest);
        System.out.println("   Résultat du rejeu: " + replayResult);
        
        // Étape 3: Vérifier la protection
        if (replayResult.contains("REPLAY_ATTACK_DETECTED") || replayResult.contains("INVALID_TIMESTAMP")) {
            System.out.println("\n✅ PROTECTION ACTIVE ! L'attaque a été détectée et bloquée.");
        } else if (replayResult.contains("SUCCESS")) {
            System.out.println("\n❌ ÉCHEC DE PROTECTION ! L'attaque a réussi !");
        }
    }
    
    private static String captureValidRequest() {
        try {
            String request = "CHECKOUT:1:" + java.util.UUID.randomUUID() + ":" + System.currentTimeMillis();
            return request;
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private static String sendRequest(String request) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            in.readLine(); // Welcome message
            out.println(request);
            String response = in.readLine();
            socket.close();
            
            return response;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}