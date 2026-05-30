package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001; // ⚠️ même port que serveur

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    private String clientId; // important pour panier/commande

    // ── CONNEXION ────────────────────────────
    public boolean connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(System.in);

            System.out.println(in.readLine()); // message serveur
            return true;

        } catch (Exception e) {
            System.out.println("Erreur connexion : " + e.getMessage());
            return false;
        }
    }

    // ── COMMUNICATION ─────────────────────────
    private String sendRequest(String request) {
        try {
            out.println(request);
            return in.readLine();
        } catch (Exception e) {
            return "ERROR:COMMUNICATION";
        }
    }

    // ── MENU PRINCIPAL ────────────────────────
    public void showMainMenu() {
        while (true) {
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Quitter");
            System.out.print("Choix : ");

            String choix = scanner.nextLine();

            switch (choix) {
                case "1" -> handleLogin();
                case "2" -> handleRegister();
                case "0" -> {
                    closeConnection();
                    return;
                }
                default -> System.out.println("Choix invalide !");
            }
        }
    }

    // ── LOGIN ────────────────────────────────
    private void handleLogin() {
        System.out.print("Email : ");
        String email = scanner.nextLine();

        System.out.print("Mot de passe : ");
        String password = scanner.nextLine();

        String response = sendRequest("LOGIN:" + email + ":" + password);

        if (response != null && response.startsWith("LOGIN_SUCCESS")) {
            String[] parts = response.split(":");
            clientId = parts[1];

            System.out.println("Connexion réussie !");
            showClientMenu();

        } else {
            System.out.println("Login échoué !");
        }
    }

    // ── REGISTER ─────────────────────────────
    private void handleRegister() {
        System.out.print("Nom : ");
        String nom = scanner.nextLine();

        System.out.print("Prenom : ");
        String prenom = scanner.nextLine();

        System.out.print("Email : ");
        String email = scanner.nextLine();

        System.out.print("Password : ");
        String password = scanner.nextLine();

        System.out.print("Adresse : ");
        String address = scanner.nextLine();

        System.out.print("Téléphone : ");
        String phone = scanner.nextLine();

        System.out.print("Ville : ");
        String ville = scanner.nextLine();

        String response = sendRequest("REGISTER:" + nom + ":" + prenom + ":" +
                email + ":" + password + ":" + address + ":" + phone + ":" + ville);

        System.out.println(response);
    }

    // ── MENU CLIENT ──────────────────────────
    private void showClientMenu() {
        while (true) {
            System.out.println("\n=== MENU CLIENT ===");
            System.out.println("1. Voir produits");
            System.out.println("2. Détail produit");
            System.out.println("3. Panier");
            System.out.println("4. Checkout");
            System.out.println("5. Paiement");
            System.out.println("0. Logout");
            System.out.print("Choix : ");

            String choix = scanner.nextLine();

            switch (choix) {
                case "1" -> getProducts();
                case "2" -> getProductDetail();
                case "3" -> menuPanier();
                case "4" -> checkout();
                case "5" -> payment();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Choix invalide !");
            }
        }
    }

    // ── PRODUITS ─────────────────────────────
    private void getProducts() {
        String response = sendRequest("GET_PRODUCTS");
        System.out.println(response);
    }

    private void getProductDetail() {
        System.out.print("ID produit : ");
        String id = scanner.nextLine();

        String response = sendRequest("GET_PRODUCT:" + id);
        System.out.println(response);
    }

    // ── PANIER ───────────────────────────────
    private void menuPanier() {
        while (true) {
            System.out.println("\n=== PANIER ===");
            System.out.println("1. Ajouter");
            System.out.println("2. Supprimer");
            System.out.println("3. Voir panier");
            System.out.println("0. Retour");

            String choix = scanner.nextLine();

            switch (choix) {
                case "1" -> addToCart();
                case "2" -> removeFromCart();
                case "3" -> getCart();
                case "0" -> { return; }
            }
        }
    }

    private void addToCart() {
        System.out.print("ID produit : ");
        String productId = scanner.nextLine();

        System.out.print("Quantité : ");
        String qty = scanner.nextLine();

        String response = sendRequest("CART_ADD:" + clientId + ":" + productId + ":" + qty);
        System.out.println(response);
    }

    private void removeFromCart() {
        System.out.print("ID produit : ");
        String productId = scanner.nextLine();

        String response = sendRequest("CART_REMOVE:" + clientId + ":" + productId);
        System.out.println(response);
    }

    private void getCart() {
        String response = sendRequest("CART_GET:" + clientId);
        System.out.println(response);
    }

    // ── CHECKOUT ─────────────────────────────
    private void checkout() {
        String response = sendRequest("CHECKOUT:" + clientId);
        System.out.println(response);
    }

    // ── PAYMENT ──────────────────────────────
    private void payment() {
        System.out.print("UUID commande : ");
        String uuid = scanner.nextLine();

        System.out.print("Méthode (card/especes) : ");
        String method = scanner.nextLine();

        String response = sendRequest("PAYMENT:" + uuid + ":" + method);
        System.out.println(response);
    }

    // ── FERMETURE ────────────────────────────
    private void closeConnection() {
        try {
            socket.close();
            scanner.close();
        } catch (Exception e) {
            System.out.println("Erreur fermeture");
        }
    }

    // ── MAIN ─────────────────────────────────
    public static void main(String[] args) {
        ClientApp client = new ClientApp();

        if (client.connectToServer()) {
            client.showMainMenu();
        }
    }
}