package com.ascolith.textseek.feature.reportissue.ui.model

data class UiState(
    val description: String = "",
    val includeLogs: Boolean = true,
    val isSending: Boolean = false,
) {
    val canSend: Boolean get() = description.isNotBlank() && !isSending

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 5000
    }
}
