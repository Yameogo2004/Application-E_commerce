package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import java.awt.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProfileFrame extends LanguageAwareFrame {

    private static final Logger logger = LogManager.getLogger(ProfileFrame.class);
    private static final Logger authLogger = LogManager.getLogger("com.chrionline.auth");

    private final ClientSocketService clientService;
    private final AppSession session;
    private final JFrame backFrame;

    private JLabel welcomeLabel;
    private JLabel nameLabel;
    private JLabel emailLabel;
    private JLabel phoneLabel;
    private JLabel addressLabel;
    private JLabel cityLabel;
    private JButton editBtn;
    private JButton ordersBtn;
    private JButton backBtn;
    private JLabel titleLabel;

    public ProfileFrame(ClientSocketService clientService, AppSession session, JFrame backFrame) {
        this.clientService = clientService;
        this.session = session;
        this.backFrame = backFrame;
        logger.debug("Ouverture du profil - Utilisateur: {}", session.getClientId());
        initUI();
        refreshProfile();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("profile.title"));
        setSize(600, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = UITheme.cardPanel();
        header.setLayout(new BorderLayout());

        titleLabel = new JLabel("👤 " + LanguageManager.getInstance().getText("profile.title"));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(titleLabel, BorderLayout.WEST);

        root.add(header, BorderLayout.NORTH);

        JPanel content = UITheme.cardPanel();
        content.setLayout(new BorderLayout(15, 15));
        content.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel welcomePanel = new JPanel();
        welcomePanel.setOpaque(false);
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));

        welcomeLabel = new JLabel(LanguageManager.getInstance().getText("profile.welcome") + " !");
        welcomeLabel.setForeground(UITheme.GOLD);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        welcomePanel.add(welcomeLabel);
        welcomePanel.add(Box.createVerticalStrut(15));

        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER);
        welcomePanel.add(sep);

        content.add(welcomePanel, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new GridLayout(5, 2, 15, 12));

        infoPanel.add(createLabel("👤 " + LanguageManager.getInstance().getText("profile.name") + " :"));
        nameLabel = createValueLabel("---");
        infoPanel.add(nameLabel);

        infoPanel.add(createLabel("📧 " + LanguageManager.getInstance().getText("profile.email") + " :"));
        emailLabel = createValueLabel("---");
        infoPanel.add(emailLabel);

        infoPanel.add(createLabel("📱 " + LanguageManager.getInstance().getText("profile.phone") + " :"));
        phoneLabel = createValueLabel("---");
        infoPanel.add(phoneLabel);

        infoPanel.add(createLabel("🏠 " + LanguageManager.getInstance().getText("profile.address") + " :"));
        addressLabel = createValueLabel("---");
        infoPanel.add(addressLabel);

        infoPanel.add(createLabel("🏙️ " + LanguageManager.getInstance().getText("profile.city") + " :"));
        cityLabel = createValueLabel("---");
        infoPanel.add(cityLabel);

        content.add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        editBtn = UITheme.blueButton("✏️ " + LanguageManager.getInstance().getText("profile.edit"));
        ordersBtn = UITheme.primaryButton("📦 " + LanguageManager.getInstance().getText("profile.orders"));
        backBtn = UITheme.dangerButton("← " + LanguageManager.getInstance().getText("cart.back"));

        editBtn.addActionListener(e -> {
            logger.debug("Ouverture du formulaire d'édition du profil - Utilisateur: {}", session.getClientId());
            new EditProfileFrame(clientService, session, this).setVisible(true);
        });
        
        ordersBtn.addActionListener(e -> {
            logger.debug("Consultation de l'historique des commandes - Utilisateur: {}", session.getClientId());
            new OrderHistoryFrame(clientService, session).setVisible(true);
        });
        
        backBtn.addActionListener(e -> {
            logger.debug("Retour à l'écran précédent depuis le profil");
            dispose();
            backFrame.setVisible(true);
        });

        buttonPanel.add(editBtn);
        buttonPanel.add(ordersBtn);
        buttonPanel.add(backBtn);

        content.add(buttonPanel, BorderLayout.SOUTH);

        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        return label;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.MUTED);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }

    public void refreshProfile() {
        String response = clientService.getProfile(session.getClientId());

        if (response == null || response.startsWith("ERROR")) {
            logger.warn("Impossible de charger le profil - Utilisateur: {}, Erreur: {}", session.getClientId(), response);
            welcomeLabel.setText(LanguageManager.getInstance().getText("profile.welcome") + " Client #" + session.getClientId());
            nameLabel.setText("Client #" + session.getClientId());
            emailLabel.setText("...");
            phoneLabel.setText("...");
            addressLabel.setText("...");
            cityLabel.setText("...");
            return;
        }

        if (!response.startsWith("PROFILE_DATA:")) {
            logger.error("Format de réponse inattendu pour le profil - Utilisateur: {}", session.getClientId());
            return;
        }

        String data = response.substring("PROFILE_DATA:".length());
        String[] parts = data.split(";");

        String fullName = parts.length > 0 ? parts[0] : "";
        String email = parts.length > 1 ? parts[1] : "";
        String phone = parts.length > 2 ? parts[2] : "";
        String address = parts.length > 3 ? parts[3] : "";
        String city = parts.length > 4 ? parts[4] : "";

        if (fullName == null || fullName.isBlank()) {
            fullName = "Client #" + session.getClientId();
        }

        authLogger.info("👤 Consultation du profil - Utilisateur: {}, Nom: {}, Email: {}", session.getClientId(), fullName, email);
        
        welcomeLabel.setText(LanguageManager.getInstance().getText("profile.welcome") + " " + fullName);
        nameLabel.setText(fullName);
        emailLabel.setText(email == null || email.isBlank() ? "..." : email);
        phoneLabel.setText(phone == null || phone.isBlank() ? "..." : phone);
        addressLabel.setText(address == null || address.isBlank() ? "..." : address);
        cityLabel.setText(city == null || city.isBlank() ? "..." : city);
        
        logger.debug("Profil chargé - Utilisateur: {}, Nom: {}, Ville: {}", session.getClientId(), fullName, city);

        revalidate();
        repaint();
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("profile.title"));
        titleLabel.setText("👤 " + LanguageManager.getInstance().getText("profile.title"));
        welcomeLabel.setText(LanguageManager.getInstance().getText("profile.welcome") + " " + nameLabel.getText());
        editBtn.setText("✏️ " + LanguageManager.getInstance().getText("profile.edit"));
        ordersBtn.setText("📦 " + LanguageManager.getInstance().getText("profile.orders"));
        backBtn.setText("← " + LanguageManager.getInstance().getText("cart.back"));
        revalidate();
        repaint();
    }
}