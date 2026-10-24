package com.ascolith.textseek.appfunctions

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import com.ascolith.textseek.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service entry point exposing TextSeek's Quick Search to on-device
 * assistants via the AppFunctions framework.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionServiceEntryPoint(
    serviceName = "TextSeekAppFunctionService",
    appFunctionXmlFileName = "text_seek_app_function_service",
)
abstract class BaseTextSeekAppFunctionService : AppFunctionService() {

    /**
     * Start a live visual text search: opens the camera viewfinder and highlights text matching
     * [query] on screen as the user points the camera at their surroundings. The search runs
     * against the live camera feed only, so the returned intent must be launched to the
     * foreground; there is no headless result. If camera permission is missing, the app requests
     * it and then starts the search automatically.
     *
     * @param query Text to find in the camera feed. Cannot be empty or blank.
     * @param caseSensitive Whether matching is case-sensitive. Defaults to false.
     * @param highlightColor Highlight box color: "YELLOW", "GREEN", "BLUE", "RED", "PINK", or
     * "ORANGE". If omitted, the app's default color is used.
     * @return A [PendingIntent] that opens the camera screen with the search already active.
     * @throws AppFunctionInvalidArgumentException If query is blank or highlightColor is
     * unsupported. If thrown, ask the user what text to search for or offer a supported color.
     * @throws AppFunctionAppUnknownException If the device has no camera. If thrown, tell the
     * user this feature needs a camera and cannot run on this device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun startQuickSearch(
        query: String,
        caseSensitive: Boolean = false,
        @AppFunctionStringValueConstraint(
            enumValues = ["YELLOW", "GREEN", "BLUE", "RED", "PINK", "ORANGE"],
        )
        highlightColor: String? = null,
    ): PendingIntent = withContext(Dispatchers.Default) {
        if (query.isBlank()) {
            throw AppFunctionInvalidArgumentException("query must not be blank")
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            throw AppFunctionAppUnknownException(
                "This device has no camera, and Quick Search requires a live camera feed.",
            )
        }
        val colorArgb = highlightColor?.toHighlightColorArgb()

        val launchId = System.nanoTime()
        val launchIntent = Intent(this@BaseTextSeekAppFunctionService, MainActivity::class.java)
            .setAction(MainActivity.ACTION_QUICK_SEARCH)
            // Extras aren't part of identity; without this, every call shares one PendingIntent
            .setIdentifier(launchId.toString())
            .putExtra(MainActivity.EXTRA_QUERY, query.trim())
            .putExtra(MainActivity.EXTRA_CASE_SENSITIVE, caseSensitive)
            // A per-invocation id, so the camera screen can tell this launch apart from
            // one it has already handled. It is stamped here rather than on delivery
            // because, carried in the Intent, it is restored alongside the rest of the
            // launch — which is what lets a new invocation still register after the app's
            // process has been killed and its task restored.
            .putExtra(MainActivity.EXTRA_LAUNCH_ID, launchId)
            // SINGLE_TOP + CLEAR_TOP make the intent reach onNewIntent when the
            // activity is already running; NEW_TASK alone would only bring the
            // existing task forward and drop the intent.
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        if (colorArgb != null) {
            launchIntent.putExtra(MainActivity.EXTRA_HIGHLIGHT_COLOR, colorArgb)
        }

        PendingIntent.getActivity(
            this@BaseTextSeekAppFunctionService,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun String.toHighlightColorArgb(): Int =
        HIGHLIGHT_COLORS[uppercase()]
            ?: throw AppFunctionInvalidArgumentException(
                "Unsupported highlightColor: $this. Use one of ${HIGHLIGHT_COLORS.keys}.",
            )

    companion object {
        private val HIGHLIGHT_COLORS = mapOf(
            "YELLOW" to 0xFFFFEB3B.toInt(),
            "GREEN" to 0xFF4CAF50.toInt(),
            "BLUE" to 0xFF2196F3.toInt(),
            "RED" to 0xFFF44336.toInt(),
            "PINK" to 0xFFE91E63.toInt(),
            "ORANGE" to 0xFFFF9800.toInt(),
        )
    }
}
