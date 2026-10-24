package com.ascolith.textseek.core

import com.ascolith.textseek.core.model.ScaffoldConfig

fun interface ConfigurableScaffold {
    fun getScaffoldConfig(): ScaffoldConfig
}
