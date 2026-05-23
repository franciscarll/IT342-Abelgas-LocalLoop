package edu.cit.abelgas.localloop.shared.util

import edu.cit.abelgas.localloop.R

/**
 * WeatherUtils
 *
 * Maps condition strings to drawable resources.
 * Mirrors web WeatherIcon component condition checks:
 *   sunny / clear   → ic_weather_sunny
 *   rain / drizzle  → ic_weather_rainy
 *   cloud / overcast→ ic_weather_cloudy
 *   storm / thunder → ic_weather_storm
 *   else            → ic_weather_partly_cloudy  (default)
 */
object WeatherUtils {

    fun getWeatherIcon(condition: String): Int {
        val c = condition.lowercase()
        return when {
            c.contains("sunny")   || c.contains("clear")   -> R.drawable.ic_weather_sunny
            c.contains("rain")    || c.contains("drizzle") -> R.drawable.ic_weather_rainy
            c.contains("cloud")   || c.contains("overcast")-> R.drawable.ic_weather_cloudy
            c.contains("storm")   || c.contains("thunder") -> R.drawable.ic_weather_storm
            else                                            -> R.drawable.ic_weather_partly_cloudy
        }
    }
}