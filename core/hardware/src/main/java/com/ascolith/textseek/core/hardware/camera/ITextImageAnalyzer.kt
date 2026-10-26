package com.ascolith.textseek.core.hardware.camera

import androidx.camera.core.ImageAnalysis
import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.domain.model.TextRecognitionScript
import kotlinx.coroutines.flow.StateFlow

interface ITextImageAnalyzer : ImageAnalysis.Analyzer {
    val recognizedLinesFlow: StateFlow<List<RecognizedLine>>

    /** Takes effect from the next analyzed frame. */
    var script: TextRecognitionScript
}
