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

package com.davidtakac.bura.graphs.temperature

import com.davidtakac.bura.forecast.ForecastResult
import com.davidtakac.bura.temperature.Temperature
import com.davidtakac.bura.temperature.TemperatureMoment
import com.davidtakac.bura.temperature.TemperaturePeriod
import com.davidtakac.bura.temperature.TemperatureRepository
import com.davidtakac.bura.units.Units
import com.davidtakac.bura.condition.Condition
import com.davidtakac.bura.condition.ConditionMoment
import com.davidtakac.bura.condition.ConditionPeriod
import com.davidtakac.bura.condition.ConditionRepository
import com.davidtakac.bura.graphs.common.GraphTime
import com.davidtakac.bura.place.Coordinates
import com.davidtakac.bura.temperature.DewpointMoment
import com.davidtakac.bura.temperature.DewpointPeriod
import com.davidtakac.bura.temperature.DewpointRepository
import com.davidtakac.bura.temperature.FeelsLikeMoment
import com.davidtakac.bura.temperature.FeelsLikePeriod
import com.davidtakac.bura.temperature.FeelsLikeRepository
import com.davidtakac.bura.temperature.WetbulbMoment
import com.davidtakac.bura.temperature.WetbulbPeriod
import com.davidtakac.bura.temperature.WetbulbRepository
import java.time.LocalDate
import java.time.LocalDateTime

