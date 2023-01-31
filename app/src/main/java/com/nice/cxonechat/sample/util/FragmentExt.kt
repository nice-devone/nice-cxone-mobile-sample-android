package com.nice.cxonechat.sample.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Launch [repeatOnLifecycle] with supplied parameters using [Fragment.getViewLifecycleOwner]'s [lifecycleScope].
 *
 * @param state State on which the supplied [block] should be repeated, default is [State.RESUMED].
 * @param block Suspend function which should be launched in [androidx.lifecycle.LifecycleOwner.repeatOnLifecycle].
 */
internal fun Fragment.repeatOnViewOwnerLifecycle(
    state: State = State.RESUMED,
    block: suspend CoroutineScope.() -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(state, block)
    }
}
