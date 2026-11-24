package com.natantec.levegaleria

import android.net.Uri

data class Media(
    val key: String,
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateMs: Long,
    val isVideo: Boolean,
    val durationMs: Long,
    val size: Long,
    val mime: String,
    val bucketId: Long,
    val bucketName: String,
    val width: Int,
    val height: Int,
    val path: String,
)

data class Folder(
    val bucketId: Long,
    val name: String,
    val count: Int,
    val cover: Media,
    val path: String,
)

enum class Filter(val label: String) {
    ALL("Tudo"),
    PHOTOS("Fotos"),
    VIDEOS("Vídeos"),
    FAVORITES("Favoritos"),
}
