package fr.enssat.sharemybook.mitosbooking

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dagger.hilt.android.AndroidEntryPoint
import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.ui.theme.MitosBookingTheme
import fr.enssat.sharemybook.mitosbooking.ui.viewmodel.MyLibraryViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MyLibraryViewModel by viewModels()

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("scanned_barcode")
            if (barcode != null) {
                viewModel.onIsbnScanned(barcode)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var currentScreen by remember { mutableStateOf("Ma bibliothèque") }

            MitosBookingTheme {
                LaunchedEffect(Unit) {
                    viewModel.toastMessages.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                // Dialog de validation du livre scanné
                val pendingBook by viewModel.pendingBook.collectAsState()
                val isLoadingBook by viewModel.isLoadingBook.collectAsState()

                if (pendingBook != null) {
                    BookValidationDialog(
                        book = pendingBook!!,
                        isLoading = isLoadingBook,
                        onConfirm = { viewModel.confirmAddBook() },
                        onCancel = { viewModel.cancelAddBook() }
                    )
                }

                // Dialog de saisie manuelle d'un livre
                var isManualEntryDialogOpen by remember { mutableStateOf(false) }

                if (isManualEntryDialogOpen) {
                    ManualBookEntryDialog(
                        onConfirm = { title, authors, isbn, covers ->
                            viewModel.addBookManually(title, authors, isbn, covers)
                            isManualEntryDialogOpen = false
                        },
                        onCancel = { isManualEntryDialogOpen = false }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(currentScreen) },
                            actions = {
                                IconButton(onClick = { context.startActivity(Intent(context, ProfileActivity::class.java)) }) {
                                    Icon(Icons.Filled.Person, contentDescription = "Profil")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Book, contentDescription = "Ma bibliothèque") },
                                label = { Text("Ma bibliothèque") },
                                selected = currentScreen == "Ma bibliothèque",
                                onClick = { currentScreen = "Ma bibliothèque" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.People, contentDescription = "Mes prêts") },
                                label = { Text("Mes prêts") },
                                selected = currentScreen == "Mes prêts",
                                onClick = { currentScreen = "Mes prêts" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.AutoStories, contentDescription = "Mes emprunts") },
                                label = { Text("Mes emprunts") },
                                selected = currentScreen == "Mes emprunts",
                                onClick = { currentScreen = "Mes emprunts" }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen == "Ma bibliothèque") {
                            var isMenuExpanded by remember { mutableStateOf(false) }

                            AnimatedFAB(
                                isExpanded = isMenuExpanded,
                                onMainClick = { isMenuExpanded = !isMenuExpanded },
                                onScanClick = {
                                    isMenuExpanded = false
                                    scanLauncher.launch(Intent(context, ScannerActivity::class.java))
                                },
                                onManualClick = {
                                    isMenuExpanded = false
                                    isManualEntryDialogOpen = true
                                }
                            )
                        } else if (currentScreen == "Mes emprunts") {
                            var isMenuExpanded by remember { mutableStateOf(false) }

                            AnimatedFAB(
                                isExpanded = isMenuExpanded,
                                onMainClick = { isMenuExpanded = !isMenuExpanded },
                                onScanClick = {
                                    isMenuExpanded = false
                                    scanLauncher.launch(Intent(context, ScannerActivity::class.java))
                                },
                                onManualClick = {
                                    isMenuExpanded = false
                                    isManualEntryDialogOpen = true
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        "Ma bibliothèque" -> MyLibraryScreen(viewModel, Modifier.padding(innerPadding))
                        "Mes prêts" -> MyLoansScreen(viewModel, Modifier.padding(innerPadding))
                        "Mes emprunts" -> MyBorrowsScreen(viewModel, Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyLibraryScreen(viewModel: MyLibraryViewModel, modifier: Modifier = Modifier) {
    val books by viewModel.myOwnedBooks.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        items(books, key = { it.uid }) { book ->
            val borrowerName by produceState<String?>(initialValue = null, key1 = book.borrowerId) {
                if (book.borrowerId != null) {
                    value = viewModel.getUserFullName(book.borrowerId)
                }
            }

            BookItem(
                book = book,
                borrowerName = borrowerName,
                lenderName = null,
                onLoanClick = {
                    val intent = Intent(context, TransactionActivity::class.java).apply {
                        putExtra("bookId", book.uid)
                        putExtra("action", "LOAN")
                    }
                    context.startActivity(intent)
                },
                onReturnClick = { },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyLoansScreen(viewModel: MyLibraryViewModel, modifier: Modifier = Modifier) {
    val books by viewModel.myLoans.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        items(books, key = { it.uid }) { book ->
            val borrowerName by produceState<String?>(initialValue = null, key1 = book.borrowerId) {
                if (book.borrowerId != null) {
                    value = viewModel.getUserFullName(book.borrowerId)
                }
            }

            BookItem(
                book = book,
                borrowerName = borrowerName,
                lenderName = null,
                onLoanClick = { },
                onReturnClick = {
                    // Le prêteur génère le QR code pour le retour
                    val intent = Intent(context, TransactionActivity::class.java).apply {
                        putExtra("bookId", book.uid)
                        putExtra("action", "RETURN")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyBorrowsScreen(viewModel: MyLibraryViewModel, modifier: Modifier = Modifier) {
    val books by viewModel.myBorrows.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        items(books, key = { it.uid }) { book ->
            val lenderName by produceState<String?>(initialValue = null, key1 = book.lenderId) {
                if (book.lenderId != null) {
                    value = viewModel.getUserFullName(book.lenderId)
                }
            }

            BookItem(
                book = book,
                borrowerName = null,
                lenderName = lenderName,
                onLoanClick = { },
                onReturnClick = {
                    // L'emprunteur scanne le QR code généré par le prêteur
                    val intent = Intent(context, ScannerActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
            )
        }
    }
}

@Composable
fun BookItem(
    book: Book,
    borrowerName: String?,
    lenderName: String?,
    onLoanClick: () -> Unit,
    onReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Card(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(120.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(book.covers),
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (book.authors != null) {
                    Text(
                        text = book.authors,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                val (statusText, statusColor) = when {
                    book.lenderId == null && book.borrowerId == null ->
                        "Disponible" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusAvailable
                    book.borrowerId != null && book.lenderId == null ->
                        "Prêté à ${borrowerName ?: "inconnu"}" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusLoaned
                    book.lenderId != null ->
                        "Prêté par ${lenderName ?: "inconnu"}" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusBorrowed
                    else ->
                        "Non disponible" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusUnavailable
                }

                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (book.lenderId == null && book.borrowerId == null) {
                        // Livre disponible - on peut le prêter
                        Button(
                            onClick = onLoanClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Prêter")
                        }
                    } else if (book.lenderId != null) {
                        // J'ai emprunté ce livre - je peux scanner le QR code du prêteur
                        Button(
                            onClick = onReturnClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Scanner le QR code")
                        }
                    } else {
                        // J'ai prêté ce livre - je génère le QR code pour le retour
                        Button(
                            onClick = onReturnClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Générer le QR code")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookValidationDialog(
    book: Book,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Confirmer l'ajout du livre")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    Text("Chargement des informations du livre...")
                } else {
                    // Afficher l'image de couverture si disponible
                    book.covers?.let { covers ->
                        if (covers.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = covers),
                                contentDescription = "Couverture",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .size(120.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Titre
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Auteurs
                    book.authors?.let { authors ->
                        if (authors.isNotEmpty()) {
                            Text(
                                text = "Auteurs: $authors",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // ISBN
                    book.isbn.let { isbn ->
                        if (isbn.isNotEmpty()) {
                            Text(
                                text = "ISBN: $isbn",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }


                    Text(
                        text = "Voulez-vous ajouter ce livre à votre bibliothèque ?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            Button(
                onClick = onCancel,
                enabled = !isLoading
            ) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualBookEntryDialog(
    onConfirm: (title: String, authors: String, isbn: String, covers: String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var authorsList by remember { mutableStateOf(listOf<String>()) }
    var authorInput by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var covers by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Ajouter un livre manuellement") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Champ Titre
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = title.isBlank()
                )

                // Champ Auteurs avec système de tags
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Auteur(s)",
                        style = MaterialTheme.typography.labelMedium
                    )

                    // Affichage des auteurs comme chips/tags
                    if (authorsList.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            authorsList.forEach { author ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(author) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Supprimer",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable {
                                                    authorsList = authorsList.filter { it != author }
                                                }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Champ pour ajouter un auteur
                    OutlinedTextField(
                        value = authorInput,
                        onValueChange = { authorInput = it },
                        label = { Text("Ajouter un auteur") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (authorInput.isNotBlank()) {
                                    authorsList = authorsList + authorInput
                                    authorInput = ""
                                }
                            }
                        ),
                        trailingIcon = {
                            if (authorInput.isNotBlank()) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Ajouter",
                                    modifier = Modifier
                                        .clickable {
                                            authorsList = authorsList + authorInput
                                            authorInput = ""
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    )
                }

                // Champ ISBN (obligatoire)
                OutlinedTextField(
                    value = isbn,
                    onValueChange = { isbn = it },
                    label = { Text("ISBN *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isbn.isBlank()
                )

                // Champ URL couverture
                OutlinedTextField(
                    value = covers,
                    onValueChange = { covers = it },
                    label = { Text("URL couverture (optionnel)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && isbn.isNotBlank()) {
                        val authorsString = authorsList.joinToString(", ")
                        onConfirm(title, authorsString, isbn, covers)
                    }
                },
                enabled = title.isNotBlank() && isbn.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun AnimatedFAB(
    isExpanded: Boolean,
    onMainClick: () -> Unit,
    onScanClick: () -> Unit,
    onManualClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 16.dp, bottom = 16.dp)
    ) {
        // Bouton Scanner (apparaît plus haut)
        AnimatedVisibility(
            visible = isExpanded,
            enter = scaleIn(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = scaleOut(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 130.dp)
        ) {
            FloatingActionButton(
                onClick = onScanClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.QrCode, contentDescription = "Scanner un livre", modifier = Modifier.size(24.dp))
            }
        }

        // Bouton Saisir (apparaît en bas mais au-dessus du principal)
        AnimatedVisibility(
            visible = isExpanded,
            enter = scaleIn(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = scaleOut(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 70.dp)
        ) {
            FloatingActionButton(
                onClick = onManualClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Saisir un livre", modifier = Modifier.size(24.dp))
            }
        }

        // FAB principal qui se transforme en croix
        FloatingActionButton(
            onClick = onMainClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(56.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (isExpanded) "Fermer" else "Ajouter un livre",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

