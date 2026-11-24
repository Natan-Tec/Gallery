@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.natantec.levegaleria.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.natantec.levegaleria.Filter
import com.natantec.levegaleria.Folder
import com.natantec.levegaleria.GalleryViewModel
import com.natantec.levegaleria.Media

// ---------------------------------------------------------------- Fotos

@Composable
fun PhotosScreen(
    vm: GalleryViewModel,
    pad: PaddingValues,
    onOpen: (List<Media>, Int) -> Unit,
) {
    val list = vm.photos
    MediaGrid(
        items = list,
        columns = vm.columns,
        grouped = true,
        selection = vm.selection,
        favorites = vm.favorites,
        onClick = { index, m ->
            if (vm.selection.isNotEmpty()) vm.toggleSelect(m.key) else onOpen(list, index)
        },
        onLongClick = { m -> vm.toggleSelect(m.key) },
        contentPadding = PaddingValues(
            top = pad.calculateTopPadding(),
            bottom = pad.calculateBottomPadding() + 8.dp,
        ),
        header = { PhotosHeader(vm, list.size) },
    )
}

@Composable
private fun PhotosHeader(vm: GalleryViewModel, count: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Text(
            text = "Fotos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = if (vm.loading) "Carregando…" else "$count itens",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            Filter.values().forEach { f ->
                FilterChip(
                    selected = vm.filter == f,
                    onClick = { vm.setFilter(f) },
                    label = { Text(f.label) },
                )
            }
        }
        if (vm.loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (count == 0) {
            Text(
                text = if (vm.filter == Filter.FAVORITES)
                    "Nenhum favorito ainda. Abra uma foto e toque no coração."
                else
                    "Nada para mostrar aqui. Confira as pastas ocultas em Ajustes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 32.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Álbuns

@Composable
fun AlbumsScreen(vm: GalleryViewModel, pad: PaddingValues) {
    val folders = vm.folders
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = pad.calculateTopPadding() + 12.dp,
            bottom = pad.calculateBottomPadding() + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    text = "Álbuns",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${folders.size} pastas · segure uma pasta para ocultar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(folders, key = { it.bucketId }) { f -> FolderCard(vm, f) }
    }
}

@Composable
private fun FolderCard(vm: GalleryViewModel, f: Folder) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.combinedClickable(
            onClick = { vm.openAlbum = f.bucketId },
            onLongClick = { menu = true },
        )
    ) {
        Box {
            AsyncImage(
                model = f.cover.uri,
                contentDescription = f.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Ocultar pasta") },
                    leadingIcon = { Icon(Icons.Rounded.VisibilityOff, contentDescription = null) },
                    onClick = {
                        menu = false
                        vm.toggleHidden(f.bucketId)
                        Toast.makeText(ctx, "Pasta ocultada. Reative em Ajustes.", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
        Text(
            text = f.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
        Text(
            text = "${f.count} itens",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun AlbumScreen(
    vm: GalleryViewModel,
    pad: PaddingValues,
    onOpen: (List<Media>, Int) -> Unit,
) {
    val list = vm.albumItems
    MediaGrid(
        items = list,
        columns = vm.columns,
        grouped = true,
        selection = vm.selection,
        favorites = vm.favorites,
        onClick = { index, m ->
            if (vm.selection.isNotEmpty()) vm.toggleSelect(m.key) else onOpen(list, index)
        },
        onLongClick = { m -> vm.toggleSelect(m.key) },
        contentPadding = PaddingValues(
            top = pad.calculateTopPadding(),
            bottom = pad.calculateBottomPadding() + 8.dp,
        ),
    )
}

@Composable
fun AlbumBar(vm: GalleryViewModel) {
    val ctx = LocalContext.current
    TopAppBar(
        title = {
            Column {
                Text(vm.albumName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${vm.albumItems.size} itens",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { vm.openAlbum = null }) { BackIcon() }
        },
        actions = {
            IconButton(onClick = {
                val id = vm.openAlbum
                if (id != null) {
                    vm.toggleHidden(id)
                    Toast.makeText(ctx, "Pasta ocultada. Reative em Ajustes.", Toast.LENGTH_SHORT).show()
                }
            }) { Icon(Icons.Rounded.VisibilityOff, contentDescription = "Ocultar pasta") }
        },
    )
}
