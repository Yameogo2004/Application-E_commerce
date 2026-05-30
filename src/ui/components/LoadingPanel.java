package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class LoadingPanel extends JPanel {

    public LoadingPanel(String message) {
        setLayout(new GridBagLayout());
        setBackground(UITheme.APP_BG);

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(240, 18));

        JLabel label = new JLabel(message == null || message.isBlank() ? "Chargement..." : message);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(UITheme.TEXT_PRIMARY);
        label.setFont(UITheme.FONT_BODY_BOLD);

        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(8));
        card.add(label);
        card.add(Box.createVerticalStrut(14));
        card.add(progressBar);
        card.add(Box.createVerticalStrut(8));

        add(card);
    }
}