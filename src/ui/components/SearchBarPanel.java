package ui.components;

import javax.swing.*;


import ui.theme.UITheme;

import java.awt.*;

public class SearchBarPanel extends JPanel {

    public interface SearchListener {
        void onSearch(String keyword);
        void onReset();
    }

    private final JTextField searchField = UITheme.styledTextField(22);

    public SearchBarPanel(String labelText, SearchListener listener) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.FONT_BODY);

        JButton searchButton = UITheme.primaryButton("Rechercher");
        JButton resetButton = UITheme.secondaryButton("Reset");

        searchButton.addActionListener(e -> {
            if (listener != null) {
                listener.onSearch(searchField.getText().trim());
            }
        });

        resetButton.addActionListener(e -> {
            searchField.setText("");
            if (listener != null) {
                listener.onReset();
            }
        });

        add(label);
        add(searchField);
        add(searchButton);
        add(resetButton);
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public String getValue() {
        return searchField.getText().trim();
    }

    public void clear() {
        searchField.setText("");
    }
}