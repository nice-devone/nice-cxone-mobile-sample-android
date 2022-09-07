package com.nice.cxonechat.sample

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.gson.Gson
import com.nice.cxonechat.CXOneChat
import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.enums.ErrorCode
import com.nice.cxonechat.listeners.*
import com.nice.cxonechat.models.visitor.ProactiveActionInfo
import com.nice.cxonechat.sample.CustomConfigurationFragment.Companion.BRAND_ID
import com.nice.cxonechat.sample.CustomConfigurationFragment.Companion.CHANNEL_ID
import com.nice.cxonechat.sample.HomeConfigurationFragment.Companion.ENVIRONMENT_SELECTED
import com.nice.cxonechat.sample.callback.SwipeToDeleteCallback
import com.nice.cxonechat.sample.model.SpinnerOption
import com.nice.cxonechat.sample.model.Thread
import com.nice.cxonechat.sample.service.PreferencesService
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*
import kotlin.collections.ArrayList


class HomeActivity : AppCompatActivity(), ThreadSelectedListener, Toolbar.OnMenuItemClickListener {

    private var toolbar: Toolbar? = null
    private var toggleButton: MaterialButtonToggleGroup? = null
    private var threadsRecyclerView: RecyclerView? = null
    private var floatingActionButton: FloatingActionButton? = null
    private var threadAdapter: ThreadAdapter? = null
    private var dialog: ProgressDialog? = null
    private var selectedLocation: SpinnerOption? = null
    private var selectedDepartment: SpinnerOption? = null
    private var isRecoveredThread = false
    private var threadList = mutableListOf<Thread>()
    private var archivedPosition: Int? = null
    private val environmentSelected: String? by lazy { intent.extras?.getString(
        ENVIRONMENT_SELECTED
    ) }
    private val brandId: String? by lazy { intent.extras?.getString(BRAND_ID) }
    private val channelId: String? by lazy { intent.extras?.getString(CHANNEL_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        this.toolbar = findViewById(R.id.my_toolbar)
        this.toggleButton = findViewById(R.id.toggleButton)
        this.toolbar?.title = ""
        setSupportActionBar(this.toolbar)

        this.toolbar!!.inflateMenu(R.menu.home_menu)
        this.toolbar!!.setOnMenuItemClickListener(this)

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
        threadsRecyclerView?.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
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
                archivedPosition = position
                CXOneChat.archiveThread(thread.id)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(threadsRecyclerView)

        showDialog()
        if (environmentSelected == "M&J") {
            CXOneChat.connect(
                "https://channels-eu1-qa.brandembassy.com/chat/",
                "wss://chat-gateway-eu1-qa.brandembassy.com",
                brandId.orEmpty().toInt(),
                channelId.orEmpty(),
                this
            )
        } else {
            CXOneChat.connect(
                CXOneEnvironment.NA1,
                brandId.orEmpty().toInt(),
                channelId.orEmpty(),
                this
            )
        }

        toggleButton?.addOnButtonCheckedListener { toggleButton, checkedId, isChecked ->
            when {
                (checkedId == R.id.button1 && isChecked) -> {
                    updateThread(true)
                }
                (checkedId == R.id.button2 && isChecked) -> {
                    updateThread(false)
                }
            }
        }

        Handler().postDelayed({
            hideDialog()
        }, 6000)
    }

    override fun onResume() {
        super.onResume()
        showDialog()
        java.lang.Thread.sleep(1500)
        CXOneChat.loadThreads()
        CXOneChat.setThreadLoadListener(threadLoadListener)
        CXOneChat.setAgentChangeListener(agentChangeListener)
        CXOneChat.setConnectedListener(connectedListener)
        CXOneChat.setThreadArchiveListener(threadArchiveListener)
        CXOneChat.setThreadsLoadListener(threadsLoadListener)
        CXOneChat.setThreadInfoLoadListener(threadInfoLoadListener)
        CXOneChat.setUnexpectedDisconnectListener(unexpectedDisconnectedListener)
        CXOneChat.setErrorListener(errorListener)
        CXOneChat.setProactivePopupActionListener(proactivePopupActionListener)
    }

    override fun onPause() {
        super.onPause()
        CXOneChat.unsetThreadLoadListener()
        CXOneChat.unsetAgentChangeListener()
        CXOneChat.unsetConnectedListener()
        CXOneChat.unsetThreadArchiveListener()
        CXOneChat.unsetThreadsLoadListener()
        CXOneChat.unsetThreadInfoLoadListener()
        CXOneChat.unsetUnexpectedDisconnectListener()
        CXOneChat.unsetErrorListener()
        CXOneChat.unsetProactivePopupActionListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onMenuItemClick(p0: MenuItem?): Boolean {
        when (p0?.title) {
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

    override fun threadSelected(thread: Thread) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("threadId", thread.id)
        intent.putExtra("isRecoveryThread", true)
        startActivity(intent)
    }

    private val threadLoadListener = ThreadLoadListener { _ ->
        runOnUiThread {
            isRecoveredThread = true
            hideDialog()
        }
    }

    private val agentChangeListener =
        AgentChangeListener { _, threadIdOnExternalPlatform ->
            CXOneChat.loadThread(threadIdOnExternalPlatform)
        }

    private val connectedListener =
        ConnectedListener {
            runOnUiThread {
                val isMultiThread = CXOneChat.channelConfig?.settings?.hasMultipleThreadsPerEndUser == true
                val customerConnection = CXOneChat.customerConnection
                PreferencesService.setFirstName(this, customerConnection?.firstName.orEmpty())
                PreferencesService.setLastName(this, customerConnection?.lastName.orEmpty())
                if (isMultiThread) {
                    CXOneChat.loadThreads()
                } else {
                    hideDialog()
                    floatingActionButton!!.visibility = View.GONE
                    verifyActiveThread()
                }
                CXOneChat.reportChatWindowOpen()
                CXOneChat.reportPageView("HomeActivity", "thread-view")

                // To manually execute a trigger, use the following line instead of reportPageView
//                CXOneChat.executeTrigger(UUID.fromString("1c3bf289-5885-43c9-91be-b92516a55dbe"))
            }
        }

    private val threadArchiveListener = ThreadArchiveListener {
        runOnUiThread {
            archivedPosition?.let {
                threadAdapter!!.removeAt(it)
                CXOneChat.loadThreads()
                hideDialog()
            }
            archivedPosition = null
        }
    }

    private val threadsLoadListener = ThreadsLoadListener { threads ->
        runOnUiThread {
            if (CXOneChat.threads.isNotEmpty()) {
                toggleButton?.check(R.id.button1)

                for (thread in CXOneChat.threads) {
                    CXOneChat.loadThreadInfo(thread.idOnExternalPlatform)
                }
            }
            hideDialog()
        }
    }

    private val threadInfoLoadListener = ThreadInfoLoadListener { _ ->
        runOnUiThread {
            updateThread(true)
        }
    }

    private val unexpectedDisconnectedListener = UnexpectedDisconnectListener {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                getString(R.string.socket_disconnected),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val proactivePopupActionListener = ProactivePopupActionListener {
        runOnUiThread {
            try {
                val variables = Gson().toJson(it)
                val jsonObject = JSONTokener(variables).nextValue() as JSONObject
                val headingText = jsonObject.getString("headingText")
                val bodyText = jsonObject.getString("bodyText")
                val action = jsonObject.getJSONObject("action")
                val actionText = action.getString("text")
                val actionUrl = action.getString("url")
                showSnackBar(headingText, bodyText, actionText, actionUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val errorListener = ErrorListener { errorCode, errorMessage ->
        when (errorCode) {
            ErrorCode.CustomerAuthorizationFailed -> {
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
        runOnUiThread {
            dialog!!.setMessage("Verifying threads, please wait.")
            dialog!!.setCancelable(false)
            dialog!!.show()
            Handler().postDelayed({
                CXOneChat.loadThread()
            }, 3000)

            Handler().postDelayed({
                showDialog()
                if (isRecoveredThread) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("isNewThread", true)
                    intent.putExtra("department", selectedDepartment)
                    intent.putExtra("location", selectedLocation)
                    intent.putExtra("isRecoveryThread", isRecoveredThread)
                    startActivity(intent)
                } else {
                    openDialog()
                }
            }, 6000)
        }
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
            intent.putExtra("firstName", PreferencesService.getFirstName(this))
            intent.putExtra("lastName", PreferencesService.getLastName(this))
            startActivity(intent)
        }
    }

    private fun showDialog() {
        dialog!!.setMessage("Loading, please wait.")
        dialog!!.setCancelable(false)
        dialog!!.show()
    }

    private fun hideDialog() {
        if (dialog!!.isShowing) {
            dialog!!.dismiss()
        }
    }

    private fun updateThread(isCurrent: Boolean) {
        threadList = mutableListOf()
        val isMultiThread = CXOneChat.channelConfig?.settings?.hasMultipleThreadsPerEndUser == true
        CXOneChat.threads.forEach {
            if (it.canAddMoreMessages == isCurrent) {
                threadList.add(
                    Thread(
                        it.idOnExternalPlatform,
                        if (isMultiThread) if (it.threadName.isNullOrEmpty()) "N/A" else it.threadName.orEmpty() else it.threadAgent?.fullName.orEmpty(),
                        it.messages.lastOrNull()?.messageContent?.payload?.text ?: "",
                        it.threadAgent?.imageUrl.orEmpty()
                    )
                )
            }
        }
        threadAdapter!!.addThreads(threadList as ArrayList<Thread>)
    }

    private fun showSnackBar(headingText: String, bodyText: String, actionText: String, actionUrl: String) {
        val parentLayout = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(parentLayout, "", Snackbar.LENGTH_INDEFINITE)
        val customSnackView: View = layoutInflater.inflate(R.layout.custom_snack_bar, null)
        snackbar.view.setBackgroundColor(Color.TRANSPARENT)

        val snackbarLayout = snackbar.view as SnackbarLayout
        snackbarLayout.setPadding(0, 0, 0, 0)

        val headingTextView: TextView = customSnackView.findViewById(R.id.headingTextView)
        val bodyTextView: TextView = customSnackView.findViewById(R.id.bodyTextView)
        val actionTextView: TextView = customSnackView.findViewById(R.id.actionTextView)
        val closeButton: ImageButton = customSnackView.findViewById(R.id.closeButton)

        headingTextView.text = headingText
        bodyTextView.text = bodyText
        actionTextView.text = actionText

        actionTextView.setOnClickListener {
            val proactiveActionInfo = ProactiveActionInfo(
                actionId = UUID.randomUUID(),
                actionType = "action-click",
                actionName = "custom-popup"
            )
            CXOneChat.reportProactiveActionClick(proactiveActionInfo)

            val proactiveActionSuccess = ProactiveActionInfo(
                actionId = UUID.randomUUID(),
                actionType = "action-success",
                actionName = "custom-popup"
            )
            CXOneChat.reportProactiveActionSuccess(proactiveActionSuccess)
        }

        closeButton.setOnClickListener {
            val proactiveActionInfo = ProactiveActionInfo(
                actionId = UUID.randomUUID(),
                actionType = "action-fail",
                actionName = "custom-popup"
            )
            CXOneChat.reportProactiveActionFail(proactiveActionInfo)
            snackbar.dismiss()
        }

        snackbarLayout.addView(customSnackView, 0)
        snackbar.show()

        val proactiveActionInfo = ProactiveActionInfo(
            actionId = UUID.randomUUID(),
            actionType = "action-display",
            actionName = "custom-popup"
        )
        CXOneChat.reportProactiveActionDisplay(proactiveActionInfo)
    }
}