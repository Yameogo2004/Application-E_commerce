package dao;

import database.DatabaseConnection;
import model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDAO {

    private final Connection conn;

    /**
     * Constructeur par défaut - utilise la connexion unique de DatabaseConnection
     */
    public OrderItemDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Constructeur avec injection de connexion (utile pour les tests)
     * @param conn Connexion déjà établie
     */
    public OrderItemDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Sauvegarde un item de commande
     * @param item OrderItem à sauvegarder
     * @param orderId ID de la commande associée
     */
    public void save(OrderItem item, int orderId) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setDouble(4, item.getPrice());
            ps.executeUpdate();

            // Récupérer l'ID généré si nécessaire
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                item.setId(rs.getInt(1));
            }
        }
    }

    /**
     * Récupère tous les items d'une commande
     * @param orderId ID de la commande
     * @return Liste des items
     */
    public List<OrderItem> findByOrder(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ? ORDER BY id ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                OrderItem item = new OrderItem(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_price")
                );
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Supprime un item de commande
     * @param orderItemId ID de l'item
     */
    public void delete(int orderItemId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime tous les items d'une commande
     * @param orderId ID de la commande
     */
    public void deleteByOrderId(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}