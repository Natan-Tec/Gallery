package com.natantec.levegaleria

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache

/** Configura o Coil de forma econômica (pouca RAM, miniaturas de vídeo). */
class LeveApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
            .allowRgb565(true)
            .crossfade(120)
            .build()
}
