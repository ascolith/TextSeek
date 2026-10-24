package com.ascolith.textseek.feature.reportissue.ui.email

import android.net.Uri

fun interface IEmailLauncher {
    /**
     * Opens the user's email app with an report addressed to support.
     *
     * @return false if no email app is installed.
     */
    fun launch(subject: String, body: String, attachment: Uri?): Boolean
}
