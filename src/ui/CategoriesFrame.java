package ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class CategoriesFrame extends JFrame {

    public CategoriesFrame(List<String> categories, Consumer<String> onCategorySelected) {
        setTitle("📂 Catégories");
        setSize(350, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("📂 Catégories", SwingConstants.CENTER);
        title.setForeground(UITheme.GOLD);
        title.setFont(UITheme.titleFont());

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("🏠 Tous les produits");

        for (String c : categories) {
            if (!c.equalsIgnoreCase("General")
                    && !c.equalsIgnoreCase("Toutes")
                    && !c.equalsIgnoreCase("Tous")
                    && !c.isEmpty()) {
                model.addElement(getIconForCategory(c) + " " + c);
            }
        }

        JList<String> list = new JList<>(model);
        list.setBackground(UITheme.CARD_2);
        list.setForeground(Color.WHITE);
        list.setSelectionBackground(UITheme.BLUE);
        list.setSelectionForeground(Color.WHITE);
        list.setFont(UITheme.normalFont());
        list.setFixedCellHeight(40);

        list.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    selectCategory(list, onCategorySelected);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        JButton chooseBtn = UITheme.primaryButton("✓ Choisir");
        JButton closeBtn = UITheme.blueButton("✖ Fermer");

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        bottom.setOpaque(false);
        bottom.add(chooseBtn);
        bottom.add(closeBtn);

        chooseBtn.addActionListener(e -> selectCategory(list, onCategorySelected));
        closeBtn.addActionListener(e -> dispose());

        root.add(title, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void selectCategory(JList<String> list, Consumer<String> onCategorySelected) {
        String selected = list.getSelectedValue();
        if (selected != null && onCategorySelected != null) {
            String categoryName = selected.contains(" ")
                    ? selected.substring(selected.indexOf(" ") + 1)
                    : selected;

            if (categoryName.equals("Tous les produits")) {
                onCategorySelected.accept("Tous");
            } else {
                onCategorySelected.accept(categoryName);
            }
            dispose();
        }
    }

    private String getIconForCategory(String category) {
        String catLower = category.toLowerCase();
        if (catLower.contains("tablette")) return "📱";
        if (catLower.contains("inform") || catLower.contains("informatique")) return "💻";
        if (catLower.contains("audio")) return "🎧";
        if (catLower.contains("gaming")) return "🎮";
        if (catLower.contains("ordi")) return "🖥️";
        if (catLower.contains("access")) return "🔌";
        if (catLower.contains("phone")) return "📱";
        if (catLower.contains("laptop")) return "💻";
        if (catLower.contains("vetement")) return "👕";
        return "📦";
    }
}