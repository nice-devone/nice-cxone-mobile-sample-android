package com.nice.cxonechat.sample

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.nice.cxonechat.CXOneChat
import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.models.channel.ChannelConfiguration
import com.nice.cxonechat.sample.HomeConfigurationFragment.Companion.ENVIRONMENT_SELECTED
import com.nice.cxonechat.sample.databinding.FragmentCustomConfigurationBinding

class CustomConfigurationFragment : Fragment() {
    private var _binding: FragmentCustomConfigurationBinding? = null
    private val binding get() = _binding!!
    private var environmentSelected = ""
    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog = ProgressDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val environmentAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            listOf("NA1", "QA")
        )

        with(binding) {
            environmentAutoCompleteTextView.setAdapter(environmentAdapter)
            environmentAutoCompleteTextView.setText("")
            environmentAutoCompleteTextView.setOnItemClickListener { parent, view, index, id ->
                environmentSelected = environmentAdapter.getItem(index).orEmpty()
            }

            useDefaultConfigurationTextView.setOnClickListener {
                val action =
                    CustomConfigurationFragmentDirections.actionCustomConfigurationToHomeConfiguration()
                view.findNavController().navigate(action)
            }

            continueButton.setOnClickListener {
                if (environmentSelected.isBlank() ||
                    brandIdTextInputLayout.editText?.text.isNullOrEmpty() ||
                    channelIdTextInputLayout.editText?.text.isNullOrEmpty()
                ) {
                    showAlert(getString(R.string.fields_cannot_be_empty))
                } else {
                    val brandId = brandIdTextInputLayout.editText?.text.toString()
                    val channelId = channelIdTextInputLayout.editText?.text.toString()

                    showDialog()
                    if (environmentSelected == "M&J") {
                        CXOneChat.getChannelConfiguration(
                            "https://channels-eu1-qa.brandembassy.com/chat/",
                            "wss://chat-gateway-eu1-qa.brandembassy.com",
                            if (brandId.isNullOrEmpty()) 6427 else brandId.toInt(),
                            channelId.ifEmpty { "chat_cac453c1-7651-4d10-abba-c4a426d932ff" }
                        ) { channelResponse, status, message, key ->
                            hideDialog()
                            if (channelResponse == null) {
                                showAlert(getString(R.string.configuration_error))
                                return@getChannelConfiguration
                            }

                            showLogin(channelResponse, brandId, channelId)
                        }
                    } else {
                        CXOneChat.getChannelConfiguration(
                            if (brandId.isNullOrEmpty()) 1386 else brandId.toInt(),
                            channelId.ifEmpty { "chat_71b74591-4659-4220-bb6b-4ad34f8078aa" },
                            CXOneEnvironment.NA1
                        ) { channelResponse, status, message, key ->
                            hideDialog()
                            if (channelResponse == null) {
                                showAlert(getString(R.string.configuration_error))
                                return@getChannelConfiguration
                            }

                            showLogin(channelResponse, brandId, channelId)
                        }
                    }
                }
            }
        }
    }

    private fun showLogin(
        channelConfiguration: ChannelConfiguration,
        brandId: String,
        channelId: String
    ) {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ENVIRONMENT_SELECTED, environmentSelected)
            putExtra(BRAND_ID, brandId)
            putExtra(CHANNEL_ID, channelId)
            putExtra(CONFIGURATION, channelConfiguration)
        }
        requireContext().startActivity(intent)
    }

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.information))
        builder.setMessage(message)

        builder.setPositiveButton(getString(R.string.ok)) { dialog, which ->
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

    companion object {
        const val BRAND_ID = "brand_id_extra"
        const val CHANNEL_ID = "channel_id_extra"
        const val CONFIGURATION = "configuration_extra"
    }
}