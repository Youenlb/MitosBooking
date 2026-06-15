# MitosBooking — Documentation technique

> Projet ENSSAT — Application Android de prêt de livres entre particuliers

## Présentation

**MitosBooking** est une application Android native permettant à des utilisateurs de gérer leur bibliothèque personnelle, de prêter des livres à d'autres utilisateurs et de suivre les emprunts en cours. L'échange s'effectue en face-à-face via un système de **QR codes transactionnels** : le propriétaire génère un QR, l'emprunteur le scanne pour finaliser l'opération.

### Fonctionnalités principales

- **Gestion de bibliothèque** — Ajout de livres par scan ISBN (code-barres EAN-13), récupération automatique des métadonnées (titre, auteurs, couverture) via l'API OpenLibrary
- **Prêt de livres** — Génération d'un QR code de transaction côté propriétaire, scan côté emprunteur, synchronisation via un backend serverless (Google Cloud Functions)
- **Retour de livres** — Même mécanique inversée : le propriétaire initie le retour, l'emprunteur confirme par scan
- **Profil utilisateur** — Identité locale persistée en Room, nécessaire pour participer aux transactions
- **3 vues principales** — Ma bibliothèque (livres possédés disponibles), Mes prêts (livres prêtés en cours), Mes emprunts (livres empruntés)

## Sommaire de la documentation

| Document | Contenu |
|----------|---------|
| [Architecture](./ARCHITECTURE.md) | Stack technique, layering, injection de dépendances, choix de conception |
| [Modèle de données](./DATA_MODEL.md) | Schéma Room, entités, sémantique des champs de prêt, identité utilisateur |
| [API & Backend](./API.md) | Endpoints Cloud Functions, API OpenLibrary, format du QR code |
| [Flux utilisateur](./USER_FLOWS.md) | Parcours détaillés ajout / prêt / retour / profil avec diagrammes de séquence |
| [Structure du projet](./PROJECT_STRUCTURE.md) | Organisation des packages, rôle de chaque fichier, fichiers legacy |

## APK pré-compilé

Un APK debug prêt à installer est disponible dans le dossier [`release/`](../release/) :

```
release/
├── app-debug.apk          # APK installable
└── output-metadata.json    # Métadonnées de build
```

> **Installation** : `adb install release/app-debug.apk`

## Build & Run

```bash
./gradlew assembleDebug
```

### Pré-requis

- Android Studio Ladybug+ ou Gradle CLI
- JDK 17
- Un appareil/émulateur API 24+ avec caméra (pour le scan)

### Configuration

| Paramètre | Valeur |
|-----------|--------|
| `minSdk` | 24 |
| `targetSdk` | 36 |
| `compileSdk` | 36 |
| Kotlin | 1.9 |
| Compose Compiler | 1.5.1 |
| JVM target | 17 |
| Firebase project | `sharemybook-30d42` (nom historique) |
| Backend region | `europe-west9` (Paris) |

### Permissions Android

| Permission | Usage |
|-----------|-------|
| `INTERNET` | Appels API OpenLibrary + Cloud Functions |
| `CAMERA` | Scan ISBN (EAN-13) et QR codes de transaction |
