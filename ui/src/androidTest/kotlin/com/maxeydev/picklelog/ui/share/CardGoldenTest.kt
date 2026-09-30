package com.maxeydev.picklelog.ui.share

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

private const val MINIMUM_CONTRAST = 4.5
private const val BACKGROUND_SAMPLE_X = 24
private const val BADGE_INSET = 10
private const val COLOUR_TOLERANCE = 30
private val BOTTOM_TEXT = listOf("result", "meta", "opponents", "partner", "score", "location", "streak")
private val TOP_TEXT = listOf("brand", "name")

@RunWith(AndroidJUnit4::class)
class CardGoldenTest {
    private lateinit var warmer: WebViewWarmer
    private lateinit var renderer: CardRenderer

    @Before
    fun setUp() {
        warmer = onMain { WebViewWarmer(targetContext).also { it.warm() } }
        renderer = CardRenderer(warmer)
    }

    @After
    fun tearDown() {
        onMain { warmer.discard() }
    }

    private fun render(
        data: CardData,
        golden: String? = null,
    ): Pair<Bitmap, JSONObject> {
        val result = runBlocking { renderer.render(data) }
        assertTrue("render failed: $result", result is CardRenderResult.Rendered)
        val rendered = result as CardRenderResult.Rendered
        golden?.let { saveForReview(it, rendered.bitmap) }
        return rendered.bitmap to JSONObject(rendered.diagnostics)
    }

    private fun contrastBehindText(
        theme: CardTheme,
        background: Int,
    ): Double =
        when (theme) {
            CardTheme.DARK, CardTheme.COURT, CardTheme.SUNSET -> contrastAgainstWhite(background)
            CardTheme.LIGHT -> contrastBetween(background, LIGHT_THEME_TEXT)
        }

    private fun assertLegible(
        bitmap: Bitmap,
        diagnostics: JSONObject,
        theme: CardTheme = CardTheme.DARK,
    ) {
        assertEquals(diagnostics.toString(), 0, diagnostics.getJSONArray("overflowing").length())
        val rects = diagnostics.getJSONObject("rects")
        (BOTTOM_TEXT + TOP_TEXT).filter { rects.has(it) }.forEach { id ->
            val rect = diagnostics.rect(id)
            val top = rect.getDouble("top").toInt().coerceAtLeast(0)
            val bottom = rect.getDouble("bottom").toInt().coerceAtMost(bitmap.height - 1)
            for (y in top..bottom step 4) {
                val contrast = contrastBehindText(theme, bitmap.getPixel(BACKGROUND_SAMPLE_X, y))
                assertTrue("contrast $contrast behind '$id' at y=$y; $diagnostics", contrast >= MINIMUM_CONTRAST)
            }
        }
    }

    @Test
    fun the_card_is_exactly_1080_by_1920_with_design_pixels_pinned_to_output_pixels() {
        val (bitmap, diagnostics) = render(sampleCard())

        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
        val badge = diagnostics.rect("result")
        val insideBadgeX = badge.getDouble("left").toInt() + BADGE_INSET
        val badgeMiddleY = ((badge.getDouble("top") + badge.getDouble("bottom")) / 2).toInt()
        val pixel = bitmap.getPixel(insideBadgeX, badgeMiddleY)
        val expected = Color.rgb(0x1b, 0x7a, 0x3d)
        val distance =
            abs(Color.red(pixel) - Color.red(expected)) +
                abs(Color.green(pixel) - Color.green(expected)) +
                abs(Color.blue(pixel) - Color.blue(expected))
        assertTrue(
            "the badge the page reports at design ($insideBadgeX, $badgeMiddleY) is not at that output pixel: " +
                "found #${Integer.toHexString(pixel)}; diagnostics $diagnostics",
            distance <= COLOUR_TOLERANCE,
        )
        assertTrue("badge ${badge.getDouble("right")} runs off the card", badge.getDouble("right") <= 1080.0)
    }

