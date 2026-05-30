package ui;

import Client.AppSession;
import Client.ClientSocketService;
import Client.ClientHashUtil;
import security.RSAKeyManager;
import security.Signer;
import ui.admin.AdminMainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.security.PrivateKey;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginFrame extends JFrame {

    private static final Logger logger = LogManager.getLogger(LoginFrame.class);
    private static final Logger authLogger = LogManager.getLogger("com.chrionline.auth");

    private final ClientSocketService clientService;
    private JTextField emailField;
    private JPanel passwordPanel;
    private JLabel statusLabel;
    private JLabel title;
    private JLabel subtitle;
    private JButton loginBtn;
    private JButton registerBtn;
    private JButton languageBtn;
    private JButton rsaAdminBtn;
    private JPanel card;

    public LoginFrame(ClientSocketService clientService) {
        this.clientService = clientService;
        initUI();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("login.title"));
        setSize(820, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Panel racine avec dégradé (style moderne)
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, UITheme.BG,
                        getWidth(), getHeight(), new Color(18, 26, 44)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBackground(UITheme.BG);

        // Card centrale
        card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(480, 620));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 44, 36, 44));

        JLabel icon = new JLabel("🛍️");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        icon.setForeground(UITheme.GOLD);

        title = new JLabel(LanguageManager.getInstance().getText("login.title"));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(UITheme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));

        subtitle = new JLabel(LanguageManager.getInstance().getText("login.subtitle"));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        emailField = createStyledTextField(LanguageManager.getInstance().getText("login.email"));
        
        // 🔐 Touche Entrée sur le champ email
        emailField.addActionListener(e -> doLogin());

        passwordPanel = UITheme.createPasswordFieldWithEye(LanguageManager.getInstance().getText("login.password"));
        passwordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(340, 72));
        passwordPanel.setPreferredSize(new Dimension(340, 72));
        
        // 🔐 Touche Entrée sur le champ mot de passe
        JPasswordField pwdField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        if (pwdField != null) {
            pwdField.addActionListener(e -> doLogin());
        }

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(UITheme.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        loginBtn = UITheme.primaryButton("🔐 " + LanguageManager.getInstance().getText("login.button"));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(340, 48));
        loginBtn.setPreferredSize(new Dimension(340, 48));

        registerBtn = UITheme.blueButton("📝 " + LanguageManager.getInstance().getText("login.register"));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(340, 45));
        registerBtn.setPreferredSize(new Dimension(340, 45));

        rsaAdminBtn = UITheme.goldButton("🔐 Connexion Admin RSA");
        rsaAdminBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        rsaAdminBtn.setMaximumSize(new Dimension(340, 45));
        rsaAdminBtn.setPreferredSize(new Dimension(340, 45));

        languageBtn = buildLanguageButton();

        card.add(icon);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(emailField);
        card.add(Box.createVerticalStrut(12));
        card.add(passwordPanel);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(rsaAdminBtn);
        card.add(Box.createVerticalStrut(15));
        card.add(languageBtn);

        root.add(card);
        setContentPane(root);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> {
            setVisible(false);
            new RegisterFrame(clientService, this).setVisible(true);
        });
        rsaAdminBtn.addActionListener(e -> autoAuthenticateWithRSA());
    }

    private JTextField createStyledTextField(String title) {
        JTextField field = UITheme.textField();
        field.setMaximumSize(new Dimension(340, 52));
        field.setPreferredSize(new Dimension(340, 52));
        field.setBorder(UITheme.titledBorder(title));
        return field;
    }

    private JButton buildLanguageButton() {
        JButton btn = new JButton(
                LanguageManager.getCurrentLanguage().getFlag() + " " +
                LanguageManager.getCurrentLanguage().getDisplayName()
        );
        btn.setBackground(UITheme.CARD);
        btn.setForeground(UITheme.MUTED);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            JPopupMenu langMenu = new JPopupMenu();
            langMenu.setBackground(UITheme.CARD);
            for (LanguageManager.Language lang : LanguageManager.Language.values()) {
                JMenuItem item = new JMenuItem(lang.getFlag() + "  " + lang.getDisplayName());
                item.setBackground(UITheme.CARD);
                item.setForeground(UITheme.TEXT);
                item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                item.addActionListener(ev -> {
                    LanguageManager.setLanguage(lang);
                    btn.setText(lang.getFlag() + " " + lang.getDisplayName());
                    refreshUI();
                });
                langMenu.add(item);
            }
            langMenu.show(btn, 0, btn.getHeight());
        });

        return btn;
    }

    private void autoAuthenticateWithRSA() {
        if (!RSAKeyManager.hasAnyPKCS12()) {
            JOptionPane.showMessageDialog(this,
                "❌ Aucun fichier PKCS#12 trouvé.\n\n" +
                "Générez un certificat PKCS#12 avec PKCS12GeneratorUtil\n" +
                "Puis placez le fichier .p12 dans le dossier keys/",
                "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        rsaAdminBtn.setEnabled(false);
        rsaAdminBtn.setText("⏳ Authentification RSA...");

        new Thread(() -> {
            try {
                RSAKeyManager.PrivateKeyData keyData;
                String email;
                PrivateKey privateKey;

                if (RSAKeyManager.hasMultiplePKCS12()) {
                    List<String> admins = RSAKeyManager.getAllPKCS12Emails();
                    String selected = (String) JOptionPane.showInputDialog(LoginFrame.this,
                        "Choisissez votre compte admin :",
                        "Connexion Admin RSA",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        admins.toArray(),
                        admins.get(0));
                    
                    if (selected == null) throw new Exception("Aucun compte sélectionné");
                    
                    JPasswordField pwdField = new JPasswordField();
                    int option = JOptionPane.showConfirmDialog(LoginFrame.this, pwdField,
                        "🔒 Mot de passe pour " + selected,
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                    
                    if (option != JOptionPane.OK_OPTION) throw new Exception("Authentification annulée");
                    
                    String password = new String(pwdField.getPassword());
                    keyData = RSAKeyManager.loadPrivateKeyFromPKCS12(selected, password);
                    email = keyData.email;
                    privateKey = keyData.privateKey;
                } else {
                    keyData = RSAKeyManager.loadDefaultPKCS12();
                    email = keyData.email;
                    privateKey = keyData.privateKey;
                }

                authLogger.info("🔐 Tentative authentification admin RSA - email: {}", email);

                // 🔑 ÉTAPE 1: Se connecter au serveur
                if (!clientService.connect()) {
                    throw new Exception("Impossible de se connecter au serveur");
                }

                // 🔑 ÉTAPE 2: Demander le challenge
                String challengeResponse = clientService.requestAdminChallenge(email);
                if (challengeResponse == null || !challengeResponse.startsWith("CHALLENGE:")) {
                    throw new Exception("Échec récupération challenge: " + challengeResponse);
                }

                String challenge = challengeResponse.substring("CHALLENGE:".length());
                
                // 🔑 ÉTAPE 3: Signer le challenge
                String signature = Signer.sign(challenge, privateKey);
                
                // 🔑 ÉTAPE 4: Vérifier la signature (le serveur doit retourner le token)
                String authResponse = clientService.verifyAdminSignature(email, signature);

                if (authResponse != null && authResponse.startsWith("AUTH_SUCCESS:")) {
                    String[] authParts = authResponse.split(":");
                    int userId = Integer.parseInt(authParts[1]);
                    String sessionToken = authParts.length > 2 ? authParts[2] : null;

                    if (sessionToken == null || sessionToken.isEmpty()) {
                        throw new Exception("Token de session non reçu du serveur");
                    }

                    // 🔑 ÉTAPE 5: Stocker le token dans clientService
                    clientService.setSessionToken(sessionToken);

                    // 🔑 ÉTAPE 6: Récupérer le profil
                    String profileResponse = clientService.getProfile(userId);

                    // 🔑 ÉTAPE 7: Créer la session utilisateur
                    AppSession session = new AppSession();
                    session.setClientId(userId);
                    session.setRole("admin");
                    session.setSessionToken(sessionToken);
                    session.refreshSession();

                    if (profileResponse != null && profileResponse.startsWith("PROFILE_DATA:")) {
                        String data = profileResponse.substring("PROFILE_DATA:".length());
                        String[] fields = data.split(";");
                        if (fields.length >= 1 && !fields[0].isEmpty()) {
                            session.setFullName(fields[0].trim());
                        } else {
                            session.setFullName("Administrateur");
                        }
                    } else {
                        session.setFullName("Administrateur");
                    }

                    authLogger.info("✅ Authentification admin RSA réussie - email: {}, id: {}, token: {}", 
                        email, userId, sessionToken.substring(0, Math.min(20, sessionToken.length())) + "...");

                    SwingUtilities.invokeLater(() -> {
                        AdminMainFrame adminFrame = new AdminMainFrame(clientService, session);
                        adminFrame.setVisible(true);
                        dispose();
                    });

                } else {
                    authLogger.warn("❌ Authentification admin RSA échouée - email: {}", email);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                            "❌ Authentification échouée: " + authResponse,
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                    });
                }

            } catch (Exception e) {
                logger.error("Erreur lors de l'authentification RSA", e);
                SwingUtilities.invokeLater(() -> {
                    String message = e.getMessage();
                    if ("MULTIPLE_KEYS_FOUND".equals(message)) {
                        message = "Plusieurs clés trouvées. Veuillez sélectionner votre compte.";
                    }
                    JOptionPane.showMessageDialog(LoginFrame.this,
                        "❌ Erreur: " + message,
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    rsaAdminBtn.setEnabled(true);
                    rsaAdminBtn.setText("🔐 Connexion Admin RSA");
                });
            }
        }).start();
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        JPasswordField passwordField = UITheme.getPasswordFieldFromPanel(passwordPanel);
        String password = passwordField == null ? "" : new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.empty"));
            return;
        }

        if (!clientService.connect()) {
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.server"));
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Connexion...");

        // 🔐 HACHER LE MOT DE PASSE CÔTÉ CLIENT AVANT ENVOI
        String hashedPassword = ClientHashUtil.hashPasswordClient(password);
        
        // 🔐 Vérifier que le hash n'est pas null
        if (hashedPassword == null) {
            statusLabel.setText("Erreur de hachage du mot de passe.");
            loginBtn.setEnabled(true);
            loginBtn.setText("🔐 " + LanguageManager.getInstance().getText("login.button"));
            return;
        }
        
        logger.debug("Hash SHA-256 généré côté client (longueur: {})", hashedPassword.length());
        
        String response = clientService.login(email, hashedPassword);
        handleLoginResponse(response, email, passwordField);

        loginBtn.setEnabled(true);
        loginBtn.setText("🔐 " + LanguageManager.getInstance().getText("login.button"));
    }

    private void handleLoginResponse(String response, String email, JPasswordField passwordField) {
        if (response != null && response.startsWith("LOGIN_SUCCESS")) {
            try {
                String[] parts = response.split(":");
                int userId = Integer.parseInt(parts[1]);
                String role = parts[2];
                String sessionToken = parts.length > 3 ? parts[3] : null;

                authLogger.info("✅ Connexion réussie - email: {}, id: {}, rôle: {}", email, userId, role);

                AppSession session = new AppSession();
                session.setClientId(userId);
                session.setRole(role);
                if (sessionToken != null) {
                    session.setSessionToken(sessionToken);
                    session.refreshSession();
                    clientService.setSessionToken(sessionToken);
                }

                // 🔐 ACTIVER LE MODE SÉCURISÉ (CHIFFREMENT HYBRIDE PAR NONCES)
                try {
                    clientService.enableSecureMode();
                    authLogger.info("🔐 Communication chiffrée AES-256 activée pour l'utilisateur {}", userId);
                } catch (Exception e) {
                    authLogger.warn("Impossible d'activer le chiffrement: {}", e.getMessage());
                    // L'application continue en mode non chiffré
                }

                String profileResponse = clientService.getProfile(userId);
                if (profileResponse != null && profileResponse.startsWith("PROFILE_DATA:")) {
                    String data = profileResponse.substring("PROFILE_DATA:".length());
                    String[] fields = data.split(";");
                    if (fields.length >= 1) {
                        session.setFullName(fields[0].trim());
                    }
                }

                // Nettoyer le mot de passe en mémoire
                if (passwordField != null) {
                    passwordField.setText("");
                }

                dispose();
                
                if ("admin".equalsIgnoreCase(role)) {
                    AdminMainFrame adminFrame = new AdminMainFrame(clientService, session);
                    adminFrame.setVisible(true);
                } else {
                    new ShopFrame(clientService, session).setVisible(true);
                }

            } catch (Exception e) {
                logger.error("Erreur lors de la connexion", e);
                statusLabel.setText(LanguageManager.getInstance().getText("login.error.invalid"));
            }

        } else if ("ERROR:ACCOUNT_NOT_ACTIVE".equals(response)) {
            authLogger.warn("Compte non activé - email: {}", email);
            JOptionPane.showMessageDialog(this,
                    "Votre compte n'est pas encore activé.\nVeuillez vérifier le code OTP.",
                    "Compte non activé", JOptionPane.WARNING_MESSAGE);
            setVisible(false);
            new OtpFrame(clientService, email, this).setVisible(true);

        } else if (response != null && response.startsWith("ERROR:TOO_MANY_ATTEMPTS")) {
            authLogger.warn("⛔ Trop de tentatives - email: {}", email);
            statusLabel.setText("❌ Trop de tentatives échouées. Réessayez dans 5 minutes.");
            JOptionPane.showMessageDialog(this,
                "Attention ! Vous avez dépassé le nombre de tentatives autorisées.\n" +
                "Veuillez réessayer dans 5 minutes.",
                "Compte temporairement bloqué", JOptionPane.WARNING_MESSAGE);

        } else if (response != null && response.startsWith("ERROR:LOGIN_FAILED:")) {
            String[] parts = response.split(":");
            String remaining = parts.length > 2 ? parts[2] : "?";
            authLogger.warn("Échec connexion - email: {}, tentatives restantes: {}", email, remaining);
            statusLabel.setText("❌ Email ou mot de passe incorrect. Tentatives restantes: " + remaining);
            
            int remainingInt = Integer.parseInt(remaining);
            if (remainingInt <= 2) {
                statusLabel.setForeground(new Color(255, 120, 120));
                statusLabel.setText("⚠️ ATTENTION ! Plus que " + remaining + " tentative(s) avant blocage !");
            } else {
                statusLabel.setForeground(UITheme.RED);
            }
            
            // Nettoyer le champ mot de passe en cas d'erreur
            if (passwordField != null) {
                passwordField.setText("");
            }

        } else {
            authLogger.warn("Échec connexion - email: {}", email);
            statusLabel.setText(LanguageManager.getInstance().getText("login.error.invalid"));
            
            // Nettoyer le champ mot de passe en cas d'erreur
            if (passwordField != null) {
                passwordField.setText("");
            }
        }
    }

    private void refreshUI() {
        setTitle(LanguageManager.getInstance().getText("login.title"));
        title.setText(LanguageManager.getInstance().getText("login.title"));
        subtitle.setText(LanguageManager.getInstance().getText("login.subtitle"));

        emailField.setBorder(UITheme.titledBorder(LanguageManager.getInstance().getText("login.email")));

        JPanel newPasswordPanel = UITheme.createPasswordFieldWithEye(LanguageManager.getInstance().getText("login.password"));
        newPasswordPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        newPasswordPanel.setMaximumSize(new Dimension(340, 72));
        newPasswordPanel.setPreferredSize(new Dimension(340, 72));

        int index = -1;
        for (int i = 0; i < card.getComponentCount(); i++) {
            if (card.getComponent(i) == passwordPanel) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            card.remove(index);
            passwordPanel = newPasswordPanel;
            card.add(passwordPanel, index);
        }

        loginBtn.setText("🔐 " + LanguageManager.getInstance().getText("login.button"));
        registerBtn.setText("📝 " + LanguageManager.getInstance().getText("login.register"));
        rsaAdminBtn.setText("🔐 " + LanguageManager.getInstance().getText("login.rsa.admin"));

        statusLabel.setText(" ");
        card.revalidate();
        card.repaint();
    }
}