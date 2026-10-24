package com.ascolith.textseek.feature.debug.ui.model

import androidx.annotation.StringRes

sealed interface SideEffect {
    data object Crash : SideEffect
    data class ShowMessage(@param:StringRes val messageResId: Int) : SideEffect
}
