package service;

import dao.CartDAO;
import model.Cart;
import model.CartItem;

public class CartService {

    private final CartDAO cartDAO;

    public CartService() {
        this.cartDAO = new CartDAO();
    }

    public Cart getCartByClient(int clientId) {
        return cartDAO.getCartByClient(clientId);
    }

    public boolean addItemToCart(int clientId, CartItem item) {
        Cart cart = cartDAO.getCartByClient(clientId);

        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        if (item == null || item.getProduct() == null) {
            System.out.println("Article invalide.");
            return false;
        }

        if (item.getQuantity() <= 0) {
            System.out.println("Quantité invalide.");
            return false;
        }

        return cartDAO.addItem(cart.getId(), item);
    }

    public boolean removeItemFromCart(int clientId, int productId) {
        Cart cart = cartDAO.getCartByClient(clientId);

        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        return cartDAO.removeItem(cart.getId(), productId);
    }

    // ✅ Nouveau
    public boolean removeItemFromCartByName(int clientId, String productName) {
        Cart cart = cartDAO.getCartByClient(clientId);

        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        if (productName == null || productName.trim().isEmpty()) {
            System.out.println("Nom produit invalide.");
            return false;
        }

        return cartDAO.removeItemByProductName(cart.getId(), productName);
    }

    public boolean clearCart(int clientId) {
        Cart cart = cartDAO.getCartByClient(clientId);

        if (cart == null) {
            System.out.println("Panier introuvable.");
            return false;
        }

        return cartDAO.clear(cart.getId());
    }

    public double calculateCartTotal(int clientId) {
        Cart cart = cartDAO.getCartByClient(clientId);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return 0.0;
        }

        return cart.calculateTotal();
    }

    public boolean isCartEmpty(int clientId) {
        Cart cart = cartDAO.getCartByClient(clientId);
        return cart == null || cart.getItems() == null || cart.getItems().isEmpty();
    }
}