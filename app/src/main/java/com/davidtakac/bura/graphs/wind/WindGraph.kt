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

package com.davidtakac.bura.graphs.wind

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.davidtakac.bura.R
import com.davidtakac.bura.common.AppTheme
import com.davidtakac.bura.graphs.common.GraphArgs
import com.davidtakac.bura.graphs.common.GraphTime
import com.davidtakac.bura.graphs.common.drawLabeledPoint
import com.davidtakac.bura.graphs.common.drawPastOverlayWithPoint
import com.davidtakac.bura.graphs.common.drawTimeAxis
import com.davidtakac.bura.graphs.common.drawVerticalAxis
import com.davidtakac.bura.graphs.common.getYAxisTicks
import com.davidtakac.bura.wind.Wind
import com.davidtakac.bura.wind.WindDirection
import com.davidtakac.bura.wind.WindSpeed
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun WindGraph(
    state: WindGraph,
    args: GraphArgs,
    max: WindSpeed,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    val oneMeterPerSecondWind = WindSpeed.fromMetersPerSecond(1.0)
    val maxOfDay = state.points.maxOf { it -> when {
        (it.gusts.value < oneMeterPerSecondWind) -> oneMeterPerSecondWind
        else -> it.gusts.value
    }
    }
    Log.i("PrecipitationGraph", "max of day: $maxOfDay")
    Log.i("WindGraph", "max of day: $maxOfDay")

    val windYAxis = getYAxisTicks(
        min = 0.0,
        max = maxOfDay.value,
        maxNumberOfTicks = 8,
        headRoomBottomPercent = 5.0,
        headRoomTopPercent = 15.0,
        possibleTickSteps = arrayListOf(1.0, 2.0, 3.0, 5.0, 10.0, 20.0, 50.0),
        isMinimumAlwaysZero = true
    )
    val maxYAxisValue = WindSpeed.from(value = windYAxis.maxYTick, unit = maxOfDay.unit)

    val plotColors = AppTheme.colors.windSpeedColors(0.0, maxYAxisValue.toMetersPerSecond())

    Canvas(modifier) {
        drawWindAxis(
            max = maxYAxisValue,
            steps = windYAxis.numberOfSteps,
            measurer = measurer,
            args = args
        )
        drawHorizontalAxisAndPlot(
            state = state,
            plotColors = plotColors,
            maxWindSpeed = maxYAxisValue,
            context = context,
            measurer = measurer,
            args = args
        )
    }
}

private fun DrawScope.drawHorizontalAxisAndPlot(
    state: WindGraph,
    plotColors: List<Color>,
    maxWindSpeed: WindSpeed,
    context: Context,
    measurer: TextMeasurer,
    args: GraphArgs
) {
    val iconSize = 24.dp.toPx()
    val iconSizeRound = iconSize.roundToInt()
    val hasSpaceFor12Icons =
        (size.width - args.startGutter - args.endGutter) - (iconSizeRound * 12) >= (12 * 2.dp.toPx())
    val iconY = ((args.topGutter / 2) - (iconSize / 2)).roundToInt()
    val range = maxWindSpeed.value

    val plotPath = Path()
    fun moveWindSpeedPlot(x: Float, y: Float) {
        with(plotPath) { if (isEmpty) moveTo(x, y) else lineTo(x, y) }
    }

    val windGustsPlotPath = Path()
    fun moveWindGustsPlot(x: Float, y: Float) {
        with(windGustsPlotPath) { if (isEmpty) moveTo(x, y) else lineTo(x, y) }
    }

    var minCenter: Pair<Offset, GraphWindGust>? = null
    var maxCenter: Pair<Offset, GraphWindGust>? = null
    var nowCenter: Offset? = null
    var lastX = 0f

    drawTimeAxis(
        measurer = measurer,
        args = args
    ) { i, x ->
        // Wind Speed line
        val point = state.points.getOrNull(i) ?: return@drawTimeAxis
        val windSpeed = point.windSpeed
        val yWindSpeed =
            ((1 - (windSpeed.value / range)) * (size.height - args.topGutter - args.bottomGutter)).toFloat() + args.topGutter
        moveWindSpeedPlot(x, yWindSpeed)

        val windGusts = point.gusts
        val yWindGusts =
            (( 1- (windGusts.value.value / range))  * (size.height - args.topGutter - args.bottomGutter)).toFloat() + args.topGutter
        moveWindGustsPlot(x, yWindGusts)
        lastX = x

        // Min, max and now indicators are drawn after the plot so they're on top of it
        if (windGusts.meta == GraphWindGust.Meta.Maximum) maxCenter = Offset(x, yWindGusts) to windGusts
        if (point.time.meta == GraphTime.Meta.Present) nowCenter = Offset(x, yWindGusts)

        // Wind direction indicators
        val drawable = AppCompatResources.getDrawable(context, R.drawable.navigation_black)
        val bitmap = drawable!!.toBitmap(width = iconSizeRound, height = iconSizeRound, config = Bitmap.Config.ARGB_8888)
        val destBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val tmpCanvas = android.graphics.Canvas(destBitmap)
        tmpCanvas.rotate(point.direction.degrees.toFloat() + 180f, bitmap.width / 2f, bitmap.height / 2f)
        tmpCanvas.drawBitmap(bitmap, 0f, 0f, null)

        if (i % (if (hasSpaceFor12Icons) 2 else 3) == 1) {
            val iconX = x - (iconSize / 2)
            drawImage(
                image = destBitmap.asImageBitmap(),
                dstOffset = IntOffset(iconX.roundToInt(), y = iconY),
                dstSize = IntSize(width = iconSizeRound, height = iconSizeRound),
            )
        }
    }
    val gradientStart = size.height - args.bottomGutter
    val gradientEnd = args.topGutter
    // Clip path makes sure the plot ends are within graph bounds
    clipPath(
        path = Path().apply {
            addRect(
                Rect(
                    offset = Offset(x = args.startGutter, y = args.topGutter),
                    size = Size(
                        width = lastX - args.startGutter,
                        height = size.height - args.topGutter - args.bottomGutter
                    )
                )
            )
        }
    ) {
        drawPath(
            plotPath,
            brush = Brush.verticalGradient(
                colors = plotColors,
                startY = gradientStart,
                endY = gradientEnd
            ),
            style = Stroke(
                width = 2.667f*density,
                join = StrokeJoin.Round,
                cap = StrokeCap.Square
            )
        )
        drawPath(
            windGustsPlotPath,
            brush = Brush.verticalGradient(
                colors = plotColors,
                startY = gradientStart,
                endY = gradientEnd
            ),
            style = Stroke(
                width = args.plotWidth,
                join = StrokeJoin.Round,
                cap = StrokeCap.Square
            )
        )
    }
    minCenter?.let { (offset, graphWindGust) ->
        drawLabeledPoint(
            label = graphWindGust.value.toString(),
            center = offset,
            args = args,
            measurer = measurer
        )
    }
    maxCenter?.let { (offset, graphWindGust) ->
        drawLabeledPoint(
            label = graphWindGust.value.toString(),
            center = offset,
            args = args,
            measurer = measurer
        )
    }
    nowCenter?.let {
        drawPastOverlayWithPoint(it, args)
    }
}