class GetTemperatureGraphs(
    private val tempRepo: TemperatureRepository,
    private val descRepo: ConditionRepository,
    private val dewpointRepo: DewpointRepository,
    private val wetbulbRepo: WetbulbRepository,
    private val feelsLikeRepo: FeelsLikeRepository,
) {
    suspend operator fun invoke(
        coords: Coordinates,
        units: Units,
        now: LocalDateTime
    ): ForecastResult<TemperatureGraphs> {
        val tempPeriod = tempRepo.period(coords, units) ?: return ForecastResult.FailedToDownload
        val descPeriod = descRepo.period(coords, units) ?: return ForecastResult.FailedToDownload
        val dewpointPeriod = dewpointRepo.period(coords, units) ?: return ForecastResult.FailedToDownload
        val wetbulbPeriod = wetbulbRepo.period(coords, units) ?: return ForecastResult.FailedToDownload
        val feelsLikePeriod = feelsLikeRepo.period(coords, units) ?: return ForecastResult.FailedToDownload

        val tempDays = tempPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        val dewpointDays = dewpointPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.FailedToDownload
        val wetbulbDays = wetbulbPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.FailedToDownload
        val feelsLikeDays = feelsLikePeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.FailedToDownload
        val conditionDays = descPeriod.daysFrom(now.toLocalDate()) ?: return ForecastResult.Outdated
        return ForecastResult.Success(
            data = TemperatureGraphs(
                minTemp = Temperature.fromDegreesCelsius(
                    tempPeriod.minimum.convertTo(Temperature.Unit.DegreesCelsius).value.coerceAtMost(
                        dewpointPeriod.minimum.convertTo(Temperature.Unit.DegreesCelsius).value
                    )
                        .coerceAtMost(feelsLikePeriod.minimum.convertTo(Temperature.Unit.DegreesCelsius).value)
                ),
                maxTemp = Temperature.fromDegreesCelsius(
                    tempPeriod.maximum.convertTo(Temperature.Unit.DegreesCelsius).value.coerceAtLeast(
                        dewpointPeriod.maximum.convertTo(Temperature.Unit.DegreesCelsius).value
                    )
                        .coerceAtLeast(feelsLikePeriod.maximum.convertTo(Temperature.Unit.DegreesCelsius).value)
                ),
                graphs = getGraphs(
                    now = now,
                    tempDays = tempDays,
                    dewpointDays = dewpointDays,
                    wetbulbDays = wetbulbDays,
                    feelsLikeDays = feelsLikeDays,
                    conditionDays = conditionDays
                )
            )
        )
    }

    private fun getGraphs(
        now: LocalDateTime,
        tempDays: List<TemperaturePeriod>,
        dewpointDays: List<DewpointPeriod>,
        wetbulbDays: List<WetbulbPeriod>,
        feelsLikeDays: List<FeelsLikePeriod>,
        conditionDays: List<ConditionPeriod>
    ): List<TemperatureGraph> = buildList {
        for (i in tempDays.indices) {
            add(
                getGraph(
                    now = now,
                    tempDay = tempDays[i],
                    dewpointDay = dewpointDays[i],
                    wetbulbDay = wetbulbDays[i],
                    feelsLikeDay = feelsLikeDays[i],
                    conditionDay = conditionDays[i],
                    nextTempDay = tempDays.getOrNull(i + 1),
                    nextDewpointDay = dewpointDays.getOrNull(i + 1),
                    nextWetbulbDay = wetbulbDays.getOrNull(i + 1),
                    nextFeelsLikeDay = feelsLikeDays.getOrNull(i + 1),
                    nextConditionDay = conditionDays.getOrNull(i + 1)
                )
            )
        }
    }

    private fun getGraph(
        now: LocalDateTime,
        tempDay: TemperaturePeriod,
        dewpointDay: DewpointPeriod,
        wetbulbDay: WetbulbPeriod,
        feelsLikeDay: FeelsLikePeriod,
        conditionDay: ConditionPeriod,
        nextTempDay: TemperaturePeriod?,
        nextDewpointDay: DewpointPeriod?,
        nextWetbulbDay: WetbulbPeriod?,
        nextFeelsLikeDay: FeelsLikePeriod?,
        nextConditionDay: ConditionPeriod?
    ): TemperatureGraph {
        val minTempMoment = tempDay.reversed().minBy { it.temperature }
        val maxTempMoment = tempDay.reversed().maxBy { it.temperature }
        return TemperatureGraph(
            day = tempDay.first().hour.toLocalDate(),
            points = buildList {
                for (i in tempDay.indices) {
                    add(
                        getPoint(
                            now = now,
                            tempMoment = tempDay[i],
                            dewpointMoment = dewpointDay[i],
                            wetbulbMoment = wetbulbDay[i],
                            feelsLikeMoment = feelsLikeDay[i],
                            minTempMoment = minTempMoment,
                            maxTempMoment = maxTempMoment,
                            conditionMoment = conditionDay[i]
                        )
                    )
                }
                val firstTempTomorrow = nextTempDay?.firstOrNull()
                val firstDewpointTomorrow = nextDewpointDay?.firstOrNull()
                val firstWetbulbTomorrow = nextWetbulbDay?.firstOrNull()
                val firstFeelsLikeTomorrow = nextFeelsLikeDay?.firstOrNull()
                if (
                    firstTempTomorrow != null &&
                    firstDewpointTomorrow != null &&
                    firstWetbulbTomorrow != null &&
                    firstFeelsLikeTomorrow != null) {
                    // The periods must match, so if there is a first temp tomorrow, there
                    // must be a matching condition tomorrow too
                    val firstConditionTomorrow = nextConditionDay!!.first()
                    add(
                        getPoint(
                            now = now,
                            tempMoment = firstTempTomorrow,
                            dewpointMoment = firstDewpointTomorrow,
                            wetbulbMoment = firstWetbulbTomorrow,
                            feelsLikeMoment = firstFeelsLikeTomorrow,
                            minTempMoment = minTempMoment,
                            maxTempMoment = maxTempMoment,
                            conditionMoment = firstConditionTomorrow
                        )
                    )
                }
            }
        )
    }

    private fun getPoint(
        now: LocalDateTime,
        tempMoment: TemperatureMoment,
        dewpointMoment: DewpointMoment,
        wetbulbMoment: WetbulbMoment,
        feelsLikeMoment: FeelsLikeMoment,
        minTempMoment: TemperatureMoment,
        maxTempMoment: TemperatureMoment,
        conditionMoment: ConditionMoment
    ): TemperatureGraphPoint = TemperatureGraphPoint(
        time = GraphTime(tempMoment.hour, now),
        temperature = GraphTemperature(
            value = tempMoment.temperature,
            meta = getTempMeta(minTempMoment, maxTempMoment, tempMoment)
        ),
        dewpoint = GraphTemperature(
            value = dewpointMoment.temperature,
            meta = GraphTemperature.Meta.Regular
        ),
        wetbulbTemperature = GraphTemperature(
            value = wetbulbMoment.temperature,
            meta = GraphTemperature.Meta.Regular
        ),
        feelsLike = GraphTemperature(
            value = feelsLikeMoment.temperature,
            meta = GraphTemperature.Meta.Regular
        ),
        condition = conditionMoment.condition,
    )

    private fun getTempMeta(
        minTempMoment: TemperatureMoment,
        maxTempMoment: TemperatureMoment,
        tempMoment: TemperatureMoment
    ): GraphTemperature.Meta =
        when {
            minTempMoment == maxTempMoment -> GraphTemperature.Meta.Regular
            tempMoment == minTempMoment -> GraphTemperature.Meta.Minimum
            tempMoment == maxTempMoment -> GraphTemperature.Meta.Maximum
            else -> GraphTemperature.Meta.Regular
        }
}

data class TemperatureGraphs(
    val minTemp: Temperature,
    val maxTemp: Temperature,
    val graphs: List<TemperatureGraph>
)

data class TemperatureGraph(
    val day: LocalDate,
    val points: List<TemperatureGraphPoint>
)

data class TemperatureGraphPoint(
    val time: GraphTime,
    val temperature: GraphTemperature,
    val dewpoint: GraphTemperature,
    val wetbulbTemperature: GraphTemperature,
    val feelsLike: GraphTemperature,
    val condition: Condition
)

data class GraphTemperature(
    val value: Temperature,
    val meta: Meta
) {
    enum class Meta {
        Minimum, Maximum, Regular
    }
}