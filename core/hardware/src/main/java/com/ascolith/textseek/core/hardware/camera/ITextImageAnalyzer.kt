package com.ascolith.textseek.core.hardware.camera

import androidx.camera.core.ImageAnalysis
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.domain.model.TextRecognitionScript
import kotlinx.coroutines.flow.StateFlow

interface ITextImageAnalyzer : ImageAnalysis.Analyzer {
    val recognizedTextsFlow: StateFlow<List<RecognizedText>>

    /** Takes effect from the next analyzed frame. */
    var script: TextRecognitionScript
}
