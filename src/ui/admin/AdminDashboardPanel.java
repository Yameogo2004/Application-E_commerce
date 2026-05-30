package ui.admin;

import Client.ClientSocketService;
import ui.theme.UITheme;
import ui.components.AppTable;
import ui.components.MetricCard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AdminDashboardPanel extends JPanel {

    private final ClientSocketService clientService;

    private final MetricCard totalProductsCard = new MetricCard("Produits", "--", "Total catalogue");
    private final MetricCard lowStockCard = new MetricCard("Stock faible", "--", "Produits à surveiller");
    private final MetricCard outOfStockCard = new MetricCard("Rupture", "--", "Produits indisponibles");
    private final MetricCard usersCard = new MetricCard("Utilisateurs", "--", "Comptes enregistrés");
    private final MetricCard ordersCard = new MetricCard("Commandes", "--", "Total commandes");
    private final MetricCard pendingCard = new MetricCard("En attente", "--", "Commandes pending");
    private final MetricCard todayRevenueCard = new MetricCard("Revenus jour", "--", "Paiements du jour");
    private final MetricCard monthRevenueCard = new MetricCard("Revenus mois", "--", "Paiements du mois");

    private final DefaultTableModel notificationsModel = new DefaultTableModel(
            new Object[]{"Titre", "Message", "Niveau", "Lu", "Date"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final DefaultTableModel stockModel = new DefaultTableModel(
            new Object[]{"Produit", "Stock", "Seuil", "Niveau", "Statut"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public AdminDashboardPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel metricsGrid = new JPanel(new GridLayout(2, 4, 14, 14));
        metricsGrid.setOpaque(false);

        metricsGrid.add(totalProductsCard);
        metricsGrid.add(lowStockCard);
        metricsGrid.add(outOfStockCard);
        metricsGrid.add(usersCard);
        metricsGrid.add(ordersCard);
        metricsGrid.add(pendingCard);
        metricsGrid.add(todayRevenueCard);
        metricsGrid.add(monthRevenueCard);

        AppTable notificationsTable = new AppTable(notificationsModel);
        AppTable stockTable = new AppTable(stockModel);

        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 14, 14));
        tablesPanel.setOpaque(false);

        tablesPanel.add(createSectionCard("Notifications récentes", new JScrollPane(notificationsTable)));
        tablesPanel.add(createSectionCard("Alertes stock", new JScrollPane(stockTable)));

        add(metricsGrid, BorderLayout.NORTH);
        add(tablesPanel, BorderLayout.CENTER);
    }

    private JPanel createSectionCard(String title, JComponent content) {
        JPanel panel = UITheme.createCardPanel();
        panel.setLayout(new BorderLayout(10, 10));

        JLabel titleLabel = UITheme.createTitleLabel(title);
        titleLabel.setFont(UITheme.FONT_H3);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public void refreshData() {
        loadSummary();
        loadNotificationsPreview();
        loadStockAlertsPreview();
    }

    private void loadSummary() {
        String response = clientService.adminGetDashboardSummary();

        if (response == null || response.startsWith("ERROR") || !response.startsWith("DASHBOARD_SUMMARY:")) {
            setFallbackMetrics();
            return;
        }

        String payload = response.substring("DASHBOARD_SUMMARY:".length());
        String[] fields = payload.split(";");

        if (fields.length < 10) {
            setFallbackMetrics();
            return;
        }

        totalProductsCard.setValue(fields[0]);
        lowStockCard.setValue(fields[1]);
        outOfStockCard.setValue(fields[2]);
        usersCard.setValue(fields[3]);
        ordersCard.setValue(fields[4]);
        pendingCard.setValue(fields[5]);
        todayRevenueCard.setValue(fields[7] + " DH");
        monthRevenueCard.setValue(fields[8] + " DH");
    }

    private void loadNotificationsPreview() {
        notificationsModel.setRowCount(0);

        String response = clientService.adminGetNotifications();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_NOTIFICATIONS")) {
            return;
        }

        String[] rows = response.split("\\|");
        int limit = Math.min(rows.length, 6);

        for (int i = 0; i < limit; i++) {
            String[] f = rows[i].split(";");
            if (f.length >= 9) {
                notificationsModel.addRow(new Object[]{
                        f[1], f[2], f[4], f[5], f[8]
                });
            }
        }
    }

    private void loadStockAlertsPreview() {
        stockModel.setRowCount(0);

        String response = clientService.adminGetStockAlerts();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_STOCK_ALERTS")) {
            return;
        }

        String[] rows = response.split("\\|");
        int limit = Math.min(rows.length, 6);

        for (int i = 0; i < limit; i++) {
            String[] f = rows[i].split(";");
            if (f.length >= 7) {
                stockModel.addRow(new Object[]{
                        f[1], f[2], f[3], f[4], f[5]
                });
            }
        }
    }

    private void setFallbackMetrics() {
        totalProductsCard.setValue("--");
        lowStockCard.setValue("--");
        outOfStockCard.setValue("--");
        usersCard.setValue("--");
        ordersCard.setValue("--");
        pendingCard.setValue("--");
        todayRevenueCard.setValue("--");
        monthRevenueCard.setValue("--");
    }
}