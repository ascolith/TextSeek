package com.ascolith.textseek.core.stringprovider

import android.content.Context
import com.ascolith.textseek.domain.feature.stringprovider.IStringProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidStringProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : IStringProvider {
    override fun getString(resId: Int): String = context.getString(resId)
    override fun getString(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
