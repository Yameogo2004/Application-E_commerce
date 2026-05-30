package service;

import dao.DashboardDAO;
import model.DashboardSummary;

public class DashboardService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final DashboardDAO dashboardDAO;
    private final NotificationService notificationService;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
        this.notificationService = new NotificationService();
    }

    public DashboardSummary getDashboardSummary() {
        notificationService.syncLowStockNotifications(DEFAULT_LOW_STOCK_THRESHOLD);
        return dashboardDAO.getDashboardSummary(DEFAULT_LOW_STOCK_THRESHOLD);
    }
}