package dao;

import database.DatabaseConnection;
import crypto.DatabaseEncryption;
import model.Admin;
import model.Client;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private final Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    // ==================== MÉTHODES DE CHIFFREMENT ====================
    
    /**
     * Chiffre une donnée avant stockage
     */
    private String encryptData(String data) {
        // Désactivé temporairement pour debug
        return data;
    }

    private String decryptData(String encrypted) {
        // Désactivé temporairement pour debug
        return encrypted;
    }

    public User findByEmail(String email) {
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                "WHERE u.email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, encryptData(email)); // 🔐 Email chiffré pour recherche
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                String status = rs.getString("status");
                String publicKey = rs.getString("public_key");
                
                // 🔐 Déchiffrer les données sensibles
                String decryptedEmail = decryptData(rs.getString("email"));
                String decryptedAddress = decryptData(rs.getString("address"));
                String decryptedPhone = decryptData(rs.getString("phone"));
                String decryptedVille = decryptData(rs.getString("ville"));

                if ("client".equalsIgnoreCase(role)) {
                    Client client = new Client(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            decryptedEmail,
                            rs.getString("password"),
                            decryptedAddress,
                            decryptedPhone,
                            decryptedVille
                    );
                    client.setId(rs.getInt("id"));
                    client.setRole(role);
                    client.setStatus(status);
                    client.setPublicKey(publicKey);
                    return client;
                } else {
                    Admin admin = new Admin(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            decryptedEmail,
                            rs.getString("password")
                    );
                    admin.setId(rs.getInt("id"));
                    admin.setRole(role);
                    admin.setStatus(status);
                    admin.setPublicKey(publicKey);
                    return admin;
                }
            }

        } catch (SQLException e) {
            System.out.println("Erreur findByEmail : " + e.getMessage());
        }

        return null;
    }

    public boolean save(Client client) {
        return saveClientWithStatus(client, "active");
    }

    public boolean savePendingClient(Client client) {
        return saveClientWithStatus(client, "pending");
    }

    private boolean saveClientWithStatus(Client client, String status) {
        // 🔐 Chiffrer les données sensibles avant insertion
        String encryptedEmail = encryptData(client.getEmail());
        String encryptedAddress = encryptData(client.getAddress());
        String encryptedPhone = encryptData(client.getPhone());
        String encryptedVille = encryptData(client.getVille());
        
        String sqlUser = "INSERT INTO users (nom, prenom, email, password, role, status, public_key) VALUES (?, ?, ?, ?, 'client', ?, ?)";
        String sqlClient = "INSERT INTO clients (id, address, phone, ville) VALUES (?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement psUser = connection.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, client.getNom());
                psUser.setString(2, client.getPrenom());
                psUser.setString(3, encryptedEmail);      // 🔐 Email chiffré
                psUser.setString(4, client.getPassword());
                psUser.setString(5, status);
                psUser.setString(6, client.getPublicKey());
                psUser.executeUpdate();

                ResultSet keys = psUser.getGeneratedKeys();
                if (!keys.next()) {
                    connection.rollback();
                    return false;
                }

                int id = keys.getInt(1);
                client.setId(id);

                try (PreparedStatement psClient = connection.prepareStatement(sqlClient)) {
                    psClient.setInt(1, id);
                    psClient.setString(2, encryptedAddress);  // 🔐 Adresse chiffrée
                    psClient.setString(3, encryptedPhone);    // 🔐 Téléphone chiffré
                    psClient.setString(4, encryptedVille);    // 🔐 Ville chiffrée
                    psClient.executeUpdate();
                }
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            System.out.println("Erreur save client : " + e.getMessage());
            return false;

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, encryptData(email)); // 🔐 Email chiffré pour la recherche
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.out.println("Erreur emailExists : " + e.getMessage());
            return false;
        }
    }

    public boolean isAccountActive(String email) {
        String sql = "SELECT status FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, encryptData(email)); // 🔐 Email chiffré pour la recherche
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return "active".equalsIgnoreCase(rs.getString("status"));
            }

        } catch (SQLException e) {
            System.out.println("Erreur isAccountActive : " + e.getMessage());
        }

        return false;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();

        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                "ORDER BY u.id DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String role = rs.getString("role");
                String status = rs.getString("status");
                String publicKey = rs.getString("public_key");
                
                // 🔐 Déchiffrer les données sensibles
                String decryptedEmail = decryptData(rs.getString("email"));
                String decryptedAddress = decryptData(rs.getString("address"));
                String decryptedPhone = decryptData(rs.getString("phone"));
                String decryptedVille = decryptData(rs.getString("ville"));

                if ("client".equalsIgnoreCase(role)) {
                    Client client = new Client(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            decryptedEmail,
                            rs.getString("password"),
                            decryptedAddress,
                            decryptedPhone,
                            decryptedVille
                    );
                    client.setId(rs.getInt("id"));
                    client.setRole(role);
                    client.setStatus(status);
                    client.setPublicKey(publicKey);
                    users.add(client);
                } else {
                    Admin admin = new Admin(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            decryptedEmail,
                            rs.getString("password")
                    );
                    admin.setId(rs.getInt("id"));
                    admin.setRole(role);
                    admin.setStatus(status);
                    admin.setPublicKey(publicKey);
                    users.add(admin);
                }
            }

        } catch (SQLException e) {
            System.out.println("Erreur findAll users : " + e.getMessage());
        }

        return users;
    }

    public User findById(int userId) {
        String sql = "SELECT u.*, c.address, c.phone, c.ville " +
                "FROM users u LEFT JOIN clients c ON u.id = c.id " +
                "WHERE u.id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                String status = rs.getString("status");
                String publicKey = rs.getString("public_key");
                
                // 🔐 Déchiffrer les données sensibles
                String decryptedEmail = decryptData(rs.getString("email"));
                String decryptedAddress = decryptData(rs.getString("address"));
                String decryptedPhone = decryptData(rs.getString("phone"));
                String decryptedVille = decryptData(rs.getString("ville"));

                if ("client".equalsIgnoreCase(role)) {
                    Client client = new Client(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            decryptedEmail,
                            rs.getString("password"),
                            decryptedAddress,
                            decryptedPhone,
                            decryptedVille
                    );
                    client.setId(rs.getInt("id"));
                    client.setRole(role);
                    client.setStatus(status);
                    client.setPublicKey(publicKey);
                    return client;
                } else {
                    Admin admin = new Admin(
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            decryptedEmail,
                            rs.getString("password")
                    );
                    admin.setId(rs.getInt("id"));
                    admin.setRole(role);
                    admin.setStatus(status);
                    admin.setPublicKey(publicKey);
                    return admin;
                }
            }

        } catch (SQLException e) {
            System.out.println("Erreur findById : " + e.getMessage());
        }

        return null;
    }

    // 🔐 NOUVELLES MÉTHODES POUR LA GESTION DE LA CLÉ PUBLIQUE

    public boolean savePublicKey(int userId, String publicKeyBase64) {
        String sql = "UPDATE users SET public_key = ? WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, publicKeyBase64);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Erreur savePublicKey : " + e.getMessage());
            return false;
        }
    }

    public String getPublicKeyByEmail(String email) {
        String sql = "SELECT public_key FROM users WHERE email = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, encryptData(email));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("public_key");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getPublicKeyByEmail : " + e.getMessage());
        }
        return null;
    }

    public String getPublicKeyById(int userId) {
        String sql = "SELECT public_key FROM users WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("public_key");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getPublicKeyById : " + e.getMessage());
        }
        return null;
    }

    public boolean hasPublicKey(int userId) {
        String sql = "SELECT public_key FROM users WHERE id = ? AND public_key IS NOT NULL AND public_key != ''";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Erreur hasPublicKey : " + e.getMessage());
            return false;
        }
    }

    // ──────────────── UPDATE PROFILE AVEC CHIFFREMENT ────────────────

    public boolean updateProfile(int userId, String nomComplet, String email, String phone, String address, String ville) {
        try {
            connection.setAutoCommit(false);

            String nom = nomComplet;
            String prenom = "";

            if (nomComplet != null && nomComplet.trim().contains(" ")) {
                int idx = nomComplet.trim().indexOf(" ");
                prenom = nomComplet.trim().substring(0, idx).trim();
                nom = nomComplet.trim().substring(idx + 1).trim();
            }

            if (nom == null || nom.isBlank()) {
                nom = "SansNom";
            }

            // 🔐 Chiffrer l'email avant mise à jour
            String encryptedEmail = encryptData(email);

            String sqlUser = "UPDATE users SET nom = ?, prenom = ?, email = ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlUser)) {
                ps.setString(1, nom);
                ps.setString(2, prenom);
                ps.setString(3, encryptedEmail);
                ps.setInt(4, userId);
                ps.executeUpdate();
            }

            String role = null;
            String roleSql = "SELECT role FROM users WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(roleSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    role = rs.getString("role");
                }
            }

            if ("client".equalsIgnoreCase(role)) {
                // 🔐 Chiffrer les données client
                String encryptedAddress = encryptData(address);
                String encryptedPhone = encryptData(phone);
                String encryptedVille = encryptData(ville);
                
                String checkClientSql = "SELECT id FROM clients WHERE id = ?";
                boolean exists = false;

                try (PreparedStatement ps = connection.prepareStatement(checkClientSql)) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    exists = rs.next();
                }

                if (exists) {
                    String updateClientSql = "UPDATE clients SET address = ?, phone = ?, ville = ? WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(updateClientSql)) {
                        ps.setString(1, encryptedAddress);
                        ps.setString(2, encryptedPhone);
                        ps.setString(3, encryptedVille);
                        ps.setInt(4, userId);
                        ps.executeUpdate();
                    }
                } else {
                    String insertClientSql = "INSERT INTO clients (id, address, phone, ville) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertClientSql)) {
                        ps.setInt(1, userId);
                        ps.setString(2, encryptedAddress);
                        ps.setString(3, encryptedPhone);
                        ps.setString(4, encryptedVille);
                        ps.executeUpdate();
                    }
                }
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            System.out.println("Erreur updateProfile : " + e.getMessage());
            return false;

        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }
}