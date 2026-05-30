package ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;


public final class UITheme {

    private UITheme() {}

    // =========================================================
    // COLORS
    // =========================================================

    public static final Color APP_BG = new Color(8, 12, 20);
    public static final Color SIDEBAR_BG = new Color(14, 20, 32);
    public static final Color TOPBAR_BG = new Color(12, 18, 28);

    public static final Color CARD_BG = new Color(20, 28, 42);
    public static final Color CARD_BG_ALT = new Color(26, 36, 54);
    public static final Color INPUT_BG = new Color(18, 26, 40);

    public static final Color BORDER = new Color(42, 56, 78);
    public static final Color BORDER_SOFT = new Color(32, 44, 64);

    public static final Color TEXT_PRIMARY = new Color(236, 243, 252);
    public static final Color TEXT_SECONDARY = new Color(174, 190, 210);
    public static final Color TEXT_MUTED = new Color(130, 146, 166);

    public static final Color SKY = new Color(88, 199, 250);
    public static final Color SKY_HOVER = new Color(62, 184, 245);
    public static final Color SKY_DARK = new Color(33, 150, 243);

    public static final Color SUCCESS = new Color(46, 204, 113);
    public static final Color WARNING = new Color(255, 193, 7);
    public static final Color DANGER = new Color(231, 76, 60);
    public static final Color INFO = new Color(88, 199, 250);

    public static final Color TABLE_HEADER_BG = new Color(19, 28, 43);
    public static final Color TABLE_ROW_BG = new Color(20, 28, 42);
    public static final Color TABLE_ROW_ALT_BG = new Color(24, 34, 50);
    public static final Color TABLE_SELECTION_BG = new Color(37, 99, 235);

    // =========================================================
    // FONTS
    // =========================================================

    public static final Font FONT_H1 = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_H2 = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FONT_H3 = new Font("SansSerif", Font.BOLD, 16);

    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("SansSerif", Font.BOLD, 12);

    // =========================================================
    // BORDERS / SPACING
    // =========================================================

    public static final int RADIUS = 16;
    public static final Insets BUTTON_INSETS = new Insets(10, 16, 10, 16);

    public static Border panelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SOFT),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }

    // =========================================================
    // COMPONENT HELPERS
    // =========================================================

    public static JPanel createPagePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(APP_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(cardBorder());
        return panel;
    }

    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_PRIMARY);
        label.setFont(FONT_H2);
        return label;
    }

    public static JLabel createSubtitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_SECONDARY);
        label.setFont(FONT_SMALL);
        return label;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, SKY, Color.WHITE);
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, CARD_BG_ALT, TEXT_PRIMARY);
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, DANGER, Color.WHITE);
        return button;
    }

    private static void styleButton(JButton button, Color bg, Color fg) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(FONT_BODY_BOLD);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(BUTTON_INSETS);
    }

    public static JTextField styledTextField(int columns) {
        JTextField field = new JTextField(columns);
        styleTextField(field);
        return field;
    }

    public static void styleTextField(JTextField field) {
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(inputBorder());
        field.setFont(FONT_BODY);
        field.setSelectedTextColor(Color.WHITE);
        field.setSelectionColor(SKY_DARK);
    }

    public static void styleTextArea(JTextArea area) {
        area.setBackground(INPUT_BG);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setBorder(inputBorder());
        area.setFont(FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setSelectedTextColor(Color.WHITE);
        area.setSelectionColor(SKY_DARK);
    }

    public static <T> void styleComboBox(JComboBox<T> comboBox) {
        comboBox.setBackground(INPUT_BG);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setFont(FONT_BODY);
    }

    public static void applyGlobalOptionPaneTheme() {
        UIManager.put("Panel.background", CARD_BG);
        UIManager.put("OptionPane.background", CARD_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

        UIManager.put("Label.foreground", TEXT_PRIMARY);

        UIManager.put("Button.background", SKY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", FONT_BODY_BOLD);

        UIManager.put("TextField.background", INPUT_BG);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);

        UIManager.put("TextArea.background", INPUT_BG);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);

        UIManager.put("ComboBox.background", INPUT_BG);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
    }
}