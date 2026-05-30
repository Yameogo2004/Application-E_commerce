package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public final class ConfirmDialog {

    private ConfirmDialog() {}

    public static boolean show(Component parent, String title, String message) {
        UITheme.applyGlobalOptionPaneTheme();

        JTextArea area = new JTextArea(message == null ? "" : message);
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(UITheme.TEXT_PRIMARY);
        area.setFont(UITheme.FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UITheme.CARD_BG);
        panel.add(area, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                title == null ? "Confirmation" : title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        return result == JOptionPane.YES_OPTION;
    }
}