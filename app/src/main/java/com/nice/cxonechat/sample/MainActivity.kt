package com.nice.cxonechat.sample

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.nice.cxonechat.CXOneChat
import com.nice.cxonechat.api.provider.AttachmentProvider
import com.nice.cxonechat.enums.MessageContentType
import com.nice.cxonechat.enums.MessageDirection
import com.nice.cxonechat.listeners.*
import com.nice.cxonechat.models.ChatThread
import com.nice.cxonechat.models.attachment.Attachment
import com.nice.cxonechat.models.attachment.AttachmentUpload
import com.nice.cxonechat.models.attachment.AttachmentUploadSuccessResponse
import com.nice.cxonechat.models.customField.CustomField
import com.nice.cxonechat.sample.custom.holders.CustomOutcomingTextMessageViewHolder
import com.nice.cxonechat.sample.custom.holders.IncomingTextAndButtonsMessageViewHolder
import com.nice.cxonechat.sample.custom.holders.OutcomingTextAndButtonsMessageViewHolder
import com.nice.cxonechat.sample.model.Message
import com.nice.cxonechat.sample.model.SpinnerOption
import com.nice.cxonechat.sample.model.User
import com.nice.cxonechat.sample.service.PreferencesService
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity : DemoMessagesActivity(), MessageInput.InputListener,
    MessageInput.TypingListener,
    MessagesListAdapter.SelectionListener,
    MessagesListAdapter.OnLoadMoreListener, MessageInput.AttachmentsListener,
    AttachmentProvider.UploadFileListener, Toolbar.OnMenuItemClickListener,
    MessageHolders.ContentChecker<Message> {

    /** The thread that is currently being viewed. */
    private var chatThread: ChatThread? = null

    private var threadName: String = ""

    private var agentTypingTextView: TextView? = null
    private var messagesListView: MessagesList? = null // TODO: Remove
    private var attachmentProvider: AttachmentProvider? = null
    private var dialog: ProgressDialog? = null
    private var toolbar: Toolbar? = null
    private var isRecoveredThread = false
    private var isCustomFieldsSent = true
    private var chatKitMessages = mutableListOf<Message>()

    companion object {
        private const val CONTENT_TYPE_PLUGIN_BUTTON: Byte = 1
        const val IMAGE_URL = "extra_image_url"
        const val VIDEO_URL = "extra_video_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.agentTypingTextView = findViewById(R.id.agentTypingTextView)
        this.toolbar = findViewById(R.id.my_toolbar)
        this.toolbar?.title = ""
        setSupportActionBar(this.toolbar)

        dialog = ProgressDialog(this)

        this.messagesListView = findViewById(R.id.messagesList)
        initAdapter()

        val input: MessageInput = findViewById(R.id.input)
        input.setInputListener(this)
        input.setTypingListener(this)
        input.setAttachmentsListener(this)

        this.attachmentProvider = AttachmentProvider()

        val isNewThread = intent.getBooleanExtra("isNewThread", false)
        val isRecoveryThread = intent.getBooleanExtra("isRecoveryThread", false)
        var currentThreadId: UUID = UUID.randomUUID()

        if (intent.getSerializableExtra("threadId") != null) {
            this.isRecoveredThread = true
            currentThreadId = intent.getSerializableExtra("threadId") as UUID
            chatThread = CXOneChat.threads.first {
                it.idOnExternalPlatform == currentThreadId
            }
        }

        if (!isRecoveryThread && isNewThread) {
            val firstName = intent.getStringExtra("firstName").orEmpty()
            val lastName = intent.getStringExtra("lastName").orEmpty()

            if (firstName != "" && lastName != "") {
                createThread(firstName, lastName)
            } else {
                this.showInitialDialog()
            }
        } else {
            this.verifyThreads(currentThreadId)
        }

        this.toolbar!!.inflateMenu(R.menu.default_menu)
        this.toolbar!!.setOnMenuItemClickListener(this)

        CXOneChat.setNewMessageListener(newMessageListener)
        CXOneChat.setAgentTypingListener(agentTypingListener)
        CXOneChat.setThreadLoadListener(threadLoadListener)
        CXOneChat.setAgentChangeListener(agentChangeListener)
        CXOneChat.setCustomPluginMessageListener(customPluginMessageListener)
        CXOneChat.setMoreMessagesLoadedListener(moreMessagesLoadedListener)
        CXOneChat.setAgentReadMessageListener(agentReadMessageListener)
        CXOneChat.setThreadUpdateListener(threadUpdateListener)

        CXOneChat.reportPageView("MainActivity", "chat-view")
    }

    override fun finish() {
        super.finish()
        CXOneChat.unsetAgentTypingListener()
        CXOneChat.unsetThreadLoadListener()
        CXOneChat.unsetAgentChangeListener()
        CXOneChat.unsetCustomPluginMessageListener()
        CXOneChat.unsetMoreMessagesLoadedListener()
        CXOneChat.unsetNewMessageListener()
        CXOneChat.unsetAgentReadMessageListener()
        CXOneChat.unsetThreadUpdateListener()
    }

    override fun onBackPressed() {
        // Don't allow navigating back if there is only a single thread
        if (CXOneChat.channelConfig!!.settings.hasMultipleThreadsPerEndUser) {
            super.onBackPressed()
        }
    }

    private fun verifyThreads(currentThreadId: UUID) {
        showDialog("Loading, please wait.")
        Handler().postDelayed({
            val id = if (chatThread?.idOnExternalPlatform != null) currentThreadId else null
            CXOneChat.loadThread(id)
        }, 3000)

        Handler().postDelayed({
            if (!isRecoveredThread) {
                this.showInitialDialog()
            }
            hideDialog()
        }, 6000)
    }

    private fun initAdapter() {
        val holders = MessageHolders()
            .registerContentType(
                CONTENT_TYPE_PLUGIN_BUTTON,
                IncomingTextAndButtonsMessageViewHolder::class.java,
                R.layout.item_custom_incoming_text_and_buttons_message,
                OutcomingTextAndButtonsMessageViewHolder::class.java,
                R.layout.item_custom_outcoming_text_and_buttons_message,
                this
            )
            .setIncomingTextLayout(R.layout.item_custom_incoming_text_message)
            .setOutcomingTextLayout(R.layout.item_custom_outcoming_text_message)
            .setOutcomingTextHolder(CustomOutcomingTextMessageViewHolder::class.java)

        super.messagesAdapter = MessagesListAdapter(super.senderId, holders, super.imageLoader)
        super.messagesAdapter.enableSelectionMode(this)
        super.messagesAdapter.setLoadMoreListener(this)
        super.messagesAdapter.setOnMessageClickListener {
            if (it.mimeType == "video/mp4") {
                it.imageUrl?.let {
                    val intent = Intent(this, VideoPreviewActivity::class.java).apply {
                        putExtra(VIDEO_URL, it)
                    }
                    startActivity(intent)
                }
            } else {
                it.imageUrl?.let {
                    val intent = Intent(this, ImagePreviewActivity::class.java).apply {
                        putExtra(IMAGE_URL, it)
                    }
                    startActivity(intent)
                }
            }
        }
        messagesListView!!.setAdapter(super.messagesAdapter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val chosenImageUri: Uri? = data!!.data

            var mBitmap: Bitmap? = null
            mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, chosenImageUri)

            val outputStream = ByteArrayOutputStream()
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

            val image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            val uploadFileRequest = AttachmentUpload()
            uploadFileRequest.fileName = "${UUID.randomUUID()}.jpg"
            uploadFileRequest.mimeType = "image/jpg"
            uploadFileRequest.content = image

            this.attachmentProvider!!.uploadFile(uploadFileRequest, this)
        }
    }

    override fun onSubmit(input: CharSequence?): Boolean {
        CXOneChat.sendMessage(input.toString(), this.chatThread!!.idOnExternalPlatform)
        val newMessage = Message("1", User("1", "Oscar", "OG", true), input.toString(), "Sent")

        super.messagesAdapter.addToStart(
            newMessage, true
        )
        chatKitMessages.add(0, newMessage)
        return true
    }

    override fun onStartTyping() {
        CXOneChat.markThreadAsRead(this.chatThread!!.idOnExternalPlatform)
        CXOneChat.reportTypingStart(this.chatThread!!.idOnExternalPlatform)
    }

    override fun onStopTyping() {
        CXOneChat.reportTypingEnd(this.chatThread!!.idOnExternalPlatform)
    }

    override fun onSelectionChanged(count: Int) {}

    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        try {
            chatThread?.let { CXOneChat.loadMoreMessages(it.idOnExternalPlatform) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAddAttachments() {
        val photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, 1)
    }

    override fun success(uploadFileResponse: AttachmentUploadSuccessResponse) {
        if (!uploadFileResponse.fileUrl.isNullOrEmpty()) {
            val attachments = mutableListOf<Attachment>()
            attachments.add(
                Attachment(
                    url = uploadFileResponse.fileUrl.toString(),
                    friendlyName = "${UUID.randomUUID()}.jpg"
                )
            )

            CXOneChat.sendAttachments(attachments, chatThread!!.idOnExternalPlatform)

            val message = Message(
                "1",
                User("1", "Oscar", "OG", true),
                null,
                "Sent"
            )
            message.setImage(Message.Image(uploadFileResponse.fileUrl))
            super.messagesAdapter.addToStart(message, true)
            chatKitMessages.add(0, message)
        }
    }

    override fun failure(status: Int, message: String, errorReason: String) {}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.default_menu, menu)
        val isMultiThread = CXOneChat.channelConfig?.settings?.hasMultipleThreadsPerEndUser == true
        if (!isMultiThread) {
            menu.getItem(0).isVisible = false
        }

        if (isMultiThread) {
            menu.getItem(2).isVisible = false
        }

        return true
    }

    override fun onMenuItemClick(p0: MenuItem?): Boolean {
        when (p0?.title) {
            "Edit" -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Change custom fields")

                val firstName = EditText(this)
                firstName.hint = "Enter your first name"

                val lastNameEditText = EditText(this)
                lastNameEditText.hint = "Enter your last name"

                val email = EditText(this)
                email.hint = "Enter your email"

                val layout = LinearLayout(this)
                layout.orientation = LinearLayout.VERTICAL
                layout.addView(firstName)
                layout.addView(lastNameEditText)
                layout.addView(email)
                builder.setView(layout)

                builder.setPositiveButton("Ok") { _, _ ->
                    val firstName = firstName.text.toString()
                    val lastName = lastNameEditText.text.toString()
                    val email = email.text.toString()

                    val customFields = mutableListOf<CustomField>()
                    val contactCustomFields = mutableListOf<CustomField>()

                    if (firstName != "") {
                        customFields.add(CustomField(ident = "firstname", value = firstName))
                    }

                    if (lastName != "") {
                        contactCustomFields.add(CustomField(ident = "lastname", value = lastName))
                    }

                    if (email != "") {
                        customFields.add(CustomField(ident = "email", value = email))
                    }

                    if (customFields.isNotEmpty()) CXOneChat.setCustomerCustomFields(customFields)
                    if (contactCustomFields.isNotEmpty()) CXOneChat.setContactCustomFields(
                        chatThread!!.idOnExternalPlatform,
                        contactCustomFields
                    )
                }

                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                builder.show()
            }
            "Set Name" -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Update thread name")

                val threadNameEditText = EditText(this)
                threadNameEditText.hint = "Enter thread name"

                val layout = LinearLayout(this)
                layout.orientation = LinearLayout.VERTICAL
                layout.addView(threadNameEditText)
                builder.setView(layout)

                builder.setPositiveButton("Ok") { _, _ ->
                        showDialog("Loading, please wait.")
                        threadName = threadNameEditText.text.toString()
                        CXOneChat.updateThreadName(threadName, chatThread!!.idOnExternalPlatform)
                    }

                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                builder.show()
            }
            "SignOut" -> {
                CXOneChat.signOut()
                val intent = Intent(this, ConfigurationActivity::class.java).apply {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }
        return true
    }

    override fun hasContentFor(message: Message?, type: Byte): Boolean {
        if (type == CONTENT_TYPE_PLUGIN_BUTTON) {
            return message!!.text == "PLUGIN" && message.payload != null && message.payload.elements.isNotEmpty()
        }
        return false
    }

    private fun showInitialDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.enter_your_details))
        builder.setCancelable(false)

        val contactClientDialogLayout = layoutInflater
            .inflate(
                R.layout.create_customer_dialog,
                null
            )
        builder.setView(contactClientDialogLayout)

        val contactClientDialog = builder.create()

        val firstNameTextInputLayout =
            contactClientDialogLayout.findViewById<TextInputLayout>(R.id.first_name_text_input_layout)
        val lastNameTextInputLayout =
            contactClientDialogLayout.findViewById<TextInputLayout>(R.id.last_name_text_input_layout)
        val acceptButton = contactClientDialogLayout.findViewById<Button>(R.id.accept_button)

        acceptButton.setOnClickListener {
            val firstName = firstNameTextInputLayout.editText!!.text.toString()
            val lastName = lastNameTextInputLayout.editText!!.text.toString()

            if (firstName.isNotBlank() && lastName.isNotBlank()) {
                PreferencesService.setFirstName(this, firstName)
                PreferencesService.setLastName(this, lastName)
                createThread(firstName, lastName)
                contactClientDialog.cancel()
            } else {
                this.showAlert(getString(R.string.fields_cannot_be_empty))
            }
        }

        contactClientDialog.show()
    }

    private fun createThread(firstName: String, lastName: String) {
        isCustomFieldsSent = false
        CXOneChat.setCustomerName(firstName, lastName)

        val location = intent.getSerializableExtra("location") as SpinnerOption
        val department = intent.getSerializableExtra("department") as SpinnerOption

        val customFields = mutableListOf<CustomField>()

        customFields.add(CustomField(ident = "location", value = location.name))
        customFields.add(CustomField(ident = "department", value = department.name))
        customFields.add(CustomField(ident = "firstname", value = firstName))

        showDialog("Creating thread, please wait.")
        Handler().postDelayed({
            val threadId = CXOneChat.createThread()
            val thread = CXOneChat.threads.first {
                it.idOnExternalPlatform == threadId
            }
            chatThread = thread
            CXOneChat.setCustomerCustomFields(customFields)
            hideDialog()
        }, 3000)
    }

    private fun showAlert(message: String) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.information))
            builder.setMessage(message)

            builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }
    }

    private val newMessageListener = NewMessageListener { message ->
        runOnUiThread {
            if (message.threadIdOnExternalPlatform != chatThread!!.idOnExternalPlatform) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.new_message_received),
                    Toast.LENGTH_SHORT
                ).show()
                return@runOnUiThread
            }

            val messageData = message.messageContent
            messagesAdapter.addToStart(
                Message(
                    message.idOnExternalPlatform.toString(),
                    User("0", "Web chat", "WC", true),
                    messageData.payload.text,
                    ""
                ), true
            )

            if (message.attachments.isNotEmpty()) {
                message.attachments.forEach {
                    val message = Message(
                        message.idOnExternalPlatform.toString(),
                        User("0", "Web chat", "WC", true),
                        null,
                        ""
                    )
                    message.setImage(Message.Image(it.url))
                    message.mimeType = it.mimeType
                    messagesAdapter.addToStart(message, true)
                }
            }
        }
    }

    private val agentTypingListener = AgentTypingListener { isTyping, threadIdOnExternalPlatform ->
        runOnUiThread {
            if (threadIdOnExternalPlatform != chatThread!!.idOnExternalPlatform) {
                return@runOnUiThread
            }

            if (isTyping) this.agentTypingTextView?.text =
                "agent is typing..." else this.agentTypingTextView?.text = ""
        }
    }

    private val threadLoadListener = ThreadLoadListener { thread ->
        runOnUiThread {
            hideDialog()
            this.isRecoveredThread = true
            this.chatThread = thread
            super.messagesAdapter.clear()

            chatThread?.let {
//                threadId = thread.idOnExternalPlatform
                val isMultiThread = CXOneChat.channelConfig?.settings?.hasMultipleThreadsPerEndUser == true
                toolbar?.title =
                    if (isMultiThread) it.threadName.orEmpty() else it.threadAgent?.fullName
                it.messages.forEach { message ->
                    if (message.direction == MessageDirection.Outbound) {
                        if (message.messageContent.type == MessageContentType.Plugin) {
                            val newMessage = Message(
                                message.idOnExternalPlatform.toString(),
                                User("0", "Web chat", "WC", true),
                                MessageContentType.Plugin.value,
                                ""
                            )
                            newMessage.payload = message.messageContent.payload
                            super.messagesAdapter.addToStart(
                                newMessage, true
                            )
                        } else {
                            super.messagesAdapter.addToStart(
                                Message(
                                    message.idOnExternalPlatform.toString(),
                                    User("0", "Web chat", "WC", true),
                                    message.messageContent.payload.text,
                                    ""
                                ), true
                            )
                        }

                        if (message.attachments.isNotEmpty()) {
                            message.attachments.forEach { attachment ->
                                val newMessage = Message(
                                    message.idOnExternalPlatform.toString(),
                                    User("0", "Web chat", "WC", true),
                                    null,
                                    ""
                                )
                                newMessage.setImage(Message.Image(attachment.url))
                                newMessage.mimeType = attachment.mimeType
                                super.messagesAdapter.addToStart(newMessage, true)
                            }
                        }
                    } else {
                        if (message.messageContent.type == MessageContentType.Plugin) {
                            val newMessage = Message(
                                message.idOnExternalPlatform.toString(),
                                User("1", "Oscar", "OG", true),
                                MessageContentType.Plugin.value,
                                "Read"
                            )
                            newMessage.payload = message.messageContent.payload
                            super.messagesAdapter.addToStart(
                                newMessage, true
                            )
                        } else {
                            super.messagesAdapter.addToStart(
                                Message(
                                    message.idOnExternalPlatform.toString(),
                                    User("1", "Oscar", "OG", true),
                                    message.messageContent.payload.text,
                                    "Read"
                                ), true
                            )
                        }

                        if (message.attachments.isNotEmpty()) {
                            message.attachments.forEach { attachment ->
                                val newMessage = Message(
                                    message.idOnExternalPlatform.toString(),
                                    User("1", "Oscar", "OG", true),
                                    null,
                                    "Read"
                                )
                                newMessage.setImage(Message.Image(attachment.url))
                                super.messagesAdapter.addToStart(newMessage, true)
                            }
                        }
                    }
                }
            }
            hideDialog()
        }
    }

    private val agentChangeListener =
        AgentChangeListener { agent, _ ->
            runOnUiThread {
                val isMultiThread = CXOneChat.channelConfig?.settings?.hasMultipleThreadsPerEndUser == true
                if (!isMultiThread) {
                    toolbar?.title = agent.fullName
                }
            }
        }

    private val customPluginMessageListener = CustomPluginMessageListener { message ->
        runOnUiThread {
            val newMessage = Message(message.idOnExternalPlatform.toString(), User("0", "Oscar", "OG", true), MessageContentType.Plugin.value, "")
            newMessage.payload = message.messageContent.payload
            super.messagesAdapter.addToStart(
                newMessage, true
            )
        }
    }

    private val moreMessagesLoadedListener = MoreMessagesLoadedListener { messages ->
        runOnUiThread {
            val messagesList = mutableListOf<Message>()
            messages.forEach {
                if (it.direction == MessageDirection.Outbound) {
                    if (it.messageContent.type == MessageContentType.Plugin) {
                        val message = Message(
                            it.idOnExternalPlatform.toString(),
                            User("0", "Web chat", "WC", true),
                            MessageContentType.Plugin.toString(),
                            ""
                        )
                        message.payload = it.messageContent.payload
                        messagesList.add(message)
                    } else {
                        messagesList.add(
                            Message(
                                it.idOnExternalPlatform.toString(),
                                User("0", "Web chat", "WC", true),
                                it.messageContent.payload.text,
                                ""
                            )
                        )
                    }

                    if (it.attachments.isNotEmpty()) {
                        it.attachments.forEach { attachment ->
                            val message = Message(
                                it.idOnExternalPlatform.toString(),
                                User("0", "Web chat", "WC", true),
                                null,
                                ""
                            )
                            message.setImage(Message.Image(attachment.url))
                            message.mimeType = attachment.mimeType
                            messagesList.add(message)
                        }
                    }
                } else {
                    if (it.messageContent.type == MessageContentType.Plugin) {
                        val message = Message(
                            it.idOnExternalPlatform.toString(),
                            User("1", "Oscar", "OG", true),
                            MessageContentType.Plugin.toString(),
                            "Read"
                        )
                        message.payload = it.messageContent.payload
                        messagesList.add(message)
                    } else {
                        messagesList.add(
                            Message(
                                it.idOnExternalPlatform.toString(),
                                User("1", "Oscar", "OG", true),
                                it.messageContent.payload.text,
                                "Read"
                            )
                        )
                    }

                    if (!it.attachments.isNullOrEmpty()) {
                        it.attachments.forEach { attachment ->
                            val message = Message(
                                it.idOnExternalPlatform.toString(),
                                User("1", "Oscar", "OG", true),
                                null,
                                "Read"
                            )
                            message.setImage(Message.Image(attachment.url))
                            message.mimeType = attachment.mimeType
                            messagesList.add(message)
                        }
                    }
                }
            }
            super.messagesAdapter.addToEnd(messagesList, false)
        }
    }

    private val agentReadMessageListener = AgentReadMessageListener {
        runOnUiThread {
            chatKitMessages.forEach {
                if (it.id == "1" && it.status == "Sent") {
                    it.status = "Read"
                    super.messagesAdapter.update("1", it)
                    super.messagesAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private val threadUpdateListener = ThreadUpdateListener {
        runOnUiThread {
            hideDialog()
            toolbar?.title = threadName
        }
    }

    private fun showDialog(message: String) {
        dialog?.setMessage(message)
        dialog?.setCancelable(false)
        dialog?.show()
    }

    private fun hideDialog() {
        if (dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }
}