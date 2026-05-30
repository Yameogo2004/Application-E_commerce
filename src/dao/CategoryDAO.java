package dao;

import database.DatabaseConnection;
import model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private final Connection conn;

    public CategoryDAO() throws SQLException {
        this.conn = DatabaseConnection.getConnection();
    }

    public void save(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                category.setId(rs.getInt(1));
            }
        }
    }

    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int categoryId) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }

    public List<Category> findAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY id DESC";

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                ));
            }
        }

        return categories;
    }

    public Category findById(int categoryId) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                );
            }
        }

        return null;
    }
}