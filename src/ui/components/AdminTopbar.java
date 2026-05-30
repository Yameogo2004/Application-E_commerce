package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class AdminTopbar extends JPanel {

    public interface TopbarActionListener {
        void onRefresh();
        void onLogout();
        void onOpenNotifications();
    }

    private final JLabel titleLabel = new JLabel("Dashboard");
    private final JLabel subtitleLabel = new JLabel("Vue d'ensemble du système");
    private final JLabel badgeLabel = new JLabel("0");

    public AdminTopbar(TopbarActionListener listener) {
        setLayout(new BorderLayout());
        setBackground(UITheme.TOPBAR_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
        setPreferredSize(new Dimension(0, 76));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleLabel.setFont(UITheme.FONT_H2);

        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        subtitleLabel.setFont(UITheme.FONT_SMALL);

        left.add(titleLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitleLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 18));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));

        JButton notifButton = UITheme.secondaryButton("Notifications");
        JButton refreshButton = UITheme.primaryButton("Actualiser");
        JButton logoutButton = UITheme.dangerButton("Déconnexion");

        badgeLabel.setOpaque(true);
        badgeLabel.setBackground(UITheme.DANGER);
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badgeLabel.setFont(UITheme.FONT_SMALL_BOLD);
        badgeLabel.setPreferredSize(new Dimension(28, 22));
        badgeLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        notifButton.addActionListener(e -> {
            if (listener != null) listener.onOpenNotifications();
        });

        refreshButton.addActionListener(e -> {
            if (listener != null) listener.onRefresh();
        });

        logoutButton.addActionListener(e -> {
            if (listener != null) listener.onLogout();
        });

        right.add(notifButton);
        right.add(badgeLabel);
        right.add(refreshButton);
        right.add(logoutButton);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    public void setPageInfo(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }

    public void setUnreadNotificationsCount(int count) {
        badgeLabel.setText(String.valueOf(Math.max(0, count)));
    }
}