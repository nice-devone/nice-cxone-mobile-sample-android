package com.nice.cxonechat.sample.ui.config

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.sample.LoginActivity
import com.nice.cxonechat.sample.R.layout
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.databinding.FragmentCustomConfigurationBinding
import com.nice.cxonechat.sample.storage.ChatConfigurationStorage.setConfiguration
import com.nice.cxonechat.sample.storage.ValueStorage
import com.nice.cxonechat.sample.ui.config.CustomEnvironments.EU_QA1
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomConfigurationFragment : Fragment() {
    private var _binding: FragmentCustomConfigurationBinding? = null
    private val binding get() = _binding!!
    private var environmentSelected: CXOneEnvironment? = CXOneEnvironment.NA1
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
        _binding = FragmentCustomConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val environmentAdapter = ArrayAdapter(
            requireContext(),
            layout.dropdown_item,
            CXOneEnvironment.values().map { it.name } + "QA"
        )

        with(binding) {
            environmentAutoCompleteTextView.setAdapter(environmentAdapter)
            environmentAutoCompleteTextView.setText("")
            environmentAutoCompleteTextView.setOnItemClickListener { _, _, index, _ ->
                val name = environmentAdapter.getItem(index) ?: return@setOnItemClickListener
                environmentSelected = runCatching { CXOneEnvironment.valueOf(name) }.getOrNull()
            }

            useDefaultConfigurationTextView.setOnClickListener {
                val action =
                    CustomConfigurationFragmentDirections.actionCustomConfigurationToHomeConfiguration()
                view.findNavController().navigate(action)
            }

            continueButton.setOnClickListener {
                val brandId = brandIdTextInputLayout.editText?.text?.toString()
                val channelId = channelIdTextInputLayout.editText?.text?.toString()
                if (
                    brandId.isNullOrEmpty() ||
                    channelId.isNullOrEmpty()
                ) {
                    showAlert(getString(string.fields_cannot_be_empty))
                    return@setOnClickListener
                }

                val environment = environmentSelected?.value
                val config = SocketFactoryConfiguration(
                    environment = environment ?: EU_QA1,
                    brandId = brandId.toLong(),
                    channelId = channelId,
                )
                storeConfigAndShowLogin(config)
            }
        }
    }

    private fun showLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        requireContext().startActivity(intent)
    }

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(string.information))
        builder.setMessage(message)

        builder.setPositiveButton(getString(string.ok)) { dialog, which ->
            dialog.cancel()
        }

        builder.show()
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
