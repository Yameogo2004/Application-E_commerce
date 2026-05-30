package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import java.awt.*;

public class EditProfileFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final ProfileFrame parentFrame;

    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextField cityField;
    private JLabel titleLabel;

    public EditProfileFrame(ClientSocketService clientService, AppSession session, ProfileFrame parentFrame) {
        this.clientService = clientService;
        this.session = session;
        this.parentFrame = parentFrame;
        initUI();
        loadUserData();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("profile.edit.title"));
        setSize(500, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(420, 450));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        titleLabel = new JLabel("✏️ " + LanguageManager.getInstance().getText("profile.edit.title"));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        nameField = createStyledTextField(LanguageManager.getInstance().getText("profile.name"));
        emailField = createStyledTextField(LanguageManager.getInstance().getText("profile.email"));
        phoneField = createStyledTextField(LanguageManager.getInstance().getText("profile.phone"));
        addressField = createStyledTextField(LanguageManager.getInstance().getText("profile.address"));
        cityField = createStyledTextField(LanguageManager.getInstance().getText("profile.city"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        JButton saveBtn = UITheme.primaryButton("💾 " + LanguageManager.getInstance().getText("profile.save"));
        JButton cancelBtn = UITheme.blueButton("✖ " + LanguageManager.getInstance().getText("profile.cancel"));

        saveBtn.addActionListener(e -> saveProfile());
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(nameField);
        card.add(Box.createVerticalStrut(12));
        card.add(emailField);
        card.add(Box.createVerticalStrut(12));
        card.add(phoneField);
        card.add(Box.createVerticalStrut(12));
        card.add(addressField);
        card.add(Box.createVerticalStrut(12));
        card.add(cityField);
        card.add(Box.createVerticalStrut(20));
        card.add(buttonPanel);

        root.add(card);
        setContentPane(root);
    }

    private JTextField createStyledTextField(String title) {
        JTextField field = UITheme.textField();
        field.setMaximumSize(new Dimension(340, 45));
        field.setPreferredSize(new Dimension(340, 45));
        field.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                title
        ));
        return field;
    }

    private void loadUserData() {
        String response = clientService.getProfile(session.getClientId());

        if (response == null || response.startsWith("ERROR") || !response.startsWith("PROFILE_DATA:")) {
            nameField.setText("Client #" + session.getClientId());
            emailField.setText("");
            phoneField.setText("");
            addressField.setText("");
            cityField.setText("");
            return;
        }

        String data = response.substring("PROFILE_DATA:".length());
        String[] parts = data.split(";");

        nameField.setText(parts.length > 0 ? parts[0] : "");
        emailField.setText(parts.length > 1 ? parts[1] : "");
        phoneField.setText(parts.length > 2 ? parts[2] : "");
        addressField.setText(parts.length > 3 ? parts[3] : "");
        cityField.setText(parts.length > 4 ? parts[4] : "");
    }

    private void saveProfile() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    LanguageManager.getInstance().getText("register.error.empty"),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String response = clientService.updateProfile(
                session.getClientId(),
                name,
                email,
                phone,
                address,
                city
        );

        if ("UPDATE_PROFILE_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this,
                    LanguageManager.getInstance().getText("profile.update.success"),
                    LanguageManager.getInstance().getText("profile.update.success"),
                    JOptionPane.INFORMATION_MESSAGE);

            if (parentFrame != null) {
                parentFrame.refreshProfile();
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    LanguageManager.getInstance().getText("profile.update.error") + "\n" + response,
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("profile.edit.title"));
        titleLabel.setText("✏️ " + LanguageManager.getInstance().getText("profile.edit.title"));

        nameField.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                LanguageManager.getInstance().getText("profile.name")
        ));
        emailField.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                LanguageManager.getInstance().getText("profile.email")
        ));
        phoneField.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                LanguageManager.getInstance().getText("profile.phone")
        ));
        addressField.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                LanguageManager.getInstance().getText("profile.address")
        ));
        cityField.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                LanguageManager.getInstance().getText("profile.city")
        ));

        revalidate();
        repaint();
    }
}