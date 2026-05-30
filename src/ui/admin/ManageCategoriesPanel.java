package ui.admin;

import Client.ClientSocketService;

import ui.components.AppTable;
import ui.components.ConfirmDialog;
import ui.components.SearchBarPanel;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class ManageCategoriesPanel extends JPanel {

    private final ClientSocketService clientService;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Description"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final AppTable table = new AppTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public ManageCategoriesPanel(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel toolbar = UITheme.createCardPanel();
        toolbar.setLayout(new BorderLayout(10, 10));

        SearchBarPanel searchBar = new SearchBarPanel("Recherche :", new SearchBarPanel.SearchListener() {
            @Override
            public void onSearch(String keyword) {
                applySearch(keyword);
            }

            @Override
            public void onReset() {
                sorter.setRowFilter(null);
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton addBtn = UITheme.primaryButton("Ajouter");
        JButton editBtn = UITheme.secondaryButton("Modifier");
        JButton deleteBtn = UITheme.dangerButton("Supprimer");
        JButton refreshBtn = UITheme.secondaryButton("Actualiser");

        right.add(addBtn);
        right.add(editBtn);
        right.add(deleteBtn);
        right.add(refreshBtn);

        table.setRowSorter(sorter);

        toolbar.add(searchBar, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshData());
        addBtn.addActionListener(e -> openCategoryDialog(false));
        editBtn.addActionListener(e -> openCategoryDialog(true));
        deleteBtn.addActionListener(e -> deleteSelectedCategory());
    }

    public void refreshData() {
        model.setRowCount(0);

        String response = clientService.adminGetCategories();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_CATEGORIES")) {
            return;
        }

        String[] rows = response.split("\\|");
        for (String row : rows) {
            String[] f = row.split(";");
            if (f.length >= 3) {
                model.addRow(new Object[]{f[0], f[1], f[2]});
            }
        }
    }

    private void applySearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(keyword)));
    }

    private void openCategoryDialog(boolean editMode) {
        Integer id = null;
        String currentName = "";
        String currentDescription = "";

        if (editMode) {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Sélectionne une catégorie.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            id = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
            currentName = model.getValueAt(modelRow, 1).toString();
            currentDescription = model.getValueAt(modelRow, 2).toString();
        }

        JTextField nameField = UITheme.styledTextField(22);
        nameField.setText(currentName);

        JTextArea descriptionArea = new JTextArea(currentDescription, 4, 22);
        UITheme.styleTextArea(descriptionArea);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBackground(UITheme.CARD_BG);
        panel.add(label("Nom"));
        panel.add(nameField);
        panel.add(label("Description"));
        panel.add(new JScrollPane(descriptionArea));

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                editMode ? "Modifier catégorie" : "Ajouter catégorie",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();

            if (name.isBlank()) throw new IllegalArgumentException("Nom obligatoire.");

            String response;
            if (editMode) {
                response = clientService.adminUpdateCategory(id, name, description);
                if ("ADMIN_UPDATE_CATEGORY_SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(this, "Catégorie modifiée.");
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur : " + response);
                }
            } else {
                response = clientService.adminAddCategory(name, description);
                if ("ADMIN_ADD_CATEGORY_SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(this, "Catégorie ajoutée.");
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur : " + response);
                }
            }

            refreshData();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage());
        }
    }

    private void deleteSelectedCategory() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne une catégorie.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = Integer.parseInt(model.getValueAt(modelRow, 0).toString());

        boolean confirm = ConfirmDialog.show(this, "Confirmation", "Supprimer cette catégorie ?");
        if (!confirm) return;

        String response = clientService.adminDeleteCategory(id);
        if ("ADMIN_DELETE_CATEGORY_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Catégorie supprimée.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur : " + response);
        }
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.FONT_BODY);
        return label;
    }
}