# Flux utilisateur

## 1. Ajout d'un livre à sa bibliothèque

L'utilisateur scanne le code-barres ISBN d'un livre physique. L'app récupère les métadonnées via OpenLibrary, affiche un dialogue de confirmation, puis insère le livre en base locale.

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant MA as MainActivity
    participant SA as ScannerActivity
    participant VM as MyLibraryViewModel
    participant BR as BookRepository
    participant OL as OpenLibrary API

    U->>MA: Appuie sur FAB "+"
    MA->>SA: startActivityForResult()
    
    Note over SA: CameraX + ML Kit<br/>Détection EAN-13

    U->>SA: Présente le code-barres du livre
    SA->>SA: BarcodeAnalyzer détecte EAN-13
    SA-->>MA: setResult(OK, barcode="978-...")
    
    MA->>VM: onIsbnScanned("978-...")
    VM->>VM: isLoadingBook = true
    VM->>BR: getBookDetailsFromApi("978-...")
    BR->>OL: GET /api/books?bibkeys=ISBN:978-...
    OL-->>BR: {title, authors, cover}
    BR-->>VM: Book(uid=isbn, title, authors, covers)
    
    VM->>VM: pendingBook = book
    Note over MA: Affiche BookValidationDialog<br/>avec couverture, titre, auteurs

    alt Utilisateur confirme
        U->>MA: Clique "Ajouter"
        MA->>VM: confirmAddBook()
        VM->>BR: insertBook(book)
        BR->>BR: bookDao.insertBook (REPLACE)
        VM->>VM: pendingBook = null
        Note over MA: Toast "Livre ajouté !"
    else Utilisateur annule
        U->>MA: Clique "Annuler"
        MA->>VM: cancelAddBook()
        VM->>VM: pendingBook = null
    end
```

**Points d'attention** :
- Si l'ISBN n'est pas trouvé sur OpenLibrary, un toast d'erreur est affiché
- L'ISBN est utilisé comme `uid` (clé primaire) — scanner deux fois le même livre fait un REPLACE
- Le `BookValidationDialog` affiche un `CircularProgressIndicator` pendant le chargement API

---

## 2. Prêt d'un livre (LOAN)

Le propriétaire initie un prêt depuis l'onglet "Ma bibliothèque" en cliquant "Prêter" sur un livre disponible. L'app crée une transaction côté backend, affiche un QR code, et attend que l'emprunteur le scanne.

```mermaid
sequenceDiagram
    actor Owner as Owner
    participant TA as TransactionActivity
    participant TVM as TransactionViewModel
    participant API as Cloud Functions
    participant SA as ScannerActivity
    participant ATA as AcceptTransactionActivity  
    participant ATVM as AcceptTransactionVM
    actor Borrower as Borrower

    Note over Owner,Borrower: Les deux utilisateurs sont physiquement côte à côte

    Owner->>TA: Clic "Prêter" sur un livre
    TA->>TVM: init (bookId, action="LOAN")
    
    TVM->>TVM: Vérifie profil complet (nom, tel, email)
    TVM->>API: POST /shareMyBook/init<br/>{action:"LOAN", book, owner}
    API-->>TVM: {shareId: "abc123"}
    
    Note over TA: Génère et affiche le QR code<br/>{"shareId":"abc123"}
    
    TVM->>TVM: Démarre pollForResult()

    Borrower->>SA: Ouvre le scanner
    SA->>SA: ML Kit détecte QR_CODE
    SA->>SA: Parse ShareIdQrCode → shareId
    SA->>ATA: Intent(shareId="abc123")

    ATA->>ATVM: loadTransaction("abc123")
    ATVM->>API: GET /shareMyBook/result/abc123
    API-->>ATVM: {action:"LOAN", book, owner, borrower:null}
    
    Note over ATA: Affiche: livre, propriétaire<br/>Bouton "Confirmer l'emprunt"

    ATVM->>ATVM: Vérifie profil complet
    Borrower->>ATA: Clique "Confirmer l'emprunt"
    ATVM->>API: POST /shareMyBook/accept/abc123<br/>{borrower}
    API-->>ATVM: {action:"LOAN", book, owner, borrower}

    Note over ATA: "Transaction acceptée !"

    ATVM->>ATVM: insertBook(book.copy(<br/>  borrowerId=moi, lenderId=owner))
    ATVM->>ATVM: insertUser(owner)
    Note over Borrower: Livre visible dans "Mes emprunts"

    loop Poll toutes les 1s (max 120)
        TVM->>API: GET /shareMyBook/result/abc123
        API-->>TVM: {borrower: non-null}
    end
    
    TVM->>TVM: updateBook(borrowerId=borrower.uid)
    TVM->>TVM: insertUser(borrower)
    Note over TA: "Transaction terminée !<br/>Prêté à Marie Martin"
    Note over Owner: Livre passe dans "Mes prêts"
