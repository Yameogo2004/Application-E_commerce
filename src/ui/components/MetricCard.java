package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class MetricCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public MetricCard(String title, String value, String subtitle) {
        setLayout(new BorderLayout(8, 8));
        setBackground(UITheme.CARD_BG);
        setBorder(UITheme.cardBorder());

        titleLabel = new JLabel(title);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);
        titleLabel.setFont(UITheme.FONT_BODY);

        valueLabel = new JLabel(value);
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setFont(UITheme.FONT_SMALL);

        add(titleLabel, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(4));
        center.add(subtitleLabel);

        add(center, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}