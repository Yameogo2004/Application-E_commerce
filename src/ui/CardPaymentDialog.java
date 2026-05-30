package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.YearMonth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CardPaymentDialog extends JDialog {

    private static final Logger logger = LogManager.getLogger(CardPaymentDialog.class);
    private static final Logger paymentLogger = LogManager.getLogger("com.chrionline.payment");

    private JTextField cardNumberField;
    private JTextField expiryField;
    private JPasswordField cvvField;
    private JTextField cardHolderField;
    private JButton payBtn;
    private JButton cancelBtn;
    private JLabel eyeLabel;
    private JCheckBox saveCardCheckbox;

    private boolean paymentConfirmed = false;
    private boolean cvvVisible = false;
    private boolean withSaveOption = false;
    private CardSaveCallback saveCallback = null;

    // Interface de callback pour enregistrer la carte
    public interface CardSaveCallback {
        void onCardSaved(CardInfo cardInfo);
    }

    // Classe pour passer les infos de la carte
    public static class CardInfo {
        public String last4;
        public String brand;
        public String cardHolder;
        public int expiryMonth;
        public int expiryYear;
        
        public CardInfo(String last4, String brand, String cardHolder, int expiryMonth, int expiryYear) {
            this.last4 = last4;
            this.brand = brand;
            this.cardHolder = cardHolder;
            this.expiryMonth = expiryMonth;
            this.expiryYear = expiryYear;
        }
    }

    // Constructeur pour paiement normal (sans enregistrement)
    public CardPaymentDialog(JFrame parent, double amount) {
        this(parent, amount, false, null);
    }

    // Constructeur complet avec option d'enregistrement
    public CardPaymentDialog(JFrame parent, double amount, boolean withSaveOption, CardSaveCallback callback) {
        super(parent, "💳 Paiement par carte bancaire", true);
        this.withSaveOption = withSaveOption;
        this.saveCallback = callback;
        initUI(amount);
        setLocationRelativeTo(parent);
    }

    private void initUI(double amount) {
        setSize(480, 620);
        setResizable(false);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel titleLabel = new JLabel("💳 Paiement sécurisé", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel amountLabel = new JLabel(String.format("Montant à payer : %.2f DH", amount), SwingConstants.CENTER);
        amountLabel.setForeground(UITheme.GOLD);
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        amountLabel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // Numéro de carte
        JLabel cardLabel = new JLabel("💳 Numéro de carte");
        cardLabel.setForeground(Color.WHITE);
        cardLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        gbc.gridy++;
        formPanel.add(cardLabel, gbc);

        cardNumberField = new JTextField();
        cardNumberField.setBackground(UITheme.INPUT_BG);
        cardNumberField.setForeground(Color.WHITE);
        cardNumberField.setCaretColor(Color.WHITE);
        cardNumberField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        cardNumberField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(10, 12, 10, 12)
        ));
        
        // Formatage automatique du numéro de carte (XXXX XXXX XXXX XXXX)
        ((AbstractDocument) cardNumberField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset);
                newString = newString.replaceAll("\\s", "");
                
                if (newString.length() > 16) {
                    return;
                }
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < newString.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(newString.charAt(i));
                }
                
                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, formatted.toString(), attr);
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset + length);
                newString = newString.replaceAll("\\s", "");
                
                if (newString.length() > 16) {
                    return;
                }
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < newString.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(newString.charAt(i));
                }
                
                fb.replace(0, fb.getDocument().getLength(), formatted.toString(), attrs);
            }
        });
        
        cardNumberField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c)) {
                    e.consume();
                }
            }
        });
        gbc.gridy++;
        formPanel.add(cardNumberField, gbc);

        // Info panel (expiration + CVV)
        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        infoPanel.setOpaque(false);

        // Expiration avec formatage MM/AA
        JPanel expiryPanel = new JPanel(new BorderLayout(5, 5));
        expiryPanel.setOpaque(false);
        JLabel expiryLabel = new JLabel("📅 Date d'expiration (MM/AA)");
        expiryLabel.setForeground(UITheme.MUTED);
        expiryLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        expiryField = new JTextField();
        expiryField.setBackground(UITheme.INPUT_BG);
        expiryField.setForeground(Color.WHITE);
        expiryField.setCaretColor(Color.WHITE);
        expiryField.setHorizontalAlignment(JTextField.CENTER);
        expiryField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        expiryField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(10, 8, 10, 8)
        ));
        
        // Formatage automatique MM/AA
        ((AbstractDocument) expiryField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset);
                
                String cleanString = newString.replace("/", "");
                
                if (cleanString.length() > 4) {
                    return;
                }
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleanString.length(); i++) {
                    if (i == 2 && cleanString.length() > 2) {
                        formatted.append("/");
                    }
                    formatted.append(cleanString.charAt(i));
                }
                
                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, formatted.toString(), attr);
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String newString = current.substring(0, offset) + text + current.substring(offset + length);
                
                String cleanString = newString.replace("/", "");
                
                if (cleanString.length() > 4) {
                    return;
                }
                
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleanString.length(); i++) {
                    if (i == 2 && cleanString.length() > 2) {
                        formatted.append("/");
                    }
                    formatted.append(cleanString.charAt(i));
                }
                
                fb.replace(0, fb.getDocument().getLength(), formatted.toString(), attrs);
            }
        });
        
        expiryField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c)) {
                    e.consume();
                }
            }
        });
        
        expiryPanel.add(expiryLabel, BorderLayout.NORTH);
        expiryPanel.add(expiryField, BorderLayout.CENTER);

        // CVV avec œil
        JPanel cvvPanel = new JPanel(new BorderLayout(5, 5));
        cvvPanel.setOpaque(false);
        JLabel cvvLabel = new JLabel("🔒 CVV (3 chiffres)");
        cvvLabel.setForeground(UITheme.MUTED);
        cvvLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        JPanel cvvFieldPanel = new JPanel(new BorderLayout());
        cvvFieldPanel.setOpaque(false);
        cvvFieldPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(0, 0, 0, 0)
        ));
        
        cvvField = new JPasswordField();
        cvvField.setBackground(UITheme.INPUT_BG);
        cvvField.setForeground(Color.WHITE);
        cvvField.setCaretColor(Color.WHITE);
        cvvField.setHorizontalAlignment(JTextField.CENTER);
        cvvField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cvvField.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        cvvField.setEchoChar('•');
        
        eyeLabel = new JLabel("👁️");
        eyeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeLabel.setForeground(UITheme.MUTED);
        eyeLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 8));
        
        eyeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleCvvVisibility();
                cvvField.requestFocusInWindow();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                eyeLabel.setForeground(UITheme.GOLD);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!cvvVisible) {
                    eyeLabel.setForeground(UITheme.MUTED);
                }
            }
        });
        
        cvvField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) || cvvField.getPassword().length >= 3) {
                    e.consume();
                }
            }
        });
        
        cvvFieldPanel.add(cvvField, BorderLayout.CENTER);
        cvvFieldPanel.add(eyeLabel, BorderLayout.EAST);
        
        cvvPanel.add(cvvLabel, BorderLayout.NORTH);
        cvvPanel.add(cvvFieldPanel, BorderLayout.CENTER);

        infoPanel.add(expiryPanel);
        infoPanel.add(cvvPanel);
        gbc.gridy++;
        formPanel.add(infoPanel, gbc);

        // Nom du titulaire
        JLabel holderLabel = new JLabel("👤 Titulaire de la carte");
        holderLabel.setForeground(Color.WHITE);
        holderLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        gbc.gridy++;
        formPanel.add(holderLabel, gbc);

        cardHolderField = new JTextField();
        cardHolderField.setBackground(UITheme.INPUT_BG);
        cardHolderField.setForeground(Color.WHITE);
        cardHolderField.setCaretColor(Color.WHITE);
        cardHolderField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cardHolderField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(10, 12, 10, 12)
        ));
        gbc.gridy++;
        formPanel.add(cardHolderField, gbc);

        // Checkbox pour enregistrer la carte
        if (withSaveOption) {
            saveCardCheckbox = new JCheckBox("💾 Enregistrer cette carte pour les prochains paiements");
            saveCardCheckbox.setForeground(Color.WHITE);
            saveCardCheckbox.setBackground(UITheme.CARD);
            saveCardCheckbox.setOpaque(false);
            saveCardCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
            gbc.gridy++;
            formPanel.add(saveCardCheckbox, gbc);
        }

        // Security note
        JLabel securityNote = new JLabel("🔒 Paiement 100% sécurisé - Transaction simulée");
        securityNote.setForeground(UITheme.SUCCESS);
        securityNote.setFont(new Font("SansSerif", Font.PLAIN, 11));
        securityNote.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy++;
        formPanel.add(securityNote, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        payBtn = UITheme.primaryButton("✅ Payer " + String.format("%.2f DH", amount));
        cancelBtn = UITheme.blueButton("❌ Annuler");

        buttonPanel.add(payBtn);
        buttonPanel.add(cancelBtn);

        root.add(titleLabel, BorderLayout.NORTH);
        root.add(amountLabel, BorderLayout.NORTH);
        root.add(formPanel, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(root);

        payBtn.addActionListener(e -> validateAndPay());
        cancelBtn.addActionListener(e -> {
            logger.debug("Paiement annulé par l'utilisateur");
            paymentConfirmed = false;
            dispose();
        });
    }

    private void toggleCvvVisibility() {
        cvvVisible = !cvvVisible;
        
        if (cvvVisible) {
            cvvField.setEchoChar((char) 0);
            eyeLabel.setText("🙈");
            eyeLabel.setForeground(UITheme.GOLD);
        } else {
            cvvField.setEchoChar('•');
            eyeLabel.setText("👁️");
            eyeLabel.setForeground(UITheme.MUTED);
        }
    }

    private boolean isValidDate(String expiry) {
        try {
            String[] parts = expiry.split("/");
            if (parts.length != 2) return false;
            
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            
            if (month < 1 || month > 12) {
                return false;
            }
            
            int fullYear = 2000 + year;
            YearMonth yearMonth = YearMonth.of(fullYear, month);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String detectCardBrand(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return "CARTE";
        String firstDigit = cardNumber.substring(0, 1);
        if (firstDigit.equals("4")) return "VISA";
        if (cardNumber.startsWith("5")) return "MASTERCARD";
        if (cardNumber.startsWith("3")) return "AMEX";
        return "CARTE";
    }

    private void validateAndPay() {
        String cvv = new String(cvvField.getPassword()).trim();
        String cardNumber = cardNumberField.getText().trim().replaceAll("\\s", "");
        String expiry = expiryField.getText().trim();
        String cardHolder = cardHolderField.getText().trim();

        paymentLogger.info("💳 Tentative de paiement par carte - Montant en cours");

        if (cardNumber.isEmpty() || cardNumber.length() < 13 || cardNumber.length() > 16) {
            paymentLogger.warn("Numéro de carte invalide - Longueur: {}", cardNumber.length());
            showError("Numéro de carte invalide (13-16 chiffres)");
            return;
        }

        if (!expiry.matches("\\d{2}/\\d{2}")) {
            paymentLogger.warn("Format date expiration invalide - Expiry: {}", expiry);
            showError("Format date invalide (MM/AA)\nExemple: 12/25");
            return;
        }

        if (!isValidDate(expiry)) {
            paymentLogger.warn("Date expiration invalide - Expiry: {}", expiry);
            showError("Date d'expiration invalide");
            return;
        }

        String[] expiryParts = expiry.split("/");
        int month = Integer.parseInt(expiryParts[0]);
        int year = Integer.parseInt(expiryParts[1]);

        java.time.YearMonth current = java.time.YearMonth.now();
        int currentYear = current.getYear() % 100;
        int currentMonth = current.getMonthValue();

        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            paymentLogger.warn("Carte expirée - Expiry: {}/{}", month, year);
            showError("Carte expirée");
            return;
        }

        if (cvv.isEmpty() || cvv.length() < 3) {
            paymentLogger.warn("CVV invalide - Longueur: {}", cvv.length());
            showError("CVV invalide (3 chiffres)");
            return;
        }

        if (cardHolder.isEmpty()) {
            paymentLogger.warn("Nom du titulaire manquant");
            showError("Nom du titulaire requis");
            return;
        }

        // Sauvegarder la carte si demandé
        if (withSaveOption && saveCardCheckbox != null && saveCardCheckbox.isSelected() && saveCallback != null) {
            String last4 = cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : cardNumber;
            String brand = detectCardBrand(cardNumber);
            CardInfo cardInfo = new CardInfo(last4, brand, cardHolder, month, year);
            paymentLogger.info("💾 Demande d'enregistrement de carte - Marque: {}, Last4: {}", brand, last4);
            saveCallback.onCardSaved(cardInfo);
        }

        paymentLogger.info("✅ Paiement par carte validé - Marque: {}", detectCardBrand(cardNumber));
        
        payBtn.setEnabled(false);
        payBtn.setText("⏳ Traitement en cours...");

        Timer timer = new Timer(1500, e -> {
            paymentConfirmed = true;
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showError(String message) {
        paymentLogger.error("❌ Erreur paiement carte: {}", message);
        JOptionPane.showMessageDialog(this, message, "Erreur de paiement", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isPaymentConfirmed() {
        return paymentConfirmed;
    }
}