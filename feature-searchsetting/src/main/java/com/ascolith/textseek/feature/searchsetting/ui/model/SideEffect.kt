package com.ascolith.textseek.feature.searchsetting.ui.model

sealed interface SideEffect {
    data object Exit : SideEffect
}
