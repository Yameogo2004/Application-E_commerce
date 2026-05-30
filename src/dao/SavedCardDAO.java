package dao;

import database.DatabaseConnection;
import model.SavedCard;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavedCardDAO {

    private final Connection connection;

    public SavedCardDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public boolean save(SavedCard card) {
        String sql = "INSERT INTO saved_cards (user_id, card_last4, card_brand, card_holder_name, expiry_month, expiry_year, is_default) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, card.getUserId());
            ps.setString(2, card.getCardLast4());
            ps.setString(3, card.getCardBrand());
            ps.setString(4, card.getCardHolderName());
            ps.setInt(5, card.getExpiryMonth());
            ps.setInt(6, card.getExpiryYear());
            ps.setBoolean(7, card.isDefault());
            
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    card.setId(rs.getInt(1));
                }
            }
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Erreur save saved_card: " + e.getMessage());
            return false;
        }
    }

    public List<SavedCard> findByUserId(int userId) {
        List<SavedCard> cards = new ArrayList<>();
        String sql = "SELECT * FROM saved_cards WHERE user_id = ? ORDER BY is_default DESC, created_at DESC";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                cards.add(mapCard(rs));
            }
        } catch (SQLException e) {
            System.out.println("Erreur findByUserId: " + e.getMessage());
        }
        return cards;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM saved_cards WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur delete: " + e.getMessage());
            return false;
        }
    }

    public boolean setDefault(int id, int userId) {
        // D'abord, retirer le default de toutes les cartes de l'utilisateur
        String resetSql = "UPDATE saved_cards SET is_default = FALSE WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(resetSql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur reset default: " + e.getMessage());
        }
        
        // Puis mettre la carte sélectionnée en default
        String sql = "UPDATE saved_cards SET is_default = TRUE WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur setDefault: " + e.getMessage());
            return false;
        }
    }

    private SavedCard mapCard(ResultSet rs) throws SQLException {
        return new SavedCard(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("card_last4"),
            rs.getString("card_brand"),
            rs.getString("card_holder_name"),
            rs.getInt("expiry_month"),
            rs.getInt("expiry_year"),
            rs.getBoolean("is_default"),
            rs.getString("created_at")
        );
    }
}