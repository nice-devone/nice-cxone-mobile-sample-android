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
import com.nice.cxonechat.sample.databinding.FragmentHomeConfigurationBinding

class HomeConfigurationFragment : Fragment() {
    private var _binding: FragmentHomeConfigurationBinding? = null
    private val binding get() = _binding!!
    private var environmentSelected = "CD"
    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog = ProgressDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val configurationAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            listOf("CD", "M&J", "Sales")
        )

        with(binding) {
            configurationAutoCompleteTextView.setAdapter(configurationAdapter)
            configurationAutoCompleteTextView.setText("CD", false)
            configurationAutoCompleteTextView.setOnItemClickListener { parent, view, index, id ->
                environmentSelected = configurationAdapter.getItem(index).orEmpty()
            }

            continueButton.setOnClickListener {
                if (environmentSelected.isBlank()) {
                    showAlert(getString(R.string.select_valid_configuration))
                } else {
                    showDialog()
                    if (environmentSelected == "M&J") {
                        CXOneChat.getChannelConfiguration(
                            "https://channels-eu1-qa.brandembassy.com/chat/",
                            "wss://chat-gateway-eu1-qa.brandembassy.com",
                            6427,
                            "chat_cac453c1-7651-4d10-abba-c4a426d932ff"
                        ) { channelResponse, status, message, key ->
                            hideDialog()
                            if (channelResponse == null) {
                                showAlert(getString(R.string.configuration_error))
                                return@getChannelConfiguration
                            }

                            showLogin(channelResponse, "6427", "chat_cac453c1-7651-4d10-abba-c4a426d932ff")
                        }
                    } else {
                        CXOneChat.getChannelConfiguration(
                            1386,
                            "chat_71b74591-4659-4220-bb6b-4ad34f8078aa",
                            CXOneEnvironment.NA1
                        ) { channelResponse, status, message, key ->
                            hideDialog()
                            if (channelResponse == null) {
                                showAlert(getString(R.string.configuration_error))
                                return@getChannelConfiguration
                            }

                            showLogin(channelResponse, "1386", "chat_71b74591-4659-4220-bb6b-4ad34f8078aa")
                        }
                    }
                }
            }

            useCustomConfigurationTextView.setOnClickListener {
                val action = HomeConfigurationFragmentDirections.actionHomeConfigurationToCustomConfigurationFragment()
                view.findNavController().navigate(action)
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLogin(
        channelConfiguration: ChannelConfiguration,
        brandId: String,
        channelId: String
    ) {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ENVIRONMENT_SELECTED, environmentSelected)
            putExtra(CustomConfigurationFragment.BRAND_ID, brandId)
            putExtra(CustomConfigurationFragment.CHANNEL_ID, channelId)
            putExtra(CustomConfigurationFragment.CONFIGURATION, channelConfiguration)
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

    companion object {
        const val ENVIRONMENT_SELECTED = "environment_selected_extra"
    }
}