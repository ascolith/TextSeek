package com.ascolith.textseek.core.model

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class TsScaffoldState {
    val snackbarHostState = SnackbarHostState()

    var config by mutableStateOf(ScaffoldConfig())
        private set

    var fabClickListener by mutableStateOf<(() -> Unit)?>(null)
        private set

    var actionClickListener by mutableStateOf<((Int) -> Unit)?>(null)
        private set

    // The destination the scaffold currently belongs to. A screen that is
    // animating out is still composed and can still recompose, so its updates
    // have to be dropped instead of overwriting the destination we moved to.
    private var owner by mutableStateOf<Any?>(null)

    /**
     * Hands the scaffold over to [newOwner] (the current destination) and resets it to
     * [newConfig]. Listeners are cleared because they belong to the screen we left.
     */
    fun updateOwner(newOwner: Any, newConfig: ScaffoldConfig) {
        if (newOwner == owner) return
        owner = newOwner
        config = newConfig
        fabClickListener = null
        actionClickListener = null
    }

    fun updateConfig(owner: Any, newConfig: ScaffoldConfig) {
        if (owner != this.owner) return
        config = newConfig
    }

    fun updateFabClickListener(owner: Any, listener: (() -> Unit)?) {
        if (owner != this.owner) return
        fabClickListener = listener
    }

    fun updateActionClickListener(owner: Any, listener: ((Int) -> Unit)?) {
        if (owner != this.owner) return
        actionClickListener = listener
    }
}

@Composable
fun rememberTsScaffoldState(): TsScaffoldState {
    return remember { TsScaffoldState() }
}
