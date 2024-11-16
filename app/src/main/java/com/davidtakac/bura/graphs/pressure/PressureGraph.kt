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

package com.davidtakac.bura.graphs.pressure

import android.content.Context
import android.util.Log
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidtakac.bura.common.AppTheme
import com.davidtakac.bura.graphs.common.GraphArgs
import com.davidtakac.bura.graphs.common.GraphTime
import com.davidtakac.bura.graphs.common.drawLabeledPoint
import com.davidtakac.bura.graphs.common.drawPastOverlayWithPoint
import com.davidtakac.bura.graphs.common.drawTimeAxis
import com.davidtakac.bura.graphs.common.drawVerticalAxis
import com.davidtakac.bura.pressure.Pressure
import java.time.LocalDate
import java.time.LocalTime
import com.davidtakac.bura.graphs.common.getYAxisTicks

@Composable
fun PressureGraph(
    state: PressureGraph,
    args: GraphArgs,
    min: Pressure,
    max: Pressure,
    modifier: Modifier = Modifier
) {
    val numberOfYAxisTicks = 7.0
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    val pressureYAxis = getYAxisTicks(
        min = min.value,
        max = max.value,
        maxNumberOfTicks = 8,
        headRoomBottomPercent = 5.0,
        headRoomTopPercent = 15.0,
        possibleTickSteps = arrayListOf(1.0, 2.0, 3.0, 5.0, 10.0, 20.0, 50.0),
        isMinimumAlwaysZero = false
    )
    Log.i("PressureGraph", "y axis: $pressureYAxis")

    val minYAxisValue = Pressure.from(value = pressureYAxis.minYTick, unit = min.unit)
    val maxYAxisValue = Pressure.from(value = pressureYAxis.maxYTick, unit = max.unit)
    Log.i("PressureGraph", "minYAxisValue = $minYAxisValue, maxYAxisValue = $maxYAxisValue")
    val plotColors = AppTheme.colors.pressureColors(minYAxisValue.toHectopascal(), maxYAxisValue.toHectopascal())

    Canvas(modifier) {
        drawPressureAxis(
            min = minYAxisValue,
            max = maxYAxisValue,
            steps = pressureYAxis.numberOfSteps,
            measurer = measurer,
            args = args
        )
        drawHorizontalAxisAndPlot(
            state = state,
            plotColors = plotColors,
            minPressure = minYAxisValue,
            maxPressure = maxYAxisValue,
            context = context,
            measurer = measurer,
            args = args
        )
    }
}

