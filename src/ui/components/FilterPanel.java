package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class FilterPanel extends JPanel {

    public interface FilterListener {
        void onApply();
        void onReset();
    }

    private final JPanel filtersContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

    public FilterPanel(FilterListener listener) {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.CARD_BG);
        setBorder(UITheme.cardBorder());

        filtersContainer.setOpaque(false);

        JButton applyButton = UITheme.primaryButton("Appliquer");
        JButton resetButton = UITheme.secondaryButton("Reset");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(applyButton);
        actions.add(resetButton);

        applyButton.addActionListener(e -> {
            if (listener != null) listener.onApply();
        });

        resetButton.addActionListener(e -> {
            if (listener != null) listener.onReset();
        });

        add(filtersContainer, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
    }

    public void addFilter(String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.FONT_BODY);

        if (component instanceof JComboBox<?> comboBox) {
            UITheme.styleComboBox(comboBox);
        }

        filtersContainer.add(label);
        filtersContainer.add(component);
    }

    public JPanel getFiltersContainer() {
        return filtersContainer;
    }
}