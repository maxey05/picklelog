package com.maxeydev.picklelog.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maxeydev.picklelog.ui.R

object PicklelogFonts {
    val wordmark: FontFamily = FontFamily(Font(R.font.baloo2_extrabold, FontWeight.ExtraBold))
}

object PicklelogTextStyles {
    val display =
        TextStyle(
            fontFamily = PicklelogFonts.wordmark,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        )

    val displaySmall =
        TextStyle(
            fontFamily = PicklelogFonts.wordmark,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        )

    val hero =
        TextStyle(
            fontFamily = PicklelogFonts.wordmark,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
        )
}
