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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.enums.CXOneEnvironment.NA1
import com.nice.cxonechat.sample.LoginActivity
import com.nice.cxonechat.sample.R.layout
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.databinding.FragmentHomeConfigurationBinding
import com.nice.cxonechat.sample.storage.ChatConfigurationStorage.setConfiguration
import com.nice.cxonechat.sample.storage.ValueStorage
import com.nice.cxonechat.sample.ui.config.CustomEnvironments.EU_QA1
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeConfigurationFragment : Fragment() {
    private var _binding: FragmentHomeConfigurationBinding? = null
    private val binding get() = _binding!!
    private var environmentSelected = "CD"
    private lateinit var dialog: ProgressDialog

    @Inject
    internal lateinit var valueStorage: ValueStorage

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

        val configurationAdapter = ArrayAdapter(
            requireContext(),
            layout.dropdown_item,
            listOf("CD", "M&J", "Sales")
        )

        with(binding) {
            configurationAutoCompleteTextView.setAdapter(configurationAdapter)
            configurationAutoCompleteTextView.setText("CD", false)
            configurationAutoCompleteTextView.setOnItemClickListener { _, _, index, _ ->
                environmentSelected = configurationAdapter.getItem(index).orEmpty()
            }

            continueButton.setOnClickListener {
                if (environmentSelected.isBlank()) {
                    showAlert(getString(string.select_valid_configuration))
                } else {
                    val configuration = when (environmentSelected) {
                        "M&J" -> SocketFactoryConfiguration(
                            environment = EU_QA1,
                            brandId = 6427,
                            channelId = "chat_cac453c1-7651-4d10-abba-c4a426d932ff"
                        )
                        else -> SocketFactoryConfiguration(
                            environment = NA1.value,
                            brandId = 1386,
                            channelId = "chat_71b74591-4659-4220-bb6b-4ad34f8078aa",
                        )
                    }
                    storeConfigAndShowLogin(configuration)
                }
            }

            useCustomConfigurationTextView.setOnClickListener {
                val action = HomeConfigurationFragmentDirections.actionHomeConfigurationToCustomConfigurationFragment()
                view.findNavController().navigate(action)
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
            valueStorage.setConfiguration(configuration)
            hideDialog()
            showLogin()
        }
    }

}
