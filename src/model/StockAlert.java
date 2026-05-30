package model;

import java.time.LocalDateTime;

public class StockAlert {

    private int productId;
    private String productName;
    private int currentStock;
    private int threshold;
    private String level;
    private String status;
    private LocalDateTime createdAt;

    public StockAlert() {
        this.createdAt = LocalDateTime.now();
    }

    public StockAlert(int productId, String productName, int currentStock, int threshold,
                      String level, String status, LocalDateTime createdAt) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.threshold = threshold;
        this.level = level;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
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

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    } 

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    } 

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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
}