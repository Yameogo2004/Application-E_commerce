package model;

public class Admin extends User {

    public Admin(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password, "admin");
    }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    @Override
    public void logout() {
        System.out.println("Admin " + getNom() + " déconnecté.");
    }

    @Override
    public String toString() {
        return "Admin{nom='" + getNom() + "', prenom='" + getPrenom() +
                "', email='" + getEmail() + "'}";
    }
}