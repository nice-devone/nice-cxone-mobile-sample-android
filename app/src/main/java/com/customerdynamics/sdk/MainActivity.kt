package com.customerdynamics.sdk

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.customerdynamics.androidlibrary.CXOneChat
import com.customerdynamics.androidlibrary.api.model.request.UploadFileRequest
import com.customerdynamics.androidlibrary.api.model.response.UploadFileResponse
import com.customerdynamics.androidlibrary.api.provider.AttachmentProvider
import com.customerdynamics.androidlibrary.enums.MessageType
import com.customerdynamics.androidlibrary.listeners.*
import com.customerdynamics.androidlibrary.models.Attachment
import com.customerdynamics.androidlibrary.models.CustomField
import com.customerdynamics.sdk.custom.holders.IncomingTextAndButtonsMessageViewHolder
import com.customerdynamics.sdk.custom.holders.OutcomingTextAndButtonsMessageViewHolder
import com.customerdynamics.sdk.model.Message
import com.customerdynamics.sdk.model.SpinnerOption
import com.customerdynamics.sdk.model.User
import com.google.android.material.textfield.TextInputLayout
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

    private var agentTypingTextView: TextView? = null
    private var messagesList: MessagesList? = null
    private var attachmentProvider: AttachmentProvider? = null
    private var dialog: ProgressDialog? = null
    private var toolbar: Toolbar? = null
    private var caseId = ""
    private var threadId: UUID? = UUID.randomUUID()
    private var isRecoveryThread = false
    private var messagesScrollToken  = ""
    private var chatMessages = mutableListOf<com.customerdynamics.androidlibrary.models.Message>()
    private var isCustomFieldsSended = true
    private var lastName = ""

    private companion object {
        private const val CONTENT_TYPE_PLUGIN_BUTTON: Byte = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Home.getPinpointManager(this)
        this.agentTypingTextView = findViewById(R.id.agentTypingTextView)
        this.toolbar = findViewById(R.id.my_toolbar)
        dialog = ProgressDialog(this)

        this.messagesList = findViewById(R.id.messagesList)
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
            this.isRecoveryThread = true
            currentThreadId = intent.getSerializableExtra("threadId") as UUID
        }

        if (!isRecoveryThread && isNewThread) {
            val firstName = intent.getStringExtra("firstName") ?: ""
            lastName = intent.getStringExtra("lastName") ?: ""

            if (firstName != "" && lastName != "") {
                createThread(firstName, lastName)
            } else {
                this.showInitialDialog()
            }
        } else {
            this.verifyThreads(currentThreadId)
        }

        this.toolbar?.title = ""
        this.toolbar!!.inflateMenu(R.menu.default_menu)
        this.toolbar!!.setOnMenuItemClickListener(this)

        CXOneChat.setReceiveMessageListener(receiveMessageListener)
        CXOneChat.setAgentTypingListener(agentTypingListener)
        CXOneChat.setRecoverThreadListener(recoverThreadListener)
        CXOneChat.setContactInboxAssigneeChangedListener(contactInboxAssigneeChangedListener)
        CXOneChat.setReceivePluginMessageListener(receivePluginMessageListener)
        CXOneChat.setMessageSentListener(messageSentListener)
        CXOneChat.setMessagesLoadedListener(messagesLoadedListener)
        CXOneChat.setMessageReceivedFromOtherThreadListener(messageReceivedFromOtherThreadListener)
        CXOneChat.setAgentReadMessageListener(agentReadMessageListener)
        CXOneChat.setThreadCreatedListener(threadCreatedListener)
    }

    override fun finish() {
        super.finish()
        CXOneChat.unsetAgentTypingListener()
        CXOneChat.unsetRecoverThreadListener()
        CXOneChat.unsetContactInboxAssigneeChangedListener()
        CXOneChat.unsetReceivePluginMessageListener()
        CXOneChat.unsetMessageSentListener()
        CXOneChat.unsetMessagesLoadedListener()
        CXOneChat.unsetMessageReceivedFromOtherThreadListener()
        CXOneChat.unsetReceiveMessageListener()
        CXOneChat.unsetAgentReadMessageListener()
        CXOneChat.unsetThreadCreatedListener()
    }

    private fun verifyThreads(currentThreadId: UUID) {
        dialog!!.setMessage("Loading, please wait.")
        dialog!!.show()
        Handler().postDelayed({
            val id = if (threadId != null) currentThreadId else null
            CXOneChat.loadThread(id)
        }, 3000)

        Handler().postDelayed({
            if (!isRecoveryThread) {
                this.showInitialDialog()
            }
            if (dialog!!.isShowing) {
                dialog!!.dismiss()
            }
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
                this)

        super.messagesAdapter = MessagesListAdapter(super.senderId, holders, super.imageLoader)
        super.messagesAdapter.enableSelectionMode(this)
        super.messagesAdapter.setLoadMoreListener(this)
        messagesList!!.setAdapter(super.messagesAdapter)
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

            val uploadFileRequest = UploadFileRequest()
            uploadFileRequest.name = "${UUID.randomUUID()}.jpg"
            uploadFileRequest.type = "image/jpg"
            uploadFileRequest.content = image

            this.attachmentProvider!!.uploadFile(uploadFileRequest, this)
        }
    }

    override fun onSubmit(input: CharSequence?): Boolean {
        CXOneChat.sendMessage(input.toString(), this.threadId!!)

        super.messagesAdapter.addToStart(
            Message("1", User("1", "Oscar", "OG", true), input.toString()), true
        )
        return true
    }

    override fun onStartTyping() {
        CXOneChat.markThreadAsRead(this.threadId!!)
        CXOneChat.reportTypingStart(this.threadId!!)
    }

    override fun onStopTyping() {
        CXOneChat.reportTypingEnd(this.threadId!!)
    }

    override fun onSelectionChanged(count: Int) {}

    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        if (this.messagesScrollToken != "") {
            CXOneChat.loadMoreMessages(this.threadId!!, this.messagesScrollToken, this.chatMessages.first().createdAt)
        }
    }

    override fun onAddAttachments() {
        val photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, 1)
    }

    override fun success(uploadFileResponse: UploadFileResponse) {
        if (!uploadFileResponse.fileUrl.isNullOrEmpty()) {
            val attachments = mutableListOf<Attachment>()
            attachments.add(
                Attachment(
                    url = uploadFileResponse.fileUrl.toString(),
                    friendlyName = "${UUID.randomUUID()}.jpg"
                )
            )

            CXOneChat.sendAttachments(attachments, threadId!!)

            val message = Message(
                "1",
                User("1", "Oscar", "OG", true),
                null
            )
            message.setImage(Message.Image(uploadFileResponse.fileUrl))
            super.messagesAdapter.addToStart(message, true)
        }
    }

    override fun failure(status: Int, message: String, errorReason: String) {}

    override fun onMenuItemClick(p0: MenuItem?): Boolean {
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

        builder.setPositiveButton("Ok",
            DialogInterface.OnClickListener { dialog, whichButton ->
                val firstName = firstName.text.toString()
                this.lastName = lastNameEditText.text.toString()
                val email = email.text.toString()

                var customFields = mutableListOf<CustomField>()
                var contactCustomFields = mutableListOf<CustomField>()

                if (firstName != "") {
                    customFields.add(CustomField(ident = "firstname", value = firstName))
                }

                if (lastName != "") {
                    contactCustomFields.add(CustomField(ident = "lastname", value = lastName))
                }

                if (email != "") {
                    customFields.add(CustomField(ident = "email", value = email))
                }

                if (customFields.isNotEmpty())  CXOneChat.setContactCustomFields(customFields)
                if (contactCustomFields.isNotEmpty()) CXOneChat.setCustomerCustomFields(threadId!!, contactCustomFields, caseId)
            })

        builder.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialog, whichButton -> dialog.cancel() })
        builder.show()
        return true
    }

    override fun hasContentFor(message: Message?, type: Byte): Boolean {
        if (type == CONTENT_TYPE_PLUGIN_BUTTON) {
            return message!!.text == "PLUGIN" && message.payload != null && message.payload.elements!!.isNotEmpty()
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
                createThread(firstName, lastName)
                contactClientDialog.cancel()
            } else {
                this.showAlert(getString(R.string.fields_cannot_be_empty))
            }
        }

        contactClientDialog.show()
    }

    private fun createThread(firstName: String, lastName: String) {
        isCustomFieldsSended = false
        CXOneChat.setCustomerName(firstName, lastName)

        val location = intent.getSerializableExtra("location") as SpinnerOption
        val department = intent.getSerializableExtra("department") as SpinnerOption

        val customFields = mutableListOf<CustomField>()

        customFields.add(CustomField(ident = "location", value = location.name))
        customFields.add(CustomField(ident = "department", value = department.name))
        customFields.add(CustomField(ident = "firstname", value = firstName))

        dialog!!.setMessage("Creating thread, please wait.")
        dialog!!.show()
        Handler().postDelayed({
            CXOneChat.createThread()
            CXOneChat.setContactCustomFields(customFields)
            dialog!!.hide()
        }, 3000)
    }

    private fun showAlert(message: String) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.information))
            builder.setMessage(message)

            builder.setPositiveButton(getString(R.string.ok)) { dialog, which ->
                dialog.cancel()
            }

            builder.show()
        }
    }

    private val receiveMessageListener = ReceiveMessage { message, contactId ->
        runOnUiThread {
            caseId = contactId
            val messageData = message.messageContent
            messagesAdapter.addToStart(
                Message(
                    UUID.randomUUID().toString(),
                    User("0", "Web chat", "WC", true),
                    messageData.payload.text
                ), true
            )

            if (!message.attachments.isNullOrEmpty()) {
                message.attachments!!.forEach {
                    val message = Message(
                        UUID.randomUUID().toString(),
                        User("0", "Web chat", "WC", true),
                        null
                    )
                    message.setImage(Message.Image(it.url))
                    messagesAdapter.addToStart(message, true)
                }
            }
        }
    }

    private val agentTypingListener = AgentTyping {
        runOnUiThread {
            if (it) this.agentTypingTextView?.text =
                "agent is typing..." else this.agentTypingTextView?.text = ""
        }
    }

    private val recoverThreadListener = RecoverThread { messages, thread, ownerAssignee, contactId, messagesScrollToken ->
        runOnUiThread {
            this.caseId = caseId
            this.isRecoveryThread = true
            this.messagesScrollToken = messagesScrollToken
            this.chatMessages.addAll(messages)
            threadId = thread.idOnExternalPlatform
            toolbar?.title = "${ownerAssignee.firstName ?: ""} ${ownerAssignee.surname ?: ""}"
            messages.forEach {
                if (it.isOwn == true) {
                    if(it.messageContent.type == MessageType.Plugin.value) {
                        val message = Message(
                            UUID.randomUUID().toString(),
                            User("0", "Web chat", "WC", true),
                            MessageType.Plugin.value
                        )
                        message.payload = it.messageContent.payload
                        super.messagesAdapter.addToStart(
                            message, true
                        )
                    } else {
                        super.messagesAdapter.addToStart(
                            Message(
                                UUID.randomUUID().toString(),
                                User("0", "Web chat", "WC", true),
                                it.messageContent.payload.text
                            ), true
                        )
                    }

                    if (!it.attachments.isNullOrEmpty()) {
                        it.attachments!!.forEach { attachment ->
                            val message = Message(
                                UUID.randomUUID().toString(),
                                User("0", "Web chat", "WC", true),
                                null
                            )
                            message.setImage(Message.Image(attachment.url))
                            super.messagesAdapter.addToStart(message, true)
                        }
                    }
                } else {
                    if(it.messageContent.type == MessageType.Plugin.value) {
                        val message = Message(
                            UUID.randomUUID().toString(),
                            User("1", "Oscar", "OG", true),
                            MessageType.Plugin.value
                        )
                        message.payload = it.messageContent.payload
                        super.messagesAdapter.addToStart(
                            message, true
                        )
                    } else {
                        super.messagesAdapter.addToStart(
                            Message(
                                UUID.randomUUID().toString(),
                                User("1", "Oscar", "OG", true),
                                it.messageContent.payload.text
                            ), true
                        )
                    }

                    if (!it.attachments.isNullOrEmpty()) {
                        it.attachments!!.forEach { attachment ->
                            val message = Message(
                                UUID.randomUUID().toString(),
                                User("1", "Oscar", "OG", true),
                                null
                            )
                            message.setImage(Message.Image(attachment.url))
                            super.messagesAdapter.addToStart(message, true)
                        }
                    }
                }
            }
            if (dialog!!.isShowing) {
                dialog!!.dismiss()
            }
        }
    }

    private val contactInboxAssigneeChangedListener = ContactInboxAssigneeChanged {
        runOnUiThread {
            toolbar?.title = "${it.firstName} ${it.surname}"
        }
    }

    private val receivePluginMessageListener = ReceivePluginMessage { message, contactId ->
        runOnUiThread {
            this.caseId = caseId
            val newMessage = Message("0", User("0", "Oscar", "OG", true), MessageType.Plugin.value)
            newMessage.payload = message.messageContent.payload
            super.messagesAdapter.addToStart(
                newMessage, true
            )
        }
    }

    private val messageSentListener = MessageSent {
        this.caseId = it

        if (!isCustomFieldsSended) {
            val contactCustomFields = mutableListOf<CustomField>()
            contactCustomFields.add(CustomField(ident = "lastname", value = lastName))
            CXOneChat.setCustomerCustomFields(threadId!!, contactCustomFields, caseId)
        }
    }

    private val messagesLoadedListener = MessagesLoaded { messages, messagesScrollToken ->
        runOnUiThread {
            this.messagesScrollToken = messagesScrollToken
            val messagesList = mutableListOf<Message>()
            messages.forEach {
                if (it.isOwn == true) {
                    if(it.messageContent.type == MessageType.Plugin.toString()) {
                        val message = Message(
                            UUID.randomUUID().toString(),
                            User("0", "Web chat", "WC", true),
                            MessageType.Plugin.toString()
                        )
                        message.payload = it.messageContent.payload
                        messagesList.add(message)
                    } else {
                        messagesList.add(Message(
                            UUID.randomUUID().toString(),
                            User("0", "Web chat", "WC", true),
                            it.messageContent.payload.text
                        ))
                    }

                    if (!it.attachments.isNullOrEmpty()) {
                        it.attachments!!.forEach { attachment ->
                            val message = Message(
                                UUID.randomUUID().toString(),
                                User("0", "Web chat", "WC", true),
                                null
                            )
                            message.setImage(Message.Image(attachment.url))
                            messagesList.add(message)
                        }
                    }
                } else {
                    if(it.messageContent.type == MessageType.Plugin.toString()) {
                        val message = Message(
                            UUID.randomUUID().toString(),
                            User("1", "Oscar", "OG", true),
                            MessageType.Plugin.toString()
                        )
                        message.payload = it.messageContent.payload
                        messagesList.add(message)
                    } else {
                        messagesList.add(
                            Message(
                                UUID.randomUUID().toString(),
                                User("1", "Oscar", "OG", true),
                                it.messageContent.payload.text
                            )
                        )
                    }

                    if (!it.attachments.isNullOrEmpty()) {
                        it.attachments!!.forEach { attachment ->
                            val message = Message(
                                UUID.randomUUID().toString(),
                                User("1", "Oscar", "OG", true),
                                null
                            )
                            message.setImage(Message.Image(attachment.url))
                            messagesList.add(message)
                        }
                    }
                }
            }
            super.messagesAdapter.addToEnd(messagesList, false)
        }
    }

    private val messageReceivedFromOtherThreadListener = MessageReceivedFromOtherThread {
        runOnUiThread {
            Toast.makeText(applicationContext, getString(R.string.new_message_received), Toast.LENGTH_SHORT).show()
        }
    }

    private val agentReadMessageListener = AgentReadMessage {
        Log.i("agentReadMessage", it.toString())
    }

    private val threadCreatedListener = ThreadCreated {
        this.threadId = it

    }
}