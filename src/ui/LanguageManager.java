package ui;

import java.util.*;

public class LanguageManager {

    public enum Language {
        FRENCH("Français", "🇫🇷", "fr"),
        ENGLISH("English", "🇬🇧", "en"),
        ARABIC("العربية", "🇲🇦", "ar");

        private final String displayName;
        private final String flag;
        private final String code;

        Language(String displayName, String flag, String code) {
            this.displayName = displayName;
            this.flag = flag;
            this.code = code;
        }

        public String getDisplayName() { return displayName; }
        public String getFlag() { return flag; }
        public String getCode() { return code; }
    }

    private static Language currentLanguage = Language.FRENCH;
    private static LanguageManager instance;

    private Map<String, Map<String, String>> translations;
    private final List<LanguageChangeListener> listeners = new ArrayList<>();

    public interface LanguageChangeListener {
        void onLanguageChanged();
    }

    private LanguageManager() {
        loadTranslations();
    }

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public static void setLanguage(Language lang) {
        currentLanguage = lang;
        if (instance != null) {
            List<LanguageChangeListener> copy = new ArrayList<>(instance.listeners);
            for (LanguageChangeListener listener : copy) {
                listener.onLanguageChanged();
            }
        }
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public void addLanguageChangeListener(LanguageChangeListener listener) {
        listeners.add(listener);
    }

    public void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    public String getText(String key) {
        Map<String, String> langMap = translations.get(currentLanguage.getCode());
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        return key;
    }

    private void loadTranslations() {
        translations = new HashMap<>();

        // ==================== FRANÇAIS ====================
        Map<String, String> fr = new HashMap<>();
        fr.put("app.title", "ChriOnline - Votre Boutique");
        fr.put("login.title", "Connexion");
        fr.put("login.subtitle", "Votre boutique en ligne");
        fr.put("login.email", "Email");
        fr.put("login.password", "Mot de passe");
        fr.put("login.button", "SE CONNECTER");
        fr.put("login.register", "CRÉER UN COMPTE");
        fr.put("login.error.empty", "Veuillez remplir tous les champs.");
        fr.put("login.error.invalid", "Email ou mot de passe incorrect.");
        fr.put("login.error.server", "Serveur inaccessible.");
        // 🔐 PROTECTION FORCE BRUTE
        fr.put("login.error.too_many_attempts", "❌ Trop de tentatives échouées. Réessayez dans 5 minutes.");
        fr.put("login.error.remaining_attempts", "❌ Email ou mot de passe incorrect. Tentatives restantes: ");
        fr.put("login.error.warning_attempts", "⚠️ ATTENTION ! Plus que ");
        fr.put("login.error.warning_attempts_end", " tentative(s) avant blocage !");
        fr.put("login.blocked.title", "Compte temporairement bloqué");
        fr.put("login.blocked.message", "Attention ! Vous avez dépassé le nombre de tentatives autorisées.\nVeuillez réessayer dans 5 minutes.\n\nSi vous avez oublié votre mot de passe, contactez l'administrateur.");

        fr.put("register.title", "Créer un compte");
        fr.put("register.subtitle", "Inscription client");
        fr.put("register.firstname", "Prénom");
        fr.put("register.lastname", "Nom");
        fr.put("register.email", "Email");
        fr.put("register.password", "Mot de passe");
        fr.put("register.confirm", "Confirmer le mot de passe");
        fr.put("register.address", "Adresse");
        fr.put("register.phone", "Téléphone");
        fr.put("register.city", "Ville");
        fr.put("register.button", "S'INSCRIRE");
        fr.put("register.back", "RETOUR");
        fr.put("register.success", "Inscription réussie !");
        fr.put("register.error.empty", "Veuillez remplir tous les champs.");
        fr.put("register.error.email", "Email invalide.");
        fr.put("register.error.password.length", "Le mot de passe doit contenir au moins 6 caractères.");
        fr.put("register.error.password.match", "Les mots de passe ne correspondent pas.");

        fr.put("shop.title", "ChriOnline - Votre Boutique");
        fr.put("shop.search", "Rechercher");
        fr.put("shop.search.placeholder", "Rechercher un produit...");
        fr.put("shop.categories", "Catégories");
        fr.put("shop.cart", "Panier");
        fr.put("shop.logout", "Déconnexion");
        fr.put("shop.welcome", "Bienvenue");
        fr.put("shop.empty", "Aucun produit trouvé");
        fr.put("shop.all", "Tous les produits");
        fr.put("shop.general", "Général");
        fr.put("shop.admin", "Admin Panel");
        fr.put("shop.profile", "Mon Profil");

        fr.put("product.category", "Catégorie");
        fr.put("product.stock", "Stock");
        fr.put("product.stock.limited", "Stock limité");
        fr.put("product.stock.out", "Rupture de stock");
        fr.put("product.price", "Prix");
        fr.put("product.add", "Ajouter");
        fr.put("product.quantity", "Quantité");
        fr.put("product.description", "Description");
        fr.put("product.no.description", "Aucune description disponible.");
        fr.put("product.details", "Détails");

        fr.put("cart.title", "Mon Panier");
        fr.put("cart.empty", "Votre panier est vide");
        fr.put("cart.product", "Produit");
        fr.put("cart.quantity", "Quantité");
        fr.put("cart.unit.price", "Prix unitaire");
        fr.put("cart.subtotal", "Sous-total");
        fr.put("cart.total", "Total");
        fr.put("cart.checkout", "VALIDER");
        fr.put("cart.clear", "Vider tout");
        fr.put("cart.back", "Retour");
        fr.put("cart.remove", "Supprimer");
        fr.put("cart.remove.confirm", "Supprimer ce produit du panier ?");
        fr.put("cart.clear.confirm", "Voulez-vous vraiment vider tout le panier ?");
        fr.put("cart.clear.success", "Panier vidé avec succès !");
        fr.put("cart.add.success", "ajouté au panier !");

        fr.put("payment.title", "Paiement sécurisé");
        fr.put("payment.order", "Commande");
        fr.put("payment.method", "Méthode de paiement");
        fr.put("payment.card", "Carte bancaire");
        fr.put("payment.cash", "Espèces");
        fr.put("payment.confirm", "CONFIRMER LE PAIEMENT");
        fr.put("payment.back.cart", "RETOUR AU PANIER");
        fr.put("payment.back.shop", "RETOUR À LA BOUTIQUE");
        fr.put("payment.success", "Paiement réussi !");
        fr.put("payment.failed", "Échec du paiement");
        fr.put("payment.cancel", "Annuler le paiement");
        fr.put("payment.new.card", "Nouvelle carte");

        fr.put("profile.title", "Mon Profil");
        fr.put("profile.welcome", "Bienvenue");
        fr.put("profile.info", "Informations personnelles");
        fr.put("profile.orders", "Mes commandes");
        fr.put("profile.order.history", "Historique des commandes");
        fr.put("profile.no.orders", "Aucune commande passée");
        fr.put("profile.order.date", "Date");
        fr.put("profile.order.total", "Total");
        fr.put("profile.order.status", "Statut");
        fr.put("profile.order.details", "Détails");
        fr.put("profile.edit", "Modifier le profil");
        fr.put("profile.edit.title", "Modifier mes informations");
        fr.put("profile.save", "Enregistrer");
        fr.put("profile.cancel", "Annuler");
        fr.put("profile.update.success", "Profil mis à jour avec succès !");
        fr.put("profile.update.error", "Erreur lors de la mise à jour du profil.");
        fr.put("profile.name", "Nom complet");
        fr.put("profile.email", "Email");
        fr.put("profile.phone", "Téléphone");
        fr.put("profile.address", "Adresse");
        fr.put("profile.city", "Ville");
        
        fr.put("login.rsa.admin", "Connexion Admin RSA (sans mot de passe)");

        fr.put("language", "Langue");
        translations.put("fr", fr);

        // ==================== ANGLAIS ====================
        Map<String, String> en = new HashMap<>();
        en.put("app.title", "ChriOnline - Your Store");
        en.put("login.title", "Login");
        en.put("login.subtitle", "Your online store");
        en.put("login.email", "Email");
        en.put("login.password", "Password");
        en.put("login.button", "LOGIN");
        en.put("login.register", "CREATE ACCOUNT");
        en.put("login.error.empty", "Please fill all fields.");
        en.put("login.error.invalid", "Invalid email or password.");
        en.put("login.error.server", "Server unavailable.");
        // 🔐 PROTECTION FORCE BRUTE
        en.put("login.error.too_many_attempts", "❌ Too many failed attempts. Please try again in 5 minutes.");
        en.put("login.error.remaining_attempts", "❌ Invalid email or password. Remaining attempts: ");
        en.put("login.error.warning_attempts", "⚠️ WARNING! Only ");
        en.put("login.error.warning_attempts_end", " attempt(s) left before block!");
        en.put("login.blocked.title", "Account temporarily blocked");
        en.put("login.blocked.message", "Warning! You have exceeded the allowed number of attempts.\nPlease try again in 5 minutes.\n\nIf you forgot your password, please contact the administrator.");

        en.put("register.title", "Create Account");
        en.put("register.subtitle", "Customer registration");
        en.put("register.firstname", "First Name");
        en.put("register.lastname", "Last Name");
        en.put("register.email", "Email");
        en.put("register.password", "Password");
        en.put("register.confirm", "Confirm Password");
        en.put("register.address", "Address");
        en.put("register.phone", "Phone");
        en.put("register.city", "City");
        en.put("register.button", "REGISTER");
        en.put("register.back", "BACK");
        en.put("register.success", "Registration successful!");
        en.put("register.error.empty", "Please fill all fields.");
        en.put("register.error.email", "Invalid email.");
        en.put("register.error.password.length", "Password must be at least 6 characters.");
        en.put("register.error.password.match", "Passwords do not match.");

        en.put("shop.title", "ChriOnline - Your Store");
        en.put("shop.search", "Search");
        en.put("shop.search.placeholder", "Search a product...");
        en.put("shop.categories", "Categories");
        en.put("shop.cart", "Cart");
        en.put("shop.logout", "Logout");
        en.put("shop.welcome", "Welcome");
        en.put("shop.empty", "No products found");
        en.put("shop.all", "All products");
        en.put("shop.general", "General");
        en.put("shop.admin", "Admin Panel");
        en.put("shop.profile", "My Profile");

        en.put("product.category", "Category");
        en.put("product.stock", "Stock");
        en.put("product.stock.limited", "Limited stock");
        en.put("product.stock.out", "Out of stock");
        en.put("product.price", "Price");
        en.put("product.add", "Add");
        en.put("product.quantity", "Quantity");
        en.put("product.description", "Description");
        en.put("product.no.description", "No description available.");
        en.put("product.details", "Details");

        en.put("cart.title", "My Cart");
        en.put("cart.empty", "Your cart is empty");
        en.put("cart.product", "Product");
        en.put("cart.quantity", "Quantity");
        en.put("cart.unit.price", "Unit Price");
        en.put("cart.subtotal", "Subtotal");
        en.put("cart.total", "Total");
        en.put("cart.checkout", "CHECKOUT");
        en.put("cart.clear", "Clear All");
        en.put("cart.back", "Back");
        en.put("cart.remove", "Remove");
        en.put("cart.remove.confirm", "Remove this product from cart?");
        en.put("cart.clear.confirm", "Are you sure you want to clear the cart?");
        en.put("cart.clear.success", "Cart cleared successfully!");
        en.put("cart.add.success", "added to cart!");

        en.put("payment.title", "Secure Payment");
        en.put("payment.order", "Order");
        en.put("payment.method", "Payment Method");
        en.put("payment.card", "Credit Card");
        en.put("payment.cash", "Cash");
        en.put("payment.confirm", "CONFIRM PAYMENT");
        en.put("payment.back.cart", "BACK TO CART");
        en.put("payment.back.shop", "BACK TO STORE");
        en.put("payment.success", "Payment successful!");
        en.put("payment.failed", "Payment failed");
        en.put("payment.cancel", "Cancel payment");
        en.put("payment.new.card", "New card");

        en.put("profile.title", "My Profile");
        en.put("profile.welcome", "Welcome");
        en.put("profile.info", "Personal Information");
        en.put("profile.orders", "My Orders");
        en.put("profile.order.history", "Order History");
        en.put("profile.no.orders", "No orders placed");
        en.put("profile.order.date", "Date");
        en.put("profile.order.total", "Total");
        en.put("profile.order.status", "Status");
        en.put("profile.order.details", "Details");
        en.put("profile.edit", "Edit Profile");
        en.put("profile.edit.title", "Edit My Information");
        en.put("profile.save", "Save");
        en.put("profile.cancel", "Cancel");
        en.put("profile.update.success", "Profile updated successfully!");
        en.put("profile.update.error", "Error updating profile.");
        en.put("profile.name", "Full Name");
        en.put("profile.email", "Email");
        en.put("profile.phone", "Phone");
        en.put("profile.address", "Address");
        en.put("profile.city", "City");
        
        en.put("login.rsa.admin", "Admin RSA Login (no password)");

        en.put("language", "Language");
        translations.put("en", en);

        // ==================== ARABE ====================
        Map<String, String> ar = new HashMap<>();
        ar.put("app.title", "شري أونلاين - متجرك");
        ar.put("login.title", "تسجيل الدخول");
        ar.put("login.subtitle", "متجرك الإلكتروني");
        ar.put("login.email", "البريد الإلكتروني");
        ar.put("login.password", "كلمة المرور");
        ar.put("login.button", "تسجيل الدخول");
        ar.put("login.register", "إنشاء حساب");
        ar.put("login.error.empty", "الرجاء ملء جميع الحقول.");
        ar.put("login.error.invalid", "البريد الإلكتروني أو كلمة المرور غير صحيحة.");
        ar.put("login.error.server", "الخادم غير متاح.");
        // 🔐 PROTECTION FORCE BRUTE
        ar.put("login.error.too_many_attempts", "❌ عدد كبير جداً من المحاولات الفاشلة. أعد المحاولة بعد 5 دقائق.");
        ar.put("login.error.remaining_attempts", "❌ بريد إلكتروني أو كلمة مرور غير صحيحة. المحاولات المتبقية: ");
        ar.put("login.error.warning_attempts", "⚠️ تحذير! متبقي ");
        ar.put("login.error.warning_attempts_end", " محاولة(ات) قبل الحظر!");
        ar.put("login.blocked.title", "الحساب محظور مؤقتاً");
        ar.put("login.blocked.message", "تحذير! لقد تجاوزت عدد المحاولات المسموح بها.\nالرجاء المحاولة مرة أخرى بعد 5 دقائق.\n\nإذا نسيت كلمة المرور، يرجى الاتصال بالمسؤول.");

        ar.put("register.title", "إنشاء حساب");
        ar.put("register.subtitle", "تسجيل عميل جديد");
        ar.put("register.firstname", "الاسم");
        ar.put("register.lastname", "اللقب");
        ar.put("register.email", "البريد الإلكتروني");
        ar.put("register.password", "كلمة المرور");
        ar.put("register.confirm", "تأكيد كلمة المرور");
        ar.put("register.address", "العنوان");
        ar.put("register.phone", "الهاتف");
        ar.put("register.city", "المدينة");
        ar.put("register.button", "تسجيل");
        ar.put("register.back", "رجوع");
        ar.put("register.success", "تم التسجيل بنجاح!");
        ar.put("register.error.empty", "الرجاء ملء جميع الحقول.");
        ar.put("register.error.email", "بريد إلكتروني غير صالح.");
        ar.put("register.error.password.length", "يجب أن تحتوي كلمة المرور على 6 أحرف على الأقل.");
        ar.put("register.error.password.match", "كلمات المرور غير متطابقة.");

        ar.put("shop.title", "شري أونلاين - متجرك");
        ar.put("shop.search", "بحث");
        ar.put("shop.search.placeholder", "ابحث عن منتج...");
        ar.put("shop.categories", "التصنيفات");
        ar.put("shop.cart", "السلة");
        ar.put("shop.logout", "تسجيل خروج");
        ar.put("shop.welcome", "مرحباً");
        ar.put("shop.empty", "لا توجد منتجات");
        ar.put("shop.all", "جميع المنتجات");
        ar.put("shop.general", "عام");
        ar.put("shop.admin", "لوحة الإدارة");
        ar.put("shop.profile", "ملفي");

        ar.put("product.category", "التصنيف");
        ar.put("product.stock", "المخزون");
        ar.put("product.stock.limited", "مخزون محدود");
        ar.put("product.stock.out", "غير متوفر");
        ar.put("product.price", "السعر");
        ar.put("product.add", "أضف");
        ar.put("product.quantity", "الكمية");
        ar.put("product.description", "الوصف");
        ar.put("product.no.description", "لا يوجد وصف متاح.");
        ar.put("product.details", "التفاصيل");

        ar.put("cart.title", "سلة التسوق");
        ar.put("cart.empty", "سلة التسوق فارغة");
        ar.put("cart.product", "المنتج");
        ar.put("cart.quantity", "الكمية");
        ar.put("cart.unit.price", "سعر الوحدة");
        ar.put("cart.subtotal", "المجموع");
        ar.put("cart.total", "الإجمالي");
        ar.put("cart.checkout", "إتمام الشراء");
        ar.put("cart.clear", "تفريغ السلة");
        ar.put("cart.back", "رجوع");
        ar.put("cart.remove", "حذف");
        ar.put("cart.remove.confirm", "هل تريد حذف هذا المنتج من السلة؟");
        ar.put("cart.clear.confirm", "هل أنت متأكد من تفريغ السلة؟");
        ar.put("cart.clear.success", "تم تفريغ السلة بنجاح!");
        ar.put("cart.add.success", "تمت الإضافة إلى السلة!");

        ar.put("payment.title", "دفع آمن");
        ar.put("payment.order", "الطلب");
        ar.put("payment.method", "طريقة الدفع");
        ar.put("payment.card", "بطاقة ائتمان");
        ar.put("payment.cash", "نقدي");
        ar.put("payment.confirm", "تأكيد الدفع");
        ar.put("payment.back.cart", "العودة للسلة");
        ar.put("payment.back.shop", "العودة للمتجر");
        ar.put("payment.success", "تم الدفع بنجاح!");
        ar.put("payment.failed", "فشل الدفع");
        ar.put("payment.cancel", "إلغاء الدفع");
        ar.put("payment.new.card", "بطاقة جديدة");

        ar.put("profile.title", "ملفي الشخصي");
        ar.put("profile.welcome", "مرحباً");
        ar.put("profile.info", "المعلومات الشخصية");
        ar.put("profile.orders", "طلباتي");
        ar.put("profile.order.history", "سجل الطلبات");
        ar.put("profile.no.orders", "لا توجد طلبات سابقة");
        ar.put("profile.order.date", "التاريخ");
        ar.put("profile.order.total", "المجموع");
        ar.put("profile.order.status", "الحالة");
        ar.put("profile.order.details", "التفاصيل");
        ar.put("profile.edit", "تعديل الملف");
        ar.put("profile.edit.title", "تعديل معلوماتي");
        ar.put("profile.save", "حفظ");
        ar.put("profile.cancel", "إلغاء");
        ar.put("profile.update.success", "تم تحديث الملف بنجاح!");
        ar.put("profile.update.error", "خطأ في تحديث الملف.");
        ar.put("profile.name", "الاسم الكامل");
        ar.put("profile.email", "البريد الإلكتروني");
        ar.put("profile.phone", "الهاتف");
        ar.put("profile.address", "العنوان");
        ar.put("profile.city", "المدينة");

        ar.put("login.rsa.admin", "دخول المشرف RSA (بدون كلمة مرور)");

        ar.put("language", "اللغة");
        translations.put("ar", ar);
    }
}