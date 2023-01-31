package com.nice.cxonechat.sample.ui.main

import android.R.layout
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.ThreadAdapter
import com.nice.cxonechat.sample.ThreadSelectedListener
import com.nice.cxonechat.sample.callback.SwipeToDeleteCallback
import com.nice.cxonechat.sample.databinding.CreateThreadDialogBinding
import com.nice.cxonechat.sample.databinding.FragmentChatThreadsBinding
import com.nice.cxonechat.sample.model.CreateThreadResult.Failure.GENERAL_FAILURE
import com.nice.cxonechat.sample.model.CreateThreadResult.Failure.REASON_THREADS_REFRESH_REQUIRED
import com.nice.cxonechat.sample.model.CreateThreadResult.Failure.REASON_THREAD_CREATION_FORBIDDEN
import com.nice.cxonechat.sample.model.CreateThreadResult.Success
import com.nice.cxonechat.sample.model.SpinnerOption
import com.nice.cxonechat.sample.model.Thread
import com.nice.cxonechat.sample.util.repeatOnViewOwnerLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatThreadsFragment : Fragment(), ThreadSelectedListener {

    private var threadAdapter: ThreadAdapter? = null

    private var binding: FragmentChatThreadsBinding? = null

    private val viewModel: ChatThreadsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val onCreateBinding = FragmentChatThreadsBinding.inflate(inflater, container, false)
        binding = onCreateBinding

        onCreateBinding.floatingActionButton.setupFab()

        val threadsRecyclerView = onCreateBinding.threadsRecyclerView
        val context = requireContext()
        threadAdapter = ThreadAdapter(ArrayList(), context, this)
        threadsRecyclerView.layoutManager = LinearLayoutManager(context)
        threadsRecyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        threadsRecyclerView.adapter = threadAdapter
        threadsRecyclerView.recycledViewPool
            .setMaxRecycledViews(0, 0) // Workaround for view duplication

        val swipeHandler = object : SwipeToDeleteCallback(context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = threadAdapter ?: return
                val threads = adapter.getThreads()
                val position = viewHolder.absoluteAdapterPosition
                val thread = threads[position]
                viewModel.archiveThread(thread.chatThread)
                // SDK currently doesn't notify about change in threads state, refresh needs to be done manually
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(threadsRecyclerView)

        onCreateBinding.toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            when {
                (checkedId == R.id.btn_current && isChecked) -> updateAdapter(true)
                (checkedId == R.id.btn_archive && isChecked) -> updateAdapter(false)
            }
        }
        updateAdapter(true)
        repeatOnViewOwnerLifecycle(State.STARTED) {
            viewModel.reportPageView()
        }
        return onCreateBinding.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshThreads()
    }

    private fun FloatingActionButton.setupFab() {
        if (viewModel.isMultiThreadEnabled) {
            setOnClickListener(::openDialog)
        } else {
            visibility = View.GONE
        }
    }

    private fun updateAdapter(showActiveThreads: Boolean) {
        repeatOnViewOwnerLifecycle {
            viewModel.threads.collect { chatThreads ->
                val threads = chatThreads
                    .filter { it.chatThread.canAddMoreMessages == showActiveThreads }
                threadAdapter?.setThreads(threads)
            }
        }
    }

    override fun threadSelected(thread: Thread) {
        viewModel.selectThread(thread.chatThread)
        val destination = ChatThreadsFragmentDirections.actionChatThreadsFragmentToChat()
        findNavController().navigate(destination)
    }

    private fun openDialog(view: View) {
        // TODO refactor
        val context = view.context
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Create conversation")
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
            ArrayAdapter<String>(context, layout.simple_spinner_dropdown_item, locationItems)
        val departmentsAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            context,
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
            dialog.dismiss()
        }

        createThreadButton.setOnClickListener {
            val customContactFields = mutableMapOf(
                "department" to selectedDepartment.name,
                "location" to selectedLocation.name,
            )
            lifecycleScope.launch {
                dialog.dismiss()
                createThread(customContactFields)
            }
        }
    }

    private suspend fun createThread(customContactFields: MutableMap<String, String>) {
        when (viewModel.createThread(customContactFields)) {
            REASON_THREADS_REFRESH_REQUIRED -> showAlert(R.string.warning_threads_refresh_required)
            REASON_THREAD_CREATION_FORBIDDEN -> showAlert(R.string.warning_thread_creation_forbidden)
            GENERAL_FAILURE -> showAlert(R.string.warning_general_failure)
            Success -> {
                val destination = ChatThreadsFragmentDirections.actionChatThreadsFragmentToChat()
                findNavController().navigate(destination)
            }
        }
    }

    private fun showAlert(@StringRes message: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.information)
        builder.setMessage(getString(message))

        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
