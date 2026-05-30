
package service;

import dao.OtpDAO;

import java.security.SecureRandom;

public class OtpService {

    private final OtpDAO otpDAO;
    private final EmailService emailService;

    public OtpService() {
        this.otpDAO = new OtpDAO();
        this.emailService = new EmailService();
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public boolean sendOtp(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        if (!otpDAO.userExists(email)) {
            System.out.println("Aucun utilisateur avec cet email : " + email);
            return false;
        }

        String code = generateCode();

        boolean saved = otpDAO.saveOtp(email, code);
        if (!saved) {
            System.out.println("Erreur sauvegarde OTP en BDD");
            return false;
        }

        boolean sent = emailService.sendOtpEmail(email, code);
        if (!sent) {
            System.out.println("Erreur envoi email OTP");
            return false;
        }

        System.out.println("OTP envoyé à : " + email);
        return true;
    }

    public boolean verifyOtp(String email, String code) {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()) {
            return false;
        }

        boolean valid = otpDAO.verifyOtp(email, code);

        if (valid) {
            otpDAO.activateAccount(email);
            System.out.println("Compte activé : " + email);
        } else {
            System.out.println("Code OTP invalide ou expiré pour : " + email);
        }

        return valid;
    }
}
