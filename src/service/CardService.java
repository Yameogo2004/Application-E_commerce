package service;

import dao.SavedCardDAO;
import model.SavedCard;

import java.util.List;

public class CardService {

    private final SavedCardDAO savedCardDAO;

    public CardService() {
        this.savedCardDAO = new SavedCardDAO();
    }

    public boolean saveCard(SavedCard card) {
        return savedCardDAO.save(card);
    }

    public List<SavedCard> getUserCards(int userId) {
        return savedCardDAO.findByUserId(userId);
    }

    public boolean deleteCard(int cardId) {
        return savedCardDAO.delete(cardId);
    }

    public boolean setDefaultCard(int cardId, int userId) {
        return savedCardDAO.setDefault(cardId, userId);
    }
    
    public String detectCardBrand(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return "CARTE";
        String firstDigit = cardNumber.substring(0, 1);
        if (firstDigit.equals("4")) return "VISA";
        if (cardNumber.startsWith("5")) return "MASTERCARD";
        if (cardNumber.startsWith("3")) return "AMEX";
        return "CARTE";
    }
}