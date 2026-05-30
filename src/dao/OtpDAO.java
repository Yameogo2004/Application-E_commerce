package dao;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OtpDAO {

    private final Connection connection;

    public OtpDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public boolean saveOtp(String email, String code) {
        deleteOtpByEmail(email);

        String sql = "INSERT INTO otp_codes (email, code, created_at, expires_at, used) " +
                "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 10 MINUTE), FALSE)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur saveOtp : " + e.getMessage());
            return false;
        }
    }

    public boolean verifyOtp(String email, String code) {
        String sql = "SELECT id FROM otp_codes " +
                "WHERE email = ? AND code = ? " +
                "AND used = FALSE " +
                "AND expires_at > NOW()";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                markAsUsed(id);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Erreur verifyOtp : " + e.getMessage());
        }

        return false;
    }

    private void markAsUsed(int id) {
        String sql = "UPDATE otp_codes SET used = TRUE WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur markAsUsed : " + e.getMessage());
        }
    }

    public void deleteOtpByEmail(String email) {
        String sql = "DELETE FROM otp_codes WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur deleteOtpByEmail : " + e.getMessage());
        }
    }

    public boolean activateAccount(String email) {
        String sql = "UPDATE users SET status = 'active' WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Erreur activateAccount : " + e.getMessage());
            return false;
        }
    }

    public boolean userExists(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Erreur userExists : " + e.getMessage());
            return false;
        }
    }
}