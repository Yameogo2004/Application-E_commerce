package dao;

import database.DatabaseConnection;
import model.StockMovement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDAO {

    public boolean save(StockMovement movement) {
        String sql = "INSERT INTO stock_movements " +
                "(product_id, movement_type, quantity, previous_stock, new_stock, reason, admin_user_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, movement.getProductId());
            ps.setString(2, movement.getMovementType());
            ps.setInt(3, movement.getQuantity());
            ps.setInt(4, movement.getPreviousStock());
            ps.setInt(5, movement.getNewStock());
            ps.setString(6, movement.getReason());

            if (movement.getAdminUserId() != null) {
                ps.setInt(7, movement.getAdminUserId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.setTimestamp(8, Timestamp.valueOf(movement.getCreatedAt()));

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    movement.setId(rs.getInt(1));
                }
            }

            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Erreur StockMovementDAO save : " + e.getMessage());
            return false;
        }
    }

    public List<StockMovement> findAll() {
        List<StockMovement> movements = new ArrayList<>();

        String sql = "SELECT sm.*, p.name AS product_name " +
                "FROM stock_movements sm " +
                "INNER JOIN products p ON p.id_product = sm.product_id " +
                "ORDER BY sm.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                movements.add(mapMovement(rs));
            }

        } catch (SQLException e) {
            System.out.println("Erreur StockMovementDAO findAll : " + e.getMessage());
        }

        return movements;
    }

    public List<StockMovement> findByProductId(int productId) {
        List<StockMovement> movements = new ArrayList<>();

        String sql = "SELECT sm.*, p.name AS product_name " +
                "FROM stock_movements sm " +
                "INNER JOIN products p ON p.id_product = sm.product_id " +
                "WHERE sm.product_id = ? " +
                "ORDER BY sm.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                movements.add(mapMovement(rs));
            }

        } catch (SQLException e) {
            System.out.println("Erreur StockMovementDAO findByProductId : " + e.getMessage());
        }

        return movements;
    }

    private StockMovement mapMovement(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");

        return new StockMovement(
                rs.getInt("id"),
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("movement_type"),
                rs.getInt("quantity"),
                rs.getInt("previous_stock"),
                rs.getInt("new_stock"),
                rs.getString("reason"),
                (Integer) rs.getObject("admin_user_id"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}