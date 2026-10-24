package com.ascolith.textseek.core.common.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val tsDispatcher: TsDispatcher)

enum class TsDispatcher {
    Default,
    IO,
}
