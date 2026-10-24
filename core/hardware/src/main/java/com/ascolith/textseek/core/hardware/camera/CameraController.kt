package com.ascolith.textseek.core.hardware.camera

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Display
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.ZoomState
import androidx.camera.core.sessionConfig
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import arrow.core.Either
import co.touchlab.kermit.Logger
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

interface ICameraController {
    val cameraStateFlow: StateFlow<CameraState>

    suspend fun bindToCamera(
        lifecycleOwner: LifecycleOwner,
        imageAnalyzer: ImageAnalysis.Analyzer,
    )

    suspend fun zoom(zoomLevel: Float)
    suspend fun setTorchEnabled(enabled: Boolean)
}

@Singleton
class CameraController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cameraExecutor: Executor,
) : ICameraController {

    private val _cameraStateFlow = MutableStateFlow(CameraState())
    override val cameraStateFlow: StateFlow<CameraState> = _cameraStateFlow

    override suspend fun bindToCamera(
        lifecycleOwner: LifecycleOwner,
        imageAnalyzer: ImageAnalysis.Analyzer,
    ) {
        val cameraPreviewUseCase = createCameraPreviewUseCase(createPreviewResolutionSelector())
        val analyzer = createImageAnalyzerUseCase(createAnalysisResolutionSelector())

        analyzer.setAnalyzer(cameraExecutor, imageAnalyzer)

        // Auto-rotation only covers the analyzer; the preview follows the display.
        val sessionConfig = sessionConfig(listOf(cameraPreviewUseCase, analyzer)) {
            isAutoRotationEnabled = true
        }
        val processCameraProvider = ProcessCameraProvider.awaitInstance(context)
        val camera = processCameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            sessionConfig,
        )
        val hasFlashUnit = camera.cameraInfo.hasFlashUnit()
        _cameraStateFlow.value = _cameraStateFlow.value.copy(
            camera = camera,
            hasFlashUnit = hasFlashUnit,
        )

        // Detaching (e.g. activity recreation on rotation) resets zoom and torch, so restore them.
        withCameraControl { it?.setLinearZoom(_cameraStateFlow.value.zoomLevel) }
        if (_cameraStateFlow.value.torchEnabled) {
            withCameraControl { it?.enableTorch(true) }
        }

        // Zoom can change outside this controller (e.g. the viewfinder's pinch-to-zoom
        // gesture calls setZoomRatio directly), so mirror the camera's own zoom state.
        // Lifecycle-scoped so the reset on stop isn't mirrored.
        val zoomObserver = Observer<ZoomState> { zoomState ->
            _cameraStateFlow.value = _cameraStateFlow.value.copy(zoomLevel = zoomState.linearZoom)
        }
        camera.cameraInfo.zoomState.observe(lifecycleOwner, zoomObserver)

        val displayManager = context.getSystemService(DisplayManager::class.java)
        val displayListener = createDisplayRotationListener(displayManager, cameraPreviewUseCase)
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

        // Cancellation signals we're done with the camera
        try {
            awaitCancellation()
        } finally {
            displayManager.unregisterDisplayListener(displayListener)
            camera.cameraInfo.zoomState.removeObserver(zoomObserver)
            analyzer.clearAnalyzer()
            // Not unbindAll: a recreated screen may already have bound its own session.
            processCameraProvider.unbind(sessionConfig)
        }
    }

    // 180° turns don't recreate the activity, so the preview isn't rebuilt for them.
    private fun createDisplayRotationListener(
        displayManager: DisplayManager,
        preview: Preview,
    ) = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            val rotation = displayManager.getDisplay(displayId)?.rotation ?: return
            preview.targetRotation = rotation
        }
    }

    private fun createImageAnalyzerUseCase(resolutionSelector: ResolutionSelector) =
        ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

    private fun createCameraPreviewUseCase(resolutionSelector: ResolutionSelector): Preview =
        Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .apply {
                setSurfaceProvider { newSurfaceRequest ->
                    handleCameraNewSurfaceRequest(newSurfaceRequest)
                }
            }

    private fun createPreviewResolutionSelector(): ResolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()

    // Analysis frames feed ML Kit, whose latency scales with pixel count; a full-sensor
    // frame is several times slower to recognize than this size without a meaningful
    // accuracy win for on-screen text.
    private fun createAnalysisResolutionSelector(): ResolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    ANALYSIS_TARGET_SIZE,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                )
            )
            .build()

    override suspend fun zoom(zoomLevel: Float) {
        val result = withCameraControl { it?.setLinearZoom(zoomLevel) }
        if (result.isLeft() && result.leftOrNull() !is CameraControl.OperationCanceledException) {
            Logger.e { "Failed to set zoom level: ${result.leftOrNull()?.message}}" }
        }
        _cameraStateFlow.value = _cameraStateFlow.value.copy(zoomLevel = zoomLevel)
    }

    private fun handleCameraNewSurfaceRequest(surfaceRequest: SurfaceRequest) {
        surfaceRequest.setTransformationInfoListener(Runnable::run) { transformationInfo ->
            _cameraStateFlow.value = _cameraStateFlow.value.copy(transformationInfo = transformationInfo)
        }
        _cameraStateFlow.value = _cameraStateFlow.value.copy(surfaceRequest = surfaceRequest)
    }

    override suspend fun setTorchEnabled(enabled: Boolean) {
        val result = withCameraControl { it?.enableTorch(enabled) }
        if (result.isLeft() && result.leftOrNull() !is CameraControl.OperationCanceledException) {
            Logger.e { "Failed to set torch: ${result.leftOrNull()?.message}" }
        }
        _cameraStateFlow.value = _cameraStateFlow.value.copy(torchEnabled = enabled)
    }

    private suspend fun withCameraControl(
        block: (CameraControl?) -> ListenableFuture<Void>?,
    ): Either<Throwable, Unit> =
        Either.catch {
            block(_cameraStateFlow.value.camera?.cameraControl)?.await()
        }

    companion object {
        // Expressed in landscape sensor orientation, per ResolutionStrategy convention.
        private val ANALYSIS_TARGET_SIZE = Size(1920, 1440)
    }
}
