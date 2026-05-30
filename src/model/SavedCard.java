package model;

public class SavedCard {
    private int id;
    private int userId;
    private String cardLast4;
    private String cardBrand;
    private String cardHolderName;
    private int expiryMonth;
    private int expiryYear;
    private boolean isDefault;
    private String createdAt;

    public SavedCard() {}

    public SavedCard(int id, int userId, String cardLast4, String cardBrand, 
                     String cardHolderName, int expiryMonth, int expiryYear, 
                     boolean isDefault, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.cardLast4 = cardLast4;
        this.cardBrand = cardBrand;
        this.cardHolderName = cardHolderName;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    
    public int getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(int expiryMonth) { this.expiryMonth = expiryMonth; }
    
    public int getExpiryYear() { return expiryYear; }
    public void setExpiryYear(int expiryYear) { this.expiryYear = expiryYear; }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getDisplayName() {
        return cardBrand + " •••• " + cardLast4 + " (exp. " + expiryMonth + "/" + expiryYear + ")";
    }
    
    public String getExpiry() {
        return String.format("%02d/%d", expiryMonth, expiryYear);
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
}