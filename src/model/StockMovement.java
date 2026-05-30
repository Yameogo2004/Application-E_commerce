package model;

import java.time.LocalDateTime;

public class StockMovement {

    private int id;
    private int productId;
    private String productName;
    private String movementType;
    private int quantity;
    private int previousStock;
    private int newStock;
    private String reason;
    private Integer adminUserId;
    private LocalDateTime createdAt;

    public StockMovement() {
        this.createdAt = LocalDateTime.now();
    }

    public StockMovement(int id, int productId, String productName, String movementType,
                         int quantity, int previousStock, int newStock,
                         String reason, Integer adminUserId, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.reason = reason;
        this.adminUserId = adminUserId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    } 

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    } 

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    } 

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    } 

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    } 

    public int getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(int previousStock) {
        this.previousStock = previousStock;
    } 

    public int getNewStock() {
        return newStock;
    }

    public void setNewStock(int newStock) {
        this.newStock = newStock;
    } 

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    } 

    public Integer getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Integer adminUserId) {
        this.adminUserId = adminUserId;
    } 

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}