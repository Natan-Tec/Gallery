@file:OptIn(ExperimentalMaterial3Api::class)

package com.natantec.levegaleria

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.natantec.levegaleria.ui.AlbumBar
import com.natantec.levegaleria.ui.AlbumScreen
import com.natantec.levegaleria.ui.AlbumsScreen
import com.natantec.levegaleria.ui.PhotosScreen
import com.natantec.levegaleria.ui.SelectionBar
import com.natantec.levegaleria.ui.SettingsScreen
import com.natantec.levegaleria.ui.ViewerScreen

data class ViewerState(val items: List<Media>, val index: Int)

private fun granted(ctx: Context, permission: String) =
    ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

private fun hasAccess(ctx: Context): Boolean = when {
    Build.VERSION.SDK_INT >= 34 ->
        granted(ctx, Manifest.permission.READ_MEDIA_IMAGES) ||
            granted(ctx, Manifest.permission.READ_MEDIA_VIDEO) ||
            granted(ctx, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    Build.VERSION.SDK_INT >= 33 ->
        granted(ctx, Manifest.permission.READ_MEDIA_IMAGES) ||
            granted(ctx, Manifest.permission.READ_MEDIA_VIDEO)

    else -> granted(ctx, Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun permissionsToAsk(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )

    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )

    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Composable
fun App(vm: GalleryViewModel) {
    val ctx = LocalContext.current
    var access by remember { mutableStateOf(hasAccess(ctx)) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        access = hasAccess(ctx)
        if (access) vm.reload()
    }

    // Ao voltar para o app (ex.: depois de liberar a permissão nas configurações) recarrega.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val ok = hasAccess(ctx)
        access = ok
        if (ok) vm.reload()
    }

    if (access) {
        Gallery(vm)
    } else {
        PermissionScreen(
            onAsk = { permLauncher.launch(permissionsToAsk()) },
            onSettings = {
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        )
    }
}

@Composable
private fun PermissionScreen(onAsk: () -> Unit, onSettings: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Text(
                "Leve Galeria",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Para mostrar suas fotos e vídeos, o app precisa de acesso à mídia do aparelho. " +
                    "Nada sai do seu celular.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Button(onClick = onAsk) { Text("Permitir acesso") }
            TextButton(onClick = onSettings) { Text("Abrir configurações do app") }
        }
    }
}

@Composable
private fun Gallery(vm: GalleryViewModel) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var viewer by remember { mutableStateOf<ViewerState?>(null) }
    var pending by remember { mutableStateOf<List<Media>>(emptyList()) }
    var confirm by remember { mutableStateOf<List<Media>?>(null) }
    val launcherHolder = remember { arrayOfNulls<ActivityResultLauncher<IntentSenderRequest>>(1) }

    fun finishDelete() {
        pending = emptyList()
        vm.clearSelection()
        viewer = null
        vm.reload()
    }

    fun runDelete(items: List<Media>) {
        val launcher = launcherHolder[0] ?: return
        pending = items
        requestDelete(ctx, items, launcher) { finishDelete() }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Android 10 libera um arquivo por vez: repete até acabar.
            if (Build.VERSION.SDK_INT == 29 && pending.isNotEmpty()) runDelete(pending)
            else finishDelete()
        }
    }
    launcherHolder[0] = deleteLauncher

    fun askDelete(items: List<Media>) {
        if (Build.VERSION.SDK_INT >= 30) runDelete(items) else confirm = items
    }

    val selecting = vm.selection.isNotEmpty()
    val inAlbum = tab == 1 && vm.openAlbum != null

    BackHandler(enabled = selecting) { vm.clearSelection() }
    BackHandler(enabled = !selecting && inAlbum) { vm.openAlbum = null }
    BackHandler(enabled = !selecting && !inAlbum && tab != 0) { tab = 0 }

    val currentList = if (tab == 1) vm.albumItems else vm.photos
    fun selectedMedia(): List<Media> = vm.visible.filter { it.key in vm.selection }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                when {
                    selecting -> SelectionBar(
                        count = vm.selection.size,
                        onClose = { vm.clearSelection() },
                        onSelectAll = { vm.selectAll(currentList) },
                        onShare = { shareMedia(ctx, selectedMedia()) },
                        onDelete = { askDelete(selectedMedia()) },
                    )

                    inAlbum -> AlbumBar(vm)
                }
            },
            bottomBar = {
                if (!selecting) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                            label = { Text("Fotos") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                            label = { Text("Álbuns") },
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                            label = { Text("Ajustes") },
                        )
                    }
                }
            },
        ) { pad ->
            when (tab) {
                0 -> PhotosScreen(vm, pad) { list, index -> viewer = ViewerState(list, index) }
                1 -> if (vm.openAlbum == null) {
                    AlbumsScreen(vm, pad)
                } else {
                    AlbumScreen(vm, pad) { list, index -> viewer = ViewerState(list, index) }
                }

                else -> SettingsScreen(vm, pad)
            }
        }

        viewer?.let { v ->
            if (v.items.isNotEmpty()) {
                ViewerScreen(
                    items = v.items,
                    startIndex = v.index,
                    favorites = vm.favorites,
                    onClose = { viewer = null },
                    onFavorite = { vm.toggleFavorite(it.key) },
                    onShare = { shareMedia(ctx, listOf(it)) },
                    onDelete = { askDelete(listOf(it)) },
                    onPlay = { openVideo(ctx, it) },
                )
            }
        }
    }

    confirm?.let { items ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Excluir ${items.size} item(ns)?") },
            text = { Text("Os arquivos serão apagados do aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    runDelete(items)
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
        )
    }
}
