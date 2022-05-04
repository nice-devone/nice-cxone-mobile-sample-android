package com.customerdynamics.sdk

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager
import com.customerdynamics.androidlibrary.CXOneChat
import com.customerdynamics.androidlibrary.enums.CXOneEnvironment
import com.customerdynamics.androidlibrary.enums.ErrorCode
import com.customerdynamics.androidlibrary.listeners.*
import com.customerdynamics.androidlibrary.models.CustomerIdentity
import com.customerdynamics.sdk.callback.SwipeToDeleteCallback
import com.customerdynamics.sdk.model.SpinnerOption
import com.customerdynamics.sdk.model.Thread
import com.customerdynamics.sdk.service.PreferencesService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.messaging.FirebaseMessaging


class Home : AppCompatActivity(), ThreadSelectedListener {

    private var threadsRecyclerView: RecyclerView? = null
    private var floatingActionButton: FloatingActionButton? = null
    private var threadAdapter: ThreadAdapter? = null
    private var dialog: ProgressDialog? = null
    private var selectedLocation: SpinnerOption? = null
    private var selectedDepartment: SpinnerOption? = null
    private var isRecoveryThread = false
    private var threadList = mutableListOf<Thread>()
    private var channelId = "chat_71b74591-4659-4220-bb6b-4ad34f8078aa"
    private lateinit var customerIdentity: CustomerIdentity

