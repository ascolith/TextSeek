package com.ascolith.textseek.feature.camerascreen.ui.di

import com.ascolith.textseek.feature.camerascreen.ui.matchextractor.ITextMatchExtractor
import com.ascolith.textseek.feature.camerascreen.ui.matchextractor.TextMatchExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CameraScreenModule {

    @Binds
    fun bindTextMatchExtractor(textMatchExtractor: TextMatchExtractor): ITextMatchExtractor
}
