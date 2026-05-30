package model;

import java.util.ArrayList;
import java.util.List;

public class Cart {

    private int id;
    private int clientId;
    private List<CartItem> items;
    private String createdAt;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public Cart(int id, int clientId, String createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.createdAt = createdAt;
        this.items = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    } 

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    } 

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    } 

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void addProduct(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Le produit ne peut pas être null.");
        }

        for (CartItem item : items) {
            if (item.getProduct() != null &&
                item.getProduct().getIdProduct() == product.getIdProduct()) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }

        CartItem newItem = new CartItem(items.size() + 1, product, quantity);
        items.add(newItem);
    }

    public boolean removeProduct(int productId) {
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            if (item.getProduct() != null &&
                item.getProduct().getIdProduct() == productId) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }

    public double calculateTotal() {
        double total = 0.0;
        for (CartItem item : items) {
            total += item.calculateSubtotal();
        }
        return total;
    }

    public void clearCart() {
        items.clear();
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}