private fun DrawScope.drawWindAxis(
    max: WindSpeed,
    steps: Int,
    measurer: TextMeasurer,
    args: GraphArgs
) {
    val range = max
    drawVerticalAxis(
        steps = steps,
        args = args
    ) { frac, endX, y ->
        val windSpeed =
            WindSpeed.fromMetersPerSecond(value = range.value * frac)
                .convertTo(max.unit)

        val valueString = windSpeed.toValueString()
        val labelString = measurer.measure(
            text = valueString,
            style = args.axisTextStyle
        )
        drawText(
            textLayoutResult = labelString,
            color = args.axisColor,
            topLeft = Offset(
                x = endX + args.endAxisTextPaddingStart,
                y = y - (labelString.size.height / 2)
            )
        )
    }
}

@Preview
@Composable
private fun WindGraphNowMiddlePreview() {
    AppTheme {
        WindGraph(
            state = previewState,
            max = WindSpeed.fromMetersPerSecond(40.0),
            args = GraphArgs.rememberWindArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview
@Composable
private fun WindGraphNowStartPreview() {
    AppTheme {
        WindGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == 0) GraphTime.Meta.Present else GraphTime.Meta.Future
                    )
                )
            }),
            max = WindSpeed.fromMetersPerSecond(40.0),
            args = GraphArgs.rememberWindArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview
@Composable
private fun WindGraphNowEndPreview() {
    AppTheme {
        WindGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == previewState.points.lastIndex) GraphTime.Meta.Present else GraphTime.Meta.Past
                    )
                )
            }),
            max = WindSpeed.fromMetersPerSecond(40.0),
            args = GraphArgs.rememberWindArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

private val previewState = WindGraph(
    day = LocalDate.parse("2023-01-01"),
    points = listOf(
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(2.2),
            direction = WindDirection(0.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(4.5), meta = GraphWindGust.Meta.Regular),
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("01:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(3.8),
            direction = WindDirection(10.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(6.5), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("02:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(8.9),
            direction = WindDirection(20.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(12.4), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("03:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(11.4),
            direction = WindDirection(30.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(15.2), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("04:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(14.9),
            direction = WindDirection(45.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(19.5), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("05:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(16.4),
            direction = WindDirection(60.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(20.5), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("06:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(19.3),
            direction = WindDirection(75.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(24.7), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("07:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(90.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("08:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(24.1),
            direction = WindDirection(105.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(35.1), meta = GraphWindGust.Meta.Maximum)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("09:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(120.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("10:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(135.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("11:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(150.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("12:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(165.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("13:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(180.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("14:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(195.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("15:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(210.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("16:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(225.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("17:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(240.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("18:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(255.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("19:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(270.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("20:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(285.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("21:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(300.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("22:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(315.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("23:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(330.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
        WindGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            windSpeed = WindSpeed.fromMetersPerSecond(22.1),
            direction = WindDirection(345.0),
            gusts = GraphWindGust(value = WindSpeed.fromMetersPerSecond(34.1), meta = GraphWindGust.Meta.Regular)
        ),
    )
)