    @Test
    fun a_second_card_from_the_same_webview_shows_the_new_match_not_the_previous_one() {
        render(sampleCard(isWin = true))

        val (bitmap, diagnostics) = render(sampleCard(isWin = false, opponents = "vs Zed"))

        val badge = diagnostics.rect("result")
        val pixel =
            bitmap.getPixel(
                badge.getDouble("left").toInt() + BADGE_INSET,
                ((badge.getDouble("top") + badge.getDouble("bottom")) / 2).toInt(),
            )
        val lossRed = Color.rgb(0x8a, 0x23, 0x30)
        val distance =
            abs(Color.red(pixel) - Color.red(lossRed)) +
                abs(Color.green(pixel) - Color.green(lossRed)) +
                abs(Color.blue(pixel) - Color.blue(lossRed))
        assertTrue(
            "the second capture still shows the first card: badge is #${Integer.toHexString(pixel)}",
            distance <= COLOUR_TOLERANCE,
        )
    }

    @Test
    fun a_card_with_no_photo_is_legible() {
        val (bitmap, diagnostics) = render(sampleCard(photo = null))

        assertFalse(diagnostics.getBoolean("photoShown"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun a_card_over_a_very_dark_photo_is_legible() {
        val (bitmap, diagnostics) = render(sampleCard(photo = photoDataUri(base = 12, spread = 10)))

        assertTrue(diagnostics.getBoolean("photoShown"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun a_card_over_a_very_light_photo_is_legible() {
        val (bitmap, diagnostics) = render(sampleCard(photo = photoDataUri(base = 245, spread = 10)))

        assertTrue(diagnostics.getBoolean("photoShown"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun long_names_and_locations_wrap_or_truncate_inside_the_card() {
        val longName = "Maximiliano Alejandro de la Cruz-Villanueva y Santisteban"
        val (bitmap, diagnostics) =
            render(
                sampleCard(
                    displayName = longName,
                    opponents = "vs $longName & Bartholomew Montgomery-Fitzgerald III",
                    location = "The Community Recreation Center Pickleball Courts, North Wing, Building 7",
                    photo = photoDataUri(base = 245, spread = 10),
                ),
            )

        assertLegible(bitmap, diagnostics)
        val opponents = diagnostics.rect("opponents")
        assertTrue(opponents.getDouble("right") <= 1080.0)
        assertTrue(diagnostics.rect("location").getDouble("right") <= 1080.0)
    }

    @Test
    fun accented_names_and_emoji_render_as_given() {
        val opponents = "vs José 🏓 Ñuñez & Zoë Łukasiewicz-李"
        val (bitmap, diagnostics) = render(sampleCard(opponents = opponents, displayName = "Mãe 🥒"))

        assertEquals(opponents, diagnostics.getJSONObject("texts").getString("opponents"))
        assertEquals("Mãe 🥒", diagnostics.getJSONObject("texts").getString("name"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun a_doubles_card_shows_both_opponents_and_the_result_as_a_word() {
        val (_, diagnostics) = render(sampleCard(opponents = "vs Ana & Ben", isWin = false))

        val texts = diagnostics.getJSONObject("texts")
        assertEquals("vs Ana & Ben", texts.getString("opponents"))
        assertTrue(texts.getString("result").contains("Loss"))
    }

    @Test
    fun hostile_characters_in_a_name_stay_text_and_do_not_change_the_page() {
        val baseline = render(sampleCard()).second.getInt("elementCount")
        val hostile = "Dave \"Q\" O'Neil \\ </script><img src=x onerror=alert(1)><b>bold</b>"

        val (_, diagnostics) = render(sampleCard(opponents = hostile))

        assertEquals(hostile, diagnostics.getJSONObject("texts").getString("opponents"))
        assertEquals(baseline, diagnostics.getInt("elementCount"))
    }

    @Test
    fun an_empty_display_name_leaves_no_label() {
        val (_, diagnostics) = render(sampleCard(displayName = ""))

        assertFalse(diagnostics.getJSONObject("texts").has("name"))
    }

    private fun goldenData(golden: CardGolden): CardData {
        val photo =
            if (golden.layout == CardLayout.PHOTO) {
                when (golden.theme) {
                    CardTheme.DARK, CardTheme.COURT, CardTheme.SUNSET -> photoDataUri(base = 245, spread = 10)
                    CardTheme.LIGHT -> photoDataUri(base = 12, spread = 10)
                }
            } else {
                null
            }
        val base =
            sampleCard(
                brand = if (golden.theme.requiresPro) "" else "Picklelog",
                photo = photo,
                ratio = golden.ratio,
                theme = golden.theme,
            )
        return when (golden.content) {
            GoldenContent.STANDARD -> base
            GoldenContent.LONG_NAMES ->
                base.copy(
                    displayName = LONG_NAME,
                    opponents = "vs $LONG_NAME & Bartholomew Montgomery-Fitzgerald III",
                    location = "The Community Recreation Center Pickleball Courts, North Wing, Building 7",
                )
            GoldenContent.EMOJI_NAMES -> base.copy(displayName = "Mãe 🥒", opponents = EMOJI_OPPONENTS)
        }
    }

    @Test
    fun every_declared_golden_renders_legibly_at_its_ratio_and_nothing_else_is_saved() {
        goldenDirectory().deleteRecursively()

        CARD_GOLDENS.forEach { golden ->
            val (bitmap, diagnostics) = render(goldenData(golden), golden = golden.name)

            assertEquals(golden.name, CARD_WIDTH_PX, bitmap.width)
            assertEquals(golden.name, golden.ratio.heightPx, bitmap.height)
            assertEquals(golden.name, golden.ratio.name, diagnostics.getString("ratio"))
            assertEquals(golden.name, golden.theme.name, diagnostics.getString("theme"))
            assertEquals(golden.name, golden.layout == CardLayout.PHOTO, diagnostics.getBoolean("photoShown"))
            assertLegible(bitmap, diagnostics, golden.theme)
            bitmap.recycle()
        }

        val saved = goldenDirectory().list().orEmpty().map { it.removeSuffix(".png") }.toSet()
        assertEquals(CARD_GOLDENS.map { it.name }.toSet(), saved)
    }

    @Test
    fun every_ratio_and_theme_stays_legible_over_very_dark_and_very_light_photos() {
        listOf(12, 245).forEach { base ->
            CardRatio.entries.forEach { ratio ->
                CardTheme.entries.forEach { theme ->
                    val (bitmap, diagnostics) =
                        render(sampleCard(photo = photoDataUri(base = base, spread = 10), ratio = ratio, theme = theme))

                    assertTrue(diagnostics.getBoolean("photoShown"))
                    assertLegible(bitmap, diagnostics, theme)
                    bitmap.recycle()
                }
            }
        }
    }

    @Test
    fun a_pro_card_with_the_wordmark_removed_shows_no_brand_and_still_fits() {
        val (bitmap, diagnostics) = render(sampleCard(brand = "", theme = CardTheme.COURT))

        assertFalse(diagnostics.getJSONObject("texts").has("brand"))
        assertTrue(diagnostics.getJSONObject("texts").has("name"))
        assertLegible(bitmap, diagnostics, CardTheme.COURT)
    }

    @Test
    fun the_free_card_keeps_its_wordmark() {
        val (_, diagnostics) = render(sampleCard())

        assertEquals("Picklelog", diagnostics.getJSONObject("texts").getString("brand"))
    }

    @Test
    fun a_pro_card_with_no_wordmark_and_no_name_leaves_the_header_empty_without_overflow() {
        val (_, diagnostics) = render(sampleCard(brand = "", displayName = "", theme = CardTheme.SUNSET))

        assertFalse(diagnostics.getJSONObject("texts").has("brand"))
        assertFalse(diagnostics.getJSONObject("texts").has("name"))
        assertEquals(0, diagnostics.getJSONArray("overflowing").length())
    }

    @Test
    fun each_pro_theme_paints_its_own_background_not_the_dark_one() {
        val court = render(sampleCard(brand = "", theme = CardTheme.COURT)).first
        val sunset = render(sampleCard(brand = "", theme = CardTheme.SUNSET)).first
        val dark = render(sampleCard(theme = CardTheme.DARK)).first

        val courtPixel = court.getPixel(CORNER_SAMPLE, CORNER_SAMPLE)
        val sunsetPixel = sunset.getPixel(CORNER_SAMPLE, CORNER_SAMPLE)
        val darkPixel = dark.getPixel(CORNER_SAMPLE, CORNER_SAMPLE)
        assertTrue(
            "court is not blue: #${Integer.toHexString(courtPixel)}",
            Color.blue(courtPixel) > Color.red(courtPixel) + HUE_MARGIN &&
                Color.blue(courtPixel) > Color.green(courtPixel) + HUE_MARGIN,
        )
        assertTrue(
            "sunset is not warm: #${Integer.toHexString(sunsetPixel)}",
            Color.red(sunsetPixel) > Color.green(sunsetPixel) + HUE_MARGIN &&
                Color.red(sunsetPixel) > Color.blue(sunsetPixel) + HUE_MARGIN,
        )
        assertTrue(
            "dark is not green: #${Integer.toHexString(darkPixel)}",
            Color.green(darkPixel) > Color.blue(darkPixel) + DARK_GREEN_MARGIN,
        )
    }

    @Test
    fun the_square_card_is_exactly_1080_by_1080_with_design_pixels_pinned_to_output_pixels() {
        val (bitmap, diagnostics) = render(sampleCard(ratio = CardRatio.SQUARE))

        assertEquals(1080, bitmap.width)
        assertEquals(1080, bitmap.height)
        val badge = diagnostics.rect("result")
        val pixel =
            bitmap.getPixel(
                badge.getDouble("left").toInt() + BADGE_INSET,
                ((badge.getDouble("top") + badge.getDouble("bottom")) / 2).toInt(),
            )
        val expected = Color.rgb(0x1b, 0x7a, 0x3d)
        val distance =
            abs(Color.red(pixel) - Color.red(expected)) +
                abs(Color.green(pixel) - Color.green(expected)) +
                abs(Color.blue(pixel) - Color.blue(expected))
        assertTrue("badge is not where the page says: #${Integer.toHexString(pixel)}", distance <= COLOUR_TOLERANCE)
    }

    @Test
    fun the_square_card_reflows_its_layout_instead_of_shrinking_the_tall_one() {
        val tall = render(sampleCard(ratio = CardRatio.TALL)).second
        val square = render(sampleCard(ratio = CardRatio.SQUARE)).second

        val tallMeta = tall.rect("meta").let { it.getDouble("bottom") - it.getDouble("top") }
        val squareMeta = square.rect("meta").let { it.getDouble("bottom") - it.getDouble("top") }
        assertTrue(
            "square text shrank like a scaled copy: $squareMeta vs $tallMeta",
            squareMeta >= tallMeta * MINIMUM_TEXT_KEPT,
        )
        val nameBottom = square.rect("name").getDouble("bottom")
        val resultTop = square.rect("result").getDouble("top")
        assertTrue("the header and details overlap: $square", nameBottom < resultTop)
        assertTrue("the square card has a huge empty middle: $square", resultTop < SQUARE_DETAILS_MUST_START_ABOVE)
        assertEquals(0, square.getJSONArray("overflowing").length())
    }

    @Test
    fun switching_ratio_back_and_forth_captures_each_card_at_its_own_size() {
        val (tall, _) = render(sampleCard(ratio = CardRatio.TALL))
        val (square, _) = render(sampleCard(ratio = CardRatio.SQUARE))
        val (tallAgain, _) = render(sampleCard(ratio = CardRatio.TALL))

        assertEquals(1920, tall.height)
        assertEquals(1080, square.height)
        assertEquals(1920, tallAgain.height)
    }
}

private const val CORNER_SAMPLE = 24
private const val HUE_MARGIN = 40
private const val DARK_GREEN_MARGIN = 10
private const val MINIMUM_TEXT_KEPT = 0.7
private const val SQUARE_DETAILS_MUST_START_ABOVE = 540.0
private const val LONG_NAME = "Maximiliano Alejandro de la Cruz-Villanueva y Santisteban"
private const val EMOJI_OPPONENTS = "vs José 🏓 Ñuñez & Zoë Łukasiewicz-李"
