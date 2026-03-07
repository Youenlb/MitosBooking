package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MyLibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val currentUserId: String = sharedPreferences.getString("user_id", null)
        ?: UUID.randomUUID().toString().also { sharedPreferences.edit().putString("user_id", it).apply() }

    private val _toastMessages = MutableSharedFlow<String>()
    val toastMessages = _toastMessages.asSharedFlow()

    // État du livre en attente de validation
    private val _pendingBook = MutableStateFlow<Book?>(null)
    val pendingBook: StateFlow<Book?> = _pendingBook

    private val _isLoadingBook = MutableStateFlow(false)
    val isLoadingBook: StateFlow<Boolean> = _isLoadingBook


    private val _errorMessage = MutableStateFlow<String?>(null)

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Livres que je possède (propriétaire ou lenderId == null)
    val myOwnedBooks = books.map { allBooks ->
        allBooks.filter {
            (it.lenderId == null || it.lenderId == currentUserId) && it.borrowerId == null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Livres que j'ai prêtés à quelqu'un (je suis le lenderId et il y a un borrowerId)
    val myLoans = books.map { allBooks ->
        allBooks.filter {
            (it.lenderId == null || it.lenderId == currentUserId) && it.borrowerId != null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Livres que j'ai empruntés (je suis le borrowerId)
    val myBorrows = books.map { allBooks ->
        allBooks.filter {
            it.borrowerId == currentUserId
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Ensure a user exists on first launch
            if (bookRepository.getAllBooks().first().isEmpty()) {
                prepopulateDatabase()
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        val booksToInsert = listOf(
            // Fantasy
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0321721349", title = "The Lord of the Rings", authors = "J.R.R. Tolkien", covers = "https://covers.openlibrary.org/b/id/8264423-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0439064873", title = "Harry Potter and the Chamber of Secrets", authors = "J.K. Rowling", covers = "https://covers.openlibrary.org/b/id/8773826-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0261102385", title = "The Hobbit", authors = "J.R.R. Tolkien", covers = "https://covers.openlibrary.org/b/id/8739161-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0439708180", title = "Harry Potter and the Sorcerer's Stone", authors = "J.K. Rowling", covers = "https://covers.openlibrary.org/b/id/10521270-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0439136365", title = "Harry Potter and the Prisoner of Azkaban", authors = "J.K. Rowling", covers = "https://covers.openlibrary.org/b/id/10521273-L.jpg", borrowerId = null, lenderId = null),

            // Science-Fiction
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0743273565", title = "The Hitchhiker's Guide to the Galaxy", authors = "Douglas Adams", covers = "https://covers.openlibrary.org/b/id/9144214-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0441172719", title = "Dune", authors = "Frank Herbert", covers = "https://covers.openlibrary.org/b/id/8234077-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0345391803", title = "The Hitchhiker's Guide to the Galaxy: The Original Radio Scripts", authors = "Douglas Adams", covers = "https://covers.openlibrary.org/b/id/240726-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0553293357", title = "Foundation", authors = "Isaac Asimov", covers = "https://covers.openlibrary.org/b/id/8225261-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0441013593", title = "Ender's Game", authors = "Orson Scott Card", covers = "https://covers.openlibrary.org/b/id/8739720-L.jpg", borrowerId = null, lenderId = null),

            // Classiques
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0141439518", title = "Pride and Prejudice", authors = "Jane Austen", covers = "https://covers.openlibrary.org/b/id/8235657-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0743273565", title = "The Great Gatsby", authors = "F. Scott Fitzgerald", covers = "https://covers.openlibrary.org/b/id/7222246-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0141182605", title = "1984", authors = "George Orwell", covers = "https://covers.openlibrary.org/b/id/8235640-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0060935467", title = "To Kill a Mockingbird", authors = "Harper Lee", covers = "https://covers.openlibrary.org/b/id/8227297-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0451524935", title = "Animal Farm", authors = "George Orwell", covers = "https://covers.openlibrary.org/b/id/8234166-L.jpg", borrowerId = null, lenderId = null),

            // Romans modernes
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0316769174", title = "The Catcher in the Rye", authors = "J.D. Salinger", covers = "https://covers.openlibrary.org/b/id/8235064-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0385720953", title = "The Da Vinci Code", authors = "Dan Brown", covers = "https://covers.openlibrary.org/b/id/8231062-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0316769488", title = "The Lovely Bones", authors = "Alice Sebold", covers = "https://covers.openlibrary.org/b/id/8227080-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0439023481", title = "The Hunger Games", authors = "Suzanne Collins", covers = "https://covers.openlibrary.org/b/id/7577795-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0316015844", title = "Twilight", authors = "Stephenie Meyer", covers = "https://covers.openlibrary.org/b/id/8227378-L.jpg", borrowerId = null, lenderId = null),

            // Thrillers et Mystères
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0307588371", title = "Gone Girl", authors = "Gillian Flynn", covers = "https://covers.openlibrary.org/b/id/7624357-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0307274939", title = "The Girl with the Dragon Tattoo", authors = "Stieg Larsson", covers = "https://covers.openlibrary.org/b/id/6995008-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0345803481", title = "Fifty Shades of Grey", authors = "E.L. James", covers = "https://covers.openlibrary.org/b/id/7615679-L.jpg", borrowerId = null, lenderId = null),

            // Développement personnel et non-fiction
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0307887894", title = "The Lean Startup", authors = "Eric Ries", covers = "https://covers.openlibrary.org/b/id/7572872-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-1594631931", title = "Thinking, Fast and Slow", authors = "Daniel Kahneman", covers = "https://covers.openlibrary.org/b/id/7625766-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0062316097", title = "Sapiens: A Brief History of Humankind", authors = "Yuval Noah Harari", covers = "https://covers.openlibrary.org/b/id/8378547-L.jpg", borrowerId = null, lenderId = null)
        )
        booksToInsert.forEach { bookRepository.insertBook(it) }
    }

    fun onIsbnScanned(isbn: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoadingBook.value = true
            try {
                val book = bookRepository.getBookDetailsFromApi(isbn)
                if (book != null) {
                    // Afficher le livre en attente de validation
                    _pendingBook.value = book.copy(lenderId = null)
                    _toastMessages.emit("Veuillez confirmer l'ajout du livre")
                } else {
                    val errorMsg = "Livre non trouvé. Veuillez vérifier l'ISBN."
                    _errorMessage.value = errorMsg
                    _toastMessages.emit(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Erreur lors de la récupération du livre : ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                _toastMessages.emit(errorMsg)
            } finally {
                _isLoadingBook.value = false
            }
        }
    }

    fun confirmAddBook() {
        val book = _pendingBook.value
        if (book != null) {
            viewModelScope.launch {
                try {
                    bookRepository.insertBook(book)
                    _toastMessages.emit("Livre ajouté à votre bibliothèque !")
                    _pendingBook.value = null
                } catch (e: Exception) {
                    val errorMsg = "Erreur lors de l'ajout du livre : ${e.localizedMessage}"
                    _errorMessage.value = errorMsg
                    _toastMessages.emit(errorMsg)
                }
            }
        }
    }

    fun cancelAddBook() {
        _pendingBook.value = null
        _errorMessage.value = null
    }

    fun addBookManually(title: String, authors: String, isbn: String, covers: String) {
        viewModelScope.launch {
            try {
                // ISBN est obligatoire
                if (isbn.isBlank()) {
                    _toastMessages.emit("L'ISBN est obligatoire")
                    return@launch
                }
                if (title.isBlank()) {
                    _toastMessages.emit("Le titre est obligatoire")
                    return@launch
                }

                val book = Book(
                    uid = UUID.randomUUID().toString(),
                    isbn = isbn,
                    title = title,
                    authors = authors.ifBlank { null },
                    covers = covers.ifBlank { null },
                    borrowerId = null,
                    lenderId = null,
                )
                bookRepository.insertBook(book)
                _toastMessages.emit("Livre ajouté à votre bibliothèque !")
            } catch (e: Exception) {
                val errorMsg = "Erreur lors de l'ajout du livre : ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                _toastMessages.emit(errorMsg)
            }
        }
    }

    // Helper function to get a user by ID and handle potential null values
    suspend fun getUserFullName(userId: String?): String {
        return if (userId != null) {
            try {
                bookRepository.getUserById(userId).first().fullName
            } catch (_: Exception) {
                "Utilisateur inconnu"
            }
        } else {
            ""
        }
    }
}
