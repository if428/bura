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

package com.davidtakac.bura.temperature

import com.davidtakac.bura.forecast.HourPeriod
import java.time.LocalDate
import java.time.LocalDateTime

class FeelsLikePeriod(moments: List<FeelsLikeMoment>) : HourPeriod<FeelsLikeMoment>(moments) {
    private var minimumFeelsLike = 120.0
    val minimum get() = minOf { feelsLikeMinimum(it.temperature) }

    val maximum get() = maxOf { it.temperature }

    override fun getDay(day: LocalDate) =
        super.getDay(day)?.let { FeelsLikePeriod(it) }

    override fun momentsFrom(hourInclusive: LocalDateTime, takeMoments: Int?) =
        super.momentsFrom(hourInclusive, takeMoments)?.let { FeelsLikePeriod(it) }

    override fun daysFrom(dayInclusive: LocalDate, takeDays: Int?) =
        super.daysFrom(dayInclusive, takeDays)?.map { FeelsLikePeriod(it) }

    private fun feelsLikeMinimum(temperature: Temperature): Temperature {
        return if (temperature.value < -90.0) Temperature.fromDegreesCelsius(minimumFeelsLike) else { minimumFeelsLike = temperature.value; return temperature}
    }
}
