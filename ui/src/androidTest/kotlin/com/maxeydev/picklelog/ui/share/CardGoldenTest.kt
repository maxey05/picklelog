package com.maxeydev.picklelog.ui.share

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        name: String,
        data: CardData,
    ): Pair<Bitmap, JSONObject> {
        val result = runBlocking { renderer.render(data) }
        assertTrue("render failed: $result", result is CardRenderResult.Rendered)
        val rendered = result as CardRenderResult.Rendered
        saveForReview(name, rendered.bitmap)
        return rendered.bitmap to JSONObject(rendered.diagnostics)
    }

    private fun assertLegible(
        bitmap: Bitmap,
        diagnostics: JSONObject,
    ) {
        assertEquals(diagnostics.toString(), 0, diagnostics.getJSONArray("overflowing").length())
        val rects = diagnostics.getJSONObject("rects")
        (BOTTOM_TEXT + TOP_TEXT).filter { rects.has(it) }.forEach { id ->
            val rect = diagnostics.rect(id)
            val top = rect.getDouble("top").toInt().coerceAtLeast(0)
            val bottom = rect.getDouble("bottom").toInt().coerceAtMost(bitmap.height - 1)
            for (y in top..bottom step 4) {
                val contrast = contrastAgainstWhite(bitmap.getPixel(BACKGROUND_SAMPLE_X, y))
                assertTrue("contrast $contrast behind '$id' at y=$y; $diagnostics", contrast >= MINIMUM_CONTRAST)
            }
        }
    }

    @Test
    fun the_card_is_exactly_1080_by_1920_with_design_pixels_pinned_to_output_pixels() {
        val (bitmap, diagnostics) = render("dimensions", sampleCard())

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
        render("first-win", sampleCard(isWin = true))

        val (bitmap, diagnostics) = render("second-loss", sampleCard(isWin = false, opponents = "vs Zed"))

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
        val (bitmap, diagnostics) = render("no-photo", sampleCard(photo = null))

        assertFalse(diagnostics.getBoolean("photoShown"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun a_card_over_a_very_dark_photo_is_legible() {
        val (bitmap, diagnostics) = render("dark-photo", sampleCard(photo = photoDataUri(base = 12, spread = 10)))

        assertTrue(diagnostics.getBoolean("photoShown"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun a_card_over_a_very_light_photo_is_legible() {
        val (bitmap, diagnostics) = render("light-photo", sampleCard(photo = photoDataUri(base = 245, spread = 10)))

        assertTrue(diagnostics.getBoolean("photoShown"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun long_names_and_locations_wrap_or_truncate_inside_the_card() {
        val longName = "Maximiliano Alejandro de la Cruz-Villanueva y Santisteban"
        val (bitmap, diagnostics) =
            render(
                "long-names",
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
        val (bitmap, diagnostics) = render("emoji-accents", sampleCard(opponents = opponents, displayName = "Mãe 🥒"))

        assertEquals(opponents, diagnostics.getJSONObject("texts").getString("opponents"))
        assertEquals("Mãe 🥒", diagnostics.getJSONObject("texts").getString("name"))
        assertLegible(bitmap, diagnostics)
    }

    @Test
    fun a_doubles_card_shows_both_opponents_and_the_result_as_a_word() {
        val (_, diagnostics) = render("doubles-loss", sampleCard(opponents = "vs Ana & Ben", isWin = false))

        val texts = diagnostics.getJSONObject("texts")
        assertEquals("vs Ana & Ben", texts.getString("opponents"))
        assertTrue(texts.getString("result").contains("Loss"))
    }

    @Test
    fun hostile_characters_in_a_name_stay_text_and_do_not_change_the_page() {
        val baseline = render("baseline", sampleCard()).second.getInt("elementCount")
        val hostile = "Dave \"Q\" O'Neil \\ </script><img src=x onerror=alert(1)><b>bold</b>"

        val (_, diagnostics) = render("hostile", sampleCard(opponents = hostile))

        assertEquals(hostile, diagnostics.getJSONObject("texts").getString("opponents"))
        assertEquals(baseline, diagnostics.getInt("elementCount"))
    }

    @Test
    fun an_empty_display_name_leaves_no_label() {
        val (_, diagnostics) = render("no-name", sampleCard(displayName = ""))

        assertFalse(diagnostics.getJSONObject("texts").has("name"))
    }
}
