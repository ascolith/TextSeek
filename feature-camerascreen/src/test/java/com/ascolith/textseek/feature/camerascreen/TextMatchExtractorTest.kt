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

    @Test
    fun `a multi word term matches consecutive words on the same line`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "flight", "to", "New", "York"),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertEquals(listOf("New York"), matches.map { it.text })
    }

    @Test
    fun `a multi word match is boxed around exactly the words it covers`() {
        val matches = extractor.extract(
            recognizedTexts = listOf(
                recognizedText("to", boundingBox = TsRect(0f, 10f, 5f, 20f)),
                recognizedText("New", boundingBox = TsRect(10f, 9f, 20f, 20f)),
                recognizedText("York", boundingBox = TsRect(25f, 10f, 40f, 22f)),
            ),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertEquals(ComposeRect(10f, 9f, 40f, 22f), matches.single().boundingBox)
    }

    @Test
    fun `a term that starts or ends mid word covers the partial words`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "gate", "B12", "boarding"),
            searchSettings = listOf(searchSetting(text = "ate B1")),
        )

        assertEquals(listOf("gate B12"), matches.map { it.text })
    }

    @Test
    fun `multi word matching ignores case unless the setting asks for it`() {
        val recognizedTexts = line(0, "New", "York")

        val insensitive = extractor.extract(
            recognizedTexts = recognizedTexts,
            searchSettings = listOf(searchSetting(text = "new york", caseSensitive = false)),
        )
        assertEquals(1, insensitive.count())

        val sensitive = extractor.extract(
            recognizedTexts = recognizedTexts,
            searchSettings = listOf(searchSetting(text = "new york", caseSensitive = true)),
        )
        assertTrue(sensitive.isEmpty())
    }

    @Test
    fun `surrounding and repeated whitespace in the term is ignored`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "New", "York"),
            searchSettings = listOf(searchSetting(text = "  New \t  York ")),
        )

        assertEquals(listOf("New York"), matches.map { it.text })
    }

    @Test
    fun `a whitespace only term is skipped rather than matching everything`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "New", "York"),
            searchSettings = listOf(searchSetting(text = "   ")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `a multi word term does not match words split across lines`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "flight", "to", "New") + line(1, "York", "JFK"),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `every occurrence of a term on a line yields its own match`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "New", "York", "to", "New", "York"),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertEquals(listOf("New York", "New York"), matches.map { it.text })
    }

    @Test
    fun `overlapping occurrences that start in different words each yield a match`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "ha", "ha", "ha"),
            searchSettings = listOf(searchSetting(text = "ha ha")),
        )

        assertEquals(listOf("ha ha", "ha ha"), matches.map { it.text })
    }

    @Test
    fun `repeated occurrences within one word yield a single match`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "aaaa"),
            searchSettings = listOf(searchSetting(text = "aa")),
        )

        assertEquals(1, matches.count())
    }

    @Test
    fun `single word terms still match on each line independently`() {
        val matches = extractor.extract(
            recognizedTexts = line(0, "gate", "B12") + line(1, "gate", "C4"),
            searchSettings = listOf(searchSetting(text = "gate")),
        )

        assertEquals(listOf("gate", "gate"), matches.map { it.text })
    }

    private fun line(lineIndex: Int, vararg words: String) =
        words.map { recognizedText(it, lineIndex = lineIndex) }

    private fun recognizedText(
        text: String,
        boundingBox: TsRect = TsRect(0f, 0f, 1f, 1f),
        lineIndex: Int = 0,
    ) = RecognizedText(
        text = text,
        boundingBox = boundingBox,
        angle = 0f,
        lineIndex = lineIndex,
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
