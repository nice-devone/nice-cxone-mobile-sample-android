package com.nice.cxonechat.sample.custom.holders.plugin

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import com.bumptech.glide.Glide
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.custom.holders.plugin.ButtonExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.SubtitleExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TextExt.bindToParent
import com.nice.cxonechat.sample.custom.holders.plugin.TitleExt.bindToParent
import com.nice.cxonechat.sample.model.PluginModel.Menu
import com.nice.cxonechat.sample.util.dpToPixels
import com.ouattararomuald.slider.ImageLoader
import com.ouattararomuald.slider.ImageLoader.Factory
import com.ouattararomuald.slider.ImageSlider
import com.ouattararomuald.slider.SliderAdapter

/**
 * Class which performs mapping of [Menu] to views and binds them to the parent view.
 */
internal object MenuExt {

    internal fun Menu.bindToParent(parent: ViewGroup): Unit = with(parent) {
        titles.firstOrNull()?.bindToParent(this)
        subtitles.firstOrNull()?.bindToParent(this)
        val images = files.associate {
            it.name to it.url
        }
        val imageSlider = ImageSlider(context)
        imageSlider.layoutParams = ViewGroup.LayoutParams(
            LayoutParams.MATCH_PARENT,
            context.dpToPixels(100f).toInt()
        )
        imageSlider.adapter = SliderAdapter(
            context = context,
            imageLoaderFactory = GlideImageLoaderFactory,
            imageUrls = images.values.toList(),
            descriptions = images.keys.toList(),
            sliderId = "slider"
        )
        addView(imageSlider)
        texts.forEach { it.bindToParent(this) }
        buttons.forEach { it.bindToParent(this) }
    }

    private object GlideImageLoaderFactory : Factory<ImageLoader> {
        override fun create(): ImageLoader = GlideImageLoader
    }

    private object GlideImageLoader : ImageLoader() {
        override fun load(path: String, imageView: ImageView) {
            Glide.with(imageView)
                .load(path)
                .placeholder(R.drawable.downloading_48px)
                .fallback(R.drawable.document_48px)
                .error(R.drawable.error_48px)
                .into(imageView)
        }
    }
}
