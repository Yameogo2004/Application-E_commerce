package dao;

import database.DatabaseConnection;
import model.Order;
import model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private final Connection conn;

    public OrderDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Sauvegarde une commande avec ses items
     * @param order Commande à sauvegarder
     * @param clientId ID du client
     */
    public void save(Order order, int clientId) throws SQLException {
        String sql = "INSERT INTO orders (order_uuid, client_id, total_price, status, created_at) VALUES (?, ?, ?, ?, ?)";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderUUID());
                ps.setInt(2, clientId);
                ps.setDouble(3, order.getTotalPrice());
                ps.setString(4, order.getStatus());
                ps.setTimestamp(5, Timestamp.valueOf(order.getCreatedAt()));
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    order.setId(rs.getInt(1));
                }
            }

            // Sauvegarder les items de la commande
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                OrderItemDAO itemDAO = new OrderItemDAO(conn);
                for (OrderItem item : order.getItems()) {
                    itemDAO.save(item, order.getId());
                }
            }

            conn.commit();

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Récupère toutes les commandes d'un client
     * @param clientId ID du client
     * @return Liste des commandes
     */
    public List<Order> findByClient(int clientId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE client_id = ? ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }
        }

        return orders;
    }

    /**
     * Récupère une commande par son UUID
     * @param uuid UUID de la commande
     * @return Commande ou null
     */
    public Order findByUUID(String uuid) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_uuid = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapOrder(rs);
            }
        }

        return null;
    }

    /**
     * Met à jour le statut d'une commande
     * @param orderId ID de la commande
     * @param newStatus Nouveau statut
     */
    public void updateStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    /**
     * Récupère toutes les commandes (avec infos client)
     * @return Liste de toutes les commandes
     */
    public List<Order> findAll() throws SQLException {
        List<Order> orders = new ArrayList<>();

        String sql = """
            SELECT 
                o.*,
                u.nom AS client_nom,
                u.prenom AS client_prenom,
                u.email AS client_email
            FROM orders o
            LEFT JOIN users u ON o.client_id = u.id
            ORDER BY o.id DESC
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Order order = mapOrder(rs);
                
                // Ajouter les informations client (pour l'affichage admin)
                order.setClientId(rs.getInt("client_id"));
                order.setClientNom(rs.getString("client_nom"));
                order.setClientPrenom(rs.getString("client_prenom"));
                order.setClientEmail(rs.getString("client_email"));

                orders.add(order);
            }
        }

        return orders;
    }

    /**
     * Supprime une commande et ses items
     * @param orderId ID de la commande
     */
    public void delete(int orderId) throws SQLException {
        try {
            conn.setAutoCommit(false);

            // Supprimer d'abord les items
            OrderItemDAO itemDAO = new OrderItemDAO(conn);
            itemDAO.deleteByOrderId(orderId);

            // Puis supprimer la commande
            String sql = "DELETE FROM orders WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Map un ResultSet en objet Order
     * @param rs ResultSet
     * @return Order
     */
    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setClientId(rs.getInt("client_id"));
        order.setOrderUUID(rs.getString("order_uuid"));
        order.setTotalPrice(rs.getDouble("total_price"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Charger les items
        OrderItemDAO itemDAO = new OrderItemDAO(conn);
        List<OrderItem> items = itemDAO.findByOrder(order.getId());
        order.setItems(items);

        return order;
    }
}