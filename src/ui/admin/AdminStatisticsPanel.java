package ui.admin;

import Client.ClientSocketService;

import ui.theme.UITheme;
import ui.components.MetricCard;

import javax.swing.*;
import java.awt.*;

public class AdminStatisticsPanel extends JPanel {

    private final ClientSocketService clientService;

    private final MetricCard totalProductsCard = new MetricCard("Produits", "--", "Catalogue global");
    private final MetricCard totalOrdersCard = new MetricCard("Commandes", "--", "Toutes les commandes");
    private final MetricCard pendingOrdersCard = new MetricCard("En attente", "--", "Commandes pending");
    private final MetricCard paidOrdersCard = new MetricCard("Payées", "--", "Commandes réglées");
    private final MetricCard todayRevenueCard = new MetricCard("Revenus jour", "--", "Total du jour");
    private final MetricCard monthRevenueCard = new MetricCard("Revenus mois", "--", "Total mensuel");

    private final JProgressBar paidRatioBar = new JProgressBar();

    public AdminStatisticsPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setOpaque(false);

        grid.add(totalProductsCard);
        grid.add(totalOrdersCard);
        grid.add(pendingOrdersCard);
        grid.add(paidOrdersCard);
        grid.add(todayRevenueCard);
        grid.add(monthRevenueCard);

        JPanel ratioPanel = UITheme.createCardPanel();
        ratioPanel.setLayout(new BorderLayout(10, 10));

        JLabel title = UITheme.createTitleLabel("Taux de commandes payées");
        title.setFont(UITheme.FONT_H3);

        paidRatioBar.setStringPainted(true);
        paidRatioBar.setForeground(UITheme.SKY_DARK);
        paidRatioBar.setBackground(UITheme.INPUT_BG);

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setOpaque(false);
        info.setForeground(UITheme.TEXT_SECONDARY);
        info.setFont(UITheme.FONT_BODY);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setText(
                "Cette section donne un résumé analytique rapide basé sur les données disponibles côté serveur. " +
                "Tu pourras plus tard y ajouter des courbes, des périodes, des tops produits et d’autres KPIs."
        );

        ratioPanel.add(title, BorderLayout.NORTH);
        ratioPanel.add(paidRatioBar, BorderLayout.CENTER);
        ratioPanel.add(info, BorderLayout.SOUTH);

        add(grid, BorderLayout.NORTH);
        add(ratioPanel, BorderLayout.CENTER);
    }

    public void refreshData() {
        String response = clientService.adminGetDashboardSummary();
        if (response == null || response.startsWith("ERROR") || !response.startsWith("DASHBOARD_SUMMARY:")) {
            return;
        }

        String payload = response.substring("DASHBOARD_SUMMARY:".length());
        String[] f = payload.split(";");
        if (f.length < 10) return;

        int totalProducts = parseInt(f[0]);
        int totalOrders = parseInt(f[4]);
        int pendingOrders = parseInt(f[5]);
        int paidOrders = parseInt(f[6]);
        double todayRevenue = parseDouble(f[7]);
        double monthRevenue = parseDouble(f[8]);

        totalProductsCard.setValue(String.valueOf(totalProducts));
        totalOrdersCard.setValue(String.valueOf(totalOrders));
        pendingOrdersCard.setValue(String.valueOf(pendingOrders));
        paidOrdersCard.setValue(String.valueOf(paidOrders));
        todayRevenueCard.setValue(todayRevenue + " DH");
        monthRevenueCard.setValue(monthRevenue + " DH");

        int ratio = totalOrders == 0 ? 0 : (int) ((paidOrders * 100.0) / totalOrders);
        paidRatioBar.setValue(ratio);
        paidRatioBar.setString(ratio + "%");
    }

    private int parseInt(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return 0; }
    }

    private double parseDouble(String v) {
        try { return Double.parseDouble(v); } catch (Exception e) { return 0.0; }
    }
}