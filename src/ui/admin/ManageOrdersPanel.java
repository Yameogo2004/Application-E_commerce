package ui.admin;

import Client.ClientSocketService;
import ui.components.AppTable;
import ui.components.FilterPanel;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class ManageOrdersPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "UUID", "Client", "Email", "Total", "Statut", "Date"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final AppTable table = new AppTable(model);
    private final JTextField searchField = UITheme.styledTextField(18);

    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"Tous", "pending", "paid", "shipped", "delivered", "cancelled"}
    );

    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public ManageOrdersPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        UITheme.styleComboBox(statusFilter);

        FilterPanel filterPanel = new FilterPanel(new FilterPanel.FilterListener() {
            @Override
            public void onApply() {
                applyFilters();
            }

            @Override
            public void onReset() {
                searchField.setText("");
                statusFilter.setSelectedIndex(0);
                sorter.setRowFilter(null);
            }
        });

        filterPanel.addFilter("Recherche :", searchField);
        filterPanel.addFilter("Statut :", statusFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton updateBtn = UITheme.primaryButton("Mettre à jour statut");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");

        right.add(updateBtn);
        right.add(refreshBtn);
        filterPanel.add(right, BorderLayout.SOUTH);

        table.setRowSorter(sorter);

        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshData());
        updateBtn.addActionListener(e -> updateSelectedOrderStatus());
    }

    public void refreshData() {
        model.setRowCount(0);

        String response = clientService.adminGetOrders();

        if (response == null || response.startsWith("ERROR") || response.equals("NO_ORDERS")) {
            return;
        }

        String[] rows = response.split("\\|");

        for (String row : rows) {
            String[] f = row.split(";", -1);

            if (f.length >= 7) {
                model.addRow(new Object[]{
                        f[0], // ID
                        f[1], // UUID
                        f[2], // Client nom + prénom
                        f[3], // Email
                        f[4], // Total
                        f[5], // Statut
                        f[6]  // Date
                });
            }
        }
    }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String selectedStatus = (String) statusFilter.getSelectedItem();

        RowFilter<DefaultTableModel, Object> filter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                String id = entry.getStringValue(0).toLowerCase();
                String uuid = entry.getStringValue(1).toLowerCase();
                String client = entry.getStringValue(2).toLowerCase();
                String email = entry.getStringValue(3).toLowerCase();
                String status = entry.getStringValue(5).toLowerCase();

                boolean searchOk = keyword.isBlank()
                        || id.contains(keyword)
                        || uuid.contains(keyword)
                        || client.contains(keyword)
                        || email.contains(keyword);

                boolean statusOk = selectedStatus == null
                        || selectedStatus.equals("Tous")
                        || status.equals(selectedStatus.toLowerCase());

                return searchOk && statusOk;
            }
        };

        sorter.setRowFilter(filter);
    }

    private void updateSelectedOrderStatus() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne une commande.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);

        int orderId = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
        String currentStatus = model.getValueAt(modelRow, 5).toString();

        String[] statuses = {"pending", "paid", "shipped", "delivered", "cancelled"};

        String selectedStatus = (String) JOptionPane.showInputDialog(
                this,
                "Choisir le nouveau statut :",
                "Mise à jour statut",
                JOptionPane.PLAIN_MESSAGE,
                null,
                statuses,
                currentStatus
        );

        if (selectedStatus == null || selectedStatus.isBlank()) {
            return;
        }

        String response = clientService.adminUpdateOrderStatus(orderId, selectedStatus);

        if ("ADMIN_UPDATE_ORDER_STATUS_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Statut mis à jour.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur : " + response);
        }
    }
}