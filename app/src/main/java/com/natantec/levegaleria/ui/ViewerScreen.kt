@file:OptIn(ExperimentalFoundationApi::class)

package com.natantec.levegaleria.ui

import android.app.Activity
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import com.natantec.levegaleria.LocalIsDark
import com.natantec.levegaleria.Media
import com.natantec.levegaleria.formatDuration
import com.natantec.levegaleria.formatFullDate
import com.natantec.levegaleria.formatSize

@Composable
fun ViewerScreen(
    items: List<Media>,
    startIndex: Int,
    favorites: Set<String>,
    onClose: () -> Unit,
    onFavorite: (Media) -> Unit,
    onShare: (Media) -> Unit,
    onDelete: (Media) -> Unit,
    onPlay: (Media) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) { items.size }
    var bars by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }

    val dark = LocalIsDark.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    BackHandler(onBack = onClose)
    LaunchedEffect(pagerState.currentPage) { zoomed = false }

    val current = items.getOrNull(pagerState.currentPage)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !zoomed,
            key = { items[it].key },
        ) { page ->
            val m = items[page]
            if (m.isVideo) {
                VideoPage(
                    m = m,
                    active = page == pagerState.currentPage,
                    bars = bars,
                    onTap = { bars = !bars },
                    onFallback = { onPlay(m) },
                )
            } else {
                ZoomImage(
                    m = m,
                    onTap = { bars = !bars },
                    onZoomChange = { if (page == pagerState.currentPage) zoomed = it },
                )
            }
        }

        if (current != null) {
            AnimatedVisibility(
                visible = bars,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = current.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatFullDate(current.dateMs),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = bars,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
                ) {
                    val fav = current.key in favorites
                    IconButton(onClick = { onShare(current) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Compartilhar", tint = Color.White)
                    }
                    IconButton(onClick = { onFavorite(current) }) {
                        Icon(
                            if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { info = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "Detalhes", tint = Color.White)
                    }
                    IconButton(onClick = { onDelete(current) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = Color.White)
                    }
                }
            }
        }
    }

    if (info && current != null) {
        AlertDialog(
            onDismissRequest = { info = false },
            confirmButton = { TextButton(onClick = { info = false }) { Text("Fechar") } },
            title = { Text("Detalhes") },
            text = {
                Column {
                    Text(current.name, style = MaterialTheme.typography.titleSmall)
                    Text(formatFullDate(current.dateMs))
                    if (current.width > 0 && current.height > 0) {
                        Text("${current.width} × ${current.height}")
                    }
                    Text(formatSize(current.size))
                    if (current.isVideo) Text("Duração: ${formatDuration(current.durationMs)}")
                    if (current.path.isNotBlank()) Text(current.path, style = MaterialTheme.typography.bodySmall)
                }
            },
        )
    }
}

@Composable
private fun VideoPage(
    m: Media,
    active: Boolean,
    bars: Boolean,
    onTap: () -> Unit,
    onFallback: () -> Unit,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(active) { if (!active) started = false }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (started && active) {
            VideoPlayer(
                m = m,
                bars = bars,
                onTap = onTap,
                onFallback = {
                    started = false
                    onFallback()
                },
            )
        } else {
            AsyncImage(
                model = m.uri,
                contentDescription = m.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTap,
                    ),
            )
            IconButton(
                onClick = { started = true },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}

/** Player embutido: VideoView nativo do Android (sem bibliotecas extras) + controles próprios. */
@Composable
private fun VideoPlayer(
    m: Media,
    bars: Boolean,
    onTap: () -> Unit,
    onFallback: () -> Unit,
) {
    val ctx = LocalContext.current
    var playing by remember { mutableStateOf(true) }
    var pos by remember { mutableIntStateOf(0) }
    var dur by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val video = remember(m.uri) {
        VideoView(ctx).apply {
            keepScreenOn = true
            setOnPreparedListener { mp ->
                dur = mp.duration
                start()
            }
            setOnCompletionListener {
                playing = false
                pos = dur
            }
            setOnErrorListener { _, _, _ ->
                onFallback()
                true
            }
            setVideoURI(m.uri)
        }
    }
    val container = remember(video) {
        FrameLayout(ctx).apply {
            addView(
                video,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                ),
            )
        }
    }

    DisposableEffect(video) { onDispose { video.stopPlayback() } }

    LaunchedEffect(playing) {
        while (playing) {
            if (!dragging) pos = video.currentPosition
            delay(250)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { container }, modifier = Modifier.fillMaxSize())

        // Camada transparente: toque mostra/esconde os controles.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                ),
        )

        AnimatedVisibility(
            visible = bars,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val shown = if (dragging) dragValue.toInt() else pos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 68.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(end = 14.dp),
            ) {
                IconButton(onClick = {
                    if (playing) {
                        video.pause()
                        playing = false
                    } else {
                        if (dur > 0 && video.currentPosition >= dur - 200) video.seekTo(0)
                        video.start()
                        playing = true
                    }
                }) {
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pausar" else "Reproduzir",
                        tint = Color.White,
                    )
                }
                Text(formatDuration(shown.toLong()), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = shown.toFloat().coerceIn(0f, dur.coerceAtLeast(1).toFloat()),
                    onValueChange = {
                        dragging = true
                        dragValue = it
                    },
                    onValueChangeFinished = {
                        video.seekTo(dragValue.toInt())
                        pos = dragValue.toInt()
                        dragging = false
                    },
                    valueRange = 0f..dur.coerceAtLeast(1).toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(formatDuration(dur.toLong()), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ZoomImage(m: Media, onTap: () -> Unit, onZoomChange: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val request = remember(m.uri) { ImageRequest.Builder(ctx).data(m.uri).size(2048).build() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                        onZoomChange(scale > 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        // Só captura o gesto com 2 dedos ou com a imagem já ampliada;
                        // com 1 dedo e zoom 1x deixa o pager trocar de foto.
                        if (pressed >= 2 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 6f)
                            val maxX = box.width * (newScale - 1f) / 2f
                            val maxY = box.height * (newScale - 1f) / 2f
                            val newOffset = if (newScale <= 1f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                            }
                            if (newScale != scale || newOffset != offset) {
                                scale = newScale
                                offset = newOffset
                                onZoomChange(newScale > 1f)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        AsyncImage(
            model = request,
            contentDescription = m.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
