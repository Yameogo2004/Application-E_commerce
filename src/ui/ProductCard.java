
package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ProductCard extends JPanel {

    private int quantity = 1;
    private final int productId;
    private final int stock;
    private final String productName;
    private final double price;
    private final String imagePath;
    private final String category;

    private JLabel stockLabel;
    private JButton addBtn;

    private final ClientSocketService clientService;
    private final AppSession session;
    private final Runnable onCartChanged;
    private final Runnable onOpenDetails;

    public ProductCard(int productId, String productName, double price, String imagePath,
                       String category, int stock,
                       ClientSocketService clientService, AppSession session,
                       Runnable onCartChanged, Runnable onOpenDetails) {

        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.imagePath = imagePath;
        this.category = category;
        this.stock = stock;
        this.clientService = clientService;
        this.session = session;
        this.onCartChanged = onCartChanged;
        this.onOpenDetails = onOpenDetails;

        initUI();
    }

    public ProductCard(int productId, String productName, double price, String imagePath,
                       String category, int stock,
                       ClientSocketService clientService, AppSession session, Runnable onCartChanged) {
        this(productId, productName, price, imagePath, category, stock, clientService, session, onCartChanged, null);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 8));
        setBackground(UITheme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setPreferredSize(new Dimension(280, 280));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(UITheme.CARD_2);
        imagePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel imageLabel = new JLabel(UITheme.loadProductImage(imagePath, 120, 120));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        imagePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openProductDetails();
            }
        });

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(productName);
        nameLabel.setForeground(UITheme.TEXT);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openProductDetails();
            }
        });

        JLabel categoryLabel = new JLabel("📁 " + category);
        categoryLabel.setForeground(UITheme.MUTED);
        categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        stockLabel = new JLabel();
        refreshStockText();
        stockLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel(String.format("💰 %.2f DH", price));
        priceLabel.setForeground(UITheme.GOLD);
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton minusBtn = UITheme.iconButton("−", UITheme.CARD_2);
        JButton plusBtn = UITheme.iconButton("+", UITheme.CARD_2);

        JTextField qtyField = new JTextField("1", 2);
        qtyField.setHorizontalAlignment(JTextField.CENTER);
        qtyField.setEditable(false);
        qtyField.setBackground(UITheme.CARD);
        qtyField.setForeground(Color.WHITE);
        qtyField.setFont(new Font("SansSerif", Font.BOLD, 12));
        qtyField.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        addBtn = UITheme.iconButton("🛒 " + LanguageManager.getInstance().getText("product.add"), UITheme.GREEN);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        addBtn.setPreferredSize(new Dimension(105, 28));

        minusBtn.addActionListener(e -> {
            if (quantity > 1) {
                quantity--;
                qtyField.setText(String.valueOf(quantity));
            }
        });

        plusBtn.addActionListener(e -> {
            if (quantity < stock) {
                quantity++;
                qtyField.setText(String.valueOf(quantity));
            }
        });

        addBtn.addActionListener(e -> {
            if (stock == 0) {
                JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("product.stock.out"));
                return;
            }

            String response = clientService.addToCart(session.getClientId(), productId, quantity);
            if ("CART_ADD_SUCCESS".equals(response)) {
                addBtn.setText("✓ " + LanguageManager.getInstance().getText("product.add"));
                addBtn.setBackground(new Color(60, 130, 60));

                Timer timer = new Timer(1500, ev -> {
                    addBtn.setText("🛒 " + LanguageManager.getInstance().getText("product.add"));
                    addBtn.setBackground(UITheme.GREEN);
                });
                timer.setRepeats(false);
                timer.start();

                if (onCartChanged != null) onCartChanged.run();
            } else {
                JOptionPane.showMessageDialog(this, "Erreur : " + response);
            }
        });

        qtyPanel.add(minusBtn);
        qtyPanel.add(qtyField);
        qtyPanel.add(plusBtn);
        qtyPanel.add(Box.createHorizontalStrut(5));
        qtyPanel.add(addBtn);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(categoryLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(stockLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(priceLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(qtyPanel);

        add(imagePanel, BorderLayout.NORTH);
        add(infoPanel, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openProductDetails();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(UITheme.CARD_2);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(UITheme.CARD);
            }
        });

        if (stock == 0) {
            addBtn.setEnabled(false);
            addBtn.setBackground(UITheme.RED);
            addBtn.setText("❌ " + LanguageManager.getInstance().getText("product.stock.out"));
        }
    }

    private void openProductDetails() {
        if (onOpenDetails != null) {
            onOpenDetails.run();
            return;
        }

        ProductDetailsFrame detailsFrame = new ProductDetailsFrame(
                clientService, session, productId, onCartChanged
        );
        detailsFrame.setVisible(true);
    }

    private void refreshStockText() {
        if (stock > 10) {
            stockLabel.setText("✅ " + LanguageManager.getInstance().getText("product.stock") + ": " + stock);
            stockLabel.setForeground(UITheme.SUCCESS);
        } else if (stock > 0) {
            stockLabel.setText("⚠️ " + LanguageManager.getInstance().getText("product.stock.limited") + ": " + stock);
            stockLabel.setForeground(UITheme.WARNING);
        } else {
            stockLabel.setText("❌ " + LanguageManager.getInstance().getText("product.stock.out"));
            stockLabel.setForeground(UITheme.RED);
        }
    }

    public void refreshTexts() {
        refreshStockText();
        if (stock > 0) {
            addBtn.setText("🛒 " + LanguageManager.getInstance().getText("product.add"));
        }
    }

    public int getProductId() {
        return productId;
    }
}
