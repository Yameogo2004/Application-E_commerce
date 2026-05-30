package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CartFrame extends LanguageAwareFrame {

    private static final Logger logger = LogManager.getLogger(CartFrame.class);
    private static final Logger paymentLogger = LogManager.getLogger("com.chrionline.payment");

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backFrame;

    private JTable table;
    private DefaultTableModel model;
    private JLabel totalLabel;
    private JButton checkoutBtn;
    private JButton clearAllBtn;
    private JButton backBtn;
    private JLabel titleLabel;

    private final List<CartProductInfo> cartProducts = new ArrayList<>();

    public CartFrame(ClientSocketService clientService, AppSession session, JFrame backFrame) {
        this.clientService = clientService;
        this.session = session;
        this.backFrame = backFrame;
        logger.debug("Ouverture du panier - Utilisateur: {}", session.getClientId());
        initUI();
        loadCart();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("cart.title"));
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = UITheme.cardPanel();
        top.setLayout(new BorderLayout());

        titleLabel = new JLabel("🛒 " + LanguageManager.getInstance().getText("cart.title"));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        top.add(titleLabel, BorderLayout.WEST);

        model = new DefaultTableModel(new Object[]{
                LanguageManager.getInstance().getText("cart.product"),
                LanguageManager.getInstance().getText("cart.quantity"),
                LanguageManager.getInstance().getText("cart.unit.price"),
                LanguageManager.getInstance().getText("cart.subtotal"),
                LanguageManager.getInstance().getText("cart.remove")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
        };

        table = new JTable(model);
        table.setBackground(UITheme.CARD_2);
        table.setForeground(Color.WHITE);
        table.setRowHeight(50);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setBackground(UITheme.CARD);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.setSelectionBackground(new Color(67, 139, 208));

        table.getColumn(LanguageManager.getInstance().getText("cart.remove")).setCellRenderer(new ButtonRenderer());
        table.getColumn(LanguageManager.getInstance().getText("cart.remove")).setCellEditor(new ButtonEditor());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        JPanel bottom = UITheme.cardPanel();
        bottom.setLayout(new BorderLayout(10, 10));

        totalLabel = new JLabel("💰 " + LanguageManager.getInstance().getText("cart.total") + ": 0.00 DH");
        totalLabel.setForeground(UITheme.GOLD);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 26));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);

        checkoutBtn = UITheme.primaryButton("💳 " + LanguageManager.getInstance().getText("cart.checkout"));
        clearAllBtn = UITheme.dangerButton("🗑️ " + LanguageManager.getInstance().getText("cart.clear"));
        backBtn = UITheme.blueButton("← " + LanguageManager.getInstance().getText("cart.back"));

        buttons.add(backBtn);
        buttons.add(clearAllBtn);
        buttons.add(checkoutBtn);

        bottom.add(totalLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        clearAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    LanguageManager.getInstance().getText("cart.clear.confirm"),
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                logger.info("Vidage complet du panier - Utilisateur: {}", session.getClientId());
                String res = clientService.clearCart(session.getClientId());
                if ("CART_CLEAR_SUCCESS".equals(res)) {
                    loadCart();
                    JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("cart.clear.success"));
                    refreshParentCartCount();
                } else {
                    logger.error("Échec vidage panier - Utilisateur: {}, Erreur: {}", session.getClientId(), res);
                    JOptionPane.showMessageDialog(this, "Erreur: " + res);
                }
            }
        });

        backBtn.addActionListener(e -> {
            logger.debug("Retour à la boutique depuis le panier - Utilisateur: {}", session.getClientId());
            backFrame.setVisible(true);
            dispose();
        });

        checkoutBtn.addActionListener(e -> checkout());
    }

    private void loadCart() {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            cartProducts.clear();

            String response = clientService.getCart(session.getClientId());

            if ("CART_EMPTY".equals(response)) {
                totalLabel.setText("💰 " + LanguageManager.getInstance().getText("cart.total") + ": 0.00 DH");
                model.addRow(new Object[]{LanguageManager.getInstance().getText("cart.empty"), "", "", "", ""});
                logger.debug("Panier vide - Utilisateur: {}", session.getClientId());
                return;
            }

            if (response == null || response.startsWith("ERROR")) {
                logger.error("Erreur chargement panier - Utilisateur: {}, Erreur: {}", session.getClientId(), response);
                JOptionPane.showMessageDialog(this, "Erreur: " + response);
                return;
            }

            String total = "0.00";
            String[] parts = response.split("\\|");

            for (String part : parts) {
                if (part.startsWith("Total=")) {
                    total = part.substring("Total=".length());
                } else if (part.startsWith("ProductId=")) {
                    int productId = Integer.parseInt(extract(part, "ProductId=", ",Product="));
                    String product = extract(part, ",Product=", ",Qty=");
                    String qty = extract(part, ",Qty=", ",Subtotal=");
                    String subtotal = part.substring(part.indexOf(",Subtotal=") + 10);

                    double sub = Double.parseDouble(subtotal);
                    int qt = Integer.parseInt(qty);
                    double unitPrice = qt > 0 ? sub / qt : 0;

                    cartProducts.add(new CartProductInfo(productId, product, qt, unitPrice, sub));

                    model.addRow(new Object[]{
                            product,
                            qt,
                            String.format("%.2f DH", unitPrice),
                            String.format("%.2f DH", sub),
                            "🗑️ " + LanguageManager.getInstance().getText("cart.remove")
                    });
                }
            }

            totalLabel.setText(String.format("💰 " + LanguageManager.getInstance().getText("cart.total") + ": %s DH", total));
            logger.debug("Panier chargé - Utilisateur: {}, Produits: {}, Total: {} DH", 
                session.getClientId(), cartProducts.size(), total);

            if (model.getRowCount() == 0) {
                model.addRow(new Object[]{LanguageManager.getInstance().getText("cart.empty"), "", "", "", ""});
            }
        });
    }

    private void removeItemFromCart(int row) {
        if (row >= 0 && row < cartProducts.size()) {
            CartProductInfo product = cartProducts.get(row);

            int confirm = JOptionPane.showConfirmDialog(this,
                    LanguageManager.getInstance().getText("cart.remove.confirm") + " \"" + product.name + "\" ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                logger.info("Suppression produit du panier - Utilisateur: {}, Produit: {}, Quantité: {}", 
                    session.getClientId(), product.name, product.quantity);
                
                String response = clientService.removeFromCart(session.getClientId(), product.productId);

                if ("CART_REMOVE_SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(this, product.name + " supprimé.");
                    loadCart();
                    refreshParentCartCount();
                } else {
                    logger.error("Échec suppression produit - Utilisateur: {}, Produit: {}, Erreur: {}", 
                        session.getClientId(), product.name, response);
                    JOptionPane.showMessageDialog(this, "Erreur: " + response);
                }
            }
        }
    }

    private void refreshParentCartCount() {
        if (backFrame instanceof ShopFrame) {
            ((ShopFrame) backFrame).refreshCartCount();
        }
    }

    private String extract(String source, String start, String end) {
        int s = source.indexOf(start);
        int e = source.indexOf(end);
        if (s == -1 || e == -1) return "";
        return source.substring(s + start.length(), e);
    }

    // ==================== CHECKOUT AVEC PROTECTION ANTI-REJEU ====================

    private void checkout() {
        if (cartProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, LanguageManager.getInstance().getText("cart.empty"));
            return;
        }

        paymentLogger.info("💰 Début du checkout - Utilisateur: {}, Nb produits: {}, Total: {} DH", 
            session.getClientId(), cartProducts.size(), session.getLastOrderTotal());

        // 🔐 PROTECTION ANTI-REJEU : Générer un nonce et un timestamp
        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        
        paymentLogger.debug("Checkout avec anti-rejeu - Nonce: {}, Timestamp: {}", nonce, timestamp);

        String response = clientService.checkoutWithAntiReplay(session.getClientId(), nonce, timestamp);

        if (response == null || response.startsWith("ERROR")) {
            paymentLogger.error("❌ Échec checkout - Utilisateur: {}, Erreur: {}", session.getClientId(), response);
            
            if (response != null && response.contains("REPLAY_ATTACK_DETECTED")) {
                JOptionPane.showMessageDialog(this, 
                    "⚠️ Une tentative de rejeu de commande a été détectée !\nVeuillez réessayer.",
                    "Sécurité", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur checkout: " + response);
            }
            return;
        }

        String[] parts = response.split(";");
        if (parts.length == 3 && "ORDER_CREATED".equals(parts[0])) {
            session.setOrderUUID(parts[1]);
            session.setLastOrderTotal(Double.parseDouble(parts[2]));
            
            paymentLogger.info("✅ Commande créée - Utilisateur: {}, UUID: {}, Total: {} DH", 
                session.getClientId(), parts[1], parts[2]);

            dispose();
            new PaymentFrame(clientService, session, backFrame).setVisible(true);
        } else {
            paymentLogger.error("❌ Erreur création commande - Utilisateur: {}, Réponse: {}", session.getClientId(), response);
            JOptionPane.showMessageDialog(this, "Erreur lors de la validation de la commande.");
        }
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("cart.title"));
        titleLabel.setText("🛒 " + LanguageManager.getInstance().getText("cart.title"));

        model.setColumnIdentifiers(new Object[]{
                LanguageManager.getInstance().getText("cart.product"),
                LanguageManager.getInstance().getText("cart.quantity"),
                LanguageManager.getInstance().getText("cart.unit.price"),
                LanguageManager.getInstance().getText("cart.subtotal"),
                LanguageManager.getInstance().getText("cart.remove")
        });

        checkoutBtn.setText("💳 " + LanguageManager.getInstance().getText("cart.checkout"));
        clearAllBtn.setText("🗑️ " + LanguageManager.getInstance().getText("cart.clear"));
        backBtn.setText("← " + LanguageManager.getInstance().getText("cart.back"));

        loadCart();
        revalidate();
        repaint();
    }

    static class CartProductInfo {
        int productId;
        String name;
        int quantity;
        double unitPrice;
        double subtotal;

        CartProductInfo(int productId, String name, int quantity, double unitPrice, double subtotal) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(UITheme.RED);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? LanguageManager.getInstance().getText("cart.remove") : value.toString());
            return this;
        }
    }

    class ButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JButton button;
        private int selectedRow;

        public ButtonEditor() {
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(UITheme.RED);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            selectedRow = row;
            button.setText((value == null) ? LanguageManager.getInstance().getText("cart.remove") : value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            SwingUtilities.invokeLater(() -> removeItemFromCart(selectedRow));
            return LanguageManager.getInstance().getText("cart.remove");
        }
    }
}