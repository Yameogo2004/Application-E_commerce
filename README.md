# 🛒 ChriOnline - Application E-commerce Sécurisée

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Socket](https://img.shields.io/badge/Communication-TCP%2FUDP-blue)](https://docs.oracle.com/javase/tutorial/networking/sockets/)
[![Crypto](https://img.shields.io/badge/Cryptographie-AES%20%7C%20RSA-green)](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html)
[![License](https://img.shields.io/badge/Licence-Académique-lightgrey)](LICENSE)

## 📋 Description

**ChriOnline** est une application e-commerce complète développée en Java, mettant en œuvre une architecture **client-serveur native basée sur les sockets TCP/UDP**.

L'application simule un processus d'achat en ligne complet (consultation de produits, panier, paiement fictif) tout en intégrant des mécanismes de **sécurité avancés** : chiffrement hybride (AES + RSA), authentification admin sans mot de passe par **défi-réponse**, et protection contre les attaques réseau (brute force, replay, DoS).

---

## ✨ Fonctionnalités principales

### 🛍️ Cœur e-commerce 
| Module | Description |
|--------|-------------|
| 👤 **Utilisateurs** | Enregistrement, authentification sécurisée, gestion de session |
| 📦 **Produits** | Consultation catalogue, affichage détaillé (nom, prix, stock, description) |
| 🛒 **Panier** | Ajout/suppression de produits, calcul automatique du total |
| 💳 **Paiement** | Simulation de paiement par carte bancaire (validation fictive) |
| 📄 **Commandes** | Validation avec génération d'ID unique, gestion des erreurs |

### 🔒 Sécurité avancée 
| Protection | Mécanisme implémenté |
|------------|----------------------|
| 🔐 **Chiffrement hybride** | Échange de clé AES via RSA (similaire à HTTPS), chiffrement des données échangées |
| 🛡️ **Anti-brute force** | Rate limiting sur tentative d'authentification |
| 🔁 **Anti-rejeu** | Utilisation de challenge/timestamp pour les commandes sensibles |
| 🚫 **Anti-DoS** | Limitation du nombre de connexions simultanées (SYN flood, UDP flood) |
| 👑 **Authentification admin sans mot de passe** | Système défi-réponse avec signature RSA (type SSH) |

---

## 🏗️ Architecture technique

┌─────────────────────────────────────────────────────────────┐
│ Serveur Java │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│ │Thread pool│ │Gestion │ │JDBC │ │Gestion clés │ │
│ │multi-client│ │sessions │ │SQLite/ │ │RSA (JKS) │ │
│ └──────────┘ └──────────┘ └──────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
│ ▲
│ TCP (opérations principales) │ UDP (notifications)
▼ │
┌─────────────────────────────────────────────────────────────┐
│ Client Java (Console/UI) │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│ │Interface │ │Gestion │ │Signature │ │Chiffrement │ │
│ │utilisateur│ │panier │ │RSA │ │AES │ │
│ └──────────┘ └──────────┘ └──────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────┘


### Contraintes techniques respectées
- **Langage** : Java (JDK 17+)
- **Réseau** : Sockets TCP (`Socket`/`ServerSocket`) + UDP (`DatagramSocket`) multi-threadé
- **Base de données** : SQLite (stockage utilisateurs, produits, commandes, clés publiques)
- **Cryptographie** : Java Cryptography Architecture (JCA) – AES (GCM), RSA (2048), SHA256withRSA
- **Stockage sécurisé** : Java Keystore (JKS) pour les clés privées serveur
- **Versionning** : Git + GitHub

---

## 🚀 Installation et exécution

### Prérequis
- Java JDK 17 ou supérieur
- Git (optionnel, pour cloner)
- Aucune base de données externe (SQLite embarquée)

### Étapes

# 1. Cloner le repository
```bash
git clone https://github.com/Yameogo2004/Application-E_commerce.git
cd Application-E_commerce
```

# 2. Compiler les sources (depuis la racine)
javac -d bin src/**/*.java

# 3. Lancer le serveur
java -cp bin com.ecommerce.server.ServerMain

# 4. Lancer un client (dans un autre terminal)
java -cp bin com.ecommerce.client.ClientMain


💡 Note : La première exécution génère automatiquement les paires de clés RSA (serveur + admin) et initialise la base SQLite.

Configuration rapide
Le serveur écoute par défaut sur le port 5000 (TCP) et 5001 (UDP)

La base de données ecommerce.db est créée dans le dossier racine

Les clés RSA sont stockées dans keystore.jks (mot de passe : changeit par défaut)

## 📂 Structure du projet

Application-E_commerce/
│
├── src/                      # Code source Java complet
│   ├── client/               # Côté client (interface, panier, crypto)
│   ├── server/               # Côté serveur (threads, DB, sessions)
│   ├── common/               # Classes partagées (protocole, crypto)
│   └── resources/            # Fichiers de configuration
│
├── bin/                      # Classes compilées
├── lib/                      # Dépendances (aucune, tout est standard)
├── logs/                     # Traces d'exécution
├── Keys/                     # Clés RSA générées (admin et serveur)
├── image/                    # Assets pour l'interface
│
├── .gitignore                # Fichiers exclus du versionnement
├── LICENSE                   # Licence académique
└── README.md                 # Ce fichier


👤 Auteur  **Yameogo2004 -fath - Nachda**
