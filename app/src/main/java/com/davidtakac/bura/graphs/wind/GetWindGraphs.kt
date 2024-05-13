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

import com.davidtakac.bura.forecast.ForecastResult
import com.davidtakac.bura.graphs.common.GraphTime
import com.davidtakac.bura.gust.GustRepository
import com.davidtakac.bura.place.Coordinates
import com.davidtakac.bura.units.Units
import com.davidtakac.bura.wind.WindDirection
import com.davidtakac.bura.wind.WindRepository
import com.davidtakac.bura.wind.WindSpeed
import java.time.LocalDate
import java.time.LocalDateTime

class GetWindGraphs(
    private val windPeriodRepo: WindRepository,
    private val gustsRepo: GustRepository,
) {
    suspend operator fun invoke(
        coords: Coordinates,
        units: Units,
        now: LocalDateTime
    ): ForecastResult<WindGraphs> {
        val windSpeed = windPeriodRepo.period(coords, units) ?: return ForecastResult.FailedToDownload
        val windGusts = gustsRepo.period(coords, units) ?: return ForecastResult.FailedToDownload
        val windSpeedDays = windSpeed.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        val windGustsDays = windGusts.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        return ForecastResult.Success(
            data = WindGraphs(
                max = windGustsDays.maxOf { it.maxOf { it.speed} },
                graphs = windSpeedDays.mapIndexed { dayIdx, day ->
                    WindGraph(
                        day = day.first().hour.toLocalDate(),
                        points = buildList {
                            addAll(
                                day.mapIndexed { momentIdx, moment ->
                                    WindGraphPoint(
                                        time = GraphTime(
                                            hour = moment.hour,
                                            now = now
                                        ),
                                        windSpeed = moment.wind.speed,
                                        direction = moment.wind.direction,
                                        gusts = GraphWindGust(value = moment.wind.gusts, meta = if(moment.wind.gusts == day.maxOf { it.wind.gusts }) GraphWindGust.Meta.Maximum else GraphWindGust.Meta.Regular)
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

data class WindGraphs(
    val max: WindSpeed,
    val graphs: List<WindGraph>
)

data class WindGraph(
    val day: LocalDate,
    val points: List<WindGraphPoint>
)

data class WindGraphPoint(
    val time: GraphTime,
    val windSpeed: WindSpeed,
    val gusts: GraphWindGust,
    val direction: WindDirection
)

data class GraphWindGust(
    val value: WindSpeed,
    val meta: Meta
) {
    enum class Meta {
        Maximum, Regular
    }
}