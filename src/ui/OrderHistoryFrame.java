package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class OrderHistoryFrame extends LanguageAwareFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private JTable ordersTable;
    private DefaultTableModel model;
    private JLabel titleLabel;

    public OrderHistoryFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session = session;
        initUI();
        loadOrders();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("profile.order.history"));
        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = UITheme.cardPanel();
        header.setLayout(new BorderLayout());

        titleLabel = new JLabel("📦 " + LanguageManager.getInstance().getText("profile.order.history"));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.add(titleLabel, BorderLayout.WEST);

        root.add(header, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{
                LanguageManager.getInstance().getText("profile.order.date"),
                LanguageManager.getInstance().getText("profile.order.total"),
                LanguageManager.getInstance().getText("profile.order.status"),
                LanguageManager.getInstance().getText("profile.order.details")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        ordersTable = new JTable(model);
        ordersTable.setBackground(UITheme.CARD_2);
        ordersTable.setForeground(Color.WHITE);
        ordersTable.setRowHeight(40);
        ordersTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ordersTable.getTableHeader().setBackground(UITheme.CARD);
        ordersTable.getTableHeader().setForeground(Color.WHITE);
        ordersTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        ordersTable.setSelectionBackground(new Color(67, 139, 208));

        ordersTable.getColumn(LanguageManager.getInstance().getText("profile.order.details")).setCellRenderer(new ButtonRenderer());
        ordersTable.getColumn(LanguageManager.getInstance().getText("profile.order.details")).setCellEditor(new ButtonEditor());

        JScrollPane scroll = new JScrollPane(ordersTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        root.add(scroll, BorderLayout.CENTER);

        JPanel bottom = UITheme.cardPanel();
        JButton closeBtn = UITheme.blueButton(LanguageManager.getInstance().getText("profile.cancel"));
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void loadOrders() {
        model.setRowCount(0);

        model.addRow(new Object[]{"2026-03-22", "1250.00 DH", "paid", "Voir"});
        model.addRow(new Object[]{"2026-03-21", "890.00 DH", "pending", "Voir"});
        model.addRow(new Object[]{"2026-03-20", "2450.00 DH", "delivered", "Voir"});
    }

    private void showOrderDetails(int row) {
        String date = (String) model.getValueAt(row, 0);
        String total = (String) model.getValueAt(row, 1);
        String status = (String) model.getValueAt(row, 2);

        JOptionPane.showMessageDialog(this,
                "📦 Détails de la commande\n\n" +
                        "Date: " + date + "\n" +
                        "Total: " + total + "\n" +
                        "Statut: " + status,
                "Détails commande",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("profile.order.history"));
        titleLabel.setText("📦 " + LanguageManager.getInstance().getText("profile.order.history"));
        model.setColumnIdentifiers(new Object[]{
                LanguageManager.getInstance().getText("profile.order.date"),
                LanguageManager.getInstance().getText("profile.order.total"),
                LanguageManager.getInstance().getText("profile.order.status"),
                LanguageManager.getInstance().getText("profile.order.details")
        });
        revalidate();
        repaint();
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(UITheme.BLUE);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? LanguageManager.getInstance().getText("profile.order.details") : value.toString());
            return this;
        }
    }

    class ButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JButton button;
        private int selectedRow;

        public ButtonEditor() {
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(UITheme.BLUE);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("SansSerif", Font.BOLD, 11));
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            selectedRow = row;
            button.setText((value == null) ? LanguageManager.getInstance().getText("profile.order.details") : value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            SwingUtilities.invokeLater(() -> showOrderDetails(selectedRow));
            return LanguageManager.getInstance().getText("profile.order.details");
        }
    }
}