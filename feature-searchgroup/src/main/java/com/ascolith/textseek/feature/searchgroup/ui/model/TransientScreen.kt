package com.ascolith.textseek.feature.searchgroup.ui.model

sealed interface TransientScreen {
    data object None : TransientScreen
    data object Loading : TransientScreen

    /** The entity this screen was opened for does not exist any more. */
    data object NotFound : TransientScreen
    data class Error(val message: String) : TransientScreen
}
