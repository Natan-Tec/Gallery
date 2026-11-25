@file:OptIn(ExperimentalMaterial3Api::class)

package com.natantec.levegaleria.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.natantec.levegaleria.GalleryViewModel
import kotlin.math.roundToInt

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsScreen(vm: GalleryViewModel, pad: PaddingValues) {
    val folders = vm.allFolders
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = pad.calculateTopPadding() + 12.dp,
            bottom = pad.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item { SectionTitle("Aparência") }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                listOf("Sistema", "Claro", "Escuro").forEachIndexed { i, label ->
                    FilterChip(
                        selected = vm.theme == i,
                        onClick = { vm.changeTheme(i) },
                        label = { Text(label) },
                    )
                }
            }
        }

        item { SectionTitle("Colunas da grade: ${vm.columns}") }
        item {
            Slider(
                value = vm.columns.toFloat(),
                onValueChange = { vm.changeColumns(it.roundToInt()) },
                valueRange = 2f..6f,
                steps = 3,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item { SectionTitle("Pastas") }
        item {
            Text(
                text = "Desligue uma pasta para esconder todos os arquivos dela da galeria. " +
                    "Os arquivos continuam no aparelho; só deixam de aparecer aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        items(folders, key = { it.bucketId }) { f ->
            val shown = f.bucketId !in vm.hidden
            ListItem(
                headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text(
                        text = "${f.count} itens · ${f.path}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = if (shown) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                },
                trailingContent = {
                    Switch(checked = shown, onCheckedChange = { vm.toggleHidden(f.bucketId) })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.toggleHidden(f.bucketId) },
            )
        }

        item { SectionTitle("Sobre") }
        item {
            Text(
                text = "Leve Galeria 1.0 · lê suas fotos e vídeos direto do aparelho, " +
                    "sem internet, sem conta e sem anúncios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
