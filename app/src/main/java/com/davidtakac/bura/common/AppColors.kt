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

package com.davidtakac.bura.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

data class AppColors(
    private val temperatureColors: List<Color>,
    private val windSpeedColorsMetersPerSecond: List<Color>,
    private val uvIndexColors: Map<Int, Color>,
    val popColor: Color,
    val rainColor: Color,
    val showersColor: Color,
    val snowColor: Color,
    val precipitationColor: Color,
) {
    fun temperatureColors(fromCelsius: Double, toCelsius: Double): List<Color> =
        temperatureColors.slice(getIndexOfNearestColorTemperature(fromCelsius)..getIndexOfNearestColorTemperature(toCelsius))

    fun windSpeedColors(fromMetersPerSecond: Double, toMetersPerSecond: Double): List<Color> =
        windSpeedColorsMetersPerSecond.slice(getIndexOfNearestColorWindSpeed(fromMetersPerSecond)..getIndexOfNearestColorWindSpeed(toMetersPerSecond))

    val uvIndexColorStops: List<Pair<Float, Color>>  get() =
        uvIndexColors.map { it.key / 11f to it.value }

    private fun getIndexOfNearestColorWindSpeed(windSpeedMs: Double): Int =
        windSpeedMs.roundToInt().coerceIn(0, 100)
    private fun getIndexOfNearestColorTemperature(celsius: Double): Int =
        40 + celsius.roundToInt().coerceIn(-40, 55)

    companion object {
        val ForDarkTheme get() = AppColors(
            temperatureColors = darkTemperatureColors,
            windSpeedColorsMetersPerSecond = darkWindSpeedColorsMetersPerSecond,
            uvIndexColors = darkUvIndexColors,
            popColor = Color(0xFF64B5F6),
            rainColor = Color(0xFF64B5F6),
            showersColor = Color(0xFF4DB6AC),
            snowColor = Color(0xFFE0E0E0),
            precipitationColor = Color(0xFF9575CD),
        )

        val ForLightTheme get() = AppColors(
            temperatureColors = darkTemperatureColors,
            windSpeedColorsMetersPerSecond = darkWindSpeedColorsMetersPerSecond,
            uvIndexColors = darkUvIndexColors,
            popColor = Color(0xFF2196F3),
            rainColor = Color(0xFF2196F3),
            showersColor = Color(0xFF009688),
            snowColor = Color(0xFF9E9E9E),
            precipitationColor = Color(0xFF3F51B5),
        )
    }
}

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        temperatureColors = listOf(),
        windSpeedColorsMetersPerSecond = listOf(),
        popColor = Color.Unspecified,
        rainColor = Color.Unspecified,
        showersColor = Color.Unspecified,
        snowColor = Color.Unspecified,
        precipitationColor = Color.Unspecified,
        uvIndexColors = mapOf()
    )
}

private val darkWindSpeedColorsMetersPerSecond = listOf(
    Color(113, 64, 158), // 0 m/s 0 Bft
    Color(87, 76, 158), // 1 m/s 1 Bft
    Color(95, 65, 158), // 2 m/s 2 Bft
    Color(75, 75, 158), // 3 m/s
    Color(65, 80, 158), // 4 m/s, 3 Bft
    Color(70, 100, 158), // 5 m/s
    Color(70, 120, 158), // 6 m/s, 4 Bft
    Color(79, 139, 154), // 7 m/s
    Color(30, 168, 80), // 8 m/s, 5 Bft
    Color(53, 168, 70), // 9 m/s
    Color(76, 168, 60), // 10 m/s
    Color(100, 168, 50), // 11 m/s, 6 Bft
    Color(148, 187, 65),
    Color(195, 210, 79), // 13 m/s
    Color(240, 240, 90), // 14 m/s, 7 Bft
    Color(237, 210, 77),
    Color(235, 180, 55), // 16 m/s
    Color(240, 100, 45), // 17 m/s, 8 Bft
    Color(230, 90, 35),
    Color(220, 80, 25), // 20 m/s
    Color(150, 50, 45), // 21 m/s, 9 Bft
    Color(111, 40, 52), // 22 m/s
    Color(94, 30, 58),  // 23 m/s
    Color(65, 20, 65), // 24 m/s
    Color(85, 20, 95), // 25 m/s, 10 Bft
    Color(112, 45, 131), // 26 m/s
    Color(139, 70, 168), // 27 m/s
    Color(165, 95, 205), // 28 m/s
    Color(200, 100, 220), // 29 m/s, 11 Bft
    Color(215, 140, 230), // 30 m/s
    Color(230, 180, 240), // 31 m/s
    Color(245, 220, 250), // 32 m/s
    Color(220, 220, 220), // 33 m/s, 12 Bft
    Color(197, 197, 197), // 34 m/s
    Color(164, 164, 164), // 35 m/s
    Color(140, 140, 140), // 36 m/s
    Color(117, 117, 117), // 37 m/s
    Color(94, 94, 94), // 38 m/s
    Color(70, 70, 70), // 39 m/s
    Color(57, 57, 57), // 40 m/s
    Color(34, 34, 34), // 41 m/s
    Color(10, 10, 10), // 42 m/s
    Color(20, 20, 20), // 43 m/s,
    Color(53, 53, 40), // 44 m/s
    Color(86, 86, 60), // 45 m/s
    Color(119, 119, 80), // 46 m/s
    Color(152, 152, 100), // 47 m/s
    Color(185, 185, 120), // 48 m/s
    Color(195, 215, 135), // 49 m/s
    Color(173, 194, 124), // 50 m/s
    Color(151, 173, 113), // 51 m/s
    Color(130, 153, 103), // 52 m/s
    Color(108, 132, 92), // 53 m/s
    Color(86, 111, 81), // 54 m/s
    Color(64, 90, 71), // 55 m/s
    Color(42, 70, 60), // 56 m/s
    Color(20, 50, 50), // 57 m/s
    Color(15, 35, 40), // 58 m/s
    Color(51, 52, 56), // 59 m/s
    Color(87, 69, 72), // 60 m/s
    Color(122, 85, 87), // 61 m/s
    Color(157, 102, 103), // 62 m/s
    Color(195, 119, 119), // 63 m/s
    Color(230, 135, 135), // 64 m/s
    Color(201, 118, 117), // 65 m/s
    Color(172, 101, 99), // 66 m/s
    Color(143, 84, 81), // 67 m/s
    Color(114, 67, 63), // 68 m/s
    Color(85, 50, 45), // 69 m/s
    Color(75, 40, 35), // 70 m/s
)

