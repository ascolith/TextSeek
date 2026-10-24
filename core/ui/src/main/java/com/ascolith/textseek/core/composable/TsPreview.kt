package com.ascolith.textseek.core.composable

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.ascolith.textseek.core.ui.TextSeekTheme

@Composable
fun TsPreview(
    content: @Composable () -> Unit,
) {
    TextSeekTheme {
        Surface {
            content()
        }
    }
}
