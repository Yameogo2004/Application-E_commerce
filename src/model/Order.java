package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order {

    private int id;
    private String orderUUID;
    private double totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItem> items;
    private Payment payment;
    
    // ==================== INFORMATIONS CLIENT (pour l'admin) ====================
    private int clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;

    public Order() {
        this.id = 0;
        this.orderUUID = UUID.randomUUID().toString();
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.totalPrice = 0.0;
        this.items = new ArrayList<>();
    }

    public Order(int id) {
        this.id = id;
        this.orderUUID = UUID.randomUUID().toString();
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.totalPrice = 0.0;
        this.items = new ArrayList<>();
    }

    // ==================== GETTERS / SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    } 

    public String getOrderUUID() {
        return orderUUID;
    }

    public void setOrderUUID(String orderUUID) {
        this.orderUUID = orderUUID;
    } 

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    } 

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    } 

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    } 

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        calculTotal();
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    // ==================== INFORMATIONS CLIENT ====================

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getClientNom() {
        return clientNom;
    }

    public void setClientNom(String clientNom) {
        this.clientNom = clientNom;
    }

    public String getClientPrenom() {
        return clientPrenom;
    }

    public void setClientPrenom(String clientPrenom) {
        this.clientPrenom = clientPrenom;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    public String getClientFullName() {
        if (clientPrenom == null && clientNom == null) return "";
        if (clientPrenom == null) return clientNom;
        if (clientNom == null) return clientPrenom;
        return clientPrenom + " " + clientNom;
    }

    public void ajouterItem(OrderItem item) {
        if (item != null) {
            items.add(item);
            calculTotal();
        }
    }

    public double calculTotal() {
        double total = 0.0;
        if (items != null) {
            for (OrderItem item : items) {
                total += item.calculSubtotal();
            }
        }
        this.totalPrice = total;
        return total;
    }

    public void validerCommande() {
        this.status = "validated";
    }

    public void payerCommande(Payment payment) {
        this.payment = payment;

        if (payment != null && payment.processPayment()) {
            this.status = "paid";
        } else {
            this.status = "pending";
        }
    }

    public void cancelOrder() {
        this.status = "cancelled";
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", uuid=" + orderUUID + ", total=" + totalPrice + 
               ", status=" + status + ", client=" + getClientFullName() + "}";
    }
}