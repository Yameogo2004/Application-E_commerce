package dao;

import database.DatabaseConnection;
import model.DashboardSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardDAO {

    public DashboardSummary getDashboardSummary(int lowStockThreshold) {
        DashboardSummary summary = new DashboardSummary();

        try (Connection conn = DatabaseConnection.getConnection()) {

            summary.setTotalProducts(queryInt(conn, "SELECT COUNT(*) FROM products"));
            summary.setLowStockProducts(queryInt(conn,
                    "SELECT COUNT(*) FROM products WHERE stock > 0 AND stock <= ?", lowStockThreshold));
            summary.setOutOfStockProducts(queryInt(conn,
                    "SELECT COUNT(*) FROM products WHERE stock <= 0"));

            summary.setTotalUsers(queryInt(conn,
                    "SELECT COUNT(*) FROM users"));

            summary.setTotalOrders(queryInt(conn,
                    "SELECT COUNT(*) FROM orders"));

            summary.setPendingOrders(queryInt(conn,
                    "SELECT COUNT(*) FROM orders WHERE status = 'pending'"));

            summary.setPaidOrders(queryInt(conn,
                    "SELECT COUNT(*) FROM orders WHERE status = 'paid'"));

            summary.setTodayRevenue(queryDouble(conn,
                    "SELECT COALESCE(SUM(amount), 0) FROM payments " +
                            "WHERE status = 'success' AND DATE(paid_at) = CURDATE()"));

            summary.setMonthRevenue(queryDouble(conn,
                    "SELECT COALESCE(SUM(amount), 0) FROM payments " +
                            "WHERE status = 'success' " +
                            "AND YEAR(paid_at) = YEAR(CURDATE()) " +
                            "AND MONTH(paid_at) = MONTH(CURDATE())"));

            summary.setUnreadNotifications(queryInt(conn,
                    "SELECT COUNT(*) FROM notifications WHERE is_read = 0"));

        } catch (SQLException e) {
            System.out.println("Erreur DashboardDAO getDashboardSummary : " + e.getMessage());
        }

        return summary;
    }

    private int queryInt(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private double queryDouble(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0.0;
        }
    }

    private void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}