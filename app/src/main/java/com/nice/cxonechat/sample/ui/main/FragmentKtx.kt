package com.nice.cxonechat.sample.ui.main

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Launch [repeatOnLifecycle] with supplied parameters using [Fragment.getViewLifecycleOwner]'s [lifecycleScope].
 */
fun Fragment.repeatOnViewOwnerLifecycle(state: State = State.RESUMED, block: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(state, block)
    }
}
