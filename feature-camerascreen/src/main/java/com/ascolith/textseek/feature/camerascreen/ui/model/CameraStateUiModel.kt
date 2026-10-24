package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.camera.core.SurfaceRequest
import com.ascolith.textseek.core.hardware.camera.CameraState

data class CameraStateUiModel(
    val surfaceRequest: SurfaceRequest? = null,
    val transformationInfo: SurfaceRequest.TransformationInfo? = null,
    val zoomLevel: Float = 0f,
    val torchEnabled: Boolean = false,
    val hasFlashUnit: Boolean = false,
)

fun CameraState.toUiModel() =
    CameraStateUiModel(
        surfaceRequest = surfaceRequest,
        transformationInfo = transformationInfo,
        zoomLevel = zoomLevel,
        torchEnabled = torchEnabled,
        hasFlashUnit = hasFlashUnit,
    )
