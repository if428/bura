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

package com.davidtakac.bura.graphs.common

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class YAxis(val tickStep: Int, val minYTick: Double, val maxYTick: Double, val numberOfSteps: Int) {
    override fun toString(): String {
        return "minYTick: $minYTick, maxYTick: $maxYTick, tickStep: $tickStep, number of steps: $numberOfSteps"
    }
}

fun getYAxisTicks(
    min: Double,
    max: Double,
    maxNumberOfTicks: Int,
    headRoomBottomPercent: Double,
    headRoomTopPercent: Double,
    possibleTickSteps: List<Double>,
    isMinimumAlwaysZero: Boolean
): YAxis {
    if (min >= max) {
        throw Exception("min $min >= max $max")
    }
    val rangeWithoutHeadroom = max - min
    val minWithHeadRoom = when (isMinimumAlwaysZero) {
        true  -> min
        false -> min - (0.01 * headRoomBottomPercent * rangeWithoutHeadroom)
    }
    val maxWithHeadRoom = max + (0.01 * headRoomTopPercent * rangeWithoutHeadroom)
    val tickStep = determineTickStep(minWithHeadRoom, maxWithHeadRoom, maxNumberOfTicks, possibleTickSteps)
    val minYTick = roundDown(minWithHeadRoom, tickStep)
    val maxYTick = roundUp(maxWithHeadRoom, tickStep)
    val numberOfSteps = ((maxYTick - minYTick) / tickStep).roundToInt()
    return YAxis(tickStep = tickStep.roundToInt(), minYTick = minYTick, maxYTick = maxYTick, numberOfSteps = numberOfSteps)
}

fun determineTickStep(min: Double, max: Double, maxNumberOfTicks: Int, possibleTickSteps: List<Double>): Double {
    if (min >= max) {
        throw Exception("min $min >= max $max")
    }
    return possibleTickSteps.first { ((max - min) / it) <= maxNumberOfTicks.toDouble() }
}

fun roundUp(value: Double, roundUpTo: Double): Double {
    return ceil(value / roundUpTo) * roundUpTo
}

fun roundDown(value: Double, roundDownTo: Double): Double {
    return floor(value / roundDownTo) * roundDownTo
}
