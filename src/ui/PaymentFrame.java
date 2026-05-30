package ui;

import Client.AppSession;
import Client.ClientSocketService;
import model.SavedCard;
import service.CardService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PaymentFrame extends LanguageAwareFrame {

    private static final Logger logger = LogManager.getLogger(PaymentFrame.class);
    private static final Logger paymentLogger = LogManager.getLogger("com.chrionline.payment");

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backHome;
    private final CardService cardService;

    private JLabel titleLabel;
    private JLabel orderLbl;
    private JLabel totalLbl;
    private JComboBox<String> methods;
    private JButton payBtn;
    private JButton backToCartBtn;
    private JButton homeBtn;
    private JLabel methodLabel;
    private JComboBox<SavedCard> savedCardsCombo;
    private JPanel savedCardsPanel;

    public PaymentFrame(ClientSocketService clientService, AppSession session, JFrame backHome) {
        this.clientService = clientService;
        this.session = session;
        this.backHome = backHome;
        this.cardService = new CardService();
        initUI();
        loadSavedCards();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("payment.title"));
        setSize(600, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(480, 500));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(24, 32, 24, 32)
        ));

        titleLabel = new JLabel("💳 " + LanguageManager.getInstance().getText("payment.title"));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        JLabel icon = new JLabel("💳");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        icon.setForeground(UITheme.GOLD);

        String shortUuid = session.getOrderUUID() != null && session.getOrderUUID().length() >= 8
                ? session.getOrderUUID().substring(0, 8) + "..."
                : session.getOrderUUID();

        orderLbl = new JLabel(LanguageManager.getInstance().getText("payment.order") + " #" + shortUuid);
        orderLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        orderLbl.setForeground(UITheme.MUTED);
        orderLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

        totalLbl = new JLabel(String.format("💰 " + LanguageManager.getInstance().getText("cart.total") + ": %.2f DH", session.getLastOrderTotal()));
        totalLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        totalLbl.setForeground(UITheme.GOLD);
        totalLbl.setFont(new Font("SansSerif", Font.BOLD, 22));

        // ==================== Cartes enregistrées ====================
        savedCardsPanel = new JPanel();
        savedCardsPanel.setOpaque(false);
        savedCardsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        savedCardsPanel.setLayout(new BoxLayout(savedCardsPanel, BoxLayout.Y_AXIS));
        savedCardsPanel.setMaximumSize(new Dimension(380, 80));
        savedCardsPanel.setVisible(false);

        JLabel savedCardsLabel = new JLabel("💳 Mes cartes enregistrées");
        savedCardsLabel.setForeground(UITheme.MUTED);
        savedCardsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        savedCardsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        savedCardsCombo = new JComboBox<>();
        savedCardsCombo.setBackground(UITheme.CARD_2);
        savedCardsCombo.setForeground(Color.WHITE);
        savedCardsCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        savedCardsCombo.setMaximumSize(new Dimension(340, 35));
        savedCardsCombo.addItem(null);
        
        savedCardsCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    return super.getListCellRendererComponent(list, "➕ " + LanguageManager.getInstance().getText("payment.new.card"), index, isSelected, cellHasFocus);
                }
                SavedCard card = (SavedCard) value;
                return super.getListCellRendererComponent(list, card.getDisplayName(), index, isSelected, cellHasFocus);
            }
        });
        
        savedCardsCombo.addActionListener(e -> {
            SavedCard selected = (SavedCard) savedCardsCombo.getSelectedItem();
            if (selected != null) {
                JPasswordField cvvField = new JPasswordField();
                Object[] message = {
                    "Utiliser la carte " + selected.getDisplayName(),
                    "Veuillez entrer le CVV (3 chiffres) :", cvvField
                };
                int option = JOptionPane.showConfirmDialog(PaymentFrame.this, message, 
                        "Paiement sécurisé", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                
                if (option == JOptionPane.OK_OPTION) {
                    String cvv = new String(cvvField.getPassword()).trim();
                    if (cvv.isEmpty() || cvv.length() != 3) {
                        JOptionPane.showMessageDialog(PaymentFrame.this, "CVV invalide (3 chiffres requis)", 
                                "Erreur", JOptionPane.ERROR_MESSAGE);
                        savedCardsCombo.setSelectedIndex(0);
                        return;
                    }
                    processPayment("card", selected);
                } else {
                    savedCardsCombo.setSelectedIndex(0);
                }
            }
        });

        savedCardsPanel.add(savedCardsLabel);
        savedCardsPanel.add(Box.createVerticalStrut(5));
        savedCardsPanel.add(savedCardsCombo);
        savedCardsPanel.add(Box.createVerticalStrut(10));
        
        JSeparator separator = new JSeparator();
        separator.setForeground(UITheme.BORDER);
        separator.setMaximumSize(new Dimension(380, 1));
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);
        // ====================================================================

        JPanel methodPanel = new JPanel();
        methodPanel.setOpaque(false);
        methodPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        methodPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        methodLabel = new JLabel(LanguageManager.getInstance().getText("payment.method") + ":");
        methodLabel.setForeground(Color.WHITE);

        methods = new JComboBox<>(new String[]{
                "💳 " + LanguageManager.getInstance().getText("payment.card"),
                "💰 " + LanguageManager.getInstance().getText("payment.cash")
        });
        methods.setBackground(UITheme.CARD_2);
        methods.setForeground(Color.WHITE);
        methods.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        methods.addActionListener(e -> {
            String selected = (String) methods.getSelectedItem();
            boolean isCard = selected != null && selected.contains("💳");
            savedCardsPanel.setVisible(isCard);
            card.revalidate();
            card.repaint();
        });

        methodPanel.add(methodLabel);
        methodPanel.add(methods);

        payBtn = UITheme.primaryButton("💳 " + LanguageManager.getInstance().getText("payment.confirm"));
        payBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        payBtn.setMaximumSize(new Dimension(300, 45));
        payBtn.setPreferredSize(new Dimension(300, 45));

        backToCartBtn = UITheme.blueButton("← " + LanguageManager.getInstance().getText("payment.back.cart"));
        backToCartBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToCartBtn.setMaximumSize(new Dimension(300, 40));
        backToCartBtn.setPreferredSize(new Dimension(300, 40));

        homeBtn = UITheme.goldButton("🏠 " + LanguageManager.getInstance().getText("payment.back.shop"));
        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        homeBtn.setMaximumSize(new Dimension(300, 40));
        homeBtn.setPreferredSize(new Dimension(300, 40));

        card.add(Box.createVerticalStrut(10));
        card.add(icon);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(orderLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(totalLbl);
        card.add(Box.createVerticalStrut(15));
        card.add(savedCardsPanel);
        card.add(separator);
        card.add(Box.createVerticalStrut(10));
        card.add(methodPanel);
        card.add(Box.createVerticalStrut(20));
        card.add(payBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(backToCartBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(homeBtn);

        root.add(card);
        setContentPane(root);

        payBtn.addActionListener(e -> {
            String methodRaw = methods.getSelectedItem().toString();
            String method = methodRaw.contains("💳") ? "card" : "especes";

            if (method.equals("card")) {
                SavedCard selectedCard = (SavedCard) savedCardsCombo.getSelectedItem();
                if (selectedCard != null) {
                    return;
                }
                
                paymentLogger.info("💳 Ouverture formulaire paiement par carte - Montant: {} DH", session.getLastOrderTotal());
                
                CardPaymentDialog cardDialog = new CardPaymentDialog(PaymentFrame.this, session.getLastOrderTotal(), true, (cardInfo) -> {
                    SavedCard newCard = new SavedCard();
                    newCard.setUserId(session.getClientId());
                    newCard.setCardLast4(cardInfo.last4);
                    newCard.setCardBrand(cardInfo.brand);
                    newCard.setCardHolderName(cardInfo.cardHolder);
                    newCard.setExpiryMonth(cardInfo.expiryMonth);
                    newCard.setExpiryYear(cardInfo.expiryYear);
                    newCard.setDefault(false);
                    
                    if (cardService.saveCard(newCard)) {
                        paymentLogger.info("💾 Nouvelle carte enregistrée - Utilisateur: {}, Marque: {}, Last4: {}", 
                            session.getClientId(), cardInfo.brand, cardInfo.last4);
                        JOptionPane.showMessageDialog(PaymentFrame.this, 
                            "💾 Carte enregistrée pour vos prochains paiements !", 
                            "Succès", JOptionPane.INFORMATION_MESSAGE);
                        loadSavedCards();
                    }
                });
                cardDialog.setVisible(true);

                if (!cardDialog.isPaymentConfirmed()) {
                    paymentLogger.info("Paiement par carte annulé par l'utilisateur");
                    return;
                }

                processPaymentWithAntiReplay(method, null);
            } else {
                paymentLogger.info("💰 Paiement en espèces sélectionné - Montant: {} DH", session.getLastOrderTotal());
                processPaymentWithAntiReplay(method, null);
            }
        });

        backToCartBtn.addActionListener(e -> {
            logger.debug("Retour au panier depuis l'écran de paiement");
            dispose();
            new CartFrame(clientService, session, backHome).setVisible(true);
        });

        homeBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    LanguageManager.getInstance().getText("payment.cancel") + " ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                paymentLogger.info("Paiement annulé par l'utilisateur - UUID: {}", session.getOrderUUID());
                session.clearOrderData();
                dispose();
                backHome.setVisible(true);
            }
        });
    }

    private void loadSavedCards() {
        savedCardsCombo.removeAllItems();
        savedCardsCombo.addItem(null);
        
        List<SavedCard> cards = cardService.getUserCards(session.getClientId());
        for (SavedCard card : cards) {
            savedCardsCombo.addItem(card);
        }
        
        if (!cards.isEmpty()) {
            paymentLogger.debug("{} carte(s) enregistrée(s) chargée(s) pour l'utilisateur {}", cards.size(), session.getClientId());
        }
        
        savedCardsPanel.setVisible(!cards.isEmpty() && methods.getSelectedItem().toString().contains("💳"));
    }

    // ==================== PAIEMENT AVEC PROTECTION ANTI-REJEU ====================

    private void processPaymentWithAntiReplay(String method, SavedCard savedCard) {
        payBtn.setEnabled(false);
        payBtn.setText("⏳ " + LanguageManager.getInstance().getText("payment.confirm") + "...");

        String shortUuid = session.getOrderUUID() != null && session.getOrderUUID().length() >= 8
                ? session.getOrderUUID().substring(0, 8) + "..."
                : session.getOrderUUID();

        // 🔐 PROTECTION ANTI-REJEU : Générer un nonce et un timestamp
        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        
        paymentLogger.info("💸 Traitement du paiement avec anti-rejeu - UUID: {}, Méthode: {}, Montant: {} DH, Nonce: {}", 
            session.getOrderUUID(), method, session.getLastOrderTotal(), nonce.substring(0, 8) + "...");

        Timer timer = new Timer(500, ev -> {
            String response = clientService.payWithAntiReplay(session.getOrderUUID(), method, nonce, timestamp);

            if (response.startsWith("PAYMENT_SUCCESS")) {
                String paymentMethod = method.equals("card") ? "💳 Carte bancaire" : "💰 Espèces";
                if (savedCard != null) {
                    paymentMethod = "💳 " + savedCard.getDisplayName();
                }
                
                paymentLogger.info("✅ Paiement réussi - UUID: {}, Méthode: {}, Montant: {} DH", 
                    session.getOrderUUID(), method, session.getLastOrderTotal());
                
                JOptionPane.showMessageDialog(PaymentFrame.this,
                        "✅ " + LanguageManager.getInstance().getText("payment.success") + "\n\n" +
                                LanguageManager.getInstance().getText("payment.order") + ": " + shortUuid + "\n" +
                                paymentMethod + "\n" +
                                LanguageManager.getInstance().getText("cart.total") + ": " + session.getLastOrderTotal() + " DH",
                        LanguageManager.getInstance().getText("payment.success"), JOptionPane.INFORMATION_MESSAGE);
                session.clearOrderData();
                dispose();
                backHome.setVisible(true);
            } else {
                paymentLogger.error("❌ Paiement échoué - UUID: {}, Méthode: {}, Erreur: {}", 
                    session.getOrderUUID(), method, response);
                
                if (response != null && response.contains("REPLAY_ATTACK_DETECTED")) {
                    JOptionPane.showMessageDialog(PaymentFrame.this,
                        "⚠️ Une tentative de rejeu de paiement a été détectée !\nVeuillez réessayer.",
                        "Sécurité", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(PaymentFrame.this,
                        "❌ " + LanguageManager.getInstance().getText("payment.failed") + "\n\n" + response,
                        LanguageManager.getInstance().getText("payment.failed"), JOptionPane.ERROR_MESSAGE);
                }
                payBtn.setEnabled(true);
                payBtn.setText("💳 " + LanguageManager.getInstance().getText("payment.confirm"));
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    // Ancienne méthode conservée pour compatibilité (non utilisée)
    private void processPayment(String method, SavedCard savedCard) {
        processPaymentWithAntiReplay(method, savedCard);
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("payment.title"));
        titleLabel.setText("💳 " + LanguageManager.getInstance().getText("payment.title"));

        String shortUuid = session.getOrderUUID() != null && session.getOrderUUID().length() >= 8
                ? session.getOrderUUID().substring(0, 8) + "..."
                : session.getOrderUUID();

        orderLbl.setText(LanguageManager.getInstance().getText("payment.order") + " #" + shortUuid);
        totalLbl.setText(String.format("💰 " + LanguageManager.getInstance().getText("cart.total") + ": %.2f DH", session.getLastOrderTotal()));
        methodLabel.setText(LanguageManager.getInstance().getText("payment.method") + ":");

        methods.removeAllItems();
        methods.addItem("💳 " + LanguageManager.getInstance().getText("payment.card"));
        methods.addItem("💰 " + LanguageManager.getInstance().getText("payment.cash"));

        payBtn.setText("💳 " + LanguageManager.getInstance().getText("payment.confirm"));
        backToCartBtn.setText("← " + LanguageManager.getInstance().getText("payment.back.cart"));
        homeBtn.setText("🏠 " + LanguageManager.getInstance().getText("payment.back.shop"));

        revalidate();
        repaint();
    }
}