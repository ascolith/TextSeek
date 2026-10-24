package com.ascolith.textseek.domain.feature.time

import kotlin.time.Instant

fun interface ITimeProvider {
    fun now(): Instant
}
