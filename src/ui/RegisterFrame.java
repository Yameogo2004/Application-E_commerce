package ui;

import Client.ClientSocketService;
import Client.ClientHashUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegisterFrame extends JFrame {

    private static final Logger logger = LogManager.getLogger(RegisterFrame.class);
    private static final Logger authLogger = LogManager.getLogger("com.chrionline.auth");

    private final ClientSocketService clientService;
    private final JFrame backFrame;

    private JTextField nomField;
    private JTextField prenomField;
    private JTextField emailField;
    private JPanel passwordPanel;
    private JPanel confirmPasswordPanel;
    private JTextField addressField;
    private JTextField phoneField;
    private JComboBox<String> countryCodeCombo;
    private JTextField villeField;
    private JLabel statusLabel;
    
    // Composants pour l'indicateur de force du mot de passe
    private JProgressBar strengthBar;
    private JLabel strengthLabel;

    public RegisterFrame(ClientSocketService clientService, JFrame backFrame) {
        this.clientService = clientService;
        this.backFrame = backFrame;
        initUI();
    }

    /**
     * Renderer personnalisé pour l'affichage des pays avec drapeau
     */
    private class CountryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            
            if (value != null) {
                String text = value.toString();
                label.setText(text);
                label.setFont(new Font("SansSerif", Font.PLAIN, 12));
                if (isSelected) {
                    label.setBackground(new Color(67, 139, 208));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(UITheme.INPUT_BG);
                    label.setForeground(Color.WHITE);
                }
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            }
            return label;
        }
    }

    private void initUI() {
        setTitle("📝 ChriOnline - Inscription");
        setSize(1000, 980);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(500, 780));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(22, 35, 22, 35)
        ));

         
        JLabel title = new JLabel("📝 Créer un compte");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Inscription client");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(UITheme.normalFont());

        nomField = createStyledTextField("👤 Nom");
        prenomField = createStyledTextField("👤 Prénom");
        emailField = createStyledTextField("📧 Email");

        passwordPanel = UITheme.createPasswordFieldWithEye("🔒 Mot de passe");
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(500, 60));
        passwordPanel.setPreferredSize(new Dimension(500, 60));

        // ========== INDICATEUR DE FORCE DU MOT DE PASSE ==========
        JPanel strengthPanel = new JPanel();
        strengthPanel.setLayout(new BoxLayout(strengthPanel, BoxLayout.Y_AXIS));
        strengthPanel.setOpaque(false);
        strengthPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        strengthPanel.setMaximumSize(new Dimension(500, 55));
        strengthPanel.setPreferredSize(new Dimension(500, 55));

        strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(false);
        strengthBar.setForeground(new Color(150, 150, 150));
        strengthBar.setBackground(new Color(60, 65, 75));
        strengthBar.setPreferredSize(new Dimension(500, 4));
        strengthBar.setMaximumSize(new Dimension(500, 4));

        strengthLabel = new JLabel(" ");
        strengthLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        strengthLabel.setForeground(new Color(150, 150, 150));
        strengthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        strengthPanel.add(strengthBar);
        strengthPanel.add(Box.createVerticalStrut(6));
        strengthPanel.add(strengthLabel);

        // Listener pour mettre à jour l'indicateur en temps réel
        JPasswordField pwdField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        if (pwdField != null) {
            pwdField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
                private void updateStrength() {
                    String password = new String(pwdField.getPassword()).trim();
                    updatePasswordStrengthIndicator(password);
                }
            });
        }
        // ========================================================

        confirmPasswordPanel = UITheme.createPasswordFieldWithEye("🔒 Confirmer le mot de passe");
        confirmPasswordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPasswordPanel.setMaximumSize(new Dimension(500, 60));
        confirmPasswordPanel.setPreferredSize(new Dimension(500, 60));

        addressField = createStyledTextField("🏠 Adresse");

        // ========== CHAMP TÉLÉPHONE AVEC INDICATIF PAYS ==========
        JPanel phoneContainer = new JPanel();
        phoneContainer.setLayout(new BoxLayout(phoneContainer, BoxLayout.X_AXIS));
        phoneContainer.setOpaque(false);
        phoneContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        phoneContainer.setMaximumSize(new Dimension(500, 48));
        phoneContainer.setPreferredSize(new Dimension(500, 48));

        // Sélecteur de code pays avec drapeaux et format amélioré
        String[] countries = {
            "🇲🇦  Maroc                +212",
            "🇫🇷  France               +33",
            "🇩🇿  Algérie              +213", 
            "🇹🇳  Tunisie              +216",
            "🇸🇳  Sénégal              +221",
            "🇨🇮  Côte d'Ivoire        +225",
            "🇨🇦  Canada               +1",
            "🇧🇪  Belgique             +32",
            "🇨🇭  Suisse               +41",
            "🇩🇪  Allemagne            +49",
            "🇪🇸  Espagne              +34",
            "🇮🇹  Italie               +39",
            "🇬🇧  Royaume-Uni          +44",
            "🇺🇸  États-Unis           +1",
            "🇦🇪  Émirats              +971"
        };
        countryCodeCombo = new JComboBox<>(countries);
        countryCodeCombo.setBackground(UITheme.INPUT_BG);
        countryCodeCombo.setForeground(Color.WHITE);
        countryCodeCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countryCodeCombo.setPreferredSize(new Dimension(200, 38));
        countryCodeCombo.setMaximumSize(new Dimension(200, 38));
        countryCodeCombo.setRenderer(new CountryRenderer());

        // Champ numéro de téléphone
        phoneField = UITheme.textField();
        phoneField.setBorder(UITheme.titledBorder("Numéro"));
        phoneField.setPreferredSize(new Dimension(280, 38));
        phoneField.setMaximumSize(new Dimension(280, 38));
        
     // Filtre pour n'accepter que les chiffres et limiter à 15
        ((AbstractDocument) phoneField.getDocument()).setDocumentFilter(new DocumentFilter() {
            private final int MAX_LENGTH = 15;  // Maximum de chiffres
            
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                if (text != null && text.matches("\\d*")) {
                    int newLength = fb.getDocument().getLength() + text.length();
                    if (newLength <= MAX_LENGTH) {
                        super.insertString(fb, offset, text, attr);
                    }
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text != null && text.matches("\\d*")) {
                    int newLength = fb.getDocument().getLength() - length + text.length();
                    if (newLength <= MAX_LENGTH) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            }
        });

        phoneContainer.add(countryCodeCombo);
        phoneContainer.add(Box.createHorizontalStrut(12));
        phoneContainer.add(phoneField);
        // =========================================================

        villeField = createStyledTextField("🏙️ Ville");

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(UITheme.smallFont());

        JButton registerBtn = UITheme.primaryButton("✅ S'INSCRIRE");
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(500, 45));
        registerBtn.setPreferredSize(new Dimension(500, 45));

        JButton backBtn = UITheme.blueButton("← RETOUR");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(500, 45));
        backBtn.setPreferredSize(new Dimension(500, 45));

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(18));
        card.add(nomField);
        card.add(Box.createVerticalStrut(10));
        card.add(prenomField);
        card.add(Box.createVerticalStrut(10));
        card.add(emailField);
        card.add(Box.createVerticalStrut(10));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(2));
        card.add(strengthPanel);
        card.add(Box.createVerticalStrut(8));
        card.add(confirmPasswordPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(addressField);
        card.add(Box.createVerticalStrut(10));
        card.add(phoneContainer);
        card.add(Box.createVerticalStrut(10));
        card.add(villeField);
        card.add(Box.createVerticalStrut(12));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(backBtn);

        root.add(card);
        setContentPane(root);

        registerBtn.addActionListener(e -> register());
        backBtn.addActionListener(e -> {
            backFrame.setVisible(true);
            dispose();
        });
    }

    private JTextField createStyledTextField(String title) {
        JTextField field = UITheme.textField();
        field.setMaximumSize(new Dimension(500, 48));
        field.setPreferredSize(new Dimension(500, 48));
        field.setBorder(UITheme.titledBorder(title));
        return field;
    }

    /**
     * Évalue la force du mot de passe et retourne un score (0-4)
     */
    private int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) return 0;
        
        int score = 0;
        
        // Longueur (2 points maximum)
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        
        // Critères de complexité
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[@#$%!?&*+=_-].*")) score++;
        
        // Score maximum de 4 pour l'affichage
        return Math.min(score, 4);
    }

    /**
     * Retourne la couleur associée à la force du mot de passe
     */
    private Color getStrengthColor(int strength) {
        switch (strength) {
            case 0: return new Color(150, 150, 150); // Gris
            case 1: return new Color(230, 80, 80);   // Rouge
            case 2: return new Color(255, 165, 0);   // Orange
            case 3: return new Color(100, 200, 100); // Vert clair
            case 4: return new Color(50, 150, 50);   // Vert foncé
            default: return new Color(150, 150, 150);
        }
    }

    /**
     * Retourne le texte associé à la force du mot de passe
     */
    private String getStrengthText(int strength) {
        switch (strength) {
            case 0: return "Saisissez un mot de passe";
            case 1: return "⚠️ Mot de passe faible";
            case 2: return "⚡ Mot de passe moyen";
            case 3: return "✅ Mot de passe fort";
            case 4: return "✅✅ Mot de passe très fort";
            default: return "";
        }
    }

    /**
     * Met à jour l'indicateur de force du mot de passe
     */
    private void updatePasswordStrengthIndicator(String password) {
        int strength = getPasswordStrength(password);
        int percentage = strength * 25;
        
        strengthBar.setValue(percentage);
        strengthBar.setForeground(getStrengthColor(strength));
        strengthLabel.setText(getStrengthText(strength));
        strengthLabel.setForeground(getStrengthColor(strength));
    }

    /**
     * Vérifie si le mot de passe est suffisamment fort
     * Critères: 12+ caractères, majuscule, minuscule, chiffre, caractère spécial
     */
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 12) {
            return false;
        }
        
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[@#$%!?&*+=_-].*");
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Vérifie si le téléphone est valide selon le pays choisi
     */
    private boolean isValidPhone(String phone, String countrySelection) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        
        // Supprimer les espaces
        String phoneDigits = phone.replaceAll("\\s", "");
        
        // Vérifier que ce sont uniquement des chiffres
        if (!phoneDigits.matches("\\d+")) {
            return false;
        }
        
        int length = phoneDigits.length();

     // 🔐 Vérifier que la longueur ne dépasse pas 15 chiffres
     if (length > 15) {
         return false;
     }
        
        // Vérification selon le pays
        if (countrySelection.contains("+212")) { // Maroc
            return length == 9 || length == 10;
        } else if (countrySelection.contains("+33")) { // France
            return length == 9;
        } else if (countrySelection.contains("+213")) { // Algérie
            return length == 9;
        } else if (countrySelection.contains("+216")) { // Tunisie
            return length == 8;
        } else if (countrySelection.contains("+221")) { // Sénégal
            return length == 9;
        } else if (countrySelection.contains("+225")) { // Côte d'Ivoire
            return length == 8;
        } else if (countrySelection.contains("+44")) { // Royaume-Uni
            return length == 10;
        } else if (countrySelection.contains("+1")) { // USA/Canada
            return length == 10;
        } else if (countrySelection.contains("+32")) { // Belgique
            return length == 9;
        } else if (countrySelection.contains("+41")) { // Suisse
            return length == 9;
        } else if (countrySelection.contains("+49")) { // Allemagne
            return length == 10 || length == 11;
        } else if (countrySelection.contains("+34")) { // Espagne
            return length == 9;
        } else if (countrySelection.contains("+39")) { // Italie
            return length == 10;
        } else if (countrySelection.contains("+971")) { // Émirats
            return length == 9;
        }
        
        // Par défaut : entre 8 et 12 chiffres
        return length >= 8 && length <= 12;
    }

    /**
     * Extrait le code pays depuis la sélection
     */
    private String extractCountryCode(String countrySelection) {
        if (countrySelection == null) return "";
        int start = countrySelection.lastIndexOf("+");
        int end = countrySelection.length();
        if (start != -1 && start < end) {
            return countrySelection.substring(start, end).trim();
        }
        return "";
    }

    private void register() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();

        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        JPasswordField confirmPasswordField = UITheme.getPasswordFieldFromPanel(confirmPasswordPanel);

        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String countrySelection = (String) countryCodeCombo.getSelectedItem();
        String ville = villeField.getText().trim();

        // Validation des champs obligatoires
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()
                || address.isEmpty() || phone.isEmpty() || ville.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs.");
            logger.warn("Tentative d'inscription avec champs manquants - email: {}", email.isEmpty() ? "non renseigné" : email);
            return;
        }

        // Validation email
        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Email invalide.");
            logger.warn("Tentative d'inscription avec email invalide - email: {}", email);
            return;
        }

        // 🔐 Validation du téléphone (selon le pays)
        if (!isValidPhone(phone, countrySelection)) {
            statusLabel.setText("📱 Téléphone invalide pour " + countrySelection.substring(0, Math.min(30, countrySelection.length())) + " (vérifiez le nombre de chiffres).");
            logger.warn("Tentative d'inscription avec téléphone invalide - email: {}, phone: {}", email, phone);
            return;
        }

        // 🔐 Validation de la force du mot de passe
        if (!isStrongPassword(password)) {
            statusLabel.setText("⚠️ Mot de passe trop faible : 12+ caractères, majuscule, minuscule, chiffre et caractère spécial requis.");
            strengthLabel.setText("❌ " + getStrengthText(1));
            strengthLabel.setForeground(getStrengthColor(1));
            strengthBar.setValue(25);
            strengthBar.setForeground(getStrengthColor(1));
            logger.warn("Tentative d'inscription avec mot de passe faible - email: {}", email);
            return;
        }

        // Vérification de la correspondance des mots de passe
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Les mots de passe ne correspondent pas.");
            passwordField.setText("");
            confirmPasswordField.setText("");
            updatePasswordStrengthIndicator("");
            logger.warn("Tentative d'inscription avec mots de passe non concordants - email: {}", email);
            return;
        }

        if (!clientService.connect()) {
            statusLabel.setText("Serveur inaccessible.");
            logger.error("Serveur inaccessible lors de l'inscription - email: {}", email);
            return;
        }

        // 🔐 HACHER LE MOT DE PASSE CÔTÉ CLIENT AVANT ENVOI
        String hashedPassword = ClientHashUtil.hashPasswordClient(password);
        
        if (hashedPassword == null) {
            statusLabel.setText("Erreur de hachage du mot de passe.");
            logger.error("Erreur de hachage SHA-256 - email: {}", email);
            return;
        }
        
        // Extraire l'indicatif pour stockage
        String countryCode = extractCountryCode(countrySelection);
        String fullPhoneNumber = countryCode + phone;
        
        logger.debug("Hash SHA-256 généré côté client (longueur: {})", hashedPassword.length());
        logger.debug("Téléphone complet: {}", fullPhoneNumber);

        authLogger.info("📝 Tentative d'inscription - email: {}", email);
        logger.debug("Détails inscription - Nom: {}, Prénom: {}, Ville: {}, Tél: {}", nom, prenom, ville, fullPhoneNumber);

        String response = clientService.register(
                nom, prenom, email, hashedPassword, address, fullPhoneNumber, ville
        );

        if ("REGISTER_SUCCESS_OTP_SENT".equals(response)) {
            authLogger.info("✅ Inscription réussie - email: {}, OTP envoyé", email);
            
            passwordField.setText("");
            confirmPasswordField.setText("");
            
            JOptionPane.showMessageDialog(this,
                    "✅ Compte créé.\nUn code OTP a été envoyé à votre email.",
                    "Vérification requise", JOptionPane.INFORMATION_MESSAGE);

            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();

        } else if ("ERROR:EMAIL_ALREADY_EXISTS".equals(response)) {
            authLogger.warn("❌ Inscription échouée - Email déjà utilisé: {}", email);
            statusLabel.setText("Cet email est déjà utilisé.");

        } else if ("REGISTER_SUCCESS_BUT_OTP_FAILED".equals(response)) {
            authLogger.error("❌ Inscription réussie mais échec envoi OTP - email: {}", email);
            JOptionPane.showMessageDialog(this,
                    "Compte créé, mais l'envoi du code a échoué.\nEssayez de vous reconnecter puis renvoyez le code.",
                    "Attention", JOptionPane.WARNING_MESSAGE);

            setVisible(false);
            new OtpFrame(clientService, email, backFrame).setVisible(true);
            dispose();

        } else {
            authLogger.error("❌ Inscription échouée - email: {}, Erreur: {}", email, response);
            statusLabel.setText("Erreur : " + response);
        }
    }
}