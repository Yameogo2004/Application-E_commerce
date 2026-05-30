package ui;

import javax.swing.*;

import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UITheme {

    public static final Color BG = new Color(34, 37, 46);
    public static final Color CARD = new Color(45, 49, 60);
    public static final Color CARD_2 = new Color(53, 58, 71);
    public static final Color GREEN = new Color(89, 168, 79);
    public static final Color BLUE = new Color(67, 139, 208);
    public static final Color RED = new Color(197, 76, 76);
    public static final Color TEXT = new Color(240, 240, 240);
    public static final Color MUTED = new Color(180, 180, 180);
    public static final Color BORDER = new Color(70, 74, 84);
    public static final Color GOLD = new Color(255, 193, 7);
    public static final Color WARNING = new Color(255, 152, 0);
    public static final Color SUCCESS = new Color(76, 175, 80);
    public static final Color INPUT_BG = new Color(58, 62, 74);

    private static final String IMAGES_DIR = "image/";
    private static final Map<String, ImageIcon> imageCache = new HashMap<>();

    private UITheme() {
    }

    public static TitledBorder titledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER),
                title
        );
        border.setTitleColor(TEXT);
        border.setTitleFont(normalFont());
        return border;
    }

    public static TitledBorder titledBorder(String title, Font font) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER),
                title
        );
        border.setTitleColor(TEXT);
        border.setTitleFont(font);
        return border;
    }

    private static ImageIcon createPlaceholderIcon(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setColor(new Color(70, 74, 84));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g2d.getFontMetrics();

        String text = "No Image";
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();

        g2d.drawString(text, textX, textY);
        g2d.drawRect(0, 0, width - 1, height - 1);

        g2d.dispose();
        return new ImageIcon(img);
    }

    public static ImageIcon loadProductImage(String path, int width, int height) {
        if (path == null || path.trim().isEmpty()) {
            return getScaledPlaceholder(width, height);
        }

        String normalizedPath = path.trim().replace("\\", "/");
        String fileName = new File(normalizedPath).getName();
        String cacheKey = normalizedPath + "_" + width + "x" + height;

        if (imageCache.containsKey(cacheKey)) {
            return imageCache.get(cacheKey);
        }

        try {
            File imageFile;

            File directFile = new File(normalizedPath);
            if (directFile.exists()) {
                imageFile = directFile;
            } else {
                imageFile = new File(IMAGES_DIR + fileName);
            }

            if (!imageFile.exists()) {
                ImageIcon placeholder = getScaledPlaceholder(width, height);
                imageCache.put(cacheKey, placeholder);
                return placeholder;
            }

            ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());

            if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                ImageIcon placeholder = getScaledPlaceholder(width, height);
                imageCache.put(cacheKey, placeholder);
                return placeholder;
            }

            Image scaledImg = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImg);
            imageCache.put(cacheKey, scaledIcon);
            return scaledIcon;

        } catch (Exception e) {
            System.err.println("Erreur chargement image: " + normalizedPath + " - " + e.getMessage());
            ImageIcon placeholder = getScaledPlaceholder(width, height);
            imageCache.put(cacheKey, placeholder);
            return placeholder;
        }
    }

    private static ImageIcon getScaledPlaceholder(int width, int height) {
        String cacheKey = "placeholder_" + width + "x" + height;

        if (imageCache.containsKey(cacheKey)) {
            return imageCache.get(cacheKey);
        }

        ImageIcon placeholder = createPlaceholderIcon(width, height);
        imageCache.put(cacheKey, placeholder);
        return placeholder;
    }

    public static void clearCache() {
        imageCache.clear();
    }

    public static JPanel createPasswordFieldWithEye(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setBackground(INPUT_BG);
        passwordField.setForeground(TEXT);
        passwordField.setCaretColor(TEXT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
        passwordField.setFont(normalFont());
        passwordField.setEchoChar('•');

        JLabel eyeLabel = new JLabel("👁️");
        eyeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeLabel.setForeground(MUTED);
        eyeLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        final boolean[] isVisible = {false};

        eyeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isVisible[0] = !isVisible[0];

                if (isVisible[0]) {
                    passwordField.setEchoChar((char) 0);
                    eyeLabel.setText("🙈");
                    eyeLabel.setForeground(GOLD);
                } else {
                    passwordField.setEchoChar('•');
                    eyeLabel.setText("👁️");
                    eyeLabel.setForeground(MUTED);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                eyeLabel.setForeground(GOLD);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isVisible[0]) {
                    eyeLabel.setForeground(MUTED);
                }
            }
        });

        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.setOpaque(false);
        fieldPanel.setBorder(titledBorder(title));

        fieldPanel.add(passwordField, BorderLayout.CENTER);
        fieldPanel.add(eyeLabel, BorderLayout.EAST);

        panel.add(fieldPanel, BorderLayout.CENTER);
        panel.putClientProperty("passwordField", passwordField);

        return panel;
    }

    public static JPasswordField getPasswordFieldFromPanel(JPanel panel) {
        Object field = panel.getClientProperty("passwordField");
        return (field instanceof JPasswordField) ? (JPasswordField) field : null;
    }

    public static Font titleFont() {
        return new Font("SansSerif", Font.BOLD, 28);
    }

    public static Font subtitleFont() {
        return new Font("SansSerif", Font.BOLD, 18);
    }

    public static Font normalFont() {
        return new Font("SansSerif", Font.PLAIN, 14);
    }

    public static Font smallFont() {
        return new Font("SansSerif", Font.PLAIN, 12);
    }

    public static Font priceFont() {
        return new Font("SansSerif", Font.BOLD, 18);
    }

    public static void addHoverEffect(JButton button, Color hoverColor) {
        Color originalColor = button.getBackground();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(GREEN);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(b, new Color(109, 188, 99));
        return b;
    }

    public static JButton blueButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(b, new Color(87, 159, 228));
        return b;
    }

    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(RED);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(b, new Color(217, 96, 96));
        return b;
    }

    public static JButton goldButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(GOLD);
        b.setForeground(new Color(34, 37, 46));
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(b, new Color(255, 213, 67));
        return b;
    }

    public static JButton iconButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(btn, bg.brighter());
        return btn;
    }

    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setFont(normalFont());
        return field;
    }

    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setFont(normalFont());
        field.setEchoChar('•');
        return field;
    }

    public static JPanel darkPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        return p;
    }

    public static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return p;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(normalFont());
        return l;
    }

    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(smallFont());
        return l;
    }

    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(titleFont());
        return l;
    }

    public static JLabel priceLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(GOLD);
        l.setFont(priceFont());
        return l;
    }
}