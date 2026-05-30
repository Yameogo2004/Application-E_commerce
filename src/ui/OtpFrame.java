package ui;

import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class OtpFrame extends JFrame {

    private final ClientSocketService clientService;
    private final String email;
    private final JFrame backFrame;

    private JTextField otpField;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private Timer countdownTimer;
    private int secondsLeft = 600;

    public OtpFrame(ClientSocketService clientService, String email, JFrame backFrame) {
        this.clientService = clientService;
        this.email = email;
        this.backFrame = backFrame;
        initUI();
        startCountdown();
    }

    private void initUI() {
        setTitle("ChriOnline - Vérification Email");
        setSize(760, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new GridBagLayout());

        JPanel card = UITheme.cardPanel();
        card.setPreferredSize(new Dimension(460, 360));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(28, 36, 28, 36)
        ));

        JLabel icon = new JLabel("📧");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 38));

        JLabel title = new JLabel("Vérification Email");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(UITheme.titleFont());

        JLabel subtitle = new JLabel("Code envoyé à : " + email);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UITheme.MUTED);
        subtitle.setFont(UITheme.smallFont());

        timerLabel = new JLabel("Expire dans : 10:00");
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setForeground(new Color(100, 200, 100));
        timerLabel.setFont(UITheme.smallFont());

        otpField = UITheme.textField();
        otpField.setMaximumSize(new Dimension(340, 56));
        otpField.setPreferredSize(new Dimension(340, 56));
        otpField.setBackground(new Color(58, 62, 74));
        otpField.setForeground(Color.WHITE);
        otpField.setCaretColor(Color.WHITE);
        otpField.setFont(new Font("SansSerif", Font.BOLD, 24));
        otpField.setHorizontalAlignment(JTextField.CENTER);
        otpField.setBorder(UITheme.titledBorder("Code à 6 chiffres"));

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(new Color(255, 120, 120));
        statusLabel.setFont(UITheme.smallFont());

        JButton verifyBtn = UITheme.primaryButton("Vérifier");
        verifyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyBtn.setMaximumSize(new Dimension(340, 48));
        verifyBtn.setPreferredSize(new Dimension(340, 48));

        JButton resendBtn = UITheme.blueButton("Renvoyer le code");
        resendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resendBtn.setMaximumSize(new Dimension(340, 42));
        resendBtn.setPreferredSize(new Dimension(340, 42));

        JButton backBtn = UITheme.blueButton("← Retour au login");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(340, 42));
        backBtn.setPreferredSize(new Dimension(340, 42));

        card.add(icon);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(6));
        card.add(timerLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(otpField);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(verifyBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(resendBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(backBtn);

        root.add(card);
        setContentPane(root);

        verifyBtn.addActionListener(e -> verifyCode());
        resendBtn.addActionListener(e -> resendCode());
        backBtn.addActionListener(e -> {
            if (countdownTimer != null) countdownTimer.stop();
            backFrame.setVisible(true);
            dispose();
        });

        otpField.addActionListener(e -> verifyCode());
    }

    private void verifyCode() {
        String code = otpField.getText().trim();

        if (code.isEmpty() || code.length() != 6) {
            statusLabel.setText("Entrez le code à 6 chiffres.");
            return;
        }

        if (!clientService.connect()) {
            statusLabel.setText("Serveur inaccessible.");
            return;
        }

        String response = clientService.verifyOtp(email, code);

        if ("OTP_VERIFIED".equals(response)) {
            if (countdownTimer != null) countdownTimer.stop();

            JOptionPane.showMessageDialog(this,
                    "Email vérifié ! Vous pouvez maintenant vous connecter.",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

            LoginFrame loginFrame = new LoginFrame(clientService);
            loginFrame.setVisible(true);
            dispose();
        } else {
            statusLabel.setText("Code incorrect ou expiré. Réessayez.");
            otpField.setText("");
        }
    }

    private void resendCode() {
        if (!clientService.connect()) {
            statusLabel.setText("Serveur inaccessible.");
            return;
        }

        String response = clientService.sendOtp(email);

        if ("OTP_SENT".equals(response)) {
            statusLabel.setForeground(new Color(100, 200, 100));
            statusLabel.setText("Nouveau code envoyé !");
            secondsLeft = 600;
            timerLabel.setForeground(new Color(100, 200, 100));
        } else {
            statusLabel.setForeground(new Color(255, 120, 120));
            statusLabel.setText("Erreur envoi. Réessayez.");
        }
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            secondsLeft--;

            int minutes = secondsLeft / 60;
            int seconds = secondsLeft % 60;
            timerLabel.setText(String.format("Expire dans : %02d:%02d", minutes, seconds));

            if (secondsLeft <= 60) {
                timerLabel.setForeground(new Color(255, 120, 120));
            }

            if (secondsLeft <= 0) {
                countdownTimer.stop();
                timerLabel.setText("Code expiré !");
                statusLabel.setText("Code expiré — cliquez sur Renvoyer.");
            }
        });

        countdownTimer.start();
    }
}