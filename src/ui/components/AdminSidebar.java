package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminSidebar extends JPanel {

    public interface NavigationListener {
        void onNavigate(String pageId);
    }

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final NavigationListener listener;
    private String activePage = "dashboard";

    public AdminSidebar(NavigationListener listener) {
        this.listener = listener;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(UITheme.SIDEBAR_BG);
        setPreferredSize(new Dimension(230, 0));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(24, 20, 18, 20));

        JLabel title = new JLabel("ChriOnline");
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setFont(UITheme.FONT_H2);

        JLabel subtitle = new JLabel("Admin Dashboard");
        subtitle.setForeground(UITheme.TEXT_SECONDARY);
        subtitle.setFont(UITheme.FONT_SMALL);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);

        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 14, 14));

        addNavButton(navPanel, "dashboard", "Dashboard");
        addNavButton(navPanel, "products", "Produits");
        addNavButton(navPanel, "categories", "Catégories");
        addNavButton(navPanel, "orders", "Commandes");
        addNavButton(navPanel, "users", "Utilisateurs");
        addNavButton(navPanel, "notifications", "Notifications");
        addNavButton(navPanel, "stockHistory", "Historique Stock");
        addNavButton(navPanel, "statistics", "Statistiques");

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 18, 20, 18));

        JLabel note = new JLabel("Dark Sky Theme");
        note.setForeground(UITheme.TEXT_MUTED);
        note.setFont(UITheme.FONT_SMALL);
        footer.add(note, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
        add(navPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setActivePage(activePage);
    }

    private void addNavButton(JPanel parent, String pageId, String label) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(UITheme.CARD_BG);
        button.setForeground(UITheme.TEXT_PRIMARY);
        button.setFont(UITheme.FONT_BODY);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(190, 42));
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        button.addActionListener(e -> {
            setActivePage(pageId);
            if (listener != null) listener.onNavigate(pageId);
        });

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!pageId.equals(activePage)) {
                    button.setBackground(UITheme.CARD_BG_ALT);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!pageId.equals(activePage)) {
                    button.setBackground(UITheme.CARD_BG);
                }
            }
        });

        navButtons.put(pageId, button);
        parent.add(button);
        parent.add(Box.createVerticalStrut(8));
    }

    public void setActivePage(String pageId) {
        this.activePage = pageId;

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(pageId);
            JButton button = entry.getValue();
            button.setBackground(active ? UITheme.SKY_DARK : UITheme.CARD_BG);
            button.setForeground(UITheme.TEXT_PRIMARY);
            button.setFont(active ? UITheme.FONT_BODY_BOLD : UITheme.FONT_BODY);
        }
    }
}