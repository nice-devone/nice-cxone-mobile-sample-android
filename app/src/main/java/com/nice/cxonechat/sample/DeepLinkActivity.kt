package com.nice.cxonechat.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar

class DeepLinkActivity : AppCompatActivity() {
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_link)

        toolbar = findViewById(R.id.my_toolbar)

        toolbar?.setNavigationOnClickListener {
            this.finish()
        }
    }
}