
package model;

import java.time.LocalDateTime;

public class Product {

    private int idProduct;
    private String name;
    private String description;
    private String image;
    private double price;
    private int stock;
    private LocalDateTime createdAt;
    private Category category;

    public Product(int idProduct, String name, String description, String image,
                   double price, int stock) {
        this.idProduct = idProduct;
        this.name = name;
        this.description = description;
        this.image = image;
        this.price = price;
        this.stock = stock;
        this.createdAt = LocalDateTime.now();
        this.category = null; 
    }

    // ── Getters & Setters ─────────────────────
    public int getIdProduct() { return idProduct; }
    public void setIdProduct(int idProduct) { this.idProduct = idProduct; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Category getCategory() { return category; }  // <-- getter
    public void setCategory(Category category) { this.category = category; } // <-- setter
    
    

   
    public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public void consulterProduit() {
        System.out.println("Produit : " + name);
        System.out.println("Description : " + description);
        System.out.println("Prix : " + price);
        System.out.println("Stock : " + stock);
        System.out.println("Image : " + image);
        if (category != null) {
            System.out.println("Catégorie ID : " + category.getId());
        }
    }

    public void modifierStock(int newStock) {
        this.stock = newStock;
        System.out.println("Stock mis à jour : " + stock);
    }
}
