package model;

import java.util.Date;

public abstract class User {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role;
    private String status;
    private Date createdAt;
    private String publicKey;  // 🔐 Clé publique RSA pour authentification admin

    public User(String nom, String prenom, String email, String password, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = "active";
        this.createdAt = new Date();
        this.publicKey = null;  // Par défaut, pas de clé publique
    }

    public abstract boolean login(String email, String password);
    public abstract void logout();

    // ──────────────── GETTERS & SETTERS ────────────────

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    } 

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    } 

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    } 

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    } 

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    } 

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    } 

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    // 🔐 NOUVEAU - Getter et Setter pour la clé publique
    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    // 🔐 Méthode utilitaire pour vérifier si l'utilisateur a une clé publique
    public boolean hasPublicKey() {
        return publicKey != null && !publicKey.isEmpty();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom +
                "', email='" + email + "', role='" + role + "', status='" + status + 
                "', hasPublicKey=" + (publicKey != null) + "'}";
    }
}