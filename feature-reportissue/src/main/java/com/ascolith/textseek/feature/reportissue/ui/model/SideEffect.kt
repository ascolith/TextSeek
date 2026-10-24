package com.ascolith.textseek.feature.reportissue.ui.model

sealed interface SideEffect {
    data object AttachLogsFailed : SideEffect
    data object NoEmailApp : SideEffect
}
