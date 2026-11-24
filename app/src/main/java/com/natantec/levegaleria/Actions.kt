package com.natantec.levegaleria

import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun shareMedia(ctx: Context, items: List<Media>) {
    if (items.isEmpty()) return
    val intent = if (items.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = items[0].mime
            putExtra(Intent.EXTRA_STREAM, items[0].uri)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { it.uri }))
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        ctx.startActivity(Intent.createChooser(intent, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "Nenhum app para compartilhar", Toast.LENGTH_SHORT).show()
    }
}

fun openVideo(ctx: Context, m: Media) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(m.uri, m.mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        ctx.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, "Nenhum player de vídeo instalado", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Exclui arquivos do aparelho.
 * Android 11+: pede confirmação pelo sistema (createDeleteRequest).
 * Android 10: pode pedir permissão por arquivo (RecoverableSecurityException).
 * Android 8/9: apaga direto.
 * [onDone] roda quando a exclusão terminou sem precisar de diálogo do sistema.
 */
fun requestDelete(
    ctx: Context,
    items: List<Media>,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    onDone: () -> Unit,
) {
    if (items.isEmpty()) return
    val resolver = ctx.contentResolver
    val uris = items.map { it.uri }
    try {
        if (Build.VERSION.SDK_INT >= 30) {
            val pending = MediaStore.createDeleteRequest(resolver, uris)
            launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            uris.forEach { resolver.delete(it, null, null) }
            onDone()
        }
    } catch (e: SecurityException) {
        if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
            launcher.launch(
                IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
            )
        } else {
            Toast.makeText(ctx, "Não foi possível excluir", Toast.LENGTH_SHORT).show()
        }
    }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    bytes >= 1L shl 20 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
    bytes >= 1L shl 10 -> String.format(Locale.getDefault(), "%d KB", bytes / 1024)
    else -> "$bytes B"
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    else String.format(Locale.getDefault(), "%d:%02d", m, s)
}

fun formatFullDate(ms: Long): String =
    SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale("pt", "BR")).format(Date(ms))

/** Dia local (para agrupar por data). */
fun dayKey(ms: Long): Long =
    (ms + TimeZone.getDefault().getOffset(ms)) / 86_400_000L
