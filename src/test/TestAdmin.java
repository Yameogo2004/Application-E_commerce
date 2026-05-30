package test;

import Client.ClientSocketService;

public class TestAdmin {
    public static void main(String[] args) {
        ClientSocketService client = new ClientSocketService();
        
        if (client.connect()) {
            System.out.println("✅ Connexion établie");
            
            // Tester ADMIN_AUTH_CHALLENGE
            String response = client.sendRequest("ADMIN_AUTH_CHALLENGE:ouedraogoariel43@gmail.com");
            System.out.println("Réponse du serveur: " + response);
            
        } else {
            System.out.println("❌ Connexion échouée");
        }
        
        client.close();
    }
}