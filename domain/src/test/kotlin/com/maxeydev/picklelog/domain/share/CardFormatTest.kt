package com.maxeydev.picklelog.domain.share

import org.junit.Assert.assertEquals
import org.junit.Test

class CardFormatTest {
    @Test
    fun `a match with a photo gets the photo layout by default`() {
        assertEquals(CardLayout.PHOTO, CardFormat.DEFAULT.layoutFor(hasPhoto = true))
    }

    @Test
    fun `a match without a photo always gets the no-photo layout`() {
        assertEquals(CardLayout.NO_PHOTO, CardFormat.DEFAULT.layoutFor(hasPhoto = false))
        assertEquals(CardLayout.NO_PHOTO, CardFormat(layoutOverride = CardLayout.PHOTO).layoutFor(hasPhoto = false))
    }

    @Test
    fun `a manual override can hide a photo the match has`() {
        assertEquals(CardLayout.NO_PHOTO, CardFormat(layoutOverride = CardLayout.NO_PHOTO).layoutFor(hasPhoto = true))
    }

    @Test
    fun `the default is the tall dark story card`() {
        assertEquals(CardRatio.TALL, CardFormat.DEFAULT.ratio)
        assertEquals(CardTheme.DARK, CardFormat.DEFAULT.theme)
    }
}
