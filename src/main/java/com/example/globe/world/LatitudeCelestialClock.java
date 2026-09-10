package com.example.globe.world;

/** Keeps the celestial daily orbit continuous even when the Sun never sets or rises. */
public final class LatitudeCelestialClock {
    private LatitudeCelestialClock() {
    }

    public static double skyOrbitProgress(LatitudeCalendarMath.SolarPosition solar) {
        if (solar == null) {
            throw new IllegalArgumentException("solar must not be null");
        }

        double localDayProgress = fraction(solar.localDayProgress());
        if (solar.polarDay() || solar.polarNight()) {
            return localDayProgress;
        }

        double daylight = solar.daylightFraction();
        double sunrise = 0.25 - daylight * 0.5;
        double sunset = 0.25 + daylight * 0.5;
        if (localDayProgress >= sunrise && localDayProgress < sunset) {
            double localDay = (localDayProgress - sunrise) / daylight;
            return localDay * 0.5;
        }

        double nightLength = 1.0 - daylight;
        double localNight = localDayProgress < sunrise
                ? (localDayProgress + 1.0 - sunset) / nightLength
                : (localDayProgress - sunset) / nightLength;
        return 0.5 + localNight * 0.5;
    }

    private static double fraction(double value) {
        return value - Math.floor(value);
    }
}
