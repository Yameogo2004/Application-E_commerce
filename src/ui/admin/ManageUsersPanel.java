package ui.admin;

import Client.ClientSocketService;
import ui.components.AppTable;
import ui.components.FilterPanel;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class ManageUsersPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Prénom", "Email", "Rôle", "Statut"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final AppTable table = new AppTable(model);
    private final JTextField searchField = UITheme.styledTextField(18);
    private final JComboBox<String> roleFilter = new JComboBox<>(new String[]{"Tous", "admin", "client"});
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);

    public ManageUsersPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        UITheme.styleComboBox(roleFilter);

        FilterPanel filterPanel = new FilterPanel(new FilterPanel.FilterListener() {
            @Override
            public void onApply() {
                applyFilters();
            }

            @Override
            public void onReset() {
                searchField.setText("");
                roleFilter.setSelectedIndex(0);
                sorter.setRowFilter(null);
            }
        });

        filterPanel.addFilter("Recherche :", searchField);
        filterPanel.addFilter("Rôle :", roleFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton refreshBtn = UITheme.secondaryButton("Actualiser");
        right.add(refreshBtn);

        filterPanel.add(right, BorderLayout.SOUTH);

        table.setRowSorter(sorter);

        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshData());
    }

    public void refreshData() {
        model.setRowCount(0);

        String response = clientService.adminGetUsers();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_USERS")) {
            return;
        }

        String[] rows = response.split("\\|");
        for (String row : rows) {
            String[] f = row.split(";");
            if (f.length >= 6) {
                model.addRow(new Object[]{f[0], f[1], f[2], f[3], f[4], f[5]});
            } else if (f.length >= 5) {
                model.addRow(new Object[]{f[0], f[1], f[2], f[3], f[4], ""});
            }
        }
    }

    private void applyFilters() {
        final String keyword = searchField.getText().trim().toLowerCase();
        final Object selectedRole = roleFilter.getSelectedItem();
        final String role = selectedRole == null ? "tous" : selectedRole.toString().toLowerCase();

        RowFilter<DefaultTableModel, Integer> filter = new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String nom = entry.getStringValue(1).toLowerCase();
                String prenom = entry.getStringValue(2).toLowerCase();
                String email = entry.getStringValue(3).toLowerCase();
                String userRole = entry.getStringValue(4).toLowerCase();

                boolean searchOk = keyword.isEmpty()
                        || nom.contains(keyword)
                        || prenom.contains(keyword)
                        || email.contains(keyword);

                boolean roleOk = role.equals("tous") || userRole.equals(role);

                return searchOk && roleOk;
            }
        };

        sorter.setRowFilter(filter);
    }
}