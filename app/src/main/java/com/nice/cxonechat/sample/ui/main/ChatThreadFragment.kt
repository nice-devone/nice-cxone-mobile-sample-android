package com.nice.cxonechat.sample.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import com.nice.cxonechat.sample.domain.AttachmentSharingRepository
import com.nice.cxonechat.sample.model.AttachmentMessage
import com.nice.cxonechat.sample.model.Message
import com.nice.cxonechat.sample.model.PluginMessage
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.OnPopupActionState.ReceivedOnPopupAction
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.FAILURE
import com.nice.cxonechat.sample.ui.main.ChatThreadViewModel.ReportOnPopupAction.SUCCESS
import com.nice.cxonechat.sample.util.dpToPixels
import com.nice.cxonechat.sample.util.repeatOnViewOwnerLifecycle
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import javax.inject.Inject

/**
 * Fragment presenting UI of one concrete chat thread (conversation).
 */
@AndroidEntryPoint
class ChatThreadFragment : Fragment(),
    MessageInput.InputListener,
    MessageInput.TypingListener,
    MessagesListAdapter.OnLoadMoreListener,
    MessageInput.AttachmentsListener {

    private var messagesAdapter: MessagesListAdapter<Message>? = null

    private var toolbar: androidx.appcompat.widget.Toolbar? = null

    private val viewModel: ChatThreadViewModel by viewModels()

    private var fragmentBinding: FragmentChatThreadBinding? = null

    private val activityLauncher by lazy {
        ActivityLauncher(requireActivity().activityResultRegistry)
            .also(lifecycle::addObserver)
    }

    @Inject
    internal lateinit var attachmentSharingRepository: AttachmentSharingRepository

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(RequestPermission()) { isGranted ->
            if (!isGranted) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.no_notifications_title)
                    .setMessage(R.string.no_notifications_message)
                    .setNeutralButton(R.string.ok, null)
                    .show()
            }
        }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermissions(
                Manifest.permission.POST_NOTIFICATIONS,
                R.string.notifications_rationale
            )
        }
        activityLauncher // activity launcher has to self-register before onStart
    }

    private fun checkNotificationPermissions(permission: String, @StringRes rationale: Int) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> Unit

            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
                showRationale(permission, rationale)

            else ->
                requestPermissionLauncher.launch(permission)
        }
    }

    private fun showRationale(permission: String, @StringRes rationale: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.permission_requested))
            .setMessage(rationale)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .show()
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

    private fun Context.getDrawableResource(uri: String) =
        if(uri.startsWith("android.resource://")) {
            getDrawable(uri.substringAfterLast("/"))
        } else {
            null
        }

    @SuppressLint("DiscouragedApi")
    private fun Context.getDrawable(named: String): Drawable? {
        val id = resources.getIdentifier(named, "drawable", packageName)

        return if(id == Resources.ID_NULL) {
            null
        } else {
            AppCompatResources.getDrawable(
                this,
                id,
            )
        }
    }

    private fun initAdapter(binding: FragmentChatThreadBinding) {
        val imageLoader = ImageLoader { imageView: ImageView, url: String?, _: Any? ->
            val safeUrl = url ?: return@ImageLoader
            val context = context ?: return@ImageLoader
            val thumbnailSize = context.dpToPixels(240f).toInt()
            val image = context.getDrawableResource(safeUrl)

            if(image != null) {
                imageView.setImageDrawable(image)
            } else {
                Glide.with(this)
                    .load(safeUrl)
                    .override(thumbnailSize)
                    .placeholder(R.drawable.downloading_48px)
                    .fallback(R.drawable.document_48px)
                    .error(R.drawable.error_48px)
                    .into(imageView)
            }
        }
        val holders = MessageHolders()
            .registerContentType(
                CONTENT_TYPE_PLUGIN_BUTTON,
                IncomingTextAndButtonsMessageViewHolder::class.java,
                R.layout.item_custom_plugin_message,
                OutcomingTextAndButtonsMessageViewHolder::class.java,
                R.layout.item_custom_plugin_message
            ) { message, type ->
                if (type != CONTENT_TYPE_PLUGIN_BUTTON) false else message is PluginMessage
            }
            .setIncomingTextLayout(R.layout.item_custom_incoming_text_message)
            .setOutcomingTextLayout(R.layout.item_custom_outcoming_text_message)
            .setOutcomingTextHolder(CustomOutcomingTextMessageViewHolder::class.java)

        val adapter = MessagesListAdapter<Message>(SENDER_ID, holders, imageLoader)
        messagesAdapter = adapter
        adapter.setLoadMoreListener(this)
        adapter.setOnMessageClickListener { message ->
            if (message !is AttachmentMessage) return@setOnMessageClickListener
            val url = message.originalUrl
            val mimeType = message.mimeType.orEmpty()
            val directions = when {
                mimeType.startsWith("image/") -> ChatThreadFragmentDirections.actionChatThreadFragmentToImagePreviewActivity(url)
                mimeType.startsWith("video/") -> ChatThreadFragmentDirections.actionChatThreadFragmentToVideoPreviewActivity(url)
                else -> {
                    openWithAndroid(message)
                    null
                }
            } ?: return@setOnMessageClickListener
            findNavController().navigate(directions)
        }
        adapter.setOnMessageLongClickListener(::onMessageLongClick)
        binding.messagesList.setAdapter(adapter)
    }


    private fun onMessageLongClick(message: Message) {
        if (message !is AttachmentMessage) return
        val context = context ?: return
        lifecycleScope.launch {
            val intent = attachmentSharingRepository.createSharingIntent(message, context)
            if (intent == null) {
                Toast.makeText(requireContext(), "Unable to store attachment for sharing, please try again later", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent.createChooser(intent, null))
            }
        }
    }

    private fun openWithAndroid(message: AttachmentMessage) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(message.originalUrl), message.mimeType)
        }
        val context = context ?: return
        val packageManager = context.packageManager

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            AlertDialog.Builder(context)
                .setTitle(R.string.unsupported_type_title)
                .setMessage(getString(R.string.unsupported_type_message, message.mimeType))
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
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

    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        viewModel.loadMore()
    }

    override fun onAddAttachments() {
        AlertDialog.Builder(context ?: return)
            .setTitle(getString(R.string.attachment_picker_title))
            .setSingleChoiceItems(
                R.array.attachment_type_labels,
                -1
            ) { dialog, which ->
                resources
                    .getStringArray(R.array.attachment_type_mimetypes)
                    .getOrNull(which)
                    ?.let(activityLauncher::getContent)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

    /**
     * [LifecycleObserver][androidx.lifecycle.LifecycleObserver] intended to interface between the [ChatThreadFragment]
     * and document picker activities to pick attachments.  This is now the recommended method for calling the document
     * picker to fetch an image, video, or other document.
     *
     * At some point this could be expanded to support
     * [TakePicture][androidx.activity.result.contract.ActivityResultContracts.TakePicture] and friends.
     */
    inner class ActivityLauncher(
        private val registry: ActivityResultRegistry
    ) : DefaultLifecycleObserver {
        private var getContent: ActivityResultLauncher<String>? = null

        override fun onCreate(owner: LifecycleOwner) {
            getContent = registry.register("key", owner, GetContent()) { uri ->
                val safeUri = uri ?: return@register

                viewModel.sendAttachment(null, safeUri)
            }
        }

        /**
         * start a foreign activity to find an attachment with the indicated mime type
         *
         * [mimeType] is one of the strings contained in the string-array resource
         * attachment_type_mimetypes.
         *
         * Note that this will work for finding existing resources, but not for opening
         * the camera for photos or videos.
         *
         * @param mimeType attachment type to find.
         *
         */
        fun getContent(mimeType: String) = getContent?.launch(mimeType)
    }
}
