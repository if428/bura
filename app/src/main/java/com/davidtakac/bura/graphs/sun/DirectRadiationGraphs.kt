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

import com.davidtakac.bura.condition.Condition
import com.davidtakac.bura.condition.ConditionMoment
import com.davidtakac.bura.condition.ConditionPeriod
import com.davidtakac.bura.condition.ConditionRepository
import com.davidtakac.bura.forecast.ForecastResult
import com.davidtakac.bura.units.Units
import com.davidtakac.bura.graphs.common.GraphTime
import com.davidtakac.bura.place.Coordinates
import com.davidtakac.bura.sun.DirectRadiationMoment
import com.davidtakac.bura.sun.DirectRadiationPeriod
import com.davidtakac.bura.sun.DirectRadiationRepository
import java.time.LocalDate
import java.time.LocalDateTime

class GetDirectRadiationGraphs(
    private val directRadiationRepository: DirectRadiationRepository,
    private val conditionRepository: ConditionRepository,
) {
    suspend operator fun invoke(
        coords: Coordinates,
        units: Units,
        now: LocalDateTime
    ): ForecastResult<DirectRadiationGraphs> {
        val directRadiationPeriod = directRadiationRepository.period(coords, units) ?: return ForecastResult.FailedToDownload
        val conditionPeriod = conditionRepository.period(coords, units) ?: return ForecastResult.FailedToDownload

        val directRadiationDays = directRadiationPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        val conditionDays = conditionPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        return ForecastResult.Success(
            data = DirectRadiationGraphs(
                max = directRadiationDays.maxOf { it.maxOf { it.directRadiation } },
                graphs = getGraphs(
                    now = now,
                    directRadiationDays = directRadiationDays,
                    conditionDays = conditionDays,
                )
            )
        )
    }

    private fun getGraphs(
        now: LocalDateTime,
        directRadiationDays: List<DirectRadiationPeriod>,
        conditionDays: List<ConditionPeriod>,
    ): List<DirectRadiationGraph> = buildList {
        for (i in directRadiationDays.indices) {
            add(
                getGraph(
                    now = now,
                    directRadiationDay = directRadiationDays[i],
                    conditionDay = conditionDays[i],
                    nextDirectionRadiationDay = directRadiationDays.getOrNull(i + 1),
                    nextConditionDay = conditionDays.getOrNull(i + 1),
                )
            )
        }
    }

    private fun getGraph(
        now: LocalDateTime,
        directRadiationDay: DirectRadiationPeriod,
        conditionDay: ConditionPeriod,
        nextDirectionRadiationDay: DirectRadiationPeriod?,
        nextConditionDay: ConditionPeriod?
    ): DirectRadiationGraph {
        return DirectRadiationGraph(
            day = directRadiationDay.first().hour.toLocalDate(),
            points = buildList {
                for (i in directRadiationDay.indices) {
                    add(
                        getPoint(
                            now = now,
                            dierectRadiationMoment = directRadiationDay[i],
                            conditionMoment = conditionDay[i],
                        )
                    )
                }
                val firstDirectRadiationTomorrow = nextDirectionRadiationDay?.firstOrNull()
                val firstConditionsTomorrow = nextConditionDay?.firstOrNull()
                if (
                    firstDirectRadiationTomorrow != null &&
                    firstConditionsTomorrow != null
                    ) {
                    // The periods must match, so if there is a first temp tomorrow, there
                    // must be a matching condition tomorrow too
                    add(
                        getPoint(
                            now = now,
                            dierectRadiationMoment = firstDirectRadiationTomorrow,
                            conditionMoment = firstConditionsTomorrow,
                        )
                    )
                }
            }
        )
    }

    private fun getPoint(
        now: LocalDateTime,
        dierectRadiationMoment: DirectRadiationMoment,
        conditionMoment: ConditionMoment,
    ): DirectRadiationGraphPoint = DirectRadiationGraphPoint(
        time = GraphTime(dierectRadiationMoment.hour, now),
        condition = Condition(wmoCode = conditionMoment.condition.wmoCode, isDay = conditionMoment.condition.isDay),
        directRadiation = GraphDirectRadiation(
            value = dierectRadiationMoment.directRadiation,
        ),
    )
}

data class DirectRadiationGraphs(
    val max: Double,
    val graphs: List<DirectRadiationGraph>
)

data class DirectRadiationGraph(
    val day: LocalDate,
    val points: List<DirectRadiationGraphPoint>
)

data class DirectRadiationGraphPoint(
    val time: GraphTime,
    val condition: Condition,
    val directRadiation: GraphDirectRadiation,
)

data class GraphDirectRadiation(
    val value: Double,
)