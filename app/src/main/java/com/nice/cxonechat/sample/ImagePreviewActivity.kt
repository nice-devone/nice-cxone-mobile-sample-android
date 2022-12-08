package com.nice.cxonechat.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.navArgs
import com.bumptech.glide.Glide
import com.nice.cxonechat.sample.databinding.ActivityImagePreviewBinding

class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImagePreviewBinding
    private val imageUrl: String by lazy { navArgs<ImagePreviewActivityArgs>().value.imageUrl }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            this.finish()
        }
        Glide.with(this).load(imageUrl).into(binding.photoView)
    }
}
