package ui.admin;

import Client.AppSession;

import Client.ClientSocketService;
import ui.theme.UITheme;
import ui.LoginFrame;
import ui.components.AdminSidebar;
import ui.components.AdminTopbar;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminMainFrame extends JFrame {

    private final ClientSocketService clientService;
    private final AppSession session;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final AdminTopbar topbar;
    private final AdminSidebar sidebar;

    private final AdminDashboardPanel dashboardPanel;
    private final ManageProductsPanel productsPanel;
    private final ManageCategoriesPanel categoriesPanel;
    private final ManageOrdersPanel ordersPanel;
    private final ManageUsersPanel usersPanel;
    private final NotificationsPanel notificationsPanel;
    private final StockHistoryPanel stockHistoryPanel;
    private final AdminStatisticsPanel statisticsPanel;

    private final Map<String, String[]> pageMeta = new LinkedHashMap<>();

    public AdminMainFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session = session;

        UITheme.applyGlobalOptionPaneTheme();

        pageMeta.put("dashboard", new String[]{"Dashboard", "Vue d'ensemble du système"});
        pageMeta.put("products", new String[]{"Produits", "Gestion du catalogue produits"});
        pageMeta.put("categories", new String[]{"Catégories", "Gestion des catégories produits"});
        pageMeta.put("orders", new String[]{"Commandes", "Suivi et mise à jour des commandes"});
        pageMeta.put("users", new String[]{"Utilisateurs", "Clients et comptes du système"});
        pageMeta.put("notifications", new String[]{"Notifications", "Alertes système et stock faible"});
        pageMeta.put("stockHistory", new String[]{"Historique Stock", "Mouvements et ajustements du stock"});
        pageMeta.put("statistics", new String[]{"Statistiques", "Indicateurs principaux du système"});

        dashboardPanel = new AdminDashboardPanel(clientService);
        productsPanel = new ManageProductsPanel(clientService, session);
        categoriesPanel = new ManageCategoriesPanel(clientService);
        ordersPanel = new ManageOrdersPanel(clientService);
        usersPanel = new ManageUsersPanel(clientService);
        notificationsPanel = new NotificationsPanel(clientService);
        stockHistoryPanel = new StockHistoryPanel(clientService, session);
        statisticsPanel = new AdminStatisticsPanel(clientService);

        sidebar = new AdminSidebar(this::navigateTo);
        topbar = new AdminTopbar(new AdminTopbar.TopbarActionListener() {
            @Override
            public void onRefresh() {
                refreshCurrentPage();
                refreshTopbarBadge();
            }

            @Override
            public void onLogout() {
                int confirm = JOptionPane.showConfirmDialog(
                        AdminMainFrame.this,
                        "Voulez-vous vous déconnecter ?",
                        "Déconnexion",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    // Invalider la session
                    clientService.invalidateSession();
                    
                    // Fermer la fenêtre admin
                    dispose();
                    
                    // Ouvrir la fenêtre de connexion
                    new LoginFrame(clientService).setVisible(true);
                }
            }

            @Override
            public void onOpenNotifications() {
                navigateTo("notifications");
            }
        });

        initUI();
        registerPages();
        navigateTo("dashboard");
        refreshTopbarBadge();
    }

    private void initUI() {
        setTitle("ChriOnline - Admin V2");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1380, 860);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.APP_BG);

        contentPanel.setBackground(UITheme.APP_BG);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UITheme.APP_BG);
        center.add(topbar, BorderLayout.NORTH);
        center.add(contentPanel, BorderLayout.CENTER);

        root.add(sidebar, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void registerPages() {
        contentPanel.add(dashboardPanel, "dashboard");
        contentPanel.add(productsPanel, "products");
        contentPanel.add(categoriesPanel, "categories");
        contentPanel.add(ordersPanel, "orders");
        contentPanel.add(usersPanel, "users");
        contentPanel.add(notificationsPanel, "notifications");
        contentPanel.add(stockHistoryPanel, "stockHistory");
        contentPanel.add(statisticsPanel, "statistics");
    }

    private void navigateTo(String pageId) {
        cardLayout.show(contentPanel, pageId);
        sidebar.setActivePage(pageId);

        String[] meta = pageMeta.getOrDefault(pageId, new String[]{"Admin", "Panneau d'administration"});
        topbar.setPageInfo(meta[0], meta[1]);

        refreshCurrentPage();
        refreshTopbarBadge();
    }

    private void refreshCurrentPage() {
        if (dashboardPanel.isShowing()) dashboardPanel.refreshData();
        if (productsPanel.isShowing()) productsPanel.refreshData();
        if (categoriesPanel.isShowing()) categoriesPanel.refreshData();
        if (ordersPanel.isShowing()) ordersPanel.refreshData();
        if (usersPanel.isShowing()) usersPanel.refreshData();
        if (notificationsPanel.isShowing()) notificationsPanel.refreshData();
        if (stockHistoryPanel.isShowing()) stockHistoryPanel.refreshData();
        if (statisticsPanel.isShowing()) statisticsPanel.refreshData();
    }

    private void refreshTopbarBadge() {
        topbar.setUnreadNotificationsCount(notificationsPanel.getUnreadCount());
    }
}