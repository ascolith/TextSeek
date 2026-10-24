package com.ascolith.textseek.ui.navigation

import androidx.annotation.StringRes
import org.robolectric.RuntimeEnvironment

internal fun string(@StringRes id: Int, vararg formatArgs: Any): String =
    RuntimeEnvironment.getApplication().getString(id, *formatArgs)
