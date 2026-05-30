package ui.admin;

import Client.ClientSocketService;

import ui.theme.UITheme;
import ui.components.AppTable;
import ui.components.FilterPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class NotificationsPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Titre", "Message", "Type", "Niveau", "Lu", "EntityType", "EntityId", "Date"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final AppTable table = new AppTable(model);
    private final JComboBox<String> levelFilter = new JComboBox<>(new String[]{"Tous", "WARNING", "CRITICAL", "INFO"});
    private final JComboBox<String> readFilter = new JComboBox<>(new String[]{"Tous", "true", "false"});
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);

    public NotificationsPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        UITheme.styleComboBox(levelFilter);
        UITheme.styleComboBox(readFilter);

        FilterPanel filterPanel = new FilterPanel(new FilterPanel.FilterListener() {
            @Override
            public void onApply() {
                applyFilters();
            }

            @Override
            public void onReset() {
                levelFilter.setSelectedIndex(0);
                readFilter.setSelectedIndex(0);
                sorter.setRowFilter(null);
            }
        });

        filterPanel.addFilter("Niveau :", levelFilter);
        filterPanel.addFilter("Lu :", readFilter);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton markReadBtn = UITheme.primaryButton("Marquer comme lu");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");

        right.add(markReadBtn);
        right.add(refreshBtn);
        filterPanel.add(right, BorderLayout.SOUTH);

        table.setRowSorter(sorter);

        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshData());
        markReadBtn.addActionListener(e -> markSelectedAsRead());
    }

    public void refreshData() {
        model.setRowCount(0);

        String response = clientService.adminGetNotifications();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_NOTIFICATIONS")) {
            return;
        }

        String[] rows = response.split("\\|");
        for (String row : rows) {
        	String[] f = row.split(";", -1);
            if (f.length >= 9) {
                model.addRow(new Object[]{
                        f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8]
                });
            }
        }
    }

    public int getUnreadCount() {
        int count = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object value = model.getValueAt(i, 5);
            if (value != null && "false".equalsIgnoreCase(value.toString())) {
                count++;
            }
        }
        return count;
    }

    private void applyFilters() {
        final Object selectedLevel = levelFilter.getSelectedItem();
        final Object selectedRead = readFilter.getSelectedItem();

        final String level = selectedLevel == null ? "Tous" : selectedLevel.toString();
        final String read = selectedRead == null ? "Tous" : selectedRead.toString();

        RowFilter<DefaultTableModel, Integer> filter = new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String currentLevel = entry.getStringValue(4);
                String currentRead = entry.getStringValue(5);

                boolean levelOk = "Tous".equals(level) || currentLevel.equalsIgnoreCase(level);
                boolean readOk = "Tous".equals(read) || currentRead.equalsIgnoreCase(read);

                return levelOk && readOk;
            }
        };

        sorter.setRowFilter(filter);
    }

    private void markSelectedAsRead() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne une notification.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int notificationId = Integer.parseInt(model.getValueAt(modelRow, 0).toString());

        String response = clientService.adminMarkNotificationRead(notificationId);
        if ("ADMIN_MARK_NOTIFICATION_READ_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Notification marquée comme lue.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur : " + response);
        }
    }
}