private fun DrawScope.drawHorizontalAxisAndPlot(
    state: PressureGraph,
    plotColors: List<Color>,
    minPressure: Pressure,
    maxPressure: Pressure,
    context: Context,
    measurer: TextMeasurer,
    args: GraphArgs
) {
    val iconSize = 24.dp.toPx()
//    val iconSizeRound = iconSize.roundToInt()
//    val hasSpaceFor12Icons =
//        (size.width - args.startGutter - args.endGutter) - (iconSizeRound * 12) >= (12 * 2.dp.toPx())
  //  val iconY = ((args.topGutter / 2) - (iconSize / 2)).roundToInt()
    val range = (maxPressure - minPressure).value

    val plotPath = Path()
    fun movePressurePlot(x: Float, y: Float) {
        with(plotPath) { if (isEmpty) moveTo(x, y) else lineTo(x, y) }
    }

    var minCenter: Pair<Offset, GraphPressure>? = null
    var maxCenter: Pair<Offset, GraphPressure>? = null
    var nowCenter: Offset? = null
    var lastX = 0f

    drawTimeAxis(
        measurer = measurer,
        args = args
    ) { i, x ->
        // Wind Speed line
        val point = state.points.getOrNull(i) ?: return@drawTimeAxis
        val pressure = point.pressure
        val yPressure =
            ((1 - ((pressure.value.value - minPressure.value) / range)) * (size.height - args.topGutter - args.bottomGutter)).toFloat() + args.topGutter
        movePressurePlot(x, yPressure)

        lastX = x

        // Min, max and now indicators are drawn after the plot so they're on top of it
        if (pressure.meta == GraphPressure.Meta.Minimum) minCenter = Offset(x, yPressure) to pressure
        if (pressure.meta == GraphPressure.Meta.Maximum) maxCenter = Offset(x, yPressure) to pressure
        if (point.time.meta == GraphTime.Meta.Present) nowCenter = Offset(x, yPressure)

        // Wind direction indicators
//        val drawable = AppCompatResources.getDrawable(context, R.drawable.navigation_black)
//        val bitmap = drawable!!.toBitmap(width = iconSizeRound, height = iconSizeRound, config = Bitmap.Config.ARGB_8888)
//        val destBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//        val tmpCanvas = android.graphics.Canvas(destBitmap)
//        tmpCanvas.rotate(point.direction.degrees.toFloat() + 180f, bitmap.width / 2f, bitmap.height / 2f)
//        tmpCanvas.drawBitmap(bitmap, 0f, 0f, null)
//
//        if (i % (if (hasSpaceFor12Icons) 2 else 3) == 1) {
//            val iconX = x - (iconSize / 2)
//            drawImage(
//                image = destBitmap.asImageBitmap(),
//                dstOffset = IntOffset(iconX.roundToInt(), y = iconY),
//                dstSize = IntSize(width = iconSizeRound, height = iconSizeRound),
//            )
//        }
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

private fun DrawScope.drawPressureAxis(
    min: Pressure,
    max: Pressure,
    steps: Int,
    measurer: TextMeasurer,
    args: GraphArgs
) {
    val range = max - min
    drawVerticalAxis(
        steps = steps,
        args = args
    ) { frac, endX, y ->
        val pressure =
            Pressure.from(value = range.value * frac, unit = min.unit)
        Log.i("PressureGraph", "min: $min, pressure tick :$pressure")

        val valueString = (min + pressure).toValueString()
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
private fun PressureGraphNowMiddlePreview() {
    AppTheme {
        PressureGraph(
            state = previewState,
            min = Pressure.fromHectopascal(960.0),
            max = Pressure.fromHectopascal(1060.0),
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
private fun PressureGraphNowStartPreview() {
    AppTheme {
        PressureGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == 0) GraphTime.Meta.Present else GraphTime.Meta.Future
                    )
                )
            }),
            min = Pressure.fromHectopascal(960.0),
            max = Pressure.fromHectopascal(1060.0),
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
private fun PressureGraphNowEndPreview() {
    AppTheme {
        PressureGraph(
            state = previewState.copy(points = previewState.points.mapIndexed { idx, pt ->
                pt.copy(
                    time = GraphTime(
                        pt.time.value,
                        meta = if (idx == previewState.points.lastIndex) GraphTime.Meta.Present else GraphTime.Meta.Past
                    )
                )
            }),
            min = Pressure.fromHectopascal(960.0),
            max = Pressure.fromHectopascal(1060.0),
            args = GraphArgs.rememberWindArgs(),
            modifier = Modifier
                .width(400.dp)
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

private val previewState = PressureGraph(
    day = LocalDate.parse("2023-01-01"),
    points = listOf(
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(963.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("01:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(959.2), meta = GraphPressure.Meta.Minimum),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("02:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(969.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("03:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(979.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("04:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(989.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("05:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(999.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("06:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1009.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("07:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1019.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("08:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1029.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("09:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1039.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("10:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1049.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("11:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1059.2), meta = GraphPressure.Meta.Maximum),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("12:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1054.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("13:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1049.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("14:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1044.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("15:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1039.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("16:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1034.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("17:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1029.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("18:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1024.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("19:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1019.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("20:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1014.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("21:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1009.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("22:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(1004.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("23:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(999.2), meta = GraphPressure.Meta.Regular),
        ),
        PressureGraphPoint(
            time = GraphTime(
                value = LocalTime.parse("00:00"),
                meta = GraphTime.Meta.Past
            ),
            pressure = GraphPressure(value = Pressure.fromHectopascal(994.2), meta = GraphPressure.Meta.Regular),
        ),
    )
)