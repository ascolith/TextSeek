package com.ascolith.textseek.feature.overview.ui.model

sealed interface TransientScreen {
    data object None : TransientScreen
    data object Loading : TransientScreen
    data class Error(val throwable: Throwable) : TransientScreen
}
