package ui.components;

import javax.swing.*;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import ui.theme.UITheme;

import java.awt.*;

public class AppTable extends JTable {

    public AppTable(DefaultTableModel model) {
        super(model);
        initStyle();
    }

    private void initStyle() {
        setRowHeight(30);
        setFont(UITheme.FONT_BODY);
        setForeground(UITheme.TEXT_PRIMARY);
        setBackground(UITheme.TABLE_ROW_BG);
        setGridColor(UITheme.BORDER_SOFT);
        setSelectionBackground(UITheme.TABLE_SELECTION_BG);
        setSelectionForeground(Color.WHITE);
        setFillsViewportHeight(true);
        setShowGrid(true);
        setIntercellSpacing(new Dimension(0, 1));

        getTableHeader().setBackground(UITheme.TABLE_HEADER_BG);
        getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UITheme.TABLE_ROW_BG : UITheme.TABLE_ROW_ALT_BG);
                    c.setForeground(UITheme.TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
    }
}