package com.nice.cxonechat.sample

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.nice.cxonechat.sample.common.SampleAppBaseActivity
import com.nice.cxonechat.sample.databinding.ActivityConfigurationBinding
import com.nice.cxonechat.sample.storage.ChatConfigurationStorage.getConfiguration
import com.nice.cxonechat.sample.storage.EnvironmentStorage.getEnvironment
import com.nice.cxonechat.sample.storage.ValueStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConfigurationActivity : SampleAppBaseActivity() {

    private lateinit var binding: ActivityConfigurationBinding
    private lateinit var navController: NavController
    private lateinit var navGraph: NavGraph

    @Inject
    internal lateinit var valueStorage: ValueStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = ""

        finishUiSetupAsync()
    }

    private fun finishUiSetupAsync() {
        lifecycleScope.launch {
            val environment = valueStorage.getEnvironment()
            val configuration = valueStorage.getConfiguration(environment)
            if (configuration != null) {
                val intent = Intent(this@ConfigurationActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            } else {
                initializeUI()
            }
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
