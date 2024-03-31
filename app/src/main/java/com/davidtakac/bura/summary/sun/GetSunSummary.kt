/*
 * This file is part of Bura.
 *
 * Bura is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Bura is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Bura. If not, see <https://www.gnu.org/licenses/>.
 */

package com.davidtakac.bura.summary.sun

import com.davidtakac.bura.place.Location
import com.davidtakac.bura.forecast.ForecastResult
import com.davidtakac.bura.sun.SunEvent
import com.davidtakac.bura.sun.SunMoment
import com.davidtakac.bura.sun.SunRepository
import com.davidtakac.bura.units.Units
import com.davidtakac.bura.condition.ConditionRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private const val LATER_HOUR_THRESH = 25

class GetSunSummary(
    private val sunRepo: SunRepository,
    private val descRepo: ConditionRepository,
) {
    suspend operator fun invoke(location: Location, units: Units, now: LocalDateTime): ForecastResult<SunSummary> {
        val futureSun = sunRepo.period(location, units)?.momentsFrom(now)
        val firstSun = futureSun?.firstOrNull()
        return when {
            firstSun == null -> outOfSight(now, location, units)
            firstSun.event == SunEvent.Sunrise -> ForecastResult.Success(sunrise(now, location.timeZone, futureSun, firstSun))
            else -> ForecastResult.Success(sunset(now, location.timeZone, futureSun, firstSun))
        }
    }

    private suspend fun outOfSight(now: LocalDateTime, location: Location, units: Units): ForecastResult<SunSummary> {
        val descPeriod = descRepo.period(location, units) ?: return ForecastResult.FailedToDownload
        val futureDesc = descPeriod.momentsFrom(now) ?: return ForecastResult.Outdated
        val isDayNow = futureDesc[now]!!.condition.isDay
        val lastMoment = futureDesc.last().hour
        val duration = Duration.between(now, lastMoment).plusHours(1)
        return ForecastResult.Success(if (isDayNow) {
            Sunrise.OutOfSight(duration)
        } else {
            Sunset.OutOfSight(duration)
        })
    }

    private fun sunrise(
        now: LocalDateTime,
        timeZone: ZoneId,
        futureSun: List<SunMoment>,
        firstSun: SunMoment
    ): Sunrise {
        val sunrise = firstSun.time
        return if (ChronoUnit.HOURS.between(now, sunrise) >= LATER_HOUR_THRESH) {
            Sunrise.Later(sunrise.atZone(timeZone).toLocalDateTime())
        } else {
            val sunset = futureSun[1].time
            if (ChronoUnit.HOURS.between(now, sunset) < LATER_HOUR_THRESH) {
                Sunrise.WithSunsetSoon(
                    time = sunrise.atZone(timeZone).toLocalTime(),
                    sunset = futureSun[1].time.atZone(timeZone).toLocalTime()
                )
            } else {
                Sunrise.WithSunsetLater(
                    time = sunrise.atZone(timeZone).toLocalTime(),
                    sunset = futureSun[1].time.atZone(timeZone).toLocalDateTime()
                )
            }
        }
    }

    private fun sunset(
        now: LocalDateTime,
        timeZone: ZoneId,
        futureSun: List<SunMoment>,
        firstSun: SunMoment
    ): Sunset {
        val sunset = firstSun.time
        return if (ChronoUnit.HOURS.between(now, sunset) >= LATER_HOUR_THRESH) {
            Sunset.Later(sunset.atZone(timeZone).toLocalDateTime())
        } else {
            val sunrise = futureSun[1].time
            if (ChronoUnit.HOURS.between(now, sunrise) < LATER_HOUR_THRESH) {
                Sunset.WithSunriseSoon(
                    time = firstSun.time.atZone(timeZone).toLocalTime(),
                    sunrise = futureSun[1].time.atZone(timeZone).toLocalTime()
                )
            } else {
                Sunset.WithSunriseLater(
                    time = firstSun.time.atZone(timeZone).toLocalTime(),
                    sunrise = futureSun[1].time.atZone(timeZone).toLocalDateTime()
                )
            }
        }
    }
}

sealed interface SunSummary

sealed interface Sunrise : SunSummary {
    data class WithSunsetSoon(
        val time: LocalTime,
        val sunset: LocalTime
    ) : Sunrise

    data class WithSunsetLater(
        val time: LocalTime,
        val sunset: LocalDateTime
    ) : Sunrise

    data class Later(val time: LocalDateTime) : Sunrise

    data class OutOfSight(val forDuration: Duration) : Sunrise
}

sealed interface Sunset : SunSummary {
    data class WithSunriseSoon(
        val time: LocalTime,
        val sunrise: LocalTime
    ) : Sunset

    data class WithSunriseLater(
        val time: LocalTime,
        val sunrise: LocalDateTime
    ) : Sunset

    data class Later(val time: LocalDateTime) : Sunset

    data class OutOfSight(val forDuration: Duration) : Sunset
}