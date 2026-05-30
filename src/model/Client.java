
package model;

public class Client extends User {

    private String address;
    private String phone;
    private String ville;

    public Client(String nom, String prenom, String email,
                  String password, String address, String phone, String ville) {
        super(nom, prenom, email, password, "client");
        this.address = address;
        this.phone = phone;
        this.ville = ville;
    }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    @Override
    public void logout() {
        System.out.println("Client " + getNom() + " déconnecté.");
    }

    public boolean register() {
        System.out.println("Inscription de " + getNom() + " " + getPrenom());
        return true;
    }

    public void viewProfile() {
        System.out.println("=== Mon Profil ===");
        System.out.println("Nom     : " + getNom());
        System.out.println("Prénom  : " + getPrenom());
        System.out.println("Email   : " + getEmail());
        System.out.println("Ville   : " + ville);
        System.out.println("Tél     : " + phone);
    }

    public void updateProfile(String address, String phone, String ville) {
        this.address = address;
        this.phone = phone;
        this.ville = ville;
        System.out.println("Profil mis à jour");
    }

    public void deleteAccount() {
        System.out.println("Compte de " + getNom() + " supprimé.");
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    } 

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    } 

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    @Override
    public String toString() {
        return "Client{nom='" + getNom() + "', prenom='" + getPrenom() +
                "', email='" + getEmail() + "', ville='" + ville + "'}";
    }
}
