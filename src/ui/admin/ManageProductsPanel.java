package ui.admin;

import Client.AppSession;
import Client.ClientSocketService;
import ui.components.AppTable;
import ui.components.ConfirmDialog;
import ui.components.SearchBarPanel;
import ui.theme.UITheme;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.LinkedHashMap;

public class ManageProductsPanel extends JPanel {

    private final ClientSocketService clientService;
    @SuppressWarnings("unused")
    private final AppSession session;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nom", "Prix", "Image", "Catégorie", "Stock"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final AppTable table = new AppTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);

    public ManageProductsPanel(ClientSocketService clientService, AppSession session) {
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
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        toolbar.add(searchBar, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshData());
        addBtn.addActionListener(e -> openProductDialog(false));
        editBtn.addActionListener(e -> openProductDialog(true));
        deleteBtn.addActionListener(e -> deleteSelectedProduct());
    }

    public void refreshData() {
        model.setRowCount(0);

        String response = clientService.getProducts();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_PRODUCTS")) {
            return;
        }

        String[] rows = response.split("\\|");
        for (String row : rows) {
            String[] f = row.split(";");
            if (f.length >= 6) {
                model.addRow(new Object[]{f[0], f[1], f[2], f[3], f[4], f[5]});
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

    private void openProductDialog(boolean editMode) {
        Integer productId = null;
        String currentName = "";
        String currentDescription = "";
        String currentPrice = "";
        String currentStock = "";
        String currentImage = "image/default.jpg";
        String currentCategoryName = "";

        if (editMode) {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Sélectionne un produit.");
                return;
            }

            int modelRow = table.convertRowIndexToModel(row);
            productId = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
            currentName = model.getValueAt(modelRow, 1).toString();
            currentPrice = model.getValueAt(modelRow, 2).toString();
            currentImage = model.getValueAt(modelRow, 3).toString();
            currentCategoryName = model.getValueAt(modelRow, 4).toString();
            currentStock = model.getValueAt(modelRow, 5).toString();

            String response = clientService.getProduct(productId);
            if (response != null && !response.startsWith("ERROR")) {
                String[] f = response.split(";");
                if (f.length >= 7) {
                    currentDescription = f[3];
                    currentStock = f[4];
                    currentImage = f[5];
                    currentCategoryName = f[6];
                }
            }
        }

        ProductFormDialog dialog = new ProductFormDialog(
                SwingUtilities.getWindowAncestor(this),
                editMode,
                productId,
                currentName,
                currentDescription,
                currentPrice,
                currentStock,
                currentImage,
                currentCategoryName
        );

        dialog.setVisible(true);

        if (!dialog.isSubmitted()) {
            return;
        }

        try {
            String name = dialog.getProductName();
            String description = dialog.getDescription();
            double price = Double.parseDouble(dialog.getPriceText());
            int stock = Integer.parseInt(dialog.getStockText());
            String image = dialog.getImagePath();
            CategoryItem selected = dialog.getSelectedCategory();

            if (name.isBlank()) {
                throw new IllegalArgumentException("Nom obligatoire.");
            }

            if (description.isBlank()) {
                throw new IllegalArgumentException("Description obligatoire.");
            }

            if (price < 0) {
                throw new IllegalArgumentException("Prix invalide.");
            }

            if (stock < 0) {
                throw new IllegalArgumentException("Stock invalide.");
            }

            if (selected == null) {
                throw new IllegalArgumentException("Catégorie obligatoire.");
            }

            if (image == null || image.isBlank()) {
                image = "image/default.jpg";
            }

            String response;
            if (editMode) {
                response = clientService.adminUpdateProduct(
                        productId,
                        name,
                        description,
                        price,
                        stock,
                        image,
                        selected.id
                );

                if ("ADMIN_UPDATE_PRODUCT_SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(this, "Produit modifié avec succès.");
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur : " + response);
                }
            } else {
                response = clientService.adminAddProduct(
                        name,
                        description,
                        price,
                        stock,
                        image,
                        selected.id
                );

                if ("ADMIN_ADD_PRODUCT_SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(this, "Produit ajouté avec succès.");
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur : " + response);
                }
            }

            refreshData();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Prix ou stock invalide.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage());
        }
    }

