package com.ost.application.core.settings
enum class TemperatureUnit {
    SYSTEM,
    CELSIUS,
    FAHRENHEIT;
    companion object {
        val DEFAULT = SYSTEM
        fun fromKey(key: String?): TemperatureUnit =
            entries.find { it.name == key } ?: DEFAULT
    }
}
object TemperaturePrefKeys {
    const val TEMPERATURE_UNIT = "temperature_unit"
}
fun TemperatureUnit.resolve(systemCountryCode: String): TemperatureUnit = when (this) {
    TemperatureUnit.SYSTEM -> {
        val fahrenheitCountries = setOf("US", "LR", "MM")
        if (systemCountryCode.uppercase() in fahrenheitCountries) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
    }
    else -> this
}
fun Double.celsiusToFahrenheit(): Double = this * 9.0 / 5.0 + 32.0
fun Double.fahrenheitToCelsius(): Double = (this - 32.0) * 5.0 / 9.0
fun formatTemperature(celsius: Double, unit: TemperatureUnit, systemCountryCode: String): String {
    val resolved = unit.resolve(systemCountryCode)
    return when (resolved) {
        TemperatureUnit.FAHRENHEIT -> "${celsius.celsiusToFahrenheit().roundToOneDecimalOrInt()}°F"
        else -> "${celsius.roundToOneDecimalOrInt()}°C"
    }
}
private fun Double.roundToOneDecimalOrInt(): String {
    val rounded = kotlin.math.round(this * 10) / 10
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}