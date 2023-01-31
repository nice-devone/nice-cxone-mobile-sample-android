package com.nice.cxonechat.sample.ui.config

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.sample.LoginActivity
import com.nice.cxonechat.sample.R.layout
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.databinding.FragmentHomeConfigurationBinding
import com.nice.cxonechat.sample.storage.SdkConfiguration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeConfigurationFragment : Fragment() {
    private var _binding: FragmentHomeConfigurationBinding? = null
    private val binding get() = _binding!!
    private var environmentSelected: SdkConfiguration? = null
    private lateinit var dialog: ProgressDialog

    private val viewModel: HomeConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog = ProgressDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupConfigurationAdapter()
        with(binding) {
            useCustomConfigurationTextView.setOnClickListener {
                val action = HomeConfigurationFragmentDirections.actionHomeConfigurationToCustomConfigurationFragment()
                view.findNavController().navigate(action)
            }
        }
    }

    private fun setupConfigurationAdapter() = lifecycleScope.launch {
        val configurations = viewModel.getAssetConfigurations()
            ?.takeIf { it.configurations.isNotEmpty() }
            ?: return@launch
        val configurationMap = configurations.configurations.associateBy { it.name }
        val configurationNames = configurationMap.keys.toList()
        val configurationAdapter = ArrayAdapter(
            requireContext(),
            layout.dropdown_item,
            configurationNames
        )
        val firstConfiguration = configurationNames.first()
        // Preselect first for convenience
        environmentSelected = configurationMap[firstConfiguration]
        with(binding) {
            configurationAutoCompleteTextView.setAdapter(configurationAdapter)
            configurationAutoCompleteTextView.setText(firstConfiguration, false)
            configurationAutoCompleteTextView.setOnItemClickListener { _, _, index, _ ->
                environmentSelected = configurationMap[configurationAdapter.getItem(index)]
            }

            continueButton.setOnClickListener {
                val selected = environmentSelected
                if (selected == null) {
                    showAlert(getString(string.select_valid_configuration))
                } else {
                    val configuration = selected.toSocketFactoryConfiguration()
                    storeConfigAndShowLogin(configuration)
                }
            }
        }
    }

    private fun showAlert(message: String) {
        val builder = Builder(requireContext())
        builder.setTitle(getString(string.information))
        builder.setMessage(message)

        builder.setPositiveButton(getString(string.ok)) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        requireContext().startActivity(intent)
    }

    private fun showDialog() {
        dialog.setMessage("Loading, please wait.")
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun hideDialog() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun storeConfigAndShowLogin(configuration: SocketFactoryConfiguration) {
        lifecycleScope.launch {
            showDialog()
            viewModel.setConfiguration(configuration)
            hideDialog()
            showLogin()
        }
    }

}
