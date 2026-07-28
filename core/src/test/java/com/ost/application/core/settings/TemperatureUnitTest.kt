package com.ost.application.core.settings
import org.junit.Assert.assertEquals
import org.junit.Test
class TemperatureUnitTest {
    @Test
    fun `system unit resolves to fahrenheit only in fahrenheit countries`() {
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.SYSTEM.resolve("US"))
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.SYSTEM.resolve("us"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.SYSTEM.resolve("DE"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.SYSTEM.resolve("RU"))
    }
    @Test
    fun `explicit unit ignores country`() {
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.CELSIUS.resolve("US"))
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.FAHRENHEIT.resolve("DE"))
    }
    @Test
    fun `conversions round-trip`() {
        assertEquals(32.0, 0.0.celsiusToFahrenheit(), 0.0001)
        assertEquals(0.0, 32.0.fahrenheitToCelsius(), 0.0001)
        assertEquals(25.0, 25.0.celsiusToFahrenheit().fahrenheitToCelsius(), 0.0001)
    }
    @Test
    fun `formatting uses resolved unit and trims trailing zero`() {
        assertEquals("25°C", formatTemperature(25.0, TemperatureUnit.CELSIUS, "US"))
        assertEquals("32°F", formatTemperature(0.0, TemperatureUnit.SYSTEM, "US"))
        assertEquals("25.5°C", formatTemperature(25.5, TemperatureUnit.CELSIUS, "DE"))
    }
    @Test
    fun `unknown pref key falls back to default`() {
        assertEquals(TemperatureUnit.SYSTEM, TemperatureUnit.fromKey("garbage"))
        assertEquals(TemperatureUnit.SYSTEM, TemperatureUnit.fromKey(null))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromKey("CELSIUS"))
    }
}