```

**Effets sur les bases locales** :

| Appareil | Action Room | Résultat |
|----------|-------------|----------|
| Owner | `updateBook(borrowerId = borrower.uid)` | Le livre passe dans l'onglet "Mes prêts" |
| Owner | `insertUser(borrower)` | Stocke le profil de l'emprunteur pour affichage |
| Borrower | `insertBook(book, borrowerId=self, lenderId=owner)` | Le livre apparaît dans "Mes emprunts" |
| Borrower | `insertUser(owner)` | Stocke le profil du propriétaire |

---

## 3. Retour d'un livre (RETURN)

Le propriétaire initie le retour depuis l'onglet "Mes prêts" en cliquant "Générer le QR code". Le flux est identique au prêt mais avec `action="RETURN"`.

```mermaid
sequenceDiagram
    actor Owner as Owner
    participant TVM as TransactionViewModel
    participant API as Cloud Functions
    participant ATVM as AcceptTransactionVM
    actor Borrower as Borrower

    Owner->>TVM: init (bookId, action="RETURN")
    
    Note over TVM: book.copy(borrowerId=null)<br/>avant envoi au backend

    TVM->>API: POST /shareMyBook/init<br/>{action:"RETURN", book, owner}
    API-->>TVM: {shareId}
    Note over Owner: Affiche QR code

    TVM->>TVM: Démarre poll

    Borrower->>ATVM: Scan QR → loadTransaction(shareId)
    ATVM->>API: GET /shareMyBook/result/{shareId}
    API-->>ATVM: {action:"RETURN", book, owner}
    
    Note over Borrower: Bouton "Confirmer le retour"

    Borrower->>ATVM: acceptTransaction()
    ATVM->>API: POST /shareMyBook/accept/{shareId}
    API-->>ATVM: OK
    
    ATVM->>ATVM: deleteBook(book)
    Note over Borrower: Livre supprimé de<br/>"Mes emprunts"

    TVM->>API: GET /shareMyBook/result/{shareId}
    API-->>TVM: {borrower: non-null}
    
    TVM->>TVM: deleteBook(book) côté owner
    Note over Owner: borrowerId remis à null<br/>Livre revient dans "Ma bibliothèque"
```

**Différences avec le LOAN** :
- Le `book` envoyé au backend a son `borrowerId` mis à `null` avant envoi
- Côté borrower : `deleteBook` au lieu de `insertBook`
- Côté owner : le livre retrouve `borrowerId = null` et repasse dans l'onglet "Ma bibliothèque"

---

## 4. Profil utilisateur

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant PA as ProfileActivity
    participant PVM as ProfileViewModel
    participant BR as BookRepository
    participant SP as SharedPreferences

    U->>PA: Clic icône profil (TopAppBar)
    PA->>PVM: init
    PVM->>SP: getString("user_id")
    
    alt user_id existe
        PVM->>BR: getUserById(userId)
        BR-->>PVM: Flow<User>
        Note over PA: Champs pré-remplis<br/>(fullName, tel, email)
    else premier lancement
        PVM->>SP: putString("user_id", UUID.random())
        Note over PA: Champs vides
    end

    U->>PA: Saisit nom, tel, email
    U->>PA: Clique "Enregistrer"
    PA->>PVM: saveUser(fullName, tel, email)
    PVM->>BR: insertUser(User(uid, fullName, tel, email))
    Note over BR: INSERT ... ON CONFLICT REPLACE
    Note over PA: Toast "Profil enregistré"
```

**Validation du profil** : les `TransactionViewModel` et `AcceptTransactionViewModel` vérifient que `fullName`, `tel` et `email` sont non vides avant d'autoriser une transaction. Si le profil est incomplet, un message d'erreur est affiché et l'opération est bloquée.

---

## 5. Scanner — Double usage

Le `ScannerActivity` sert à deux flux distincts selon le type de code détecté :

```mermaid
flowchart TD
    START[ScannerActivity lancé] --> PERM{Permission CAMERA ?}
    PERM -->|Non| DEMANDE[Demande permission]
    DEMANDE -->|Refusée| FIN1[finish avec toast]
    DEMANDE -->|Accordée| CAMERA
    PERM -->|Oui| CAMERA[CameraX + ML Kit<br/>analyse frame par frame]
    
    CAMERA --> DETECT{Type de code ?}
    
    DETECT -->|EAN-13| BARCODE[setResult OK<br/>extra: scanned_barcode]
    BARCODE --> FIN2[finish → retour à MainActivity]
    
    DETECT -->|QR Code| PARSE{Parse JSON<br/>ShareIdQrCode ?}
    PARSE -->|Valide| ACCEPT[Intent → AcceptTransactionActivity<br/>extra: shareId]
    PARSE -->|Invalide| TOAST[Toast erreur<br/>QR non reconnu]
    TOAST --> FIN3[finish]
```

**Détails techniques** :
- `BarcodeAnalyzer` est un `ImageAnalysis.Analyzer` avec un flag `@Volatile isScanning` pour éviter les détections multiples
- Les formats acceptés : `Barcode.FORMAT_QR_CODE` et `Barcode.FORMAT_EAN_13`
- Le scanner s'arrête dès la première détection réussie (`isScanning = false`)

---

## 6. Données de démo (seed)

Au premier lancement, si la table `books` est vide, `MyLibraryViewModel` insère automatiquement 26 livres de démonstration avec des URLs de couverture OpenLibrary valides. Cela permet de tester l'app immédiatement sans scanner de livres physiques.

Catégories du seed : Fantasy (5), Science-Fiction (5), Classiques (5), Romans modernes (5), Thrillers (3), Non-fiction (3).
