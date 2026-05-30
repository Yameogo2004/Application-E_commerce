package test;

import Client.ClientSocketService;

import java.util.UUID;

public class TestRegister {

    public static void main(String[] args) {

        ClientSocketService client = new ClientSocketService();

        if (!client.connect()) {
            System.out.println("Impossible de se connecter au serveur !");
            return;
        }

        // Génération d'un email unique pour éviter les doublons
        String uniqueEmail = "testuser_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        String nom = "TestNom";
        String prenom = "TestPrenom";
        String password = "TestPass123";
        String address = "123 Rue Exemple";
        String phone = "0600000000";
        String ville = "TestVille";

        // Envoi de la requête REGISTER
        String response = client.register(nom, prenom, uniqueEmail, password, address, phone, ville);
        System.out.println("Réponse serveur : " + response);

        // Essai LOGIN avec ce nouveau compte
        String loginResponse = client.login(uniqueEmail, password);
        System.out.println("Réponse LOGIN : " + loginResponse);

        client.close();
    }
}