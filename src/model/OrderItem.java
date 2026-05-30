package model;

public class OrderItem {

    private int id;
    private Product product;
    private int quantity;
    private double unitPrice;

    public OrderItem() {
    }

    public OrderItem(int id, int productId, int quantity, double unitPrice) {
        this.id = id;
        this.product = new Product(productId, "", "", "", unitPrice, 0);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    } 

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    } 

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    } 

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getProductId() {
        return product != null ? product.getIdProduct() : 0;
    }

    public double getPrice() {
        return unitPrice;
    }

    public double calculSubtotal() {
        return unitPrice * quantity;
    }
}