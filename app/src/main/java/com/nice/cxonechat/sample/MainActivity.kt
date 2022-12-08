package com.nice.cxonechat.sample

import android.R.layout
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.nice.cxonechat.sample.databinding.ActivityMainBinding
import com.nice.cxonechat.sample.databinding.CreateCustomerDialogBinding
import com.nice.cxonechat.sample.databinding.CreateThreadDialogBinding
import com.nice.cxonechat.sample.model.SpinnerOption
import com.nice.cxonechat.sample.ui.main.ChatViewModel
import com.nice.cxonechat.sample.ui.main.UserDetails
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    Toolbar.OnMenuItemClickListener {

    private var toolbar: Toolbar? = null

    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbarById = findViewById<Toolbar>(R.id.my_toolbar)
        toolbar = toolbarById
        toolbarById.title = ""
        setSupportActionBar(this.toolbar)

        toolbarById.inflateMenu(R.menu.default_menu)
        toolbarById.setOnMenuItemClickListener(this)

        lifecycleScope.launch {
            if (!chatViewModel.isMultiThreadEnabled) {
                if (!chatViewModel.isUserSetupRequired()) {
                    if (chatViewModel.isFirstThread()) {
                        openCreateThreadDialog()
                    } else {
                        startFragmentNavigation()
                    }
                } else {
                    showInitialDialog()
                }
            } else {
                startFragmentNavigation()
            }
        }
    }

    private fun startFragmentNavigation() {
        val navigationStart = if (chatViewModel.isMultiThreadEnabled) R.navigation.threads else R.navigation.chat
        val navHostFragment = NavHostFragment.create(navigationStart)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, navHostFragment)
            .setPrimaryNavigationFragment(navHostFragment)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        val controller = findNavController(R.id.nav_host_fragment)
        controller.addOnDestinationChangedListener { _, _, _ -> invalidateOptionsMenu() }
    }

//    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        // Don't allow navigating back if there is only a single thread
//        if (chatViewModel.isMultiThreadEnabled) {
//            super.onBackPressed()
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.default_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu == null) return super.onPrepareOptionsMenu(null)
        val navController = findNavController(R.id.nav_host_fragment)
        val isInChat = navController.currentDestination?.id == R.id.chatThreadFragment
        val isMultiThread = chatViewModel.isMultiThreadEnabled
        if (isInChat) {
            menu[0].isVisible = isMultiThread
            menu[1].isVisible = true
            menu[2].isVisible = !isMultiThread
        } else {
            menu[0].isVisible = false
            menu[1].isVisible = false
            menu[2].isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuItemClick(p0: MenuItem?): Boolean {
        when (p0?.title) {
            "Edit" -> showEditDialog()
            "Set Name" -> showEditThreadNameDialog()
            "SignOut" -> lifecycleScope.launch {
                chatViewModel.signOut()
                val intent = Intent(this@MainActivity, ConfigurationActivity::class.java).apply {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }
        return true
    }

    private fun showEditThreadNameDialog() {
        val builder = Builder(this)
        builder.setTitle("Update thread name")

        val threadNameEditText = EditText(this)
        threadNameEditText.hint = "Enter thread name"

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(threadNameEditText)
        builder.setView(layout)

        builder.setPositiveButton("Ok") { dialog, _ ->
            chatViewModel.setThreadName(threadNameEditText.text.toString())
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    @Suppress(
        "LongMethod" // TODO refactor
    )
    private fun showEditDialog() {
        val context = this
        lifecycleScope.launch {
            val userDetails = chatViewModel.getUserDetails()
            val firstNameEditText = EditText(context)
            firstNameEditText.setText(userDetails.firstName)
            firstNameEditText.hint = resources.getString(
                R.string.detail_hint_prefix,
                resources.getString(R.string.first_name).lowercase(),
            )

            val lastNameEditText = EditText(context)
            lastNameEditText.setText(userDetails.lastName)
            lastNameEditText.hint = resources.getString(
                R.string.detail_hint_prefix,
                resources.getString(R.string.last_name).lowercase(),
            )

            val emailEditText = EditText(context)
            emailEditText.hint = resources.getString(
                R.string.detail_hint_prefix,
                resources.getString(R.string.email).lowercase(),
            )

            val threadTopicEditText = EditText(context)
            threadTopicEditText.hint = resources.getString(R.string.thread_topic_hint)

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(firstNameEditText)
                addView(lastNameEditText)
                addView(emailEditText)
                addView(threadTopicEditText)
            }

            val builder = with(Builder(context)) {
                setTitle(resources.getString(R.string.change_details_label))
                setView(layout)
            }

            builder.setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
                val firstName = firstNameEditText.text.toString()
                val lastName = lastNameEditText.text.toString()
                val email = emailEditText.text.toString()
                val threadTopic = threadTopicEditText.text.toString()

                val customFields = mutableMapOf<String, String>()
                val contactCustomFields = mutableMapOf<String, String>()

                if (firstName.isBlank()) {
                    showAlert("Firstname can't be blank")
                    return@setPositiveButton
                }

                if (lastName.isBlank()) {
                    showAlert("Lastname can't be blank")
                    return@setPositiveButton
                }

                if (email.isNotEmpty()) {
                    customFields += "email" to email
                }

                if (threadTopic.isNotEmpty()) {
                    contactCustomFields += "request_type" to threadTopic
                }

                val newUserDetails = UserDetails(
                    firstName, lastName, customFields, contactCustomFields
                )

                lifecycleScope.launch {
                    chatViewModel.setUserDetails(newUserDetails)
                    dialog.dismiss()
                }
            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
        }
    }

    private fun showInitialDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.enter_your_details))
        builder.setCancelable(false)

        val binding = CreateCustomerDialogBinding.inflate(layoutInflater, null, false)
        builder.setView(binding.root)

        val contactClientDialog = builder.create()

        val firstNameTextInputLayout = binding.firstNameTextInputLayout
        val lastNameTextInputLayout = binding.lastNameTextInputLayout
        val acceptButton = binding.acceptButton

        acceptButton.setOnClickListener {
            val firstName = firstNameTextInputLayout.editText?.text?.toString().orEmpty()
            val lastName = lastNameTextInputLayout.editText?.text?.toString().orEmpty()

            lifecycleScope.launch {
                if (firstName.isNotBlank() && lastName.isNotBlank()) {
                    chatViewModel.setUserDetails(UserDetails(firstName, lastName))
                    contactClientDialog.dismiss()
                    openCreateThreadDialog()
                } else {
                    showAlert(getString(R.string.fields_cannot_be_empty))
                }
            }

        }

        contactClientDialog.show()
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

    private fun openCreateThreadDialog() {
        // TODO refactor
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Creating thread, please wait.")
        val dialogLayout = CreateThreadDialogBinding.inflate(layoutInflater)
        builder.setView(dialogLayout.root)

        val locationSpinner = dialogLayout.locationSpinner
        val departmentSpinner = dialogLayout.departmentSpinner
        val cancelButton = dialogLayout.cancelButton
        val createThreadButton = dialogLayout.createThread
        val locations = listOf(
            SpinnerOption(name = "West Coast", id = "WC"),
            SpinnerOption(name = "Northeast", id = "NE"),
            SpinnerOption(name = "Southeast", id = "SE"),
            SpinnerOption(name = "Midwest", id = "MW")
        )
        val departments = listOf(
            SpinnerOption(name = "Sales", id = "sales"),
            SpinnerOption(name = "Services", id = "services")
        )

        var selectedLocation = locations.first()
        var selectedDepartment = departments.first()

        val locationItems = locations.map { it.name }
        val departmentItems = departments.map { it.name }
        val locationsAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, layout.simple_spinner_dropdown_item, locationItems)
        val departmentsAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            layout.simple_spinner_dropdown_item,
            departmentItems
        )

        locationSpinner.adapter = locationsAdapter
        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                selectedLocation = locations[position]
            }
        }

        departmentSpinner.adapter = departmentsAdapter
        departmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                selectedDepartment = departments[position]
            }
        }

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()

        cancelButton.setOnClickListener {
            dialog.cancel()
            finish()
        }
        createThreadButton.setOnClickListener {
            val customContactFields = mutableMapOf(
                "department" to selectedDepartment.name,
                "location" to selectedLocation.name,
            )

            chatViewModel.createThread(customContactFields)
            dialog.dismiss()
            startFragmentNavigation()
        }
    }
}
