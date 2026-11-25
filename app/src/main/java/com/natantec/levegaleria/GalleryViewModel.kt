package com.natantec.levegaleria

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var loadJob: Job? = null
    private val reloadRunnable = Runnable { reload() }
    private val observer = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            mainHandler.removeCallbacks(reloadRunnable)
            mainHandler.postDelayed(reloadRunnable, 600)
        }
    }

    var all by mutableStateOf<List<Media>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var hidden by mutableStateOf(prefs.hidden)
        private set
    var favorites by mutableStateOf(prefs.favorites)
        private set
    var theme by mutableIntStateOf(prefs.theme)
        private set
    var columns by mutableIntStateOf(prefs.columns)
        private set
    var filter by mutableStateOf(Filter.ALL)
        private set
    var selection by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Álbum aberto na aba Álbuns (null = lista de álbuns). */
    var openAlbum by mutableStateOf<Long?>(null)

    /** Tudo que não está em pasta oculta. */
    val visible: List<Media> by derivedStateOf {
        if (hidden.isEmpty()) all else all.filter { it.bucketId !in hidden }
    }

    /** Lista da aba Fotos, já com o filtro aplicado. */
    val photos: List<Media> by derivedStateOf {
        when (filter) {
            Filter.ALL -> visible
            Filter.PHOTOS -> visible.filter { !it.isVideo }
            Filter.VIDEOS -> visible.filter { it.isVideo }
            Filter.FAVORITES -> visible.filter { it.key in favorites }
        }
    }

    /** Pastas visíveis (aba Álbuns). */
    val folders: List<Folder> by derivedStateOf {
        buildFolders(visible).sortedByDescending { it.cover.dateMs }
    }

    /** Todas as pastas, inclusive as ocultas (tela de Ajustes). */
    val allFolders: List<Folder> by derivedStateOf {
        buildFolders(all).sortedBy { it.name.lowercase() }
    }

    val albumItems: List<Media> by derivedStateOf {
        val id = openAlbum
        if (id == null) emptyList() else visible.filter { it.bucketId == id }
    }

    val albumName: String
        get() = folders.firstOrNull { it.bucketId == openAlbum }?.name ?: ""

    init {
        val cr = app.contentResolver
        cr.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        cr.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        mainHandler.removeCallbacks(reloadRunnable)
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val ctx = getApplication<Application>()
            val list = withContext(Dispatchers.IO) {
                runCatching { MediaRepo.load(ctx) }.getOrDefault(emptyList())
            }
            all = list
            loading = false
            if (selection.isNotEmpty()) {
                val keys = list.mapTo(HashSet()) { it.key }
                selection = selection.filterTo(HashSet()) { it in keys }
            }
        }
    }

    private fun buildFolders(list: List<Media>): List<Folder> =
        list.groupBy { it.bucketId }.map { (id, items) ->
            val first = items[0]
            Folder(
                bucketId = id,
                name = first.bucketName.ifBlank { "Sem nome" },
                count = items.size,
                cover = first,
                path = first.path.substringBeforeLast('/', ""),
            )
        }

    fun changeFilter(f: Filter) { filter = f }

    fun changeTheme(v: Int) {
        theme = v
        prefs.theme = v
    }

    fun changeColumns(v: Int) {
        val n = v.coerceIn(2, 6)
        columns = n
        prefs.columns = n
    }

    fun toggleHidden(bucketId: Long) {
        val next = if (bucketId in hidden) hidden - bucketId else hidden + bucketId
        hidden = next
        prefs.hidden = next
        if (openAlbum == bucketId) openAlbum = null
        selection = emptySet()
    }

    fun toggleFavorite(key: String) {
        val next = if (key in favorites) favorites - key else favorites + key
        favorites = next
        prefs.favorites = next
    }

    fun toggleSelect(key: String) {
        selection = if (key in selection) selection - key else selection + key
    }

    fun clearSelection() { selection = emptySet() }

    fun selectAll(list: List<Media>) {
        selection = list.mapTo(HashSet()) { it.key }
    }
}