    companion object : Callback<UserStateDetails> {
        private var pinpointManager: PinpointManager? = null
        private val TAG = PushListenerService::class.simpleName

        fun getPinpointManager(applicationContext: Context?): PinpointManager? {
            if (pinpointManager == null) {
                val awsConfig = AWSConfiguration(applicationContext)
                AWSMobileClient.getInstance().initialize(applicationContext, this)
                val pinpointConfig = PinpointConfiguration(
                    applicationContext,
                    AWSMobileClient.getInstance(),
                    awsConfig
                )
                pinpointManager = PinpointManager(pinpointConfig)
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                            return@OnCompleteListener
                        }
                        val token = task.result
                        Log.d(TAG, "Registering push notifications token: $token")
                        pinpointManager!!.notificationClient.registerDeviceToken(token)
                    })
            }
            return pinpointManager
        }

        override fun onResult(result: UserStateDetails?) {}

        override fun onError(e: Exception?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        getPinpointManager(this)

        dialog = ProgressDialog(this)

        threadsRecyclerView = findViewById(R.id.threads_recycler_view)
        floatingActionButton = findViewById(R.id.floating_action_button)

        floatingActionButton!!.setOnClickListener {
            openDialog()
        }

        threadAdapter =
            ThreadAdapter(ArrayList(), this, this)
        threadsRecyclerView?.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                this
            )
        threadsRecyclerView?.adapter = threadAdapter
        if (threadsRecyclerView?.recycledViewPool != null) {
            threadsRecyclerView?.recycledViewPool!!.setMaxRecycledViews(0, 0)
        }

        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val threads = threadAdapter!!.getThreads()
                val position = viewHolder.adapterPosition
                val thread = threads[position]
                showDialog()
                CXOneChat.archiveThread(thread.id.toString())
                threadAdapter!!.removeAt(position)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(threadsRecyclerView)

        val consumerId = PreferencesService.getConsumerId(this)

        CXOneChat.connect(
            CXOneEnvironment.NA1,
            1386,
            channelId,
            if (consumerId != "") consumerId else null,
            this
        )
        java.lang.Thread.sleep(1500)
        CXOneChat.connectChat()
        showDialog()
    }

    override fun onResume() {
        super.onResume()
        CXOneChat.setRecoverThreadListener(recoverThreadListener)
        CXOneChat.setContactInboxAssigneeChangedListener(contactInboxAssigneeChangedListener)
        CXOneChat.setConnectedChatListener(connectedChatListener)
        CXOneChat.setArchiveThreadListener(archiveThreadListener)
        CXOneChat.setConfigurationReceivedListener(configurationReceivedListener)
        CXOneChat.setThreadListFetchedListener(threadListFetchedListener)
        CXOneChat.setSocketDisconnectedListener(socketDisconnectedListener)
        CXOneChat.setThreadMetadataLoadedListener(threadMetadataLoadedListener)
        CXOneChat.setConnectedListener(connectedListener)
        CXOneChat.setErrorListener(errorListener)
    }

    override fun onPause() {
        super.onPause()
        CXOneChat.unsetRecoverThreadListener()
        CXOneChat.unsetContactInboxAssigneeChangedListener()
        CXOneChat.unsetConnectedChatListener()
        CXOneChat.unsetArchiveThreadListener()
        CXOneChat.unsetConfigurationReceivedListener()
        CXOneChat.unsetThreadListFetchedListener()
        CXOneChat.unsetSocketDisconnectedListener()
        CXOneChat.unsetThreadMetadataLoadedListener()
        CXOneChat.unsetConnectedListener()
        CXOneChat.unsetErrorListener()
    }

    override fun threadSelected(thread: Thread) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("threadId", thread.id)
        intent.putExtra("isRecoveryThread", true)
        startActivity(intent)
    }

    private val recoverThreadListener = RecoverThread { messages, thread, ownerAssignee, contactId, messagesScrollToken ->
        runOnUiThread {
            isRecoveryThread = true
            hideDialog()
        }
    }

    private val contactInboxAssigneeChangedListener = ContactInboxAssigneeChanged {
        CXOneChat.loadThread()
    }

    private val connectedChatListener = ConnectedChat { consumerIdentityId, threadId, customerIdentity ->
        this.customerIdentity = customerIdentity
        PreferencesService.setConsumerId(this, consumerIdentityId)
    }

    private val archiveThreadListener = ArchiveThread {
        runOnUiThread {
            hideDialog()
        }
    }

    private val configurationReceivedListener = ConfigurationReceived {
        runOnUiThread {
            if (it != null && !it.settings.hasMultipleThreadsPerEndUser) {
                hideDialog()
                floatingActionButton!!.visibility = View.GONE
                verifyActiveThread()
            } else {
                CXOneChat.loadThreads()
            }
        }
    }

    private val threadListFetchedListener = ThreadListFetched { threads ->
        runOnUiThread {
            threadList = mutableListOf<Thread>()
            threads.forEach {
                if (it.canAddMoreMessages == true) {
                    threadList.add(
                        Thread(
                            it.idOnExternalPlatform,
                            "",
                            "",
                            it.image ?: ""
                        )
                    )
                    CXOneChat.loadThreadInfo(it.idOnExternalPlatform)
                }
            }
            threadAdapter!!.addThreads(threadList as ArrayList<Thread>)
            threadAdapter!!.notifyDataSetChanged()
            hideDialog()
        }
    }

    private val socketDisconnectedListener = SocketDisconnected {
        if (it) {
            runOnUiThread {
                Toast.makeText(applicationContext, getString(R.string.socket_disconnected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val threadMetadataLoadedListener = ThreadMetadataLoaded { threadMetadata ->
        runOnUiThread {
            val thread = threadList.firstOrNull { "${channelId}_${it.id}" ==  threadMetadata.lastMessage.postId }

            if (thread != null) {
                thread.username = threadMetadata.ownerAssignee?.fullName ?: ""
                thread.message = threadMetadata.lastMessage.messageContent.text ?: ""
                thread.agentImage = threadMetadata.ownerAssignee?.image ?: ""
                threadAdapter!!.updateThread(thread)
            }
        }
    }

    private val connectedListener = Connected {
        PreferencesService.setVisitorId(this, it.toString())
    }

    private val errorListener = Error { errorCode, errorMessage ->
        when (errorCode) {
            ErrorCode.ConsumerAuthorizationFailed.value -> {
                showAlert("$errorCode - $errorMessage")
            }
        }
    }

    private fun showAlert(message: String) {
        runOnUiThread {
            hideDialog()

            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.information))
            builder.setMessage(message)

            builder.setPositiveButton(getString(R.string.ok)) { dialog, which ->
                dialog.cancel()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

            builder.show()
        }
    }

    private fun verifyActiveThread() {
        dialog!!.setMessage("Verifying threads, please wait.")
        dialog!!.show()
        Handler().postDelayed({
            CXOneChat.loadThread()
        }, 3000)

        Handler().postDelayed({
            showDialog()
            if (isRecoveryThread) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("isNewThread", true)
                intent.putExtra("department", selectedDepartment)
                intent.putExtra("location", selectedLocation)
                intent.putExtra("isRecoveryThread", isRecoveryThread)
                startActivity(intent)
            } else {
                openDialog()
            }
        }, 6000)

    }
    
    private fun openDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Create conversation")
        val dialogLayout = layoutInflater
            .inflate(
                R.layout.create_thread_dialog,
                null
            )
        builder.setView(dialogLayout)

        val locationSpinner = dialogLayout.findViewById<Spinner>(R.id.location_spinner)
        val departmentSpinner = dialogLayout.findViewById<Spinner>(R.id.department_spinner)
        val cancelButton = dialogLayout.findViewById<Button>(R.id.cancel_button)
        val createThreadButton = dialogLayout.findViewById<Button>(R.id.create_thread)
        val locations = ArrayList<SpinnerOption>()
        val departments = ArrayList<SpinnerOption>()

        locations.add(SpinnerOption(name = "West Coast", id = "WC"))
        locations.add(SpinnerOption(name = "Northeast", id = "NE"))
        locations.add(SpinnerOption(name = "Southeast", id = "SE"))
        locations.add(SpinnerOption(name = "Midwest", id = "MW"))

        departments.add(SpinnerOption(name = "Sales", id = "sales"))
        departments.add(SpinnerOption(name = "Services", id = "services"))

        selectedLocation = locations.first()
        selectedDepartment = departments.first()

        val locationItems = locations.map { it.name }
        val departmentItems = departments.map { it.name }
        val locationsAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, locationItems)
        val departmentsAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            departmentItems
        )

        locationSpinner!!.adapter = locationsAdapter
        locationSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedLocation = locations[position]
            }
        }

        departmentSpinner!!.adapter = departmentsAdapter
        departmentSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedDepartment = departments[position]
            }
        }

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()

        cancelButton.setOnClickListener {
            dialog.hide()
        }
        createThreadButton.setOnClickListener {
            dialog.hide()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isNewThread", true)
            intent.putExtra("department", selectedDepartment)
            intent.putExtra("location", selectedLocation)
            intent.putExtra("firstName", customerIdentity.firstName ?: "")
            intent.putExtra("lastName", customerIdentity.lastName ?: "")
            startActivity(intent)
        }
    }

    private fun showDialog() {
        dialog!!.setMessage("Loading, please wait.")
        dialog!!.show()
    }

    private fun hideDialog() {
        if (dialog!!.isShowing) {
            dialog!!.dismiss()
        }
    }
}