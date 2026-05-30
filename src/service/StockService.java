package service;

import dao.StockMovementDAO;
import model.Product;
import model.StockAlert;
import model.StockMovement;

import java.util.ArrayList;
import java.util.List;

public class StockService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final ProductService productService;
    private final StockMovementDAO stockMovementDAO;
    private final NotificationService notificationService;

    public StockService() {
        this.productService = new ProductService();
        this.stockMovementDAO = new StockMovementDAO();
        this.notificationService = new NotificationService();
    }

    public List<StockAlert> getLowStockAlerts() {
        return getLowStockAlerts(DEFAULT_LOW_STOCK_THRESHOLD);
    }

    public List<StockAlert> getLowStockAlerts(int threshold) {
        List<StockAlert> alerts = new ArrayList<>();
        List<Product> products = productService.getAllProducts();

        for (Product product : products) {
            if (product.getStock() <= threshold) {
                String level = product.getStock() <= 0 ? "CRITICAL" : "WARNING";
                String status = product.getStock() <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK";

                alerts.add(new StockAlert(
                        product.getIdProduct(),
                        product.getName(),
                        product.getStock(),
                        threshold,
                        level,
                        status,
                        product.getCreatedAt()
                ));
            }
        }

        return alerts;
    }

    public List<StockMovement> getStockHistory() {
        return stockMovementDAO.findAll();
    }

    public List<StockMovement> getStockHistoryByProduct(int productId) {
        return stockMovementDAO.findByProductId(productId);
    }

    public boolean adjustStock(int productId, int quantity, String movementType, String reason, Integer adminUserId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return false;
        }

        String normalizedType = normalizeMovementType(movementType);
        if (normalizedType == null) {
            return false;
        }

        int previousStock = product.getStock();
        int newStock;

        switch (normalizedType) {
            case "ENTREE":
                newStock = previousStock + quantity;
                break;
            case "SORTIE":
                newStock = previousStock - quantity;
                break;
            case "AJUSTEMENT":
                newStock = previousStock + quantity;
                break;
            default:
                return false;
        }

        if (newStock < 0) {
            return false;
        }

        boolean updated = productService.updateProductStock(productId, newStock);
        if (!updated) {
            return false;
        }

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setProductName(product.getName());
        movement.setMovementType(normalizedType);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setReason(reason != null && !reason.isBlank() ? reason : "Aucune raison");
        movement.setAdminUserId(adminUserId);

        boolean saved = stockMovementDAO.save(movement);

        Product updatedProduct = productService.getProductById(productId);
        notificationService.syncProductStockNotification(updatedProduct, DEFAULT_LOW_STOCK_THRESHOLD);

        return saved;
    }

    private String normalizeMovementType(String movementType) {
        if (movementType == null) return null;

        String value = movementType.trim().toUpperCase();

        if (value.equals("ENTREE")) return "ENTREE";
        if (value.equals("SORTIE")) return "SORTIE";
        if (value.equals("AJUSTEMENT")) return "AJUSTEMENT";

        return null;
    }
}