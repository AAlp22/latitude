package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatitudeCelestialGeometryTest {
    @Test
    void northernSummerDeclinationTiltsTheSunTowardNegativeZNorth() {
        LatitudeCalendarMath.SolarPosition solar = LatitudeCalendarMath.solarAt(
                64L * LatitudeCalendarMath.TICKS_PER_DAY, 0.0, 0.0);

        assertEquals(23.44, solar.solarDeclinationDegrees(), 1.0e-9);
        assertEquals(-23.44, LatitudeCelestialGeometry.sunDeclinationRotationDegrees(solar), 1.0e-9);
    }

    @Test
    void northernWinterDeclinationTiltsTheSunTowardPositiveZSouth() {
        LatitudeCalendarMath.SolarPosition solar = LatitudeCalendarMath.solarAt(
                192L * LatitudeCalendarMath.TICKS_PER_DAY, 0.0, 0.0);

        assertEquals(-23.44, solar.solarDeclinationDegrees(), 1.0e-9);
        assertEquals(23.44, LatitudeCelestialGeometry.sunDeclinationRotationDegrees(solar), 1.0e-9);
    }
}
