package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class StatusBadge extends JLabel {

    public StatusBadge(String text, Color bgColor, Color fgColor) {
        super(text, SwingConstants.CENTER);
        setOpaque(true);
        setBackground(bgColor);
        setForeground(fgColor);
        setFont(UITheme.FONT_SMALL_BOLD);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    public static StatusBadge forOrderStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();

        return switch (normalized) {
            case "paid" -> new StatusBadge("PAID", new Color(32, 86, 56), Color.WHITE);
            case "pending" -> new StatusBadge("PENDING", new Color(117, 86, 20), Color.WHITE);
            case "shipped" -> new StatusBadge("SHIPPED", new Color(27, 74, 122), Color.WHITE);
            case "delivered" -> new StatusBadge("DELIVERED", new Color(46, 125, 50), Color.WHITE);
            case "cancelled" -> new StatusBadge("CANCELLED", new Color(130, 38, 50), Color.WHITE);
            default -> new StatusBadge(status == null ? "UNKNOWN" : status.toUpperCase(), UITheme.CARD_BG_ALT, UITheme.TEXT_PRIMARY);
        };
    }

    public static StatusBadge forLevel(String level) {
        String normalized = level == null ? "" : level.trim().toUpperCase();

        return switch (normalized) {
            case "CRITICAL" -> new StatusBadge("CRITICAL", new Color(130, 38, 50), Color.WHITE);
            case "WARNING" -> new StatusBadge("WARNING", new Color(117, 86, 20), Color.WHITE);
            case "INFO" -> new StatusBadge("INFO", new Color(27, 74, 122), Color.WHITE);
            default -> new StatusBadge(level == null ? "N/A" : level, UITheme.CARD_BG_ALT, UITheme.TEXT_PRIMARY);
        };
    }

    public static StatusBadge forBoolean(boolean value, String trueText, String falseText) {
        return value
                ? new StatusBadge(trueText, new Color(32, 86, 56), Color.WHITE)
                : new StatusBadge(falseText, new Color(130, 38, 50), Color.WHITE);
    }
}