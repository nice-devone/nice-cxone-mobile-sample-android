package com.nice.cxonechat.sample.ui.main

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.gson.Gson
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.custom.holders.CustomOutcomingTextMessageViewHolder
import com.nice.cxonechat.sample.custom.holders.IncomingTextAndButtonsMessageViewHolder
import com.nice.cxonechat.sample.custom.holders.OutcomingTextAndButtonsMessageViewHolder
import com.nice.cxonechat.sample.databinding.CustomSnackBarBinding
import com.nice.cxonechat.sample.databinding.FragmentChatThreadBinding
import com.nice.cxonechat.sample.model.AttachmentMessage
import com.nice.cxonechat.sample.model.Message
import com.nice.cxonechat.sample.model.PluginMessage
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.OnPopupActionState.ReceivedOnPopupAction
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.FAILURE
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.SUCCESS
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*

@AndroidEntryPoint
class ChatThreadFragment : Fragment(),
    MessageInput.InputListener,
    MessageInput.TypingListener,
    MessagesListAdapter.SelectionListener,
    MessagesListAdapter.OnLoadMoreListener,
    MessageInput.AttachmentsListener {

    private var messagesAdapter: MessagesListAdapter<Message>? = null

    private var toolbar: androidx.appcompat.widget.Toolbar? = null

    private val viewModel: ChatThreadViewModel by viewModels()

    private var fragmentBinding: FragmentChatThreadBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentChatThreadBinding.inflate(layoutInflater, container, false)
        fragmentBinding = binding
        initAdapter(binding)

        binding.input.setupListeners(this)
        registerAgentTypingListener()
        registerOnPopupActionListener()
        registerChatMetadataListener()
        registerMessageListener()
        container?.findViewById<Toolbar>(R.id.toolbar)
        return binding.root
    }

    private fun registerOnPopupActionListener() {
        repeatOnViewOwnerLifecycle {
            viewModel.actionState.filterIsInstance<ReceivedOnPopupAction>().collect {
                val rawVariables = it.variables
                try {
                    val variables = Gson().toJson(rawVariables)
                    val jsonObject = JSONTokener(variables).nextValue() as JSONObject
                    val headingText = jsonObject.getString("headingText")
                    val bodyText = jsonObject.getString("bodyText")
                    val action = jsonObject.getJSONObject("action")
                    val actionText = action.getString("text")
                    val actionUrl = action.getString("url")
                    val data = SnackbarSetupData(headingText, bodyText, actionText, actionUrl, it)
                    showSnackBar(data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun registerAgentTypingListener() {
        repeatOnViewOwnerLifecycle {
            viewModel.agentState.collect { agentIsTyping ->
                val binding = fragmentBinding ?: return@collect
                val agentTypingText = if (agentIsTyping) "agent is typing..." else ""
                binding.agentTypingTextView.text = agentTypingText
            }
        }
    }

    private fun registerChatMetadataListener() {
        repeatOnViewOwnerLifecycle {
            viewModel.chatMetadata.collect { chatData ->
                toolbar?.title = chatData.threadName
            }
        }
    }

    private fun registerMessageListener() {
        repeatOnViewOwnerLifecycle {
            viewModel.messages.collect { messages ->
                messagesAdapter?.run {
                    // TODO diff against current state
                    clear()
                    addToEnd(messages, false)
                }
            }
        }
    }

    private fun initAdapter(binding: FragmentChatThreadBinding) {
        val imageLoader = ImageLoader { imageView: ImageView, url: String?, _: Any? ->
            Glide.with(this)
                .load(url)
                .into(imageView)
        }
        val holders = MessageHolders()
            .registerContentType(
                CONTENT_TYPE_PLUGIN_BUTTON,
                IncomingTextAndButtonsMessageViewHolder::class.java,
                R.layout.item_custom_incoming_text_and_buttons_message,
                OutcomingTextAndButtonsMessageViewHolder::class.java,
                R.layout.item_custom_outcoming_text_and_buttons_message
            ) { message, type ->
                if (type != CONTENT_TYPE_PLUGIN_BUTTON) false else message is PluginMessage
            }
            .setIncomingTextLayout(R.layout.item_custom_incoming_text_message)
            .setOutcomingTextLayout(R.layout.item_custom_outcoming_text_message)
            .setOutcomingTextHolder(CustomOutcomingTextMessageViewHolder::class.java)

        val adapter = MessagesListAdapter<Message>(SENDER_ID, holders, imageLoader)
        messagesAdapter = adapter
        adapter.enableSelectionMode(this)
        adapter.setLoadMoreListener(this)
        adapter.setOnMessageClickListener { message ->
            if (message !is AttachmentMessage) return@setOnMessageClickListener
            val url = message.imageUrl
            val mimeType = message.mimeType.orEmpty()
            val directions = when {
                mimeType.startsWith("video/") -> ChatThreadFragmentDirections.actionChatThreadFragmentToVideoPreviewActivity(url)
                else -> ChatThreadFragmentDirections.actionChatThreadFragmentToImagePreviewActivity(url)
            }
            findNavController().navigate(directions)
        }
        binding.messagesList.setAdapter(adapter)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reportPageView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding?.input?.setupListeners(null)
        fragmentBinding = null
    }

    // TODO implement menu handling

    private fun MessageInput.setupListeners(listener: ChatThreadFragment?) {
        setInputListener(listener)
        setTypingListener(listener)
        setAttachmentsListener(listener)
    }

    override fun onSubmit(input: CharSequence?): Boolean {
        input ?: return false
        viewModel.sendMessage(input.toString())
        return true
    }

    override fun onStartTyping() {
        viewModel.reportThreadRead()
        viewModel.reportTypingStarted()
    }

    override fun onStopTyping() {
        viewModel.reportTypingEnd()
    }

    override fun onSelectionChanged(count: Int) {
        // No-op
    }

    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        viewModel.loadMore()
    }

    override fun onAddAttachments() {
        val photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val chosenImageUri: Uri = data.data ?: return
            viewModel.sendAttachment(null, chosenImageUri)
        }
    }

    private fun showSnackBar(data: SnackbarSetupData) {
        val binding = fragmentBinding ?: return
        val parentLayout = binding.root
        val snackbar = Snackbar.make(parentLayout, "", Snackbar.LENGTH_INDEFINITE)
        val snackBinding = CustomSnackBarBinding.inflate(layoutInflater, null, false)
        snackbar.view.setBackgroundColor(Color.TRANSPARENT)

        val snackbarLayout = snackbar.view as SnackbarLayout
        snackbarLayout.setPadding(0, 0, 0, 0)

        val headingTextView: TextView = snackBinding.headingTextView
        val bodyTextView: TextView = snackBinding.bodyTextView
        val actionTextView: TextView = snackBinding.actionTextView
        val closeButton: ImageButton = snackBinding.closeButton

        headingTextView.text = data.headingText
        bodyTextView.text = data.bodyText
        actionTextView.text = data.actionText

        val action = data.action

        actionTextView.setOnClickListener {
            viewModel.reportOnPopupActionClicked(action)
            // TODO build intent for the actionUrl
            viewModel.reportOnPopupAction(SUCCESS, action)
            snackbar.dismiss()
        }

        closeButton.setOnClickListener {
            viewModel.reportOnPopupAction(FAILURE, action)
            snackbar.dismiss()
        }

        snackbarLayout.addView(snackBinding.root, 0)
        snackbar.show()

        viewModel.reportOnPopupActionDisplayed(action)
    }

    private class SnackbarSetupData(
        val headingText: String,
        val bodyText: String,
        val actionText: String,
        val actionUrl: String,
        val action: ReceivedOnPopupAction,
    )


    companion object {
        internal const val SENDER_ID = "1"
        private const val CONTENT_TYPE_PLUGIN_BUTTON: Byte = 1
    }
}
