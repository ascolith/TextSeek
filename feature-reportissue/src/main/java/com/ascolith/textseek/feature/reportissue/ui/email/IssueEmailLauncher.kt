package com.ascolith.textseek.feature.reportissue.ui.email

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.ascolith.textseek.resources.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The mailto selector limits the handlers to email apps while the ACTION_SEND intent itself still
 * carries the attachment. The intent must not declare a MIME type: the selector is resolved with
 * it, and no email app matches mailto plus a type.
 */
class IssueEmailLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : IEmailLauncher {

    override fun launch(subject: String, body: String, attachment: Uri?): Boolean {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.support_email)))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (attachment != null) {
                putExtra(Intent.EXTRA_STREAM, attachment)
                clipData = ClipData.newRawUri(null, attachment)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selector = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            // Started from the application context, which has no task of its own
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(emailIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
