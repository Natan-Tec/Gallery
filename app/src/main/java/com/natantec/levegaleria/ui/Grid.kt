@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.natantec.levegaleria.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.natantec.levegaleria.Media
import com.natantec.levegaleria.dayKey
import com.natantec.levegaleria.formatDuration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface GridEntry {
    data class Header(val title: String) : GridEntry
    data class Item(val media: Media, val index: Int) : GridEntry
}

private fun dayLabel(ms: Long, today: Long): String {
    val diff = today - dayKey(ms)
    if (diff == 0L) return "Hoje"
    if (diff == 1L) return "Ontem"
    val cal = Calendar.getInstance()
    val thisYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = ms
    val sameYear = cal.get(Calendar.YEAR) == thisYear
    val pattern = if (sameYear) "EEEE, d 'de' MMMM" else "d 'de' MMMM 'de' yyyy"
    val text = SimpleDateFormat(pattern, Locale("pt", "BR")).format(Date(ms))
    return text.replaceFirstChar { it.uppercase() }
}

fun buildEntries(list: List<Media>, grouped: Boolean): List<GridEntry> {
    if (!grouped) return list.mapIndexed { i, m -> GridEntry.Item(m, i) }
    val out = ArrayList<GridEntry>(list.size + 64)
    val today = dayKey(System.currentTimeMillis())
    var lastDay = Long.MIN_VALUE
    list.forEachIndexed { i, m ->
        val d = dayKey(m.dateMs)
        if (d != lastDay) {
            out.add(GridEntry.Header(dayLabel(m.dateMs, today)))
            lastDay = d
        }
        out.add(GridEntry.Item(m, i))
    }
    return out
}

@Composable
fun MediaGrid(
    items: List<Media>,
    columns: Int,
    grouped: Boolean,
    selection: Set<String>,
    favorites: Set<String>,
    onClick: (Int, Media) -> Unit,
    onLongClick: (Media) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    header: (@Composable () -> Unit)? = null,
) {
    val entries = remember(items, grouped) { buildEntries(items, grouped) }
    val selecting = selection.isNotEmpty()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (header != null) {
            item(key = "header-slot", span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        items(
            items = entries,
            key = { e ->
                when (e) {
                    is GridEntry.Header -> "h-" + e.title
                    is GridEntry.Item -> e.media.key
                }
            },
            span = { e ->
                if (e is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
            },
            contentType = { e -> if (e is GridEntry.Header) 0 else 1 },
        ) { e ->
            when (e) {
                is GridEntry.Header -> Text(
                    text = e.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
                )

                is GridEntry.Item -> MediaCell(
                    m = e.media,
                    selected = e.media.key in selection,
                    selecting = selecting,
                    fav = e.media.key in favorites,
                    onClick = { onClick(e.index, e.media) },
                    onLongClick = { onLongClick(e.media) },
                )
            }
        }
    }
}

@Composable
private fun MediaCell(
    m: Media,
    selected: Boolean,
    selecting: Boolean,
    fav: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pad by animateDpAsState(if (selected) 10.dp else 0.dp, label = "selectPad")
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = m.uri,
            contentDescription = m.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .clip(RoundedCornerShape(if (selected) 14.dp else 0.dp)),
        )

        if (m.isVideo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = formatDuration(m.durationMs),
                    color = Color.White,
                    fontSize = 11.sp,
                )
            }
        }

        if (fav && !selecting) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = "Favorito",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp),
            )
        }

        if (selecting) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.35f)
                    )
                    .border(1.5.dp, Color.White, CircleShape),
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = { Text("$count selecionado(s)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Cancelar seleção") }
        },
        actions = {
            IconButton(onClick = onSelectAll) { Icon(Icons.Rounded.SelectAll, contentDescription = "Selecionar tudo") }
            IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, contentDescription = "Compartilhar") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Excluir") }
        },
    )
}

@Composable
fun BackIcon() {
    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
}
