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

package com.davidtakac.bura.wind

import com.davidtakac.bura.pressure.Pressure
import java.util.Objects

class WindSpeed private constructor(
    private val metersPerSecond: Double,
    var value: Double,
    val unit: Unit
) : Comparable<WindSpeed> {
    val beaufort: Int = when {
        metersPerSecond < 0.3 -> 0
        metersPerSecond <= 1.5 -> 1
        metersPerSecond <= 3.3 -> 2
        metersPerSecond <= 5.5 -> 3
        metersPerSecond <= 7.9 -> 4
        metersPerSecond <= 10.7 -> 5
        metersPerSecond <= 13.8 -> 6
        metersPerSecond <= 17.1 -> 7
        metersPerSecond <= 20.7 -> 8
        metersPerSecond <= 24.4 -> 9
        metersPerSecond <= 28.4 -> 10
        metersPerSecond <= 32.6 -> 11
        else -> 12
    }

    fun convertTo(unit: Unit): WindSpeed {
        val newValue = metersPerSecond * when (unit) {
            Unit.MetersPerSecond -> 1.0
            Unit.KilometersPerHour -> 3.6
            Unit.MilesPerHour -> 2.2369
            Unit.Knots -> 1.94384
        }
        return WindSpeed(
            metersPerSecond = metersPerSecond,
            value = newValue,
            unit = unit
        )
    }

    enum class Unit {
        MetersPerSecond,
        KilometersPerHour,
        MilesPerHour,
        Knots
    }

    override fun compareTo(other: WindSpeed): Int =
        metersPerSecond.compareTo(other.metersPerSecond)

    override fun equals(other: Any?): Boolean =
        other is WindSpeed && other.metersPerSecond == metersPerSecond && other.value == value && other.unit == unit

    override fun hashCode(): Int =
        Objects.hash(metersPerSecond, value, unit)

    override fun toString(): String {
        val suffix = when (unit) {
            Unit.MetersPerSecond -> "m/s"
            Unit.KilometersPerHour -> "km/h"
            Unit.MilesPerHour -> "mi/h"
            Unit.Knots -> "kn"
        }
        return "${String.format("%.1f", value)} $suffix ($beaufort Bft)"
    }

    fun toValueString(fractions: Int = 0): String {
        val result = this.toMetersPerSecond()
        return "${String.format("%.${fractions}f", result)}"
    }

    fun toMetersPerSecond(): Double {
        return value / when (unit) {
            Unit.MetersPerSecond -> 1.0
            Unit.KilometersPerHour -> 3.6
            Unit.Knots -> 1.94384
            Unit.MilesPerHour -> 2.2369
        }
    }

    companion object {
        fun fromMetersPerSecond(value: Double): WindSpeed = WindSpeed(
            metersPerSecond = value,
            value = value,
            unit = Unit.MetersPerSecond
        )

        fun toMetersPerSecond(value: Double, unit: Unit): Double =
            when(unit) {
                Unit.MetersPerSecond -> value
                Unit.Knots -> value / 1.94384
                Unit.MilesPerHour -> value / 2.2369
                Unit.KilometersPerHour -> value / 4.6
            }

        fun from(value: Double, unit: Unit): WindSpeed =
            WindSpeed(
                metersPerSecond = toMetersPerSecond(value, unit),
                value = value,
                unit = unit,
            )
    }
}