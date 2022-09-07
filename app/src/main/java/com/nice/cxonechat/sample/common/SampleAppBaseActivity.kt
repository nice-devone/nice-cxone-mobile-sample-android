package com.nice.cxonechat.sample.common

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

open class SampleAppBaseActivity: AppCompatActivity() {
    fun initActivityToolbarWithoutBack(
        toolbar: Toolbar?,
        title: String? = ""
    ) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = title
        toolbar?.navigationIcon = null
    }
}