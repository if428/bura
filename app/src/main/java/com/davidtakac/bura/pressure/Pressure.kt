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

package com.davidtakac.bura.pressure

import java.util.Objects

class Pressure private constructor(
    private val hectopascal: Double,
    val value: Double,
    val unit: Unit
) : Comparable<Pressure> {
    fun convertTo(unit: Unit): Pressure = Pressure(
        hectopascal = hectopascal,
        value = hectopascal * when (unit) {
            Unit.Hectopascal -> 1.0
            Unit.InchesOfMercury -> 0.02953
            Unit.MillimetersOfMercury -> 0.750062
        },
        unit = unit
    )

    operator fun plus(other: Pressure): Pressure {
        val sum = hectopascal + other.hectopascal
        return Pressure(
            hectopascal = sum,
            value = sum,
            unit = Unit.Hectopascal
        ).convertTo(unit)
    }

    operator fun minus(other: Pressure): Pressure {
        val difference = hectopascal - other.hectopascal
        return Pressure(
            hectopascal = difference,
            value = difference,
            unit = Unit.Hectopascal
        ).convertTo(unit)
    }

    operator fun div(other: Int): Pressure {
        val result = hectopascal / other
        return Pressure(
            hectopascal = result,
            value = result,
            unit = Unit.Hectopascal
        ).convertTo(unit)
    }

    fun toHectopascal(): Double {
        return value / when (unit) {
            Pressure.Unit.Hectopascal -> 1.0
            Pressure.Unit.InchesOfMercury -> 0.02953
            Pressure.Unit.MillimetersOfMercury -> 0.750062
        }
    }

    fun toValueString(): String {
        val result = this.toHectopascal()
        return "${String.format("%.0f", result)}"
    }


    override fun compareTo(other: Pressure): Int =
        hectopascal.compareTo(other.hectopascal)

    override fun equals(other: Any?): Boolean =
        other is Pressure && other.hectopascal == hectopascal && other.value == value && other.unit == unit

    override fun hashCode(): Int = Objects.hash(hectopascal, value, unit)

    override fun toString(): String {
        val suffix = when (unit) {
            Unit.Hectopascal -> "hPa"
            Unit.InchesOfMercury -> "inHg"
            Unit.MillimetersOfMercury -> "mmHg"
        }
        return "${String.format("%.1f", value)} $suffix"
    }

    enum class Unit {
        Hectopascal,
        InchesOfMercury,
        MillimetersOfMercury
    }

    companion object {
        fun fromHectopascal(value: Double): Pressure =
            Pressure(
                hectopascal = value,
                value = value,
                unit = Unit.Hectopascal
            )

        fun addFraction(pressure: Pressure, frac: Double): Pressure {
            val pressureHectopascal = pressure.toHectopascal()
            val newValueHectopascal = pressureHectopascal + (pressureHectopascal / frac)
            val result = when(pressure.unit) {
                Pressure.Unit.Hectopascal -> fromHectopascal(
                    newValueHectopascal
                ).convertTo(Pressure.Unit.Hectopascal)
                Pressure.Unit.MillimetersOfMercury -> fromHectopascal(
                    newValueHectopascal
                ).convertTo(Pressure.Unit.MillimetersOfMercury)
                Pressure.Unit.InchesOfMercury -> fromHectopascal(newValueHectopascal)
                    .convertTo(Pressure.Unit.InchesOfMercury)
            }
            return result
        }
    }
}