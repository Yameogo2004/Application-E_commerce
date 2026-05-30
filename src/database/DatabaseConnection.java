package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/chrionline?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    private DatabaseConnection() {
        // Constructeur privé pour empêcher l'instanciation
    }

    /**
     * Récupère la connexion à la base de données
     * @return Connection
     * @throws RuntimeException si la connexion échoue
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion à MySQL réussie !");
            }
            return connection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver MySQL introuvable.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de se connecter à la base de données.", e);
        }
    }

    /**
     * Ferme la connexion à la base de données
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                System.out.println("🔒 Connexion BDD fermée.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
}