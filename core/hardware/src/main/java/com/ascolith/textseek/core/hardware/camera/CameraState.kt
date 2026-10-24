package com.ascolith.textseek.core.hardware.camera

import androidx.camera.core.Camera
import androidx.camera.core.SurfaceRequest

data class CameraState(
    val camera: Camera? = null,
    val surfaceRequest: SurfaceRequest? = null,
    val transformationInfo: SurfaceRequest.TransformationInfo? = null,
    val zoomLevel: Float = DEFAULT_ZOOM_LEVEL,
    val torchEnabled: Boolean = false,
    val hasFlashUnit: Boolean = false,
) {
    companion object {
        const val DEFAULT_ZOOM_LEVEL = 0.3f
    }
}
