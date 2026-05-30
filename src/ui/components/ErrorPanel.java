package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class ErrorPanel extends JPanel {

    public interface RetryListener {
        void onRetry();
    }

    public ErrorPanel(String title, String message, RetryListener listener) {
        setLayout(new GridBagLayout());
        setBackground(UITheme.APP_BG);

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title == null || title.isBlank() ? "Une erreur est survenue" : title);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(UITheme.DANGER);
        titleLabel.setFont(UITheme.FONT_H3);

        JLabel messageLabel = new JLabel("<html><div style='text-align:center; width:280px;'>" +
                (message == null ? "" : message) + "</div></html>");
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setForeground(UITheme.TEXT_SECONDARY);
        messageLabel.setFont(UITheme.FONT_BODY);

        JButton retryButton = UITheme.primaryButton("Réessayer");
        retryButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        retryButton.addActionListener(e -> {
            if (listener != null) listener.onRetry();
        });

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(retryButton);

        add(card);
    }
}