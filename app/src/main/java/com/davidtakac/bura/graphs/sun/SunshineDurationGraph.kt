/*
 * Copyright 2024 David Takač
 *
 * This file is part of Bura.
 *
 * Bura is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Bura is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Bura. If not, see <https://www.gnu.org/licenses/>.
 */

package com.davidtakac.bura.graphs.sun

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.davidtakac.bura.common.AppTheme
import com.davidtakac.bura.condition.Condition
import com.davidtakac.bura.condition.image
import com.davidtakac.bura.graphs.common.GraphArgs
import com.davidtakac.bura.graphs.common.GraphTime
import com.davidtakac.bura.graphs.common.drawPastOverlay
import com.davidtakac.bura.graphs.common.drawTimeAxis
import com.davidtakac.bura.graphs.common.drawVerticalAxis
import com.davidtakac.bura.graphs.common.getYAxisTicks
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun SunshineDurationGraph(
    state: SunshineDurationGraph,
    args: GraphArgs,
    modifier: Modifier = Modifier
) {
    val sunshineDurationAxis = getYAxisTicks(
        min = 0.0,
        max = 60.0,
        maxNumberOfTicks = 8,
        headRoomBottomPercent = 0.0,
        headRoomTopPercent = 0.0,
        possibleTickSteps = arrayListOf(5.0, 10.0, 20.0),
        isMinimumAlwaysZero = false
    )
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        drawSunshineDurationAxis(
            unit = "min",
            minMinutes = 0.0,
            maxMinutes = 60.0,
            steps = sunshineDurationAxis.numberOfSteps,
            context = context,
            measurer = measurer,
            args = args
        )
        drawHorizontalAxisAndBars(
            state = state,
            max = 60.0,
            sunshineDurationColor = Color.hsv(55.0f, 0.67f, 0.833f, 1f),
            context = context,
            measurer = measurer,
            args = args
        )
    }
}

private fun DrawScope.drawHorizontalAxisAndBars(
    state: SunshineDurationGraph,
    max: Double,
    sunshineDurationColor: Color,
    context: Context,
    measurer: TextMeasurer,
    args: GraphArgs
) {
    val iconSize = 24.dp.toPx()
    val iconSizeRound = iconSize.roundToInt()
    val hasSpaceFor12Icons = (size.width - args.startGutter - args.endGutter) - (iconSizeRound * 12) >= (12 * 2.dp.toPx())
    val iconY = ((args.topGutter / 2) - (iconSize / 2)).roundToInt()
    val range = max

    var nowX: Float? = null
    drawTimeAxis(
        measurer = measurer,
        args = args
    ) { i, x ->
        // Temperature line
        val point = state.points.getOrNull(i) ?: return@drawTimeAxis
        if (point.time.meta == GraphTime.Meta.Present) nowX = x

        val sunshineDurationMinutes = point.sunshineDuration
        val sunshineDurationHeight = ((sunshineDurationMinutes.value / range) * (size.height - args.topGutter - args.bottomGutter)).toFloat()

        val barSpacing = 1.dp.toPx()
        val desiredBarWidth = 8.dp.toPx()
        val bottomOfGraph = size.height - args.bottomGutter
        val topOfSunshineDuration = bottomOfGraph - sunshineDurationHeight

        val barX = if (i == 0) x + desiredBarWidth / 4 else x
        val barWidth = if (i == 0) desiredBarWidth / 2 else desiredBarWidth
        drawLine(
            brush = SolidColor(sunshineDurationColor),
            start = Offset(barX, bottomOfGraph),
            end = Offset(barX, topOfSunshineDuration),
            strokeWidth = barWidth
        )

        // Condition icons
        if (i % (if (hasSpaceFor12Icons) 2 else 3) == 1) {
            val iconX = x - (iconSize / 2)
            val iconDrawable = AppCompatResources.getDrawable(context, point.condition.image(context, args.icons))!!
            drawImage(
                image = iconDrawable.toBitmap(width = iconSizeRound, height = iconSizeRound).asImageBitmap(),
                dstOffset = IntOffset(iconX.roundToInt(), y = iconY),
                dstSize = IntSize(width = iconSizeRound, height = iconSizeRound),
            )
        }
    }

    nowX?.let {
        drawPastOverlay(it, args)
    }
}


private fun DrawScope.drawSunshineDurationAxis(
    unit: String,
    maxMinutes: Double,
    minMinutes: Double,
    steps: Int,
    context: Context,
    measurer: TextMeasurer,
    args: GraphArgs
) {
    val rangeMinutes = maxMinutes - minMinutes
    drawVerticalAxis(
        steps = steps,
        args = args
    ) { frac, endX, y ->
        val labelValue = rangeMinutes * frac
        val sunshineDurationMinutes = measurer.measure(
            text = "${labelValue.roundToInt()} $unit",
            style = args.axisTextStyle
        )
        drawText(
            textLayoutResult = sunshineDurationMinutes,
            color = args.axisColor,
            topLeft = Offset(
                x = endX + args.endAxisTextPaddingStart,
                y = y - (sunshineDurationMinutes.size.height / 2)
            )
        )
    }
}

@Preview
@Composable
private fun SunshineDurationGraphNowMiddlePreview() {
    AppTheme {
        SunshineDurationGraph(
            state = previewState,
            args = GraphArgs.rememberHourlySunshineDurationArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview
@Composable
private fun SunshineDurationGraphNowStartPreview() {
    AppTheme {
        SunshineDurationGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == 0) GraphTime.Meta.Present else GraphTime.Meta.Future
                    )
                )
            }),
            args = GraphArgs.rememberHourlySunshineDurationArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview
@Composable
private fun SunshineDurationGraphNowEndPreview() {
    AppTheme {
        SunshineDurationGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == previewState.points.lastIndex) GraphTime.Meta.Present else GraphTime.Meta.Past
                    )
                )
            }),
            args = GraphArgs.rememberHourlySunshineDurationArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

private val previewState = SunshineDurationGraph(
    day = LocalDate.parse("2023-01-01"),
    points = listOf(
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
            ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("01:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("02:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("03:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("04:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("05:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("06:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("07:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("08:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("09:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(2.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("10:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(20.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("11:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(60.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("12:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(60.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("13:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(48.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("14:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(42.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("15:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(48.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("16:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(54.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("17:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("18:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("19:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("20:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("21:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("22:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("23:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        SunshineDurationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            sunshineDuration = GraphSunshineDuration(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        )
    )
)