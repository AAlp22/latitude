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
        double orbit;
        if (localDayProgress >= sunrise && localDayProgress < sunset) {
            double localDay = (localDayProgress - sunrise) / daylight;
            orbit = localDay * 0.5;
        } else {
            double nightLength = 1.0 - daylight;
            double localNight = localDayProgress < sunrise
                    ? (localDayProgress + 1.0 - sunset) / nightLength
                    : (localDayProgress - sunset) / nightLength;
            orbit = 0.5 + localNight * 0.5;
        }
        // The compressed day/night arcs meet the polar full-orbit branch only at daylight
        // extremes; without blending, walking across the polar-day boundary made the sun's
        // sky position jump and change speed ("sun moves extremely quickly at the poles").
        double polarBlend = Math.max(
                Math.max(0.0, Math.min(1.0, (daylight - 0.75) / 0.25)),
                Math.max(0.0, Math.min(1.0, (0.25 - daylight) / 0.25)));
        return orbit * (1.0 - polarBlend) + localDayProgress * polarBlend;
    }

    private static double fraction(double value) {
        return value - Math.floor(value);
    }
}
