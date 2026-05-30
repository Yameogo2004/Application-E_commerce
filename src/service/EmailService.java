
package service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

	private static final String FROM_EMAIL = "nourouddine.nachda@gmail.com";
	private static final String APP_PASSWORD = "sehn ovlu pvtp epxj";

    public boolean sendOtpEmail(String toEmail, String otpCode) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("ChriOnline - Code de verification");

            String body = "Bonjour,\n\n"
                    + "Votre code de verification ChriOnline est :\n\n"
                    + otpCode + "\n\n"
                    + "Ce code expire dans 10 minutes.\n\n"
                    + "L'equipe ChriOnline";

            message.setText(body);
            Transport.send(message);

            System.out.println("Email envoye a : " + toEmail);
            return true;

        } catch (MessagingException e) {
            System.out.println("Erreur envoi email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
