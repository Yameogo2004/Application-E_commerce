package service;

import dao.ProductDAO;
import model.Product;

import java.util.List;

public class ProductService {

    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    /**
     * Récupère tous les produits
     * @return Liste de tous les produits
     */
    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    /**
     * Récupère un produit par son ID
     * @param id ID du produit
     * @return Produit ou null
     */
    public Product getProductById(int id) {
        return productDAO.findById(id);
    }

    /**
     * Récupère un produit par son nom
     * @param name Nom du produit
     * @return Produit ou null
     */
    public Product getProductByName(String name) {
        return productDAO.findByName(name);
    }

    /**
     * Ajoute un nouveau produit
     * @param product Produit à ajouter
     * @return true si réussi
     */
    public boolean addProduct(Product product) {
        return productDAO.save(product);
    }

    /**
     * Met à jour un produit existant
     * @param product Produit à mettre à jour
     * @return true si réussi
     */
    public boolean updateProduct(Product product) {
        return productDAO.update(product);
    }

    /**
     * Met à jour uniquement le stock d'un produit
     * @param productId ID du produit
     * @param newStock Nouveau stock
     * @return true si réussi
     */
    public boolean updateProductStock(int productId, int newStock) {
        return productDAO.updateStock(productId, newStock);
    }

    /**
     * Supprime un produit
     * @param id ID du produit
     * @return true si réussi
     */
    public boolean deleteProduct(int id) {
        return productDAO.delete(id);
    }
}