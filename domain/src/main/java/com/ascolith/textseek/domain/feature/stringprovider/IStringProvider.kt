package com.ascolith.textseek.domain.feature.stringprovider

interface IStringProvider {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg args: Any): String
}
