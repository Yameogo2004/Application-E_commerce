package dao;

import database.DatabaseConnection;
import model.Category;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private final Connection conn;

    public ProductDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    /**
     * Récupère tous les produits
     * @return Liste de tous les produits
     */
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();

        String sql = "SELECT p.id_product, p.name, p.description, p.price, p.stock, p.image, p.created_at, " +
                "p.category_id, c.id AS c_id, c.name AS c_name, c.description AS c_description " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "ORDER BY p.id_product DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                products.add(mapProduct(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO.findAll : " + e.getMessage());
        }

        return products;
    }

    /**
     * Récupère un produit par son ID
     * @param id ID du produit
     * @return Produit ou null
     */
    public Product findById(int id) {
        String sql = "SELECT p.id_product, p.name, p.description, p.price, p.stock, p.image, p.created_at, " +
                "p.category_id, c.id AS c_id, c.name AS c_name, c.description AS c_description " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE p.id_product = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapProduct(rs);
            }

        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO.findById : " + e.getMessage());
        }

        return null;
    }

    /**
     * Récupère un produit par son nom
     * @param name Nom du produit
     * @return Produit ou null
     */
    public Product findByName(String name) {
        String sql = "SELECT p.id_product, p.name, p.description, p.price, p.stock, p.image, p.created_at, " +
                "p.category_id, c.id AS c_id, c.name AS c_name, c.description AS c_description " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE p.name = ? " +
                "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapProduct(rs);
            }

        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO.findByName : " + e.getMessage());
        }

        return null;
    }

    /**
     * Sauvegarde un nouveau produit
     * @param product Produit à sauvegarder
     * @return true si réussi
     */
    public boolean save(Product product) {
        String sql = "INSERT INTO products (name, description, price, stock, image, category_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getStock());
            ps.setString(5, product.getImage());

            if (product.getCategory() != null) {
                ps.setInt(6, product.getCategory().getId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    product.setIdProduct(rs.getInt(1));
                }
            }

            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO.save : " + e.getMessage());
            return false;
        }
    }

    /**
     * Met à jour un produit existant
     * @param product Produit à mettre à jour
     * @return true si réussi
     */
    public boolean update(Product product) {
        String sql = "UPDATE products " +
                "SET name = ?, description = ?, price = ?, stock = ?, image = ?, category_id = ? " +
                "WHERE id_product = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getStock());
            ps.setString(5, product.getImage());

            if (product.getCategory() != null) {
                ps.setInt(6, product.getCategory().getId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setInt(7, product.getIdProduct());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO.update : " + e.getMessage());
            return false;
        }
    }

    /**
     * Met à jour uniquement le stock d'un produit
     * @param productId ID du produit
     * @param newStock Nouveau stock
     * @return true si réussi
     */
    public boolean updateStock(int productId, int newStock) {
        String sql = "UPDATE products SET stock = ? WHERE id_product = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO.updateStock : " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime un produit
     * @param id ID du produit
     * @return true si réussi
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id_product = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Erreur ProductDAO.delete : " + e.getMessage());
            return false;
        }
    }

    /**
     * Convertit un ResultSet en objet Product
     * @param rs ResultSet
     * @return Product
     */
    private Product mapProduct(ResultSet rs) throws SQLException {
        Product product = new Product(
                rs.getInt("id_product"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("image"),
                rs.getDouble("price"),
                rs.getInt("stock")
        );

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            product.setCreatedAt(created.toLocalDateTime());
        }

        int categoryId = rs.getInt("c_id");
        if (!rs.wasNull()) {
            Category category = new Category(
                    categoryId,
                    rs.getString("c_name"),
                    rs.getString("c_description")
            );
            product.setCategory(category);
        }

        return product;
    }
}