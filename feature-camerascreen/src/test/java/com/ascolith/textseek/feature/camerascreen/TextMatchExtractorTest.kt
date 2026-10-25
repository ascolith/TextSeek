package com.ascolith.textseek.feature.camerascreen

import androidx.compose.ui.geometry.Rect as ComposeRect
import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.domain.model.TsPoint
import com.ascolith.textseek.domain.model.TsQuad
import com.ascolith.textseek.feature.camerascreen.ui.matchextractor.TextMatchExtractor
import com.ascolith.textseek.feature.camerascreen.ui.model.MatchOutline
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class TextMatchExtractorTest {

    private val extractor = TextMatchExtractor()

    @Test
    fun `matches a recognized word that contains the search term`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("gate", "B12")),
            searchSettings = listOf(searchSetting(text = "12")),
        )

        assertEquals(listOf("B12"), matches.map { it.text })
    }

    @Test
    fun `matching ignores case unless the setting asks for it`() {
        val recognizedLines = listOf(line("Gate", "B12"))

        val insensitive = extractor.extract(
            recognizedLines = recognizedLines,
            searchSettings = listOf(searchSetting(text = "gate", caseSensitive = false)),
        )
        assertEquals(1, insensitive.count())

        val sensitive = extractor.extract(
            recognizedLines = recognizedLines,
            searchSettings = listOf(searchSetting(text = "gate", caseSensitive = true)),
        )
        assertTrue(sensitive.isEmpty())
    }

    @Test
    fun `a case sensitive setting still matches an exact casing`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("Gate", "B12")),
            searchSettings = listOf(searchSetting(text = "Gate", caseSensitive = true)),
        )

        assertEquals(1, matches.count())
    }

    @Test
    fun `disabled settings are skipped`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("gate", "B12")),
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
            recognizedLines = listOf(line("gate", "B12")),
            searchSettings = listOf(searchSetting(text = "")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `each match carries the color of the setting that produced it`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("gate", "B12")),
            searchSettings = listOf(
                searchSetting(id = 1L, text = "gate", colorArgb = RED),
                searchSetting(id = 2L, text = "B12", colorArgb = GREEN),
            ),
        )

        assertEquals(listOf(RED, GREEN), matches.map { it.color })
    }

    @Test
    fun `one line matching two settings yields a match per setting`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("gate", "B12")),
            searchSettings = listOf(
                searchSetting(id = 1L, text = "gate", colorArgb = RED),
                searchSetting(id = 2L, text = "B12", colorArgb = GREEN),
            ),
        )

        assertEquals(listOf("gate", "B12"), matches.map { it.text })
    }

    @Test
    fun `non matching text produces no matches`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("gate", "B12")),
            searchSettings = listOf(searchSetting(text = "baggage")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `no settings and no texts both produce no matches`() {
        assertTrue(
            extractor.extract(
                recognizedLines = listOf(line("gate", "B12")),
                searchSettings = emptyList(),
            ).isEmpty()
        )
        assertTrue(
            extractor.extract(
                recognizedLines = emptyList(),
                searchSettings = listOf(searchSetting(text = "gate")),
            ).isEmpty()
        )
    }

    @Test
    fun `the bounding box is carried across to the match`() {
        val matches = extractor.extract(
            recognizedLines = listOf(
                line(
                    recognizedText("gate", outline = box(1f, 2f, 3f, 4f)),
                    recognizedText("B12", outline = box(5f, 2f, 8f, 4f)),
                )
            ),
            searchSettings = listOf(searchSetting(text = "gate")),
        )

        assertEquals(MatchOutline.of(ComposeRect(1f, 2f, 3f, 4f)), matches.single().outline)
    }

    @Test
    fun `a multi word term matches consecutive words on the same line`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("flight", "to", "New", "York")),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertEquals(listOf("New York"), matches.map { it.text })
    }

    @Test
    fun `a multi word match is boxed around exactly the words it covers`() {
        val matches = extractor.extract(
            recognizedLines = listOf(
                line(
                    recognizedText("to", outline = box(0f, 10f, 5f, 20f)),
                    recognizedText("New", outline = box(10f, 9f, 20f, 20f)),
                    recognizedText("York", outline = box(25f, 10f, 40f, 22f)),
                )
            ),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertEquals(MatchOutline.of(ComposeRect(10f, 9f, 40f, 22f)), matches.single().outline)
    }

    @Test
    fun `a term that starts or ends mid word covers the partial words`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("gate", "B12", "boarding")),
            searchSettings = listOf(searchSetting(text = "ate B1")),
        )

        assertEquals(listOf("gate B12"), matches.map { it.text })
    }

    @Test
    fun `multi word matching ignores case unless the setting asks for it`() {
        val recognizedLines = listOf(line("New", "York"))

        val insensitive = extractor.extract(
            recognizedLines = recognizedLines,
            searchSettings = listOf(searchSetting(text = "new york", caseSensitive = false)),
        )
        assertEquals(1, insensitive.count())

        val sensitive = extractor.extract(
            recognizedLines = recognizedLines,
            searchSettings = listOf(searchSetting(text = "new york", caseSensitive = true)),
        )
        assertTrue(sensitive.isEmpty())
    }

    @Test
    fun `surrounding and repeated whitespace in the term is ignored`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("New", "York")),
            searchSettings = listOf(searchSetting(text = "  New \t  York ")),
        )

        assertEquals(listOf("New York"), matches.map { it.text })
    }

    @Test
    fun `a whitespace only term is skipped rather than matching everything`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("New", "York")),
            searchSettings = listOf(searchSetting(text = "   ")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `a multi word term does not match words split across lines`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("flight", "to", "New"), line("York", "JFK")),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `every occurrence of a term on a line yields its own match`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("New", "York", "to", "New", "York")),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        assertEquals(listOf("New York", "New York"), matches.map { it.text })
    }

    @Test
    fun `overlapping occurrences that start in different words each yield a match`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("ha", "ha", "ha")),
            searchSettings = listOf(searchSetting(text = "ha ha")),
        )

        assertEquals(listOf("ha ha", "ha ha"), matches.map { it.text })
    }

    @Test
    fun `repeated occurrences within one word yield a single match`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("aaaa")),
            searchSettings = listOf(searchSetting(text = "aa")),
        )

        assertEquals(1, matches.count())
    }

    @Test
    fun `single word terms still match on each line independently`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("gate", "B12"), line("gate", "C4")),
            searchSettings = listOf(searchSetting(text = "gate")),
        )

        assertEquals(listOf("gate", "gate"), matches.map { it.text })
    }

    @Test
    fun `a CJK term matches across the elements ML Kit splits it into`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("東京", "駅", "3", "番", "線")),
            searchSettings = listOf(searchSetting(text = "東京駅")),
        )

        assertEquals(listOf("東京駅"), matches.map { it.text })
    }

    @Test
    fun `spaces typed between CJK characters in a term are ignored`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("東京", "駅", "3", "番", "線")),
            searchSettings = listOf(searchSetting(text = "駅 3 番")),
        )

        assertEquals(listOf("駅3番"), matches.map { it.text })
    }

    @Test
    fun `every CJK hit that reaches different words yields its own match`() {
        val matches = extractor.extract(
            recognizedLines = listOf(line("東京", "京", "京")),
            searchSettings = listOf(searchSetting(text = "京京")),
        )

        assertEquals(listOf("東京京", "京京"), matches.map { it.text })
    }

    @Test
    fun `scripts that space their words still need the space`() {
        val recognizedLines = listOf(line("서울", "역"))

        val spaced = extractor.extract(
            recognizedLines = recognizedLines,
            searchSettings = listOf(searchSetting(text = "서울 역")),
        )
        assertEquals(listOf("서울 역"), spaced.map { it.text })

        val unspaced = extractor.extract(
            recognizedLines = recognizedLines,
            searchSettings = listOf(searchSetting(text = "서울역")),
        )
        assertTrue(unspaced.isEmpty())
    }

    @Test
    fun `a tilted multi word match is outlined along the text`() {
        val matches = extractor.extract(
            recognizedLines = listOf(
                line(
                    recognizedText("New", outline = box(0f, 0f, 30f, 10f).rotated(TILT_DEGREES)),
                    recognizedText("York", outline = box(35f, -1f, 75f, 11f).rotated(TILT_DEGREES)),
                )
            ),
            searchSettings = listOf(searchSetting(text = "New York")),
        )

        // An upright box around the same words would be about 35 units tall rather than 12.
        val expected = box(0f, -1f, 75f, 11f).rotated(TILT_DEGREES).corners
        val actual = matches.single().outline.run { listOf(topLeft, topRight, bottomRight, bottomLeft) }
        expected.zip(actual).forEach { (expectedCorner, actualCorner) ->
            assertEquals(expectedCorner.x, actualCorner.x, TOLERANCE)
            assertEquals(expectedCorner.y, actualCorner.y, TOLERANCE)
        }
    }

    private fun line(vararg words: String) = line(*words.map { recognizedText(it) }.toTypedArray())

    private fun line(vararg words: RecognizedText) = RecognizedLine(words.toList())

    private fun recognizedText(
        text: String,
        outline: TsQuad = box(0f, 0f, 1f, 1f),
    ) = RecognizedText(
        text = text,
        outline = outline,
        angle = 0f,
    )

    private fun box(left: Float, top: Float, right: Float, bottom: Float) = TsQuad(
        topLeft = TsPoint(left, top),
        topRight = TsPoint(right, top),
        bottomRight = TsPoint(right, bottom),
        bottomLeft = TsPoint(left, bottom),
    )

    private fun TsQuad.rotated(degrees: Float): TsQuad {
        val radians = Math.toRadians(degrees.toDouble())
        val cos = cos(radians).toFloat()
        val sin = sin(radians).toFloat()
        fun TsPoint.rotated() = TsPoint(x * cos - y * sin, x * sin + y * cos)
        return TsQuad(topLeft.rotated(), topRight.rotated(), bottomRight.rotated(), bottomLeft.rotated())
    }

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
        const val TILT_DEGREES = 20f
        const val TOLERANCE = 0.001f
    }
}
