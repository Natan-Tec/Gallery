package com.natantec.levegaleria

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

/** Lê fotos e vídeos direto do MediaStore (sem copiar nada, sem banco próprio). */
object MediaRepo {

    fun load(ctx: Context): List<Media> {
        val out = ArrayList<Media>(2048)
        query(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, out)
        query(ctx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, out)
        out.sortByDescending { it.dateMs }
        return out
    }

    private fun query(ctx: Context, base: Uri, video: Boolean, out: MutableList<Media>) {
        val projection = arrayListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATA,
        )
        if (video) projection.add(MediaStore.Video.Media.DURATION)

        try {
            ctx.contentResolver.query(base, projection.toTypedArray(), null, null, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val iTaken = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val iMod = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val iMime = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val iBucket = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val iBucketName = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val iW = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val iH = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val iData = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val iDur = if (video) c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION) else -1

                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val taken = c.getLong(iTaken)
                    val date = if (taken > 0L) taken else c.getLong(iMod) * 1000L
                    val mime = c.getStringOrEmpty(iMime)
                    out.add(
                        Media(
                            key = (if (video) "v" else "i") + id,
                            id = id,
                            uri = ContentUris.withAppendedId(base, id),
                            name = c.getStringOrEmpty(iName),
                            dateMs = date,
                            isVideo = video,
                            durationMs = if (iDur >= 0) c.getLong(iDur) else 0L,
                            size = c.getLong(iSize),
                            mime = mime.ifBlank { if (video) "video/*" else "image/*" },
                            bucketId = c.getLong(iBucket),
                            bucketName = c.getStringOrEmpty(iBucketName),
                            width = c.getInt(iW),
                            height = c.getInt(iH),
                            path = c.getStringOrEmpty(iData),
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // sem permissão: lista fica vazia
        }
    }

    private fun Cursor.getStringOrEmpty(index: Int): String =
        if (index >= 0 && !isNull(index)) getString(index) ?: "" else ""
}
