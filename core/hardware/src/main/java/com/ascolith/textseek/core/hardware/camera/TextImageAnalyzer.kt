package com.ascolith.textseek.core.hardware.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import co.touchlab.kermit.Logger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.domain.model.TsPoint
import com.ascolith.textseek.domain.model.TsQuad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor
import javax.inject.Inject

class TextImageAnalyzer @Inject constructor(
    private val cameraExecutor: Executor,
) : ImageAnalysis.Analyzer, ITextImageAnalyzer {

    private val _textFoundFlow = MutableStateFlow<List<RecognizedLine>>(emptyList())
    override val recognizedLinesFlow: StateFlow<List<RecognizedLine>> = _textFoundFlow

    @Volatile
    override var script: TextRecognitionScript = TextRecognitionScript.LATIN

    // Only touched from analyze(), so a swap never closes a recognizer with a frame in flight.
    private var recognizerScript: TextRecognitionScript? = null
    private var recognizer: TextRecognizer? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            Logger.e { "Error: image is null" }
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val uprightToSensorMatrix = createUprightToSensorMatrix(imageProxy)

        // ML Kit does not rotate the pixels when given a rotation hint; it recognizes the
        // sideways text in place and maps the coordinates afterward, which produces loose,
        // offset bounding boxes (~30% oversized at rotation 90). Rotate the frame ourselves
        // so the recognizer always sees upright text and returns tight boxes.
        val image = if (rotationDegrees == 0) {
            InputImage.fromMediaImage(mediaImage, 0)
        } else {
            InputImage.fromBitmap(imageProxy.toBitmap().rotate(rotationDegrees), 0)
        }

        recognizerFor(script).process(image)
            .addOnSuccessListener(cameraExecutor) { visionText ->
                _textFoundFlow.value = visionText.textBlocks
                    .flatMap { it.lines }
                    .mapNotNull { line ->
                        val words = line.elements.mapNotNull {
                            val outline = it.getOutlineAsTsQuad(uprightToSensorMatrix) ?: return@mapNotNull null
                            RecognizedText(
                                text = it.text,
                                outline = outline,
                                angle = it.angle,
                            )
                        }
                        words.takeIf { it.isNotEmpty() }?.let(::RecognizedLine)
                    }
            }
            .addOnFailureListener(cameraExecutor) { e ->
                Logger.e { "Error: ${e.message}" }
            }
            .addOnCompleteListener(cameraExecutor) {
                imageProxy.close()
            }
    }

    private fun recognizerFor(script: TextRecognitionScript): TextRecognizer {
        val current = recognizer
        if (current != null && recognizerScript == script) return current
        current?.close()
        return TextRecognition.getClient(script.toRecognizerOptions()).also {
            recognizer = it
            recognizerScript = script
        }
    }

    private fun TextRecognitionScript.toRecognizerOptions(): TextRecognizerOptionsInterface =
        when (this) {
            TextRecognitionScript.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
            TextRecognitionScript.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            TextRecognitionScript.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            TextRecognitionScript.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            TextRecognitionScript.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
        }

    private fun Bitmap.rotate(degrees: Int): Bitmap =
        Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            Matrix().apply { postRotate(degrees.toFloat()) },
            true,
        )

    // ML Kit's corner points follow the word's tilt, clockwise from its top left, where the
    // bounding box is the upright box around them. Fall back to the box if they're missing.
    private fun Text.Element.getOutlineAsTsQuad(uprightToSensorMatrix: Matrix): TsQuad? {
        val corners = cornerPoints?.takeIf { it.size == 4 }
            ?.flatMap { listOf(it.x.toFloat(), it.y.toFloat()) }
            ?: boundingBox?.run {
                listOf(left, top, right, top, right, bottom, left, bottom).map { it.toFloat() }
            }
            ?: return null
        val points = corners.toFloatArray()
        uprightToSensorMatrix.mapPoints(points)
        return TsQuad(
            topLeft = TsPoint(points[0], points[1]),
            topRight = TsPoint(points[2], points[3]),
            bottomRight = TsPoint(points[4], points[5]),
            bottomLeft = TsPoint(points[6], points[7]),
        )
    }

    // ML Kit returns coordinates in the upright (rotated) frame. Downstream consumers map
    // them with the preview's sensor-to-buffer transform, so they must be published in
    // sensor coordinates: upright -> analysis buffer (undo rotation) -> sensor. Going
    // through the sensor frame keeps the boxes correct even though the analysis and
    // preview streams run at different resolutions.
    private fun createUprightToSensorMatrix(imageProxy: ImageProxy): Matrix {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix().apply {
            setRotate(-rotationDegrees.toFloat())
            when (rotationDegrees) {
                90 -> postTranslate(0f, imageProxy.height.toFloat())
                180 -> postTranslate(imageProxy.width.toFloat(), imageProxy.height.toFloat())
                270 -> postTranslate(imageProxy.width.toFloat(), 0f)
            }
        }
        val bufferToSensorMatrix = Matrix()
        if (imageProxy.imageInfo.sensorToBufferTransformMatrix.invert(bufferToSensorMatrix)) {
            matrix.postConcat(bufferToSensorMatrix)
        }
        return matrix
    }
}
