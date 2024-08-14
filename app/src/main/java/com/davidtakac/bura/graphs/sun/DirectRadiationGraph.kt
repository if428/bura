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
import android.util.Log
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
import com.davidtakac.bura.common.AppColors
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
fun DirectRadiationGraph(
    state: DirectRadiationGraph,
    args: GraphArgs,
    modifier: Modifier = Modifier
) {
    val minimumDirectRadiationYAxisMax = 120.0 // W/m^2, limit to consider the hour to have 60 minutes sunshine duration according to WMO
    val maxOfDay = state.points.maxOf { it -> when {
        (it.directRadiation.value < minimumDirectRadiationYAxisMax) -> minimumDirectRadiationYAxisMax
        else -> it.directRadiation.value
    }
    }
    val directRadiationAxis = getYAxisTicks(
        min = 0.0,
        max = maxOfDay,
        maxNumberOfTicks = 8,
        headRoomBottomPercent = 0.0,
        headRoomTopPercent = 1.0,
        possibleTickSteps = arrayListOf(10.0, 20.0, 50.0, 100.0, 200.0, 300.0, 500.0, 1000.0, 2000.0, 30000.0, 5000.0, 10_000.0),
        isMinimumAlwaysZero = true
    )
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        drawRadiationAxis(
            unit = "",
            min = 0.0,
            max = directRadiationAxis.maxYTick,
            steps = directRadiationAxis.numberOfSteps,
            measurer = measurer,
            args = args
        )
        drawHorizontalAxisAndBars(
            state = state,
            max = directRadiationAxis.maxYTick,
            context = context,
            measurer = measurer,
            args = args
        )
    }
}

private fun DrawScope.drawHorizontalAxisAndBars(
    state: DirectRadiationGraph,
    max: Double,
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

        val directRadiation = point.directRadiation
        val barColor = AppColors.getDirectRadiationColor(directRadiation.value)
        val directRadiationHeight = ((directRadiation.value / range) * (size.height - args.topGutter - args.bottomGutter)).toFloat()

        val barSpacing = 1.dp.toPx()
        val desiredBarWidth = 8.dp.toPx()
        val bottomOfGraph = size.height - args.bottomGutter
        val topOfDirectRadiation = bottomOfGraph - directRadiationHeight

        val barX = if (i == 0) x + desiredBarWidth / 4 else x - desiredBarWidth / 1.2f
        val barWidth = if (i == 0) desiredBarWidth / 2 else desiredBarWidth
        drawLine(
            brush = SolidColor(barColor),
            start = Offset(barX, bottomOfGraph),
            end = Offset(barX, topOfDirectRadiation),
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


private fun DrawScope.drawRadiationAxis(
    unit: String,
    max: Double,
    min: Double,
    steps: Int,
    measurer: TextMeasurer,
    args: GraphArgs
) {
    val range = max - min
    Log.i("DirectRadiationGraph", "min: $min, max: $max, range: $range")
    drawVerticalAxis(
        steps = steps,
        args = args
    ) { frac, endX, y ->
        val labelValue = range * frac
        val directRadiationWattsPerSquareMeter = measurer.measure(
            text = "${labelValue.roundToInt()} $unit",
            style = args.axisTextStyle
        )
        drawText(
            textLayoutResult = directRadiationWattsPerSquareMeter,
            color = args.axisColor,
            topLeft = Offset(
                x = endX + args.endAxisTextPaddingStart,
                y = y - (directRadiationWattsPerSquareMeter.size.height / 2)
            )
        )
    }
}

@Preview
@Composable
private fun SunshineDurationGraphNowMiddlePreview() {
    AppTheme {
        DirectRadiationGraph(
            state = previewState,
            args = GraphArgs.rememberDirectRadiationArgs(),
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
        DirectRadiationGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == 0) GraphTime.Meta.Present else GraphTime.Meta.Future
                    )
                )
            }),
            args = GraphArgs.rememberDirectRadiationArgs(),
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
        DirectRadiationGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == previewState.points.lastIndex) GraphTime.Meta.Present else GraphTime.Meta.Past
                    )
                )
            }),
            args = GraphArgs.rememberDirectRadiationArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

private val previewState = DirectRadiationGraph(
    day = LocalDate.parse("2023-01-01"),
    points = listOf(
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
            ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("01:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("02:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("03:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("04:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("05:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(4.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("06:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(250.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("07:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(800.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("08:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(1200.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("09:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(1300.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("10:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(1400.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("11:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(1500.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("12:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(1300.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("13:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(1000.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("14:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(600.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("15:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(700.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("16:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(600.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("17:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(800.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("18:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(900.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("19:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(800.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("20:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(500.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("21:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(100.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("22:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("23:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        ),
        DirectRadiationGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            directRadiation = GraphDirectRadiation(0.0),
            condition = Condition(wmoCode = 0, isDay = false),
        )
    )
)