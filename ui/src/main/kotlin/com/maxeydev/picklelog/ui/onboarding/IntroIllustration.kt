package com.maxeydev.picklelog.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.theme.PicklelogTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal const val INTRO_VIEW_WIDTH = 390f

@Immutable
private data class IntroPalette(
    val blob: Color,
    val soft: Color,
    val card: Color,
    val cardBorder: Color,
    val green: Color,
    val greenDeep: Color,
    val greenMid: Color,
    val mint: Color,
    val mintSoft: Color,
    val onMintSoft: Color,
    val flame: Color,
    val flameDeep: Color,
    val flameSoft: Color,
    val onFlameSoft: Color,
    val ink: Color,
    val muted: Color,
    val emptyCell: Color,
    val loss: Color,
    val onLoss: Color,
    val ball: Color,
    val ballHole: Color,
    val onGreen: Color,
)

@Immutable
private data class IntroArtLabels(
    val win: String,
    val loss: String,
    val score: String,
    val partner: String,
    val photo: String,
    val save: String,
    val taps: String,
    val month: String,
    val monthRecord: String,
    val winRate: String,
    val winRateValue: String,
    val streak: String,
    val cardResult: String,
    val cardScore: String,
    val cardMeta: String,
    val wordmark: String,
    val ads: String,
)

private enum class Anchor { START, MIDDLE, END }

private val UnitDensity = Density(density = 1f, fontScale = 1f)

private val DottedStroke =
    Stroke(
        width = 2f,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 7f)),
    )

private val CalendarLevels =
    listOf(
        listOf(0, 2, 0, 3, 0, 1, 0),
        listOf(1, 0, 4, 0, 2, 0, 3),
        listOf(0, 3, 0, 4, 1, 0, 2),
        listOf(2, 0, 3, 0, 4, -1, -1),
    )

private fun svgPath(data: String): Path = PathParser().parsePathString(data).toPath()

private val BlobTopLeft by lazy { svgPath("M0 0 H250 C228 70 160 104 96 132 C54 150 22 180 0 214 Z") }
private val BlobTopRight by lazy { svgPath("M390 0 H150 C170 66 236 100 300 124 C340 140 368 170 390 206 Z") }
private val BlobTopRightShort by lazy { svgPath("M390 0 H150 C170 60 234 92 298 114 C338 128 368 156 390 190 Z") }
private val SwooshLeft by lazy { svgPath("M28 300 C8 252 58 214 88 242 C108 262 76 300 46 282") }
private val SwooshRight by lazy { svgPath("M332 150 C372 168 382 214 352 234 C332 248 318 222 340 210") }
private val SwooshHigh by lazy { svgPath("M20 214 C0 172 46 140 74 162 C94 178 70 212 44 200") }
private val SwooshLow by lazy { svgPath("M34 330 C10 290 52 254 80 274") }
private val SwooshLowRight by lazy { svgPath("M330 250 C374 262 384 312 354 330 C334 342 320 316 342 306") }
private val SwooshPrivacy by lazy { svgPath("M30 236 C8 196 52 162 80 184") }
private val LeafTall by lazy { svgPath("M0 0 C-10 -42 8 -84 30 -104 C36 -62 26 -26 0 0 Z") }
private val LeafWide by lazy { svgPath("M8 0 C28 -32 58 -48 86 -48 C72 -20 44 -4 8 0 Z") }
private val LeafSmall by lazy { svgPath("M4 0 C-22 -22 -30 -50 -24 -70 C-6 -50 2 -26 4 0 Z") }
private val CheckMark by lazy { svgPath("M-10 1 L-3 8 L11 -7") }
private val FlameOuter by lazy {
    svgPath("M0 -24 C12 -8 20 2 16 14 C12 25 -12 25 -16 14 C-20 2 -9 -4 -7 -13 C-2 -7 0 -4 0 -24 Z")
}
private val FlameInner by lazy {
    svgPath("M0 -2 C6 6 9 10 7 15 C5 20 -6 20 -8 15 C-10 10 -4 7 -3 2 C-1 5 0 6 0 -2 Z")
}
private val TrendLine by lazy { svgPath("M-2 60 L40 36 L72 20 L110 -14") }
private val TrendHead by lazy { svgPath("M98 -16 L112 -16 L110 -2") }
private val Court by lazy { svgPath("M-74 -16 L74 -16 L46 -96 L-46 -96 Z") }
private val Heart by lazy {
    svgPath("M0 8 C-14 -2 -16 -14 -8 -18 C-3 -20 0 -16 0 -13 C0 -16 3 -20 8 -18 C16 -14 14 -2 0 8 Z")
}
private val Shield by lazy { svgPath("M60 54 L100 70 C100 116 86 144 60 158 C34 144 20 116 20 70 Z") }
private val ShieldCheck by lazy { svgPath("M44 104 L56 116 L78 92") }
private val Cloud by lazy {
    svgPath("M-12 8 H12 C18 8 20 -2 13 -5 C12 -13 2 -15 -2 -9 C-9 -12 -16 -6 -13 0 C-19 1 -18 8 -12 8 Z")
}
private val Shoulders by lazy { svgPath("M-12 12 C-10 2 10 2 12 12") }

@Composable
internal fun IntroIllustration(
    page: IntroPage,
    modifier: Modifier = Modifier,
) {
    val palette = introPalette()
    val labels = introArtLabels()
    val measurer = rememberTextMeasurer()
    val description = stringResource(page.illustrationDescription)
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        fitViewport(page.viewportHeight) {
            when (page) {
                IntroPage.LOGGING -> drawLoggingScene(palette, measurer, labels)
                IntroPage.STATS -> drawStatsScene(palette, measurer, labels)
                IntroPage.SHARE -> drawShareScene(palette, measurer, labels)
                IntroPage.PRIVACY -> drawPrivacyScene(palette, measurer, labels)
            }
        }
    }
}

@Composable
private fun introPalette(): IntroPalette {
    val scheme = MaterialTheme.colorScheme
    val brand = PicklelogTheme.colors
    return IntroPalette(
        blob = scheme.secondaryContainer,
        soft = scheme.surfaceContainerLow,
        card = scheme.surfaceBright,
        cardBorder = brand.cardBorder,
        green = brand.winBadge,
        greenDeep = Color(0xFF0B4A2C),
        greenMid = Color(0xFF4FB883),
        mint = brand.headerAccent,
        mintSoft = brand.countBadge,
        onMintSoft = brand.onCountBadge,
        flame = brand.streakFlame,
        flameDeep = Color(0xFFFF8A1C),
        flameSoft = scheme.tertiaryContainer,
        onFlameSoft = scheme.onTertiaryContainer,
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        emptyCell = scheme.surfaceContainer,
        loss = brand.lossBadge,
        onLoss = brand.onLossBadge,
        ball = Color(0xFFDDEB52),
        ballHole = Color(0xFFAFBE2E),
        onGreen = Color.White,
    )
}

@Composable
private fun introArtLabels(): IntroArtLabels =
    IntroArtLabels(
        win = stringResource(R.string.intro_art_win),
        loss = stringResource(R.string.intro_art_loss),
        score = stringResource(R.string.intro_art_score),
        partner = stringResource(R.string.intro_art_partner),
        photo = stringResource(R.string.intro_art_photo),
        save = stringResource(R.string.intro_art_save),
        taps = stringResource(R.string.intro_art_taps),
        month = stringResource(R.string.intro_art_month),
        monthRecord = stringResource(R.string.intro_art_month_record),
        winRate = stringResource(R.string.intro_art_win_rate),
        winRateValue = stringResource(R.string.stats_percent, 73),
        streak = stringResource(R.string.intro_art_streak),
        cardResult = stringResource(R.string.intro_art_win),
        cardScore = stringResource(R.string.intro_art_card_score),
        cardMeta = stringResource(R.string.intro_art_card_meta),
        wordmark = stringResource(R.string.intro_art_wordmark),
        ads = stringResource(R.string.intro_art_ads),
    )

private fun DrawScope.fitViewport(
    viewportHeight: Float,
    block: DrawScope.() -> Unit,
) {
    val fit = min(size.width / INTRO_VIEW_WIDTH, size.height / viewportHeight)
    val left = (size.width - INTRO_VIEW_WIDTH * fit) / 2f
    val top = size.height - viewportHeight * fit
    withTransform(
        {
            translate(left, top)
            scale(fit, fit, Offset.Zero)
        },
        block,
    )
}

private fun DrawScope.box(
    color: Color,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    drawRoundRect(color, Offset(x, y), Size(width, height), CornerRadius(radius, radius))
}

private fun DrawScope.boxOutline(
    color: Color,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
    strokeWidth: Float,
    dashed: Boolean = false,
) {
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = strokeWidth, pathEffect = effect),
    )
}

private fun DrawScope.line(
    color: Color,
    from: Offset,
    to: Offset,
    width: Float,
) {
    drawLine(color = color, start = from, end = to, strokeWidth = width, cap = StrokeCap.Round)
}

private fun DrawScope.strokePath(
    path: Path,
    color: Color,
    width: Float,
) {
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.label(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    baseline: Float,
    fontSize: Float,
    color: Color,
    weight: FontWeight = FontWeight.Bold,
    anchor: Anchor = Anchor.START,
) {
    val layout =
        measurer.measure(
            text = text,
            style = TextStyle(color = color, fontSize = fontSize.sp, fontWeight = weight),
            softWrap = false,
            maxLines = 1,
            density = UnitDensity,
        )
    val width = layout.size.width.toFloat()
    val left =
        when (anchor) {
            Anchor.START -> x
            Anchor.MIDDLE -> x - width / 2f
            Anchor.END -> x - width
        }
    drawText(layout, topLeft = Offset(left, baseline - layout.firstBaseline))
}

private fun DrawScope.ground(
    palette: IntroPalette,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
) {
    drawOval(palette.blob, Offset(INTRO_VIEW_WIDTH / 2f - radiusX, centerY - radiusY), Size(radiusX * 2f, radiusY * 2f))
}

private fun DrawScope.leafCluster(
    palette: IntroPalette,
    origin: Offset,
    mirrored: Boolean,
    factor: Float,
) {
    withTransform(
        {
            translate(origin.x, origin.y)
            scale(if (mirrored) -factor else factor, factor, Offset.Zero)
        },
    ) {
        drawPath(LeafTall, if (mirrored) palette.greenMid else palette.green)
        drawPath(LeafWide, palette.mint)
        if (!mirrored) {
            drawPath(LeafSmall, palette.greenMid)
        }
    }
}

private fun DrawScope.paddle(
    palette: IntroPalette,
    at: Offset,
    degrees: Float,
    factor: Float,
) {
    withTransform(
        {
            translate(at.x, at.y)
            rotate(degrees, Offset.Zero)
            scale(factor, factor, Offset.Zero)
        },
    ) {
        box(palette.greenDeep, -5f, 16f, 10f, 32f, 4f)
        box(palette.green, -21f, -44f, 42f, 62f, 19f)
        boxOutline(palette.mint, -14f, -37f, 28f, 48f, 13f, 2f)
    }
}

private fun DrawScope.pickleball(
    palette: IntroPalette,
    center: Offset,
    radius: Float,
) {
    drawCircle(palette.ball, radius, center)
    drawCircle(palette.ballHole, radius * 0.12f, center)
    for (step in 0 until 6) {
        val angle = Math.toRadians(step * 60.0 + 30.0)
        val hole =
            Offset(
                center.x + (cos(angle) * radius * 0.56f).toFloat(),
                center.y + (sin(angle) * radius * 0.56f).toFloat(),
            )
        drawCircle(palette.ballHole, radius * 0.11f, hole)
    }
}

private fun calendarColor(
    palette: IntroPalette,
    level: Int,
): Color =
    when (level) {
        1 -> palette.mintSoft
        2 -> palette.mint
        3 -> palette.greenMid
        4 -> palette.green
        else -> palette.emptyCell
    }

private fun DrawScope.drawLoggingScene(
    palette: IntroPalette,
    measurer: TextMeasurer,
    labels: IntroArtLabels,
) {
    drawPath(BlobTopLeft, palette.blob)
    drawCircle(palette.soft, 74f, Offset(372f, 30f))
    drawPath(SwooshLeft, palette.mint, style = DottedStroke)
    drawPath(SwooshRight, palette.mint, style = DottedStroke)
    ground(palette, 392f, 165f, 12f)
    leafCluster(palette, Offset(46f, 390f), mirrored = false, factor = 1f)
    paddle(palette, Offset(314f, 320f), 20f, 0.9f)
    leafCluster(palette, Offset(352f, 390f), mirrored = true, factor = 1f)
    translate(125f, 74f) {
        box(palette.greenDeep, 0f, 0f, 140f, 300f, 26f)
        box(palette.card, 8f, 10f, 124f, 280f, 19f)
        box(palette.cardBorder, 52f, 18f, 36f, 6f, 3f)
        box(palette.cardBorder, 20f, 40f, 70f, 8f, 4f)
        box(palette.emptyCell, 20f, 54f, 46f, 6f, 3f)
        drawCircle(palette.flame.copy(alpha = 0.35f), 42f, Offset(44f, 110f), style = Stroke(width = 2f))
        drawCircle(palette.flame.copy(alpha = 0.8f), 33f, Offset(44f, 110f), style = Stroke(width = 3f))
        box(palette.green, 20f, 82f, 48f, 56f, 12f)
        label(measurer, labels.win, 44f, 119f, 24f, palette.onGreen, FontWeight.ExtraBold, Anchor.MIDDLE)
        box(palette.loss, 76f, 82f, 48f, 56f, 12f)
        label(measurer, labels.loss, 100f, 119f, 24f, palette.onLoss, FontWeight.ExtraBold, Anchor.MIDDLE)
        listOf(labels.score to 156f, labels.partner to 188f, labels.photo to 220f).forEach { (text, y) ->
            boxOutline(palette.cardBorder, 20f, y, 104f, 24f, 8f, 1.5f, dashed = true)
            label(measurer, text, 30f, y + 16f, 11f, palette.muted, FontWeight.SemiBold)
        }
        box(palette.green, 20f, 252f, 104f, 30f, 15f)
        label(measurer, labels.save, 72f, 272f, 13f, palette.onGreen, FontWeight.Bold, Anchor.MIDDLE)
    }
    translate(284f, 96f) {
        drawCircle(palette.flame, 24f, Offset.Zero)
        strokePath(CheckMark, palette.onGreen, 4f)
        line(palette.flame, Offset(30f, -20f), Offset(38f, -26f), 3f)
        line(palette.flame, Offset(32f, 2f), Offset(42f, 2f), 3f)
        line(palette.flame, Offset(-4f, -32f), Offset(-2f, -42f), 3f)
    }
    translate(18f, 170f) {
        box(palette.mintSoft, 0f, 0f, 96f, 34f, 17f)
        label(measurer, labels.taps, 48f, 22f, 13f, palette.onMintSoft, FontWeight.Bold, Anchor.MIDDLE)
    }
    pickleball(palette, Offset(80f, 342f), 28f)
}

private fun DrawScope.drawStatsScene(
    palette: IntroPalette,
    measurer: TextMeasurer,
    labels: IntroArtLabels,
) {
    drawPath(BlobTopRight, palette.blob)
    drawCircle(palette.soft, 70f, Offset(18f, 36f))
    drawPath(SwooshHigh, palette.mint, style = DottedStroke)
    ground(palette, 392f, 165f, 12f)
    translate(150f, 54f) {
        box(palette.mintSoft, 8f, 10f, 206f, 156f, 18f)
        box(palette.card, 0f, 0f, 206f, 156f, 18f)
        boxOutline(palette.cardBorder, 0f, 0f, 206f, 156f, 18f, 2f)
        label(measurer, labels.month, 16f, 28f, 13f, palette.ink, FontWeight.ExtraBold)
        label(measurer, labels.monthRecord, 190f, 28f, 11f, palette.muted, FontWeight.SemiBold, Anchor.END)
        CalendarLevels.forEachIndexed { row, levels ->
            levels.forEachIndexed { column, level ->
                val x = 16f + column * 25f
                val y = 42f + row * 18f
                if (level < 0) {
                    boxOutline(palette.cardBorder, x, y, 20f, 12f, 3f, 1.5f, dashed = true)
                } else {
                    box(calendarColor(palette, level), x, y, 20f, 12f, 3f)
                }
            }
        }
        label(measurer, labels.winRate, 16f, 138f, 11f, palette.muted, FontWeight.SemiBold)
        box(palette.emptyCell, 72f, 130f, 84f, 8f, 4f)
        box(palette.green, 72f, 130f, 61f, 8f, 4f)
        label(measurer, labels.winRateValue, 190f, 138f, 11f, palette.ink, FontWeight.ExtraBold, Anchor.END)
    }
    translate(80f, 132f) {
        drawCircle(palette.flameSoft, 38f, Offset.Zero)
        drawPath(FlameOuter, palette.flame)
        drawPath(FlameInner, palette.flameDeep)
        box(palette.greenDeep, -56f, 46f, 112f, 30f, 15f)
        label(measurer, labels.streak, 0f, 66f, 12f, palette.onGreen, FontWeight.Bold, Anchor.MIDDLE)
    }
    translate(36f, 248f) {
        box(palette.mintSoft, 0f, 80f, 22f, 40f, 6f)
        box(palette.mint, 32f, 56f, 22f, 64f, 6f)
        box(palette.greenMid, 64f, 34f, 22f, 86f, 6f)
        box(palette.green, 96f, 6f, 22f, 114f, 6f)
        strokePath(TrendLine, palette.flame, 4f)
        strokePath(TrendHead, palette.flame, 4f)
    }
    paddle(palette, Offset(238f, 334f), -18f, 0.95f)
    pickleball(palette, Offset(308f, 352f), 30f)
    drawCircle(palette.flame, 4f, Offset(206f, 226f))
    drawCircle(palette.greenMid, 4f, Offset(350f, 244f))
    drawCircle(palette.mint, 4f, Offset(354f, 300f))
}

private fun DrawScope.drawShareScene(
    palette: IntroPalette,
    measurer: TextMeasurer,
    labels: IntroArtLabels,
) {
    drawPath(BlobTopLeft, palette.blob)
    drawCircle(palette.soft, 72f, Offset(372f, 34f))
    drawPath(SwooshLowRight, palette.mint, style = DottedStroke)
    drawPath(SwooshLow, palette.mint, style = DottedStroke)
    ground(palette, 392f, 165f, 12f)
    withTransform(
        {
            translate(195f, 214f)
            rotate(-6f, Offset.Zero)
        },
    ) {
        box(palette.mintSoft, -92f, -150f, 200f, 300f, 22f)
        box(palette.card, -100f, -160f, 200f, 300f, 22f)
        boxOutline(palette.cardBorder, -100f, -160f, 200f, 300f, 22f, 2f)
        box(palette.mint, -88f, -148f, 176f, 132f, 14f)
        drawCircle(palette.flame, 12f, Offset(56f, -122f))
        drawPath(Court, palette.green)
        drawPath(Court, palette.onGreen, style = Stroke(width = 2f))
        line(palette.onGreen, Offset(0f, -16f), Offset(0f, -54f), 2f)
        line(palette.greenDeep, Offset(-62f, -54f), Offset(62f, -54f), 4f)
        box(palette.green, -88f, 2f, 32f, 32f, 9f)
        label(measurer, labels.cardResult, -72f, 24f, 16f, palette.onGreen, FontWeight.ExtraBold, Anchor.MIDDLE)
        label(measurer, labels.cardScore, -46f, 30f, 30f, palette.ink, FontWeight.ExtraBold)
        label(measurer, labels.cardMeta, -88f, 58f, 11f, palette.muted, FontWeight.SemiBold)
        box(palette.emptyCell, -88f, 70f, 104f, 8f, 4f)
        box(palette.emptyCell, -88f, 84f, 72f, 8f, 4f)
        line(palette.emptyCell, Offset(-88f, 108f), Offset(88f, 108f), 2f)
        label(measurer, labels.wordmark, -88f, 128f, 12f, palette.green, FontWeight.ExtraBold)
        pickleball(palette, Offset(80f, 124f), 8f)
    }
    translate(330f, 104f) {
        drawCircle(palette.green, 28f, Offset.Zero)
        line(palette.onGreen, Offset(-9f, 0f), Offset(9f, -9f), 2.5f)
        line(palette.onGreen, Offset(-9f, 0f), Offset(9f, 9f), 2.5f)
        drawCircle(palette.onGreen, 4.5f, Offset(-9f, 0f))
        drawCircle(palette.onGreen, 4.5f, Offset(9f, -9f))
        drawCircle(palette.onGreen, 4.5f, Offset(9f, 9f))
    }
    withTransform(
        {
            translate(58f, 112f)
            scale(1.6f, 1.6f, Offset.Zero)
        },
    ) {
        drawPath(Heart, palette.flame)
    }
    translate(92f, 66f) {
        drawPath(Heart, palette.greenMid)
    }
    translate(336f, 196f) {
        box(palette.card, -20f, -28f, 40f, 56f, 10f)
        boxOutline(palette.greenMid, -20f, -28f, 40f, 56f, 10f, 3f)
        drawCircle(palette.greenMid, 8f, Offset(0f, -4f), style = Stroke(width = 3f))
        line(palette.greenMid, Offset(-10f, 16f), Offset(10f, 16f), 3f)
    }
    pickleball(palette, Offset(64f, 350f), 26f)
}

private fun DrawScope.drawPrivacyScene(
    palette: IntroPalette,
    measurer: TextMeasurer,
    labels: IntroArtLabels,
) {
    drawPath(BlobTopRightShort, palette.blob)
    drawCircle(palette.soft, 64f, Offset(18f, 30f))
    drawPath(SwooshPrivacy, palette.mint, style = DottedStroke)
    ground(palette, 300f, 160f, 11f)
    leafCluster(palette, Offset(340f, 300f), mirrored = true, factor = 0.85f)
    translate(135f, 40f) {
        box(palette.greenDeep, 0f, 0f, 120f, 250f, 24f)
        box(palette.card, 7f, 9f, 106f, 232f, 18f)
        box(palette.cardBorder, 42f, 16f, 36f, 6f, 3f)
        drawPath(Shield, palette.green)
        strokePath(ShieldCheck, palette.onGreen, 6f)
        box(palette.cardBorder, 22f, 180f, 76f, 8f, 4f)
        box(palette.emptyCell, 32f, 196f, 56f, 8f, 4f)
    }
    translate(84f, 86f) {
        drawCircle(palette.card, 30f, Offset.Zero)
        drawCircle(palette.mint, 30f, Offset.Zero, style = Stroke(width = 3f))
        drawPath(Cloud, palette.mintSoft)
        drawPath(Cloud, palette.green, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
        line(palette.greenDeep, Offset(-16f, -16f), Offset(16f, 16f), 3f)
    }
    translate(318f, 74f) {
        drawCircle(palette.flameSoft, 28f, Offset.Zero)
        label(measurer, labels.ads, 0f, 6f, 15f, palette.onFlameSoft, FontWeight.ExtraBold, Anchor.MIDDLE)
        drawCircle(palette.flameDeep, 18f, Offset.Zero, style = Stroke(width = 3f))
        line(palette.flameDeep, Offset(-13f, -13f), Offset(13f, 13f), 3f)
    }
    translate(316f, 174f) {
        drawCircle(palette.card, 26f, Offset.Zero)
        drawCircle(palette.mint, 26f, Offset.Zero, style = Stroke(width = 3f))
        drawCircle(palette.green, 7f, Offset(0f, -6f), style = Stroke(width = 2.5f))
        drawPath(Shoulders, palette.green, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        line(palette.greenDeep, Offset(-14f, -14f), Offset(14f, 14f), 3f)
    }
    paddle(palette, Offset(70f, 254f), -24f, 0.8f)
    pickleball(palette, Offset(112f, 276f), 24f)
}
