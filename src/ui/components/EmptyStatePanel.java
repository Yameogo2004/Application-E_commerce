package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class EmptyStatePanel extends JPanel {

    public EmptyStatePanel(String title, String message) {
        setLayout(new GridBagLayout());
        setBackground(UITheme.APP_BG);

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title == null || title.isBlank() ? "Aucune donnée" : title);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleLabel.setFont(UITheme.FONT_H3);

        JLabel messageLabel = new JLabel("<html><div style='text-align:center; width:260px;'>" +
                (message == null ? "" : message) + "</div></html>");
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setForeground(UITheme.TEXT_SECONDARY);
        messageLabel.setFont(UITheme.FONT_BODY);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(messageLabel);

        add(card);
    }
}