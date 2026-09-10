package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeCelestialClockTest {
    private static final long[] DAILY_TIMES = {0L, 6_000L, 12_000L, 18_000L};

    @Test
    void polarDayKeepsTheDailyCelestialOrbitMoving() {
        for (long timeOfDay : DAILY_TIMES) {
            LatitudeCalendarMath.SolarPosition solar = LatitudeCalendarMath.solarAt(
                    64L * LatitudeCalendarMath.TICKS_PER_DAY, timeOfDay, 80.0, 0.0);

            assertTrue(solar.polarDay());
            assertEquals(timeOfDay / (double) LatitudeCalendarMath.TICKS_PER_DAY,
                    LatitudeCelestialClock.skyOrbitProgress(solar), 1.0e-9);
        }
    }

    @Test
    void polarNightKeepsTheDailyCelestialOrbitMoving() {
        for (long timeOfDay : DAILY_TIMES) {
            LatitudeCalendarMath.SolarPosition solar = LatitudeCalendarMath.solarAt(
                    192L * LatitudeCalendarMath.TICKS_PER_DAY, timeOfDay, 80.0, 0.0);

            assertTrue(solar.polarNight());
            assertEquals(timeOfDay / (double) LatitudeCalendarMath.TICKS_PER_DAY,
                    LatitudeCelestialClock.skyOrbitProgress(solar), 1.0e-9);
        }
    }
}
