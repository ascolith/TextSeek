package com.ascolith.textseek.core.hardware.di

import com.ascolith.textseek.core.hardware.camera.CameraController
import com.ascolith.textseek.core.hardware.camera.ICameraController
import com.ascolith.textseek.core.hardware.camera.ITextImageAnalyzer
import com.ascolith.textseek.core.hardware.camera.TextImageAnalyzer
import com.ascolith.textseek.core.hardware.vibrator.VibratorController
import com.ascolith.textseek.domain.feature.vibration.IVibratorController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface HardwareModule {
    @Singleton
    @Binds
    fun bindVibratorController(vibratorController: VibratorController): IVibratorController

    @Singleton
    @Binds
    fun bindCameraController(cameraController: CameraController): ICameraController

    @Singleton
    @Binds
    fun bindTextImageAnalyzer(textImageAnalyzer: TextImageAnalyzer): ITextImageAnalyzer
}