private val darkTemperatureColors = listOf(
    Color(240, 240, 180), //  54...53
    Color(240, 240, 140), //  53...52
    Color(240, 240, 100), //  52...51
    Color(220, 220, 100), //  51...50
    Color(200, 200, 100), //  50...49
    Color(100, 100, 100), //  49...48
    Color(120, 120, 120), //  48...47
    Color(140, 140, 140), //  47...46
    Color(160, 160, 160), //  46...45
    Color(180, 180, 180), //  45...44
    Color(200, 200, 200), //  44...43
    Color(220, 192, 200), //  43...42
    Color(241, 143, 200), //  42...41
    Color(238, 124, 180), //  41...40
    Color(236, 115, 150), //  40...39
    Color(200,  92, 120), //  39...38
    Color(165,  78,  69), //  38...37
    Color(130,  55,  39), //  37...36
    Color( 94,  31,  19), //  36...35
    Color( 58,   8,   5), //  35...34
    Color( 72,  13,   9), //  34...33
    Color( 85,  19,  13), //  33...32
    Color( 99,  24,  17), //  32...31
    Color(112,  30,  21), //  31...30
    Color(142,  45,  31), //  30...29
    Color(153,  64,  39), //  29...28
    Color(162,  83,  47), //  28...27
    Color(172, 102,  54), //  27...26
    Color(181, 122,  62), //  26...25
    Color(190, 141,  70), //  25...24
    Color(200, 160,  78), //  24...23
    Color(209, 179,  86), //  23...22
    Color(217, 198,  94), //  22...21
    Color(226, 217, 102), //  21...20
    Color(208, 211, 105), //  20...19
    Color(191, 198,  98), //  19...18
    Color(173, 186,  90), //  18...17
    Color(155, 173,  83), //  17...16
    Color(138, 160,  75), //  16...15
    Color(120, 147,  68), //  15...14
    Color(102, 135,  60), //  14...13
    Color( 85, 122,  53), //  13...12
    Color( 67, 109,  45), //  12...11
    Color( 49,  96,  38), //  11...10
    Color( 62, 112,  51), //  10...9
    Color( 73, 122,  61), //   9...8
    Color( 82, 132,  71), //   8...7
    Color( 91, 142,  81), //   7...6
    Color(100, 152,  90), //   6
    Color(109, 161, 120), //   5
    Color(118, 171, 133), //   4
    Color(127, 181, 145), //   3
    Color(136, 191, 158), //   2
    Color(146, 200, 171), //   1
    Color(192, 240, 235), //   0
    Color(175, 218, 221), //  -1...-2
    Color(157, 196, 207), //  -2...-3
    Color(138, 175, 194), //  -3...-4
    Color(120, 153, 180), //  -4...-5
    Color(102, 131, 166), //  -5...-6
    Color( 83, 109, 153), //  -6...-7
    Color( 65,  88, 139), //  -7...-8
    Color( 46,  66, 125), //  -8...-9
    Color( 27,  45, 112), //  -9...-10
    Color( 17,  24,  95), // -10...-11
    Color( 37,  43, 108), // -11...-12
    Color( 57,  63, 121), // -12...-13
    Color( 77,  82, 133), // -13...-14
    Color( 98, 102, 146), // -14...-15
    Color(118, 121, 159), // -15...-16
    Color(138, 141, 171), // -16...-17
    Color(158, 160, 184), // -17...-18
    Color(179, 180, 197), // -18...-19
    Color(200, 200, 210), // -19...-20
    Color(210, 194, 203), // -20...-21
    Color(210, 176, 197), // -21...-22
    Color(200, 163, 191), // -22...-23
    Color(191, 149, 185), // -23...-24
    Color(182, 134, 179), // -24...-25
    Color(173, 120, 173), // -25...-26
    Color(164, 106, 167), // -26...-27
    Color(155,  94, 161), // -27...-28
    Color(146,  82, 155), // -28...-29
    Color(137,  70, 149), // -29...-30
    Color(100,  41, 105), // -30...-31
    Color( 94,  37,  99), // -31...-32
    Color( 87,  34,  93), // -32...-33
    Color( 81,  30,  86), // -33...-34
    Color( 74,  26,  80), // -34...-35
    Color( 68,  22,  74), // -35...-36
    Color( 61,  18,  66), // -36...-37
    Color( 55,  14,  60), // -37...-38
    Color( 48,  10,  54), // -38...-39
    Color( 41,   6,  48), // -39...-40
    Color(175, 134, 118), // -40...-41
).reversed()

private val darkUvIndexColors = mapOf(
    2 to Color(0xFF8BC34A),
    5 to Color(0xFFFFEB3B),
    7 to Color(0xFFFF9800),
    10 to Color(0xFFF44336),
    11 to Color(0xFF8E24AA)
)
