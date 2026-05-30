package dao;

import database.DatabaseConnection;
import model.Cart;
import model.CartItem;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    private final Connection conn;

    public CartDAO() {
        this.conn = DatabaseConnection.getConnection();
    }

    public Cart getCartByClient(int clientId) {
        String sqlCart = "SELECT * FROM carts WHERE client_id = ?";
        String sqlItems = "SELECT ci.id, ci.quantity, p.id_product, p.name, p.description, p.image, p.price, p.stock " +
                          "FROM cart_items ci " +
                          "LEFT JOIN products p ON ci.product_id = p.id_product " +
                          "WHERE ci.cart_id = ?";

        try (PreparedStatement psCart = conn.prepareStatement(sqlCart)) {
            psCart.setInt(1, clientId);
            ResultSet rsCart = psCart.executeQuery();

            Cart cart = null;

            if (rsCart.next()) {
                cart = new Cart();
                cart.setId(rsCart.getInt("id"));
                cart.setClientId(rsCart.getInt("client_id"));

                Timestamp createdAt = rsCart.getTimestamp("created_at");
                if (createdAt != null) {
                    cart.setCreatedAt(createdAt.toString());
                }
            } else {
                int newCartId = createCart(clientId);
                if (newCartId != -1) {
                    cart = new Cart();
                    cart.setId(newCartId);
                    cart.setClientId(clientId);
                }
            }

            if (cart != null) {
                List<CartItem> items = new ArrayList<>();

                try (PreparedStatement psItems = conn.prepareStatement(sqlItems)) {
                    psItems.setInt(1, cart.getId());
                    ResultSet rsItems = psItems.executeQuery();

                    while (rsItems.next()) {
                        CartItem item = new CartItem();
                        item.setId(rsItems.getInt("id"));
                        item.setQuantity(rsItems.getInt("quantity"));

                        int productId = rsItems.getInt("id_product");
                        if (!rsItems.wasNull()) {
                            Product product = new Product(
                                    productId,
                                    rsItems.getString("name"),
                                    rsItems.getString("description"),
                                    rsItems.getString("image"),
                                    rsItems.getDouble("price"),
                                    rsItems.getInt("stock")
                            );
                            item.setProduct(product);
                        }

                        items.add(item);
                    }
                }

                cart.setItems(items);
            }

            return cart;

        } catch (SQLException e) {
            System.out.println("Erreur getCartByClient : " + e.getMessage());
            return null;
        }
    }

    private int createCart(int clientId) {
        String sql = "INSERT INTO carts (client_id) VALUES (?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientId);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.out.println("Erreur createCart : " + e.getMessage());
        }

        return -1;
    }

    public boolean addItem(int cartId, CartItem item) {
        if (item == null || item.getProduct() == null) {
            System.out.println("Erreur addItem : produit invalide.");
            return false;
        }

        String checkSql = "SELECT * FROM cart_items WHERE cart_id = ? AND product_id = ?";
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE cart_id = ? AND product_id = ?";
        String insertSql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)";

        try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, cartId);
            psCheck.setInt(2, item.getProduct().getIdProduct());

            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, item.getQuantity());
                    psUpdate.setInt(2, cartId);
                    psUpdate.setInt(3, item.getProduct().getIdProduct());
                    psUpdate.executeUpdate();
                }
            } else {
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, cartId);
                    psInsert.setInt(2, item.getProduct().getIdProduct());
                    psInsert.setInt(3, item.getQuantity());
                    psInsert.executeUpdate();
                }
            }

            return true;

        } catch (SQLException e) {
            System.out.println("Erreur addItem : " + e.getMessage());
            return false;
        }
    }

    public boolean removeItem(int cartId, int productId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Erreur removeItem : " + e.getMessage());
            return false;
        }
    }

    // ✅ optionnel mais utile
    public boolean removeItemByProductName(int cartId, String productName) {
        String sql = "DELETE ci FROM cart_items ci " +
                     "JOIN products p ON ci.product_id = p.id_product " +
                     "WHERE ci.cart_id = ? AND p.name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setString(2, productName);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Erreur removeItemByProductName : " + e.getMessage());
            return false;
        }
    }

    public boolean clear(int cartId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Erreur clear : " + e.getMessage());
            return false;
        }
    }
}