package com.ascolith.textseek.feature.onboarding.ui.model

import androidx.annotation.StringRes

sealed interface SideEffect {
    data class ShowMessage(@param:StringRes val messageResId: Int) : SideEffect
}
