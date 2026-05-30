package dao;

import database.DatabaseConnection;
import model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public boolean save(Notification notification) {
        String sql = "INSERT INTO notifications " +
                "(title, message, type, level, is_read, entity_type, entity_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, notification.getTitle());
            ps.setString(2, notification.getMessage());
            ps.setString(3, notification.getType());
            ps.setString(4, notification.getLevel());
            ps.setBoolean(5, notification.isRead());
            ps.setString(6, notification.getEntityType());

            if (notification.getEntityId() != null) {
                ps.setInt(7, notification.getEntityId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.setTimestamp(8, Timestamp.valueOf(notification.getCreatedAt()));

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    notification.setId(rs.getInt(1));
                }
            }

            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Erreur NotificationDAO save : " + e.getMessage());
            return false;
        }
    }

    public List<Notification> findAll() {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications ORDER BY is_read ASC, created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(mapNotification(rs));
            }

        } catch (SQLException e) {
            System.out.println("Erreur NotificationDAO findAll : " + e.getMessage());
        }

        return notifications;
    }

    public List<Notification> findUnread() {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE is_read = 0 ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(mapNotification(rs));
            }

        } catch (SQLException e) {
            System.out.println("Erreur NotificationDAO findUnread : " + e.getMessage());
        }

        return notifications;
    }

    public int countUnread() {
        String sql = "SELECT COUNT(*) FROM notifications WHERE is_read = 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.out.println("Erreur NotificationDAO countUnread : " + e.getMessage());
        }

        return 0;
    }

    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Erreur NotificationDAO markAsRead : " + e.getMessage());
            return false;
        }
    }

    public boolean markUnreadProductNotificationsAsRead(int productId) {
        String sql = "UPDATE notifications " +
                "SET is_read = 1 " +
                "WHERE entity_type = 'PRODUCT' AND entity_id = ? AND is_read = 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Erreur NotificationDAO markUnreadProductNotificationsAsRead : " + e.getMessage());
            return false;
        }
    }

    public boolean existsForEntity(String type, String entityType, int entityId) {
        String sql = "SELECT COUNT(*) FROM notifications " +
                "WHERE type = ? AND entity_type = ? AND entity_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type);
            ps.setString(2, entityType);
            ps.setInt(3, entityId);

            ResultSet rs = ps.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.out.println("Erreur NotificationDAO existsForEntity : " + e.getMessage());
            return false;
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");

        return new Notification(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("type"),
                rs.getString("level"),
                rs.getBoolean("is_read"),
                rs.getString("entity_type"),
                (Integer) rs.getObject("entity_id"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}