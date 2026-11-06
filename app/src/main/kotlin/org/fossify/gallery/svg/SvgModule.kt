package org.fossify.gallery.svg

import android.content.Context
import android.graphics.drawable.PictureDrawable

import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import org.fossify.gallery.helpers.EncryptedFileModelLoader
import org.fossify.gallery.helpers.EncryptedStringModelLoader
import java.io.File
import java.io.InputStream

@GlideModule
class SvgModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(String::class.java, InputStream::class.java, EncryptedStringModelLoader.Factory())
        registry.prepend(File::class.java, InputStream::class.java, EncryptedFileModelLoader.Factory())
        registry.register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder()).append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }

    override fun isManifestParsingEnabled() = false
}
