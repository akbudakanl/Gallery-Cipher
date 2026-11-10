package org.fossify.gallery.svg

import android.content.Context
import android.graphics.drawable.PictureDrawable

import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import org.fossify.gallery.helpers.EncryptedFileModelLoader
import org.fossify.gallery.helpers.EncryptedStringModelLoader
import java.io.File
import java.io.InputStream

@GlideModule
class SvgModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, DisabledGlideNetworkLoader.Factory())
        registry.prepend(String::class.java, InputStream::class.java, EncryptedStringModelLoader.Factory())
        registry.prepend(File::class.java, InputStream::class.java, EncryptedFileModelLoader.Factory())
        registry.register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder()).append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }

    override fun isManifestParsingEnabled() = false

    private class DisabledGlideNetworkLoader : ModelLoader<GlideUrl, InputStream> {
        override fun handles(model: GlideUrl): Boolean = true

        override fun buildLoadData(
            model: GlideUrl,
            width: Int,
            height: Int,
            options: Options
        ): ModelLoader.LoadData<InputStream> {
            return ModelLoader.LoadData(model, object : DataFetcher<InputStream> {
                override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
                    callback.onLoadFailed(SecurityException("Network access is permanently disabled in Fossify Gallery Cipher"))
                }

                override fun cleanup() {}
                override fun cancel() {}
                override fun getDataClass(): Class<InputStream> = InputStream::class.java
                override fun getDataSource(): DataSource = DataSource.LOCAL
            })
        }

        class Factory : ModelLoaderFactory<GlideUrl, InputStream> {
            override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> =
                DisabledGlideNetworkLoader()

            override fun teardown() {}
        }
    }
}
