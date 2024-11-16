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

import com.davidtakac.bura.forecast.ForecastResult
import com.davidtakac.bura.graphs.common.GraphTime
import com.davidtakac.bura.place.Coordinates
import com.davidtakac.bura.pressure.Pressure
import com.davidtakac.bura.pressure.PressureRepository
import com.davidtakac.bura.units.Units
import java.time.LocalDate
import java.time.LocalDateTime

class GetPressureGraphs(
    private val pressurePeriodRepo: PressureRepository,
) {
    suspend operator fun invoke(
        coords: Coordinates,
        units: Units,
        now: LocalDateTime
    ): ForecastResult<PressureGraphs> {
        val pressure = pressurePeriodRepo.period(coords, units) ?: return ForecastResult.FailedToDownload
        val pressureDays = pressure.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        return ForecastResult.Success(
            data = PressureGraphs(
                min = pressureDays.minOf { it.minOf { it.pressure } },
                max = pressureDays.maxOf { it.maxOf { it.pressure } },
                graphs = pressureDays.mapIndexed { dayIdx, day ->
                    PressureGraph(
                        day = day.first().hour.toLocalDate(),
                        points = buildList {
                            addAll(
                                day.mapIndexed { momentIdx, moment ->
                                    PressureGraphPoint(
                                        time = GraphTime(
                                            hour = moment.hour,
                                            now = now
                                        ),
                                        pressure = GraphPressure(
                                            value = moment.pressure,
                                            meta = when (moment.pressure) {
                                                day.maxOf { it.pressure } -> GraphPressure.Meta.Maximum
                                                day.minOf { it.pressure } -> GraphPressure.Meta.Minimum
                                                else -> GraphPressure.Meta.Regular
                                            })
                                    )
                                }
                            )
                        }
                    )
                }
            )
        )
    }
}

data class PressureGraphs(
    val min: Pressure,
    val max: Pressure,
    val graphs: List<PressureGraph>
)

data class PressureGraph(
    val day: LocalDate,
    val points: List<PressureGraphPoint>,
)

data class PressureGraphPoint(
    val time: GraphTime,
    val pressure: GraphPressure,
)

data class GraphPressure(
    val value: Pressure,
    val meta: Meta
) {
    enum class Meta {
        Minimum, Regular, Maximum
    }
}