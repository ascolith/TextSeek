package com.ascolith.textseek.core.composable

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun TsSettingTextField(
    initialText: String,
    label: @Composable () -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    strikeThroughText: Boolean = false,
    autoFocus: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
            )
        )
    }
    LaunchedEffect(textFieldValue) {
        onValueChange(textFieldValue.text)
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    OutlinedTextField(
        modifier = modifier.focusRequester(focusRequester),
        value = textFieldValue,
        label = label,
        singleLine = true,

        textStyle = if (strikeThroughText) {
            LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
        } else {
            LocalTextStyle.current
        },
        onValueChange = {
            textFieldValue = it
        },
    )
}