    private void deleteSelectedProduct() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionne un produit.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = Integer.parseInt(model.getValueAt(modelRow, 0).toString());

        boolean confirm = ConfirmDialog.show(this, "Confirmation", "Supprimer ce produit ?");
        if (!confirm) {
            return;
        }

        String response = clientService.adminDeleteProduct(id);
        if ("ADMIN_DELETE_PRODUCT_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Produit supprimé.");
            refreshData();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur : " + response);
        }
    }

    private void loadCategories(JComboBox<CategoryItem> combo, Map<String, Integer> categoryMap) {
        combo.removeAllItems();
        categoryMap.clear();

        String response = clientService.adminGetCategories();
        if (response == null || response.startsWith("ERROR") || response.equals("NO_CATEGORIES")) {
            return;
        }

        String[] rows = response.split("\\|");
        for (String row : rows) {
            String[] f = row.split(";");
            if (f.length >= 2) {
                int id = Integer.parseInt(f[0]);
                String name = f[1];
                categoryMap.put(name, id);
                combo.addItem(new CategoryItem(id, name));
            }
        }
    }

    private String importImageFromExplorer() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choisir une image");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Images", "jpg", "jpeg", "png", "webp", "gif"
        ));

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        try {
            File selectedFile = chooser.getSelectedFile();

            if (selectedFile == null || !selectedFile.exists()) {
                JOptionPane.showMessageDialog(this, "Fichier image introuvable.");
                return null;
            }

            File imageDir = new File("image");
            if (!imageDir.exists()) {
                boolean created = imageDir.mkdirs();
                if (!created && !imageDir.exists()) {
                    JOptionPane.showMessageDialog(this, "Impossible de créer le dossier image.");
                    return null;
                }
            }

            String originalName = selectedFile.getName();
            String extension = "";

            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }

            String newFileName = "prod_" + System.currentTimeMillis() + extension;
            File destination = new File(imageDir, newFileName);

            Files.copy(selectedFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return "image/" + newFileName;

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors de l'import de l'image : " + ex.getMessage());
            return null;
        }
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.FONT_BODY);
        return label;
    }

    private JPanel createFieldBlock(String title, JComponent component) {
        JPanel block = new JPanel(new BorderLayout(6, 6));
        block.setOpaque(false);
        block.add(label(title), BorderLayout.NORTH);
        block.add(component, BorderLayout.CENTER);
        return block;
    }

    private static class CategoryItem {
        private final int id;
        private final String name;

        private CategoryItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class ProductFormDialog extends JDialog {

        private final JTextField nameField = UITheme.styledTextField(24);
        private final JTextArea descriptionArea = new JTextArea(5, 24);
        private final JTextField priceField = UITheme.styledTextField(24);
        private final JTextField stockField = UITheme.styledTextField(24);
        private final JTextField imageField = UITheme.styledTextField(24);
        private final JComboBox<CategoryItem> categoryBox = new JComboBox<CategoryItem>();

        private final Map<String, Integer> categoryMap = new LinkedHashMap<String, Integer>();

        private boolean submitted = false;

        ProductFormDialog(
                Window owner,
                boolean editMode,
                Integer productId,
                String currentName,
                String currentDescription,
                String currentPrice,
                String currentStock,
                String currentImage,
                String currentCategoryName
        ) {
            super(owner, editMode ? "Modifier produit" : "Ajouter produit", ModalityType.APPLICATION_MODAL);
            initDialog(editMode, currentName, currentDescription, currentPrice, currentStock, currentImage, currentCategoryName);
        }

        private void initDialog(
                boolean editMode,
                String currentName,
                String currentDescription,
                String currentPrice,
                String currentStock,
                String currentImage,
                String currentCategoryName
        ) {
            setSize(760, 620);
            setLocationRelativeTo(ManageProductsPanel.this);
            setResizable(false);

            UITheme.styleTextArea(descriptionArea);
            UITheme.styleComboBox(categoryBox);
            imageField.setEditable(false);

            loadCategories(categoryBox, categoryMap);

            nameField.setText(currentName);
            descriptionArea.setText(currentDescription);
            priceField.setText(currentPrice);
            stockField.setText(currentStock);
            imageField.setText(currentImage == null || currentImage.isBlank() ? "image/default.jpg" : currentImage);

            if (currentCategoryName != null && !currentCategoryName.isBlank()) {
                for (int i = 0; i < categoryBox.getItemCount(); i++) {
                    CategoryItem item = categoryBox.getItemAt(i);
                    if (item != null && item.name.equalsIgnoreCase(currentCategoryName)) {
                        categoryBox.setSelectedIndex(i);
                        break;
                    }
                }
            }

            JPanel root = new JPanel(new BorderLayout(16, 16));
            root.setBackground(UITheme.APP_BG);
            root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

            JPanel formCard = UITheme.createCardPanel();
            formCard.setLayout(new BorderLayout(14, 14));

            JLabel titleLabel = UITheme.createTitleLabel(editMode ? "Modifier produit" : "Ajouter un nouveau produit");
            JLabel subtitleLabel = UITheme.createSubtitleLabel("Renseigne les informations produit de façon claire et complète.");

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.add(titleLabel);
            header.add(Box.createVerticalStrut(4));
            header.add(subtitleLabel);

            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(false);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1.0;

            JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
            descriptionScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

            JButton importImageBtn = UITheme.secondaryButton("Importer image");
            importImageBtn.addActionListener(e -> {
                String importedPath = importImageFromExplorer();
                if (importedPath != null && !importedPath.isBlank()) {
                    imageField.setText(importedPath);
                }
            });

            JPanel imagePanel = new JPanel(new BorderLayout(8, 8));
            imagePanel.setOpaque(false);
            imagePanel.add(imageField, BorderLayout.CENTER);
            imagePanel.add(importImageBtn, BorderLayout.EAST);

            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(createFieldBlock("Nom du produit", nameField), gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            form.add(createFieldBlock("Catégorie", categoryBox), gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            form.add(createFieldBlock("Prix (DH)", priceField), gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            form.add(createFieldBlock("Stock disponible", stockField), gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            form.add(createFieldBlock("Image", imagePanel), gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            form.add(createFieldBlock("Description", descriptionScroll), gbc);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            footer.setOpaque(false);

            JButton cancelBtn = UITheme.secondaryButton("Annuler");
            JButton saveBtn = UITheme.primaryButton(editMode ? "Enregistrer les modifications" : "Ajouter le produit");

            cancelBtn.addActionListener(e -> dispose());
            saveBtn.addActionListener(e -> {
                submitted = true;
                dispose();
            });

            footer.add(cancelBtn);
            footer.add(saveBtn);

            formCard.add(header, BorderLayout.NORTH);
            formCard.add(form, BorderLayout.CENTER);
            formCard.add(footer, BorderLayout.SOUTH);

            root.add(formCard, BorderLayout.CENTER);
            setContentPane(root);
        }

        boolean isSubmitted() {
            return submitted;
        }

        String getProductName() {
            return nameField.getText().trim();
        }

        String getDescription() {
            return descriptionArea.getText().trim();
        }

        String getPriceText() {
            return priceField.getText().trim();
        }

        String getStockText() {
            return stockField.getText().trim();
        }

        String getImagePath() {
            return imageField.getText().trim();
        }

        CategoryItem getSelectedCategory() {
            return (CategoryItem) categoryBox.getSelectedItem();
        }
    }
}