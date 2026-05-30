package ui;

import Client.AppSession;
import Client.ClientSocketService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShopFrame extends LanguageAwareFrame {

    private static final Logger logger = LogManager.getLogger(ShopFrame.class);

    private final ClientSocketService clientService;
    private final AppSession session;

    private JTextField searchField;
    private JPanel productsContainer;
    private JLabel cartCountLabel;
    private JLabel welcomeLabel;
    private JPanel categoriesPanel;
    private JScrollPane productsScrollPane;
    private JButton categoriesBtn;
    private JButton cartBtn;
    private JButton logoutBtn;
    private JButton languageBtn;
    private JButton profileBtn;
    private JLabel logo;
    private JLabel resultLabel;
    private JButton searchBtn;

    private final List<String[]> allProducts = new ArrayList<>();
    private final List<String> allCategories = new ArrayList<>();
    private String currentCategory = "Tous";
    
    // Timer pour rafraîchissement automatique des produits
    private Timer refreshTimer;

    public ShopFrame(ClientSocketService clientService, AppSession session) {
        this.clientService = clientService;
        this.session = session;

        logger.info("🛒 Ouverture du ShopFrame - Utilisateur: {}, Rôle: {}", 
            session.getClientId(), session.getRole());

        initUI();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                filterProducts();
            }
        });

        loadAllData();
        refreshCartCount();
        
        // Démarrer le rafraîchissement automatique toutes les 10 secondes
        startAutoRefresh();
    }

    private void initUI() {
        setTitle(LanguageManager.getInstance().getText("shop.title"));
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        applyOrientation();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BG);

        JPanel topBar = createTopBar();
        mainPanel.add(topBar, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);
        splitPane.setBackground(UITheme.BG);

        JPanel sidebar = createSidebar();
        splitPane.setLeftComponent(sidebar);

        JPanel rightArea = createProductsArea();
        splitPane.setRightComponent(rightArea);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void applyOrientation() {
        if (LanguageManager.getCurrentLanguage() == LanguageManager.Language.ARABIC) {
            setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        } else {
            setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        }
    }

    private String getDisplayName() {
        String fullName = session.getFullName();

        if (fullName == null || fullName.isBlank()) {
            return "Utilisateur";
        }

        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : fullName.trim();
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setBackground(new Color(35, 38, 46));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        logo = new JLabel("🛍 ChriOnline");
        logo.setForeground(UITheme.GOLD);
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(new Color(35, 38, 46));
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(new Color(35, 38, 46));
        searchPanel.setPreferredSize(new Dimension(320, 42));
        searchPanel.setMinimumSize(new Dimension(260, 42));

        searchField = new JTextField();
        searchField.setBackground(Color.WHITE);
        searchField.setForeground(Color.BLACK);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        searchField.setToolTipText(LanguageManager.getInstance().getText("shop.search.placeholder"));

        searchBtn = new JButton(LanguageManager.getInstance().getText("shop.search"));
        searchBtn.setBackground(UITheme.GOLD);
        searchBtn.setForeground(new Color(35, 38, 46));
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        searchBtn.setFocusPainted(false);
        searchBtn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        centerWrapper.add(searchPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setBackground(new Color(35, 38, 46));

        String welcomeText = LanguageManager.getInstance().getText("shop.welcome") + ", " + getDisplayName();

        welcomeLabel = new JLabel(welcomeText);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        cartCountLabel = new JLabel("🛒 (0)");
        cartCountLabel.setForeground(UITheme.GOLD);
        cartCountLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        categoriesBtn = new JButton("📂 " + LanguageManager.getInstance().getText("shop.categories"));
        styleTopButton(categoriesBtn, UITheme.BLUE, Color.WHITE);

        cartBtn = new JButton("🛒 " + LanguageManager.getInstance().getText("shop.cart"));
        styleTopButton(cartBtn, UITheme.GREEN, Color.WHITE);

        logoutBtn = new JButton(LanguageManager.getInstance().getText("shop.logout"));
        styleTopButton(logoutBtn, UITheme.RED, Color.WHITE);

        profileBtn = new JButton("👤 " + LanguageManager.getInstance().getText("profile.title"));
        styleTopButton(profileBtn, new Color(35, 38, 46), Color.WHITE);

        languageBtn = new JButton(LanguageManager.getCurrentLanguage().getFlag() + " " +
                LanguageManager.getCurrentLanguage().getDisplayName());
        styleTopButton(languageBtn, new Color(35, 38, 46), Color.WHITE);

        languageBtn.addActionListener(e -> {
            JPopupMenu langMenu = new JPopupMenu();
            for (LanguageManager.Language lang : LanguageManager.Language.values()) {
                JMenuItem item = new JMenuItem(lang.getFlag() + " " + lang.getDisplayName());
                item.addActionListener(ev -> {
                    LanguageManager.setLanguage(lang);
                    languageBtn.setText(lang.getFlag() + " " + lang.getDisplayName());
                });
                langMenu.add(item);
            }
            langMenu.show(languageBtn, 0, languageBtn.getHeight());
        });

        profileBtn.addActionListener(e -> {
            logger.debug("Accès au profil - Utilisateur: {}", session.getClientId());
            new ProfileFrame(clientService, session, this).setVisible(true);
            setVisible(false);
        });

        searchBtn.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            logger.debug("Recherche effectuée: '{}' par utilisateur {}", searchTerm, session.getClientId());
            filterProducts();
        });
        searchField.addActionListener(e -> filterProducts());

        categoriesBtn.addActionListener(e ->
                new CategoriesFrame(new ArrayList<>(allCategories), category -> {
                    currentCategory = category;
                    logger.debug("Changement de catégorie: {} par utilisateur {}", category, session.getClientId());
                    filterProducts();
                    refreshCategoryButtons();
                }).setVisible(true)
        );

        cartBtn.addActionListener(e -> {
            logger.debug("Accès au panier - Utilisateur: {}", session.getClientId());
            new CartFrame(clientService, session, this).setVisible(true);
            setVisible(false);
        });

        logoutBtn.addActionListener(e -> {
            // Arrêter le timer avant de se déconnecter
            if (refreshTimer != null) {
                refreshTimer.stop();
            }
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Voulez-vous vraiment vous déconnecter ?",
                    "Confirmation",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                logger.info("Déconnexion - Utilisateur: {}, Rôle: {}", session.getClientId(), session.getRole());
                dispose();
                new LoginFrame(clientService).setVisible(true);
            }
        });

        rightPanel.add(welcomeLabel);
        rightPanel.add(cartCountLabel);
        rightPanel.add(profileBtn);
        rightPanel.add(categoriesBtn);
        rightPanel.add(cartBtn);
        rightPanel.add(languageBtn);
        rightPanel.add(logoutBtn);

        topBar.add(logo, BorderLayout.WEST);
        topBar.add(centerWrapper, BorderLayout.CENTER);
        topBar.add(rightPanel, BorderLayout.EAST);

        return topBar;
    }

    private void styleTopButton(JButton button, Color bg, Color fg) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UITheme.CARD);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));
        sidebar.setPreferredSize(new Dimension(280, 0));

        JLabel title = new JLabel(LanguageManager.getInstance().getText("shop.categories").toUpperCase());
        title.setForeground(UITheme.GOLD);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        categoriesPanel = new JPanel();
        categoriesPanel.setBackground(UITheme.CARD);
        categoriesPanel.setLayout(new BoxLayout(categoriesPanel, BoxLayout.Y_AXIS));

        JScrollPane catScroll = new JScrollPane(categoriesPanel);
        catScroll.setBackground(UITheme.CARD);
        catScroll.setBorder(null);
        catScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        catScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        sidebar.add(title, BorderLayout.NORTH);
        sidebar.add(catScroll, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel createProductsArea() {
        JPanel productsArea = new JPanel(new BorderLayout());
        productsArea.setBackground(UITheme.BG);
        productsArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        resultLabel = new JLabel(LanguageManager.getInstance().getText("shop.all"));
        resultLabel.setForeground(UITheme.TEXT);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(resultLabel, BorderLayout.WEST);

        productsArea.add(header, BorderLayout.NORTH);

        productsContainer = new JPanel();
        productsContainer.setBackground(UITheme.BG);
        productsContainer.setLayout(new GridLayout(0, 4, 15, 20));

        productsScrollPane = new JScrollPane(productsContainer);
        productsScrollPane.setBackground(UITheme.BG);
        productsScrollPane.getViewport().setBackground(UITheme.BG);
        productsScrollPane.setBorder(null);
        productsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        productsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        productsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        productsArea.add(productsScrollPane, BorderLayout.CENTER);

        return productsArea;
    }

    public void reloadProducts() {
        logger.debug("Rechargement des produits");
        loadAllData();
    }

    public void reloadCategoriesAndProducts() {
        logger.debug("Rechargement des catégories et produits");
        loadAllData();
    }

    private void loadAllData() {
        loadProducts();
        loadCategories();
        buildCategoriesPanel(new LinkedHashSet<>(allCategories));
        filterProducts();
    }

    private void loadProducts() {
        allProducts.clear();

        String response = clientService.getProducts();

        if (response == null || response.startsWith("ERROR") || "NO_PRODUCTS".equals(response)) {
            logger.warn("Aucun produit trouvé ou erreur lors du chargement");
            renderProducts(new ArrayList<>());
            resultLabel.setText(LanguageManager.getInstance().getText("shop.empty"));
            return;
        }

        String[] products = response.split("\\|");

        for (String product : products) {
            String[] fields = product.split(";");
            if (fields.length >= 6) {
                String[] fullFields = new String[6];
                fullFields[0] = fields[0];
                fullFields[1] = fields[1];
                fullFields[2] = fields[2];
                fullFields[3] = fields[3];
                fullFields[4] = fields[4];
                fullFields[5] = fields[5];
                allProducts.add(fullFields);
            }
        }
        
        logger.debug("{} produits chargés", allProducts.size());
    }

    private void loadCategories() {
        allCategories.clear();

        String response = clientService.getCategories();

        if (response == null || response.startsWith("ERROR") || "NO_CATEGORIES".equals(response)) {
            logger.warn("Aucune catégorie trouvée");
            return;
        }

        String[] categories = response.split("\\|");
        for (String category : categories) {
            String[] fields = category.split(";");
            if (fields.length >= 2) {
                String name = fields[1].trim();
                if (!name.isEmpty()
                        && !name.equalsIgnoreCase("General")
                        && !name.equalsIgnoreCase("Toutes")
                        && !name.equalsIgnoreCase("Tous")) {
                    allCategories.add(name);
                }
            }
        }
        
        logger.debug("{} catégories chargées", allCategories.size());
    }

    private void buildCategoriesPanel(Set<String> categories) {
        categoriesPanel.removeAll();

        JButton allBtn = createCategoryButton("🏠 " + LanguageManager.getInstance().getText("shop.all"), "Tous");
        allBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        allBtn.setMaximumSize(new Dimension(250, 40));
        categoriesPanel.add(allBtn);
        categoriesPanel.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER);
        sep.setMaximumSize(new Dimension(250, 1));
        categoriesPanel.add(sep);
        categoriesPanel.add(Box.createVerticalStrut(10));

        List<String> sortedCategories = new ArrayList<>(categories);
        sortedCategories.sort(String::compareToIgnoreCase);

        for (String category : sortedCategories) {
            if (!category.isBlank()) {
                JButton catBtn = createCategoryButton(getCategoryIcon(category) + " " + category, category);
                catBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
                catBtn.setMaximumSize(new Dimension(250, 38));
                categoriesPanel.add(catBtn);
                categoriesPanel.add(Box.createVerticalStrut(5));
            }
        }

        categoriesPanel.revalidate();
        categoriesPanel.repaint();
    }

    private String getCategoryIcon(String category) {
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

    private JButton createCategoryButton(String text, String categoryValue) {
        JButton btn = new JButton(text);
        btn.setBackground(UITheme.CARD);
        btn.setForeground(UITheme.TEXT);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        if (currentCategory.equals(categoryValue)) {
            btn.setBackground(UITheme.BLUE);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        }

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!currentCategory.equals(categoryValue)) {
                    btn.setBackground(UITheme.CARD_2);
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!currentCategory.equals(categoryValue)) {
                    btn.setBackground(UITheme.CARD);
                }
            }
        });

        btn.addActionListener(e -> {
            currentCategory = categoryValue;
            filterProducts();
            refreshCategoryButtons();
        });

        return btn;
    }

    private void refreshCategoryButtons() {
        Component[] components = categoriesPanel.getComponents();

        for (Component comp : components) {
            if (comp instanceof JButton btn) {
                String btnText = btn.getText();
                String btnCategory = btnText.contains(" ")
                        ? btnText.substring(btnText.indexOf(" ") + 1)
                        : btnText;

                if (btnCategory.equals(currentCategory)
                        || (currentCategory.equals("Tous")
                        && btnText.contains(LanguageManager.getInstance().getText("shop.all")))) {
                    btn.setBackground(UITheme.BLUE);
                    btn.setForeground(Color.WHITE);
                    btn.setFont(new Font("SansSerif", Font.BOLD, 13));
                } else {
                    btn.setBackground(UITheme.CARD);
                    btn.setForeground(UITheme.TEXT);
                    btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
            }
        }
    }

    private void renderProducts(List<String[]> list) {
        productsContainer.removeAll();

        if (list.isEmpty()) {
            productsContainer.setLayout(new BorderLayout());

            JLabel emptyLabel = new JLabel("🔍 " + LanguageManager.getInstance().getText("shop.empty"));
            emptyLabel.setForeground(UITheme.MUTED);
            emptyLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);

            productsContainer.add(emptyLabel, BorderLayout.CENTER);
        } else {
            int columns = getResponsiveColumns();
            productsContainer.setLayout(new GridLayout(0, columns, 15, 20));

            for (String[] p : list) {
                int id = Integer.parseInt(p[0]);
                String name = p[1];
                double price = Double.parseDouble(p[2]);
                String image = p[3];
                String category = p[4];
                int stock = Integer.parseInt(p[5]);

                ProductCard card = new ProductCard(
                        id,
                        name,
                        price,
                        image,
                        category,
                        stock,
                        clientService,
                        session,
                        this::refreshCartCount
                );

                productsContainer.add(card);
            }
        }

        productsContainer.revalidate();
        productsContainer.repaint();
        SwingUtilities.invokeLater(() -> productsScrollPane.getVerticalScrollBar().setValue(0));
    }

    private int getResponsiveColumns() {
        int width = productsScrollPane.getViewport().getWidth();

        if (width >= 1250) return 4;
        if (width >= 900) return 3;
        if (width >= 600) return 2;
        return 1;
    }

    private void filterProducts() {
        // Sauvegarder la position du scroll
        JScrollBar verticalBar = productsScrollPane.getVerticalScrollBar();
        int scrollPosition = verticalBar.getValue();
        
        String searchKey = searchField.getText().trim().toLowerCase();

        List<String[]> filtered = new ArrayList<>();

        for (String[] p : allProducts) {
            String name = p[1].toLowerCase();
            String category = p[4].toLowerCase();

            boolean categoryMatch = currentCategory.equals("Tous") || p[4].equalsIgnoreCase(currentCategory);
            boolean searchMatch = searchKey.isEmpty()
                    || name.contains(searchKey)
                    || category.contains(searchKey);

            if (categoryMatch && searchMatch) {
                filtered.add(p);
            }
        }

        renderProducts(filtered);
        resultLabel.setText(filtered.size() + " produit(s)");
        
        // Restaurer la position du scroll
        SwingUtilities.invokeLater(() -> verticalBar.setValue(scrollPosition));
        
        logger.debug("Filtrage produits - Catégorie: {}, Recherche: '{}', Résultats: {}", 
            currentCategory, searchKey, filtered.size());
    }

    public void refreshCartCount() {
        String response = clientService.getCart(session.getClientId());

        if ("CART_EMPTY".equals(response) || response == null || response.startsWith("ERROR")) {
            cartCountLabel.setText("🛒 (0)");
            return;
        }

        String[] parts = response.split("\\|");
        for (String part : parts) {
            if (part.startsWith("Items=")) {
                String count = part.substring("Items=".length());
                cartCountLabel.setText("🛒 (" + count + ")");
                logger.debug("Panier mis à jour - Utilisateur: {}, Nombre d'articles: {}", session.getClientId(), count);
                return;
            }
        }

        cartCountLabel.setText("🛒 (0)");
    }

    /**
     * Démarre le rafraîchissement automatique des produits toutes les 10 secondes
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer(30000, e -> {
            if (isVisible()) {
                SwingUtilities.invokeLater(() -> {
                    // Sauvegarder la position du scroll
                    JScrollBar verticalBar = productsScrollPane.getVerticalScrollBar();
                    int scrollPosition = verticalBar.getValue();
                    
                    loadProducts();
                    filterProducts();
                    
                    // Restaurer la position du scroll
                    SwingUtilities.invokeLater(() -> verticalBar.setValue(scrollPosition));
                    refreshCartCount();
                    logger.debug("Rafraîchissement automatique des produits effectué - scroll préservé");
                });
            }
        });
        refreshTimer.start();
        logger.info("Rafraîchissement automatique des produits démarré (toutes les 30 secondes)");
    }
    
    @Override
    public void dispose() {
        // Arrêter le timer lors de la fermeture
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.dispose();
    }

    @Override
    public void refreshTexts() {
        setTitle(LanguageManager.getInstance().getText("shop.title"));

        String welcomeText = LanguageManager.getInstance().getText("shop.welcome") + ", " + getDisplayName();
        welcomeLabel.setText(welcomeText);

        categoriesBtn.setText("📂 " + LanguageManager.getInstance().getText("shop.categories"));
        cartBtn.setText("🛒 " + LanguageManager.getInstance().getText("shop.cart"));
        logoutBtn.setText(LanguageManager.getInstance().getText("shop.logout"));
        profileBtn.setText("👤 " + LanguageManager.getInstance().getText("profile.title"));
        searchBtn.setText(LanguageManager.getInstance().getText("shop.search"));
        logo.setText("🏪 ChriOnline");

        applyOrientation();
        buildCategoriesPanel(new LinkedHashSet<>(allCategories));
        filterProducts();

        for (Component comp : productsContainer.getComponents()) {
            if (comp instanceof ProductCard card) {
                card.refreshTexts();
            }
        }

        revalidate();
        repaint();
    }
}