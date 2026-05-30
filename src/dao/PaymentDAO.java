package dao;

import database.DatabaseConnection;
import model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class PaymentDAO {

    private Connection conn;

    public PaymentDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    // ── Enregistrer un paiement ───────────────────────────────
    public boolean save(Payment payment) {
        String sql = "INSERT INTO payments (order_id, method, amount, status, paid_at) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getOrderId());
            ps.setString(2, payment.getMethod());
            ps.setDouble(3, payment.getAmount());
            ps.setString(4, payment.getStatus());

            if (payment.getPaidAt() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(payment.getPaidAt()));
            } else {
                ps.setTimestamp(5, null);
            }

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    payment.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.out.println("Erreur save payment : " + e.getMessage());
        }

        return false;
    }
}