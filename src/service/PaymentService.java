package service;

import dao.PaymentDAO;
import model.Payment;

public class PaymentService {

    private PaymentDAO paymentDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
    }

    // ── Simuler et enregistrer un paiement ────────────────────
    public boolean processPayment(Payment payment) {
        if (payment == null) {
            System.out.println("Paiement invalide.");
            return false;
        }

        // simulation paiement
        boolean success = payment.processPayment();

        if (!success) {
            System.out.println("Le paiement a échoué.");
            return false;
        }

        // si succès, on enregistre en base
        boolean saved = paymentDAO.save(payment);

        if (saved) {
            System.out.println("Paiement réussi et enregistré.");
            return true;
        } else {
            System.out.println("Paiement réussi mais non enregistré.");
            return false;
        }
    }
}