package service;

import dao.NotificationDAO;
import model.Notification;
import model.Product;

import java.util.List;

public class NotificationService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final NotificationDAO notificationDAO;
    private final ProductService productService;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
        this.productService = new ProductService();
    }

    public List<Notification> getAllNotifications() {
        return notificationDAO.findAll();
    }

    public List<Notification> getUnreadNotifications() {
        return notificationDAO.findUnread();
    }

    public int getUnreadCount() {
        return notificationDAO.countUnread();
    }

    public boolean markAsRead(int notificationId) {
        return notificationDAO.markAsRead(notificationId);
    }

    public void syncLowStockNotifications() {
        syncLowStockNotifications(DEFAULT_LOW_STOCK_THRESHOLD);
    }

    public void syncLowStockNotifications(int threshold) {
        List<Product> products = productService.getAllProducts();

        for (Product product : products) {
            syncProductStockNotification(product, threshold);
        }
    }

    public void syncProductStockNotification(Product product, int threshold) {
        if (product == null) return;

        int stock = product.getStock();

        if (stock > threshold) {
            notificationDAO.markUnreadProductNotificationsAsRead(product.getIdProduct());
            return;
        }

        if (stock <= 0) {
            createIfNotExists(
                    "OUT_OF_STOCK",
                    "CRITICAL",
                    "Produit en rupture",
                    "Le produit \"" + product.getName() + "\" est en rupture de stock.",
                    product.getIdProduct()
            );
            return;
        }

        createIfNotExists(
                "LOW_STOCK",
                "WARNING",
                "Stock faible",
                "Le produit \"" + product.getName() + "\" a un stock faible : " + stock,
                product.getIdProduct()
        );
    }

    private void createIfNotExists(String type, String level, String title, String message, int productId) {
        boolean exists = notificationDAO.existsForEntity(type, "PRODUCT", productId);

        if (!exists) {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type);
            notification.setLevel(level);
            notification.setRead(false);
            notification.setEntityType("PRODUCT");
            notification.setEntityId(productId);

            notificationDAO.save(notification);
        }
    }
}