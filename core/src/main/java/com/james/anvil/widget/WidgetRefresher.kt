package com.james.anvil.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Abstraction for refreshing home screen widgets.
 * Implemented in :app where the actual widget classes live.
 * This allows :feature ViewModels to trigger widget refreshes without
 * depending on the :app module.
 */
object WidgetRefresher {
    private var refreshAction: (suspend (Context) -> Unit)? = null

    /** Called from :app during initialization to register the actual refresh implementation. */
    fun init(action: suspend (Context) -> Unit) {
        refreshAction = action
    }

    /** Refresh all widgets. Safe to call even if no implementation is registered. */
    fun refreshAll(context: Context) {
        refreshAction?.let { action ->
            CoroutineScope(Dispatchers.IO).launch { action(context) }
        }
    }
}
