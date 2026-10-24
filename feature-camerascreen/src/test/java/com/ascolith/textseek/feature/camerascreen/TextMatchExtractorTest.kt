package com.ascolith.textseek.feature.camerascreen

import androidx.compose.ui.geometry.Rect as ComposeRect
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.domain.model.TsRect
import com.ascolith.textseek.feature.camerascreen.ui.matchextractor.TextMatchExtractor
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatchExtractorTest {

    private val extractor = TextMatchExtractor()

    @Test
    fun `matches a recognized text that contains the search term`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(recognizedText("gate B12")),
            searchSettings = listOf(searchSetting(text = "B12")),
        )

        assertEquals(listOf("gate B12"), matches.map { it.text })
    }

    @Test
    fun `matching ignores case unless the setting asks for it`() {
        val recognizedTexts = listOf(recognizedText("Gate B12"))

        val insensitive = extractor.extract(
            recognizedTexts = recognizedTexts,
            searchSettings = listOf(searchSetting(text = "gate", caseSensitive = false)),
        )
        assertEquals(1, insensitive.count())

        val sensitive = extractor.extract(
            recognizedTexts = recognizedTexts,
            searchSettings = listOf(searchSetting(text = "gate", caseSensitive = true)),
        )
        assertTrue(sensitive.isEmpty())
    }

    @Test
    fun `a case sensitive setting still matches an exact casing`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(recognizedText("Gate B12")),
            searchSettings = listOf(searchSetting(text = "Gate", caseSensitive = true)),
        )

        assertEquals(1, matches.count())
    }

    @Test
    fun `disabled settings are skipped`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(recognizedText("gate B12")),
            searchSettings = listOf(
                searchSetting(id = 1L, text = "gate", enabled = false),
                searchSetting(id = 2L, text = "B12", enabled = true),
            ),
        )

        assertEquals(1, matches.count())
    }

    @Test
    fun `settings with empty text are skipped rather than matching everything`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(recognizedText("gate B12")),
            searchSettings = listOf(searchSetting(text = "")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `each match carries the color of the setting that produced it`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(recognizedText("gate B12")),
            searchSettings = listOf(
                searchSetting(id = 1L, text = "gate", colorArgb = RED),
                searchSetting(id = 2L, text = "B12", colorArgb = GREEN),
            ),
        )

        assertEquals(listOf(RED, GREEN), matches.map { it.color })
    }

    @Test
    fun `one recognized text matching two settings yields a match per setting`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(recognizedText("gate B12")),
            searchSettings = listOf(
                searchSetting(id = 1L, text = "gate", colorArgb = RED),
                searchSetting(id = 2L, text = "B12", colorArgb = GREEN),
            ),
        )

        assertEquals(listOf("gate B12", "gate B12"), matches.map { it.text })
    }

    @Test
    fun `non matching text produces no matches`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(recognizedText("gate B12")),
            searchSettings = listOf(searchSetting(text = "baggage")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `no settings and no texts both produce no matches`() {
        assertTrue(
            extractor.extract(
                recognizedTexts = listOf(recognizedText("gate B12")),
                searchSettings = emptyList(),
            ).isEmpty()
        )
        assertTrue(
            extractor.extract(
                recognizedTexts = emptyList(),
                searchSettings = listOf(searchSetting(text = "gate")),
            ).isEmpty()
        )
    }

    @Test
    fun `the bounding box is carried across to the match`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(
                recognizedText("gate B12", boundingBox = TsRect(1f, 2f, 3f, 4f))
            ),
            searchSettings = listOf(searchSetting(text = "gate")),
        )

        assertEquals(ComposeRect(1f, 2f, 3f, 4f), matches.single().boundingBox)
    }

    private fun recognizedText(
        text: String,
        boundingBox: TsRect = TsRect(0f, 0f, 1f, 1f),
    ) = RecognizedText(
        text = text,
        boundingBox = boundingBox,
        angle = 0f,
    )

    private fun searchSetting(
        id: Long = 1L,
        text: String,
        caseSensitive: Boolean = false,
        enabled: Boolean = true,
        colorArgb: Int = RED,
    ) = SearchSettingUiModel(
        id = SearchSettingId(id),
        text = text,
        caseSensitive = caseSensitive,
        colorArgb = colorArgb,
        enabled = enabled,
    )

    private companion object {
        const val RED = 0xFFFF0000.toInt()
        const val GREEN = 0xFF00FF00.toInt()
    }
}
