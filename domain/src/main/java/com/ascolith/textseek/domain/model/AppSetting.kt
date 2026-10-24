package com.ascolith.textseek.domain.model

import com.ascolith.textseek.domain.model.TextRecognitionScript as Script

/**
 * A user setting, stored as a string under [key] using [encode] and [decode].
 *
 * To add a setting, declare an object here **and** add it to [all]. Backup export and import cover
 * exactly the settings in [all], and `AppSettingTest` fails if a subclass is missing from it.
 */
sealed class AppSetting<T>(
    val key: String,
    val defaultValue: T,
) {

    abstract fun encode(value: T): String

    /** Returns null when [stored] is not a valid value, so readers fall back to [defaultValue]. */
    abstract fun decode(stored: String): T?

    /**
     * The app-wide default indicator color, as opposed to a search group's own default indicator
     * color. Stored as `#AARRGGBB`, e.g. `#FFFFFF00`.
     */
    data object GlobalDefaultIndicatorColor : AppSetting<Int>(
        key = "global_default_indicator_color",
        defaultValue = SearchGroup.DEFAULT_COLOR_ARGB,
    ) {
        private const val ARGB_HEX_LENGTH = 8
        private const val HEX_RADIX = 16

        override fun encode(value: Int): String =
            "#" + value.toUInt().toString(HEX_RADIX).padStart(ARGB_HEX_LENGTH, '0').uppercase()

        override fun decode(stored: String): Int? {
            val hex = stored.removePrefix("#")
            if (hex.length != ARGB_HEX_LENGTH || !hex.all { it.digitToIntOrNull(HEX_RADIX) != null }) return null
            return hex.toUInt(HEX_RADIX).toInt()
        }
    }

    /** The script the camera recognizes text in. Stored as the [Script] name, e.g. `LATIN`. */
    data object TextRecognitionScript : AppSetting<Script>(
        key = "text_recognition_script",
        defaultValue = Script.LATIN,
    ) {
        override fun encode(value: Script): String = value.name

        override fun decode(stored: String): Script? =
            Script.entries.firstOrNull { it.name == stored }
    }

    companion object {
        /**
         * Every setting. Lazy because an eagerly built list could read an object that is still
         * being initialized (the objects are subclasses of this class) and hold null in its place.
         */
        val all: List<AppSetting<*>> by lazy { listOf(GlobalDefaultIndicatorColor, TextRecognitionScript) }
    }
}
