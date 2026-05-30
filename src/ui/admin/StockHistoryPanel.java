package ui.admin;

import Client.AppSession;

import Client.ClientSocketService;
import ui.theme.UITheme;
import ui.components.AppTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class StockHistoryPanel extends JPanel {

    private final ClientSocketService clientService;
    private final AppSession session;

    private final DefaultTableModel alertsModel = new DefaultTableModel(
            new Object[]{"ProduitID", "Produit", "Stock", "Seuil", "Niveau", "Statut", "Date"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[]{"ID", "ProduitID", "Produit", "Type", "Quantité", "Avant", "Après", "Raison", "Admin", "Date"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final AppTable alertsTable = new AppTable(alertsModel);
    private final AppTable historyTable = new AppTable(historyModel);

    public StockHistoryPanel(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session = session;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel toolbar = UITheme.createCardPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton adjustBtn = UITheme.primaryButton("Ajuster stock");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");

        toolbar.add(adjustBtn);
        toolbar.add(refreshBtn);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                createSection("Alertes stock", new JScrollPane(alertsTable)),
                createSection("Historique des mouvements", new JScrollPane(historyTable))
        );
        splitPane.setResizeWeight(0.35);

        add(toolbar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        adjustBtn.addActionListener(e -> openAdjustStockDialog());
        refreshBtn.addActionListener(e -> refreshData());
    }

    private JPanel createSection(String title, JComponent content) {
        JPanel panel = UITheme.createCardPanel();
        panel.setLayout(new BorderLayout(10, 10));

        JLabel titleLabel = UITheme.createTitleLabel(title);
        titleLabel.setFont(UITheme.FONT_H3);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public void refreshData() {
        loadAlerts();
        loadHistory();
    }

    private void loadAlerts() {
        alertsModel.setRowCount(0);

        String response = clientService.adminGetStockAlerts();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_STOCK_ALERTS")) {
            return;
        }

        String[] rows = response.split("\\|");
        for (String row : rows) {
            String[] f = row.split(";");
            if (f.length >= 7) {
                alertsModel.addRow(new Object[]{f[0], f[1], f[2], f[3], f[4], f[5], f[6]});
            }
        }
    }

    private void loadHistory() {
        historyModel.setRowCount(0);

        String response = clientService.adminGetStockHistory();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_STOCK_HISTORY")) {
            return;
        }

        String[] rows = response.split("\\|");
        for (String row : rows) {
            String[] f = row.split(";");
            if (f.length >= 10) {
                historyModel.addRow(new Object[]{
                        f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9]
                });
            }
        }
    }

    private void openAdjustStockDialog() {
        JTextField productIdField = UITheme.styledTextField(20);
        JTextField quantityField = UITheme.styledTextField(20);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"ENTREE", "SORTIE", "AJUSTEMENT"});
        UITheme.styleComboBox(typeBox);
        JTextField reasonField = UITheme.styledTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBackground(UITheme.CARD_BG);
        panel.add(label("ID Produit"));
        panel.add(productIdField);
        panel.add(label("Quantité"));
        panel.add(quantityField);
        panel.add(label("Type de mouvement"));
        panel.add(typeBox);
        panel.add(label("Raison"));
        panel.add(reasonField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Ajuster le stock",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            int productId = Integer.parseInt(productIdField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());
            String type = (String) typeBox.getSelectedItem();
            String reason = reasonField.getText().trim().replace(":", "-");
            int adminId = extractAdminId();

            String response = clientService.adminAdjustStock(productId, quantity, type, reason, adminId);
            if ("ADMIN_ADJUST_STOCK_SUCCESS".equals(response)) {
                JOptionPane.showMessageDialog(this, "Stock ajusté avec succès.");
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur : " + response);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage());
        }
    }

    private int extractAdminId() {
        if (session == null) return 0;
        return session.getUserId();
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.FONT_BODY);
        return label;
    }
}