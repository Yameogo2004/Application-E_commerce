package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import java.awt.*;

public class ProductDetailsFrame extends JFrame {

    private final int productId;

    public ProductDetailsFrame(ClientSocketService clientService, AppSession session, int productId, Runnable onCartChanged) {
        this.productId = productId;
        setTitle("📦 Détails du produit");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setBackground(UITheme.BG);

        JLabel loadingLabel = new JLabel("⏳ Chargement des détails...");
        loadingLabel.setForeground(UITheme.TEXT);
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        loadingPanel.add(loadingLabel);

        add(loadingPanel);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String name = "";
            private double price = 0;
            private String description = "";
            private int stock = 0;
            private String image = "";
            private String category = "";

            @Override
            protected Boolean doInBackground() {
                try {
                    String response = clientService.getProduct(productId);

                    if (response == null || response.startsWith("ERROR")) {
                        return false;
                    }

                    String[] parts = response.split(";");
                    if (parts.length < 7) {
                        return false;
                    }

                    name = parts[1];
                    price = Double.parseDouble(parts[2]);
                    description = parts[3];
                    stock = Integer.parseInt(parts[4]);
                    image = parts[5];
                    category = parts[6];

                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (!success) {
                        JOptionPane.showMessageDialog(ProductDetailsFrame.this,
                                "Impossible de charger les détails du produit.",
                                "Erreur", JOptionPane.ERROR_MESSAGE);
                        dispose();
                        return;
                    }

                    showProductDetails(name, price, description, stock, image, category,
                            clientService, session, onCartChanged);

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ProductDetailsFrame.this,
                            "Erreur lors du chargement: " + e.getMessage(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
            }
        };

        worker.execute();
    }

    private void showProductDetails(String name, double price, String description, int stock,
                                    String image, String category,
                                    ClientSocketService clientService, AppSession session,
                                    Runnable onCartChanged) {

        getContentPane().removeAll();

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(20, 20));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.4;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(UITheme.CARD_2);
        imagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 2, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        ImageIcon productIcon = UITheme.loadProductImage(image, 350, 350);
        JLabel imageLabel = new JLabel(productIcon);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        mainPanel.add(imagePanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel categoryLabel = new JLabel("📁 " + LanguageManager.getInstance().getText("product.category") + " : " + category);
        categoryLabel.setForeground(UITheme.MUTED);
        categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel(String.format("💰 %.2f DH", price));
        priceLabel.setForeground(UITheme.GOLD);
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel stockLabel;
        if (stock > 10) {
            stockLabel = new JLabel("✅ " + LanguageManager.getInstance().getText("product.stock") + " : " + stock);
            stockLabel.setForeground(UITheme.SUCCESS);
        } else if (stock > 0) {
            stockLabel = new JLabel("⚠️ " + LanguageManager.getInstance().getText("product.stock.limited") + " : " + stock);
            stockLabel.setForeground(UITheme.WARNING);
        } else {
            stockLabel = new JLabel("❌ " + LanguageManager.getInstance().getText("product.stock.out"));
            stockLabel.setForeground(UITheme.RED);
        }
        stockLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        stockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setForeground(UITheme.BORDER);
        separator.setMaximumSize(new Dimension(500, 2));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descTitle = new JLabel("📝 " + LanguageManager.getInstance().getText("product.description") + " :");
        descTitle.setForeground(Color.WHITE);
        descTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        descTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea descriptionArea = new JTextArea(
                description != null && !description.isEmpty()
                        ? description
                        : LanguageManager.getInstance().getText("product.no.description")
        );
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(UITheme.CARD_2);
        descriptionArea.setForeground(Color.WHITE);
        descriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(400, 120));
        descriptionScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        descriptionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel qtyLabel = new JLabel(LanguageManager.getInstance().getText("product.quantity") + " :");
        qtyLabel.setForeground(Color.WHITE);
        qtyLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, Math.max(stock, 1), 1));
        quantitySpinner.setPreferredSize(new Dimension(70, 35));

        qtyPanel.add(qtyLabel);
        qtyPanel.add(quantitySpinner);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addCartBtn = UITheme.primaryButton("🛒 " + LanguageManager.getInstance().getText("product.add"));
        addCartBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        addCartBtn.setPreferredSize(new Dimension(200, 45));

        JButton closeBtn = UITheme.blueButton("✖ Fermer");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        closeBtn.setPreferredSize(new Dimension(150, 45));

        buttonPanel.add(addCartBtn);
        buttonPanel.add(closeBtn);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(categoryLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(priceLabel);
        infoPanel.add(Box.createVerticalStrut(12));
        infoPanel.add(stockLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(separator);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(descTitle);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(descriptionScroll);
        infoPanel.add(Box.createVerticalStrut(20));
        infoPanel.add(qtyPanel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(buttonPanel);

        mainPanel.add(infoPanel, gbc);

        root.add(mainPanel, BorderLayout.CENTER);

        setContentPane(root);
        revalidate();
        repaint();

        addCartBtn.addActionListener(e -> {
            if (stock == 0) {
                JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("product.stock.out"));
                return;
            }

            int qty = (Integer) quantitySpinner.getValue();
            String addResponse = clientService.addToCart(session.getClientId(), productId, qty);

            if ("CART_ADD_SUCCESS".equals(addResponse)) {
                JOptionPane.showMessageDialog(this,
                        "✅ " + qty + " x " + name + " " + LanguageManager.getInstance().getText("cart.add.success"),
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                if (onCartChanged != null) onCartChanged.run();
            } else {
                JOptionPane.showMessageDialog(this,
                        "❌ Erreur : " + addResponse,
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> dispose());
    }
}