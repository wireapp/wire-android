package com.wire.android.shared.asset.ui.imageloader

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.wire.android.shared.asset.ui.imageloader.publicasset.PublicAssetLoaderFactory
import org.koin.core.KoinComponent
import org.koin.core.inject

@GlideModule
class WireGlideModule : AppGlideModule(), KoinComponent {

    private val publicAssetLoaderFactory by inject<PublicAssetLoaderFactory>()

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(publicAssetLoaderFactory)
    }

    private inline fun <reified T, reified U> Registry.prepend(factory: ModelLoaderFactory<T, U>) {
        this.prepend(T::class.java, U::class.java, factory)
    }
}
