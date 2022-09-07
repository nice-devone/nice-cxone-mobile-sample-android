package com.nice.cxonechat.sample

import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.nice.cxonechat.sample.common.SampleAppBaseActivity
import com.nice.cxonechat.sample.databinding.ActivityConfigurationBinding
import com.nice.cxonechat.sample.service.PreferencesService

class ConfigurationActivity : SampleAppBaseActivity() {

    private lateinit var binding: ActivityConfigurationBinding
    private lateinit var navController: NavController
    private lateinit var navGraph: NavGraph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = ""
        if (PreferencesService.getConsumerId(this).isNotEmpty() &&
            PreferencesService.getEnvironment(this).isNotEmpty()) {
            val brandId = PreferencesService.getBrandId(this)
            val channelId = PreferencesService.getChannelId(this)

            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(
                    HomeConfigurationFragment.ENVIRONMENT_SELECTED,
                    PreferencesService.getEnvironment(this@ConfigurationActivity)
                )
                if (brandId.isNotEmpty()) putExtra(CustomConfigurationFragment.BRAND_ID, brandId)
                if (channelId.isNotEmpty()) putExtra(CustomConfigurationFragment.CHANNEL_ID, channelId)
            }
            startActivity(intent)
        } else {
            initializeUI()
        }
    }

    private fun initializeUI() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val graphInflater = navHostFragment.navController.navInflater
        navGraph = graphInflater.inflate(R.navigation.configuration)
        navController = navHostFragment.navController

        navController.graph = navGraph


        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        initActivityToolbarWithoutBack(binding.toolbar, getString(R.string.configuration))

        navController.addOnDestinationChangedListener { _, _, _ ->
            binding.toolbar.navigationIcon = null
        }
    }
}