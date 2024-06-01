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
import com.davidtakac.bura.sun.HourlySunshineDurationMoment
import com.davidtakac.bura.sun.HourlySunshineDurationRepository
import com.davidtakac.bura.sun.SunshineDurationPeriod
import java.time.LocalDate
import java.time.LocalDateTime

class GetSunshineDurationGraphs(
    private val sunshineDurationRepository: HourlySunshineDurationRepository,
    private val conditionRepository: ConditionRepository,
) {
    suspend operator fun invoke(
        coords: Coordinates,
        units: Units,
        now: LocalDateTime
    ): ForecastResult<SunshineDurationGraphs> {
        val sunshineDurationPeriod = sunshineDurationRepository.period(coords, units) ?: return ForecastResult.FailedToDownload
        val conditionPeriod = conditionRepository.period(coords, units) ?: return ForecastResult.FailedToDownload

        val hourlySunshineDurationDays = sunshineDurationPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        val conditionDays = conditionPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        return ForecastResult.Success(
            data = SunshineDurationGraphs(
                graphs = getGraphs(
                    now = now,
                    sunshineDurationDays = hourlySunshineDurationDays,
                    conditionDays = conditionDays,
                )
            )
        )
    }

    private fun getGraphs(
        now: LocalDateTime,
        sunshineDurationDays: List<SunshineDurationPeriod>,
        conditionDays: List<ConditionPeriod>,
    ): List<SunshineDurationGraph> = buildList {
        for (i in sunshineDurationDays.indices) {
            add(
                getGraph(
                    now = now,
                    sunshineDurationDay = sunshineDurationDays[i],
                    conditionDay = conditionDays[i],
                    nextSunshineDurationDay = sunshineDurationDays.getOrNull(i + 1),
                    nextConditionDay = conditionDays.getOrNull(i + 1),
                )
            )
        }
    }

    private fun getGraph(
        now: LocalDateTime,
        sunshineDurationDay: SunshineDurationPeriod,
        conditionDay: ConditionPeriod,
        nextSunshineDurationDay: SunshineDurationPeriod?,
        nextConditionDay: ConditionPeriod?
    ): SunshineDurationGraph {
        return SunshineDurationGraph(
            day = sunshineDurationDay.first().hour.toLocalDate(),
            points = buildList {
                for (i in sunshineDurationDay.indices) {
                    add(
                        getPoint(
                            now = now,
                            sunshineDurationMoment = sunshineDurationDay[i],
                            conditionMoment = conditionDay[i],
                        )
                    )
                }
                val firstSunshineDurationTomorrow = nextSunshineDurationDay?.firstOrNull()
                val firstConditionsTomorrow = nextConditionDay?.firstOrNull()
                if (
                    firstSunshineDurationTomorrow != null &&
                    firstConditionsTomorrow != null
                    ) {
                    // The periods must match, so if there is a first temp tomorrow, there
                    // must be a matching condition tomorrow too
                    add(
                        getPoint(
                            now = now,
                            sunshineDurationMoment = firstSunshineDurationTomorrow,
                            conditionMoment = firstConditionsTomorrow,
                        )
                    )
                }
            }
        )
    }

    private fun getPoint(
        now: LocalDateTime,
        sunshineDurationMoment: HourlySunshineDurationMoment,
        conditionMoment: ConditionMoment,
    ): SunshineDurationGraphPoint = SunshineDurationGraphPoint(
        time = GraphTime(sunshineDurationMoment.hour, now),
        condition = Condition(wmoCode = conditionMoment.condition.wmoCode, isDay = conditionMoment.condition.isDay),
        sunshineDuration = GraphSunshineDuration(
            value = sunshineDurationMoment.sunshineDuration,
        ),
    )
}

data class SunshineDurationGraphs(
    val graphs: List<SunshineDurationGraph>
)

data class SunshineDurationGraph(
    val day: LocalDate,
    val points: List<SunshineDurationGraphPoint>
)

data class SunshineDurationGraphPoint(
    val time: GraphTime,
    val condition: Condition,
    val sunshineDuration: GraphSunshineDuration,
)

data class GraphSunshineDuration(
    val value: Double,
)