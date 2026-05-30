package ui;

import Client.AppSession;
import Client.ClientSocketService;
import model.SavedCard;
import service.CardService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SavedCardsManagerFrame extends JFrame {

    private final ClientSocketService clientService;
    private final AppSession session;
    private final CardService cardService;
    private JTable table;
    private DefaultTableModel model;

    public SavedCardsManagerFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session = session;
        this.cardService = new CardService();
        initUI();
        loadCards();
    }

    private void initUI() {
        setTitle("💳 Mes cartes enregistrées");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UITheme.darkPanel();
        root.setLayout(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("💳 Mes cartes bancaires");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        model = new DefaultTableModel(new Object[]{"Carte", "Défaut", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 2;
            }
        };

        table = new JTable(model);
        table.setBackground(UITheme.CARD_2);
        table.setForeground(Color.WHITE);
        table.setRowHeight(35);
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        JButton closeBtn = UITheme.blueButton("Fermer");
        closeBtn.addActionListener(e -> dispose());

        root.add(title, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(closeBtn, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void loadCards() {
        model.setRowCount(0);
        List<SavedCard> cards = cardService.getUserCards(session.getClientId());
        
        for (SavedCard card : cards) {
            model.addRow(new Object[]{
                card.getDisplayName(),
                card.isDefault() ? "⭐ Défaut" : "Définir",
                "🗑️ Supprimer"
            });
        }
        
        // Ajouter un bouton pour ajouter une nouvelle carte
        JButton addBtn = UITheme.primaryButton("+ Ajouter une carte");
        addBtn.addActionListener(e -> {
            new CardPaymentDialog(this, 0, true, (cardInfo) -> {
                // Callback pour sauvegarder la carte
                SavedCard newCard = new SavedCard();
                newCard.setUserId(session.getClientId());
                newCard.setCardLast4(cardInfo.last4);
                newCard.setCardBrand(cardInfo.brand);
                newCard.setCardHolderName(cardInfo.cardHolder);
                newCard.setExpiryMonth(cardInfo.expiryMonth);
                newCard.setExpiryYear(cardInfo.expiryYear);
                newCard.setDefault(cards.isEmpty()); // Première carte = défaut
                
                if (cardService.saveCard(newCard)) {
                    JOptionPane.showMessageDialog(this, "Carte enregistrée !");
                    loadCards();
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement");
                }
            }).setVisible(true);
        });
        
        ((JPanel)getContentPane()).add(addBtn, BorderLayout.NORTH);
        revalidate();
    }
}