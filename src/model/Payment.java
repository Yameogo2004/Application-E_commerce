
package model;

import java.time.LocalDateTime;

public class Payment {

    private int id;
    private int orderId;
    private String method;
    private double amount;
    private String status;
    private LocalDateTime paidAt;

    public Payment() {
    }

    public Payment(int id, int orderId, String method, double amount, String status, LocalDateTime paidAt) {
        this.id = id;
        this.orderId = orderId;
        setMethod(method);
        setAmount(amount);
        setStatus(status);
        this.paidAt = paidAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    } 

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    } 

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        if (method == null) {
            throw new IllegalArgumentException("Méthode invalide.");
        }

        String normalized = method.toLowerCase();

        if (!normalized.equals("card") && !normalized.equals("especes")) {
            throw new IllegalArgumentException("Méthode invalide.");
        }

        this.method = normalized;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Montant invalide.");
        }
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Statut invalide.");
        }

        String normalized = status.toLowerCase();

        if (!normalized.equals("pending")
                && !normalized.equals("success")
                && !normalized.equals("failed")) {
            throw new IllegalArgumentException("Statut invalide.");
        }

        this.status = normalized;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public boolean validatePayment() {
        return amount > 0 &&
                (method.equals("card") || method.equals("especes"));
    }

    public boolean processPayment() {
        if (!validatePayment()) {
            status = "failed";
            return false;
        }

        status = "success";
        paidAt = LocalDateTime.now();
        return true;
    }
}
