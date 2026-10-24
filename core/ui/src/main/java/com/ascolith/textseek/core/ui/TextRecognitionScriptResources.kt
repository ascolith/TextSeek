package com.ascolith.textseek.core.ui

import androidx.annotation.StringRes
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.resources.R

@get:StringRes
val TextRecognitionScript.labelResId: Int
    get() = when (this) {
        TextRecognitionScript.LATIN -> R.string.text_recognition_script_latin
        TextRecognitionScript.CHINESE -> R.string.text_recognition_script_chinese
        TextRecognitionScript.DEVANAGARI -> R.string.text_recognition_script_devanagari
        TextRecognitionScript.JAPANESE -> R.string.text_recognition_script_japanese
        TextRecognitionScript.KOREAN -> R.string.text_recognition_script_korean
    }

/** A greeting written in the script, to show what it looks like. */
@get:StringRes
val TextRecognitionScript.sampleResId: Int
    get() = when (this) {
        TextRecognitionScript.LATIN -> R.string.text_recognition_script_sample_latin
        TextRecognitionScript.CHINESE -> R.string.text_recognition_script_sample_chinese
        TextRecognitionScript.DEVANAGARI -> R.string.text_recognition_script_sample_devanagari
        TextRecognitionScript.JAPANESE -> R.string.text_recognition_script_sample_japanese
        TextRecognitionScript.KOREAN -> R.string.text_recognition_script_sample_korean
    }
