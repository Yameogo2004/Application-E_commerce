package model;

public class CartItem {

    private int id;
    private Product product;
    private int quantity;

    public CartItem() {
    }

    public CartItem(int id, Product product, int quantity) {
        this.id = id;
        this.product = product;
        setQuantity(quantity);
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
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0.");
        }
        this.quantity = quantity;
    }

    public double calculateSubtotal() {
        if (product == null) {
            return 0.0;
        }
        return product.getPrice() * quantity;
    }
}