package com.example.globe.world;

/** Pure world-calendar and solar calculations; callers supply Latitude's north-positive latitude. */
public final class LatitudeCalendarMath {
    public static final long TICKS_PER_DAY = 24_000L;
    public static final int DAYS_PER_YEAR = 256;
    public static final int DAYS_PER_SEASON = 64;
    public static final int DAYS_PER_MONTH = 32;
    public static final int DAYS_PER_WEEK = 8;
    public static final int DAYS_PER_LUNAR_CYCLE = 32;
    public static final int MONTHS_PER_YEAR = DAYS_PER_YEAR / DAYS_PER_MONTH;
    public static final int SEASONS_PER_YEAR = 4;
    public static final double AXIAL_TILT_DEGREES = 23.44;

    private static final double TAU = Math.PI * 2.0;
    private static final double AXIAL_TILT_RADIANS = Math.toRadians(AXIAL_TILT_DEGREES);

    private LatitudeCalendarMath() {
    }

    public enum Season {
        SPRING,
        SUMMER,
        FALL,
        WINTER;

        public Season opposite() {
            return switch (this) {
                case SPRING -> FALL;
                case SUMMER -> WINTER;
                case FALL -> SPRING;
                case WINTER -> SUMMER;
            };
        }
    }

    public enum LunarPhase {
        NEW,
        WAXING_CRESCENT,
        FIRST_QUARTER,
        WAXING_GIBBOUS,
        FULL,
        WANING_GIBBOUS,
        LAST_QUARTER,
        WANING_CRESCENT;

        private static LunarPhase fromDayZero(int dayZero) {
            return values()[Math.floorMod(dayZero, DAYS_PER_LUNAR_CYCLE) / 4];
        }
    }

    public record CalendarDate(
            long year,
            int dayOfYear,
            int month,
            int dayOfMonth,
            int weekOfYear,
            int weekOfMonth,
            int dayOfWeek,
            int lunarDay,
            LunarPhase lunarPhase,
            Season season,
            double yearProgress
    ) {
    }

    public record SolarPosition(
            double latitudeDegrees,
            double longitudeDegrees,
            double localDayProgress,
            double localSolarTimeHours,
            double solarDeclinationDegrees,
            double solarElevationRadians,
            double daylightFraction,
            double dailySolarEnergy,
            double solarHeatSignal,
            double seasonalStrength,
            double equatorialWetness,
            double lunarDeclinationDegrees,
            double lunarElevationRadians,
            double lunarIllumination,
            double skyLightFactor,
            boolean polarDay,
            boolean polarNight,
            Season localSeason
    ) {
    }

    public static CalendarDate calendarAt(long worldTimeTicks) {
        long absoluteDay = Math.floorDiv(worldTimeTicks, TICKS_PER_DAY);
        int dayZero = (int) Math.floorMod(absoluteDay, DAYS_PER_YEAR);
        int dayOfMonthZero = Math.floorMod(dayZero, DAYS_PER_MONTH);
        int weekOfMonthZero = dayOfMonthZero / DAYS_PER_WEEK;
        int lunarDayZero = (int) Math.floorMod(absoluteDay, DAYS_PER_LUNAR_CYCLE);
        double dayProgress = dayProgress(worldTimeTicks);
        double yearProgress = (dayZero + dayProgress) / (double) DAYS_PER_YEAR;

        return new CalendarDate(
                Math.floorDiv(absoluteDay, DAYS_PER_YEAR) + 1L,
                dayZero + 1,
                dayZero / DAYS_PER_MONTH + 1,
                dayOfMonthZero + 1,
                dayZero / DAYS_PER_WEEK + 1,
                weekOfMonthZero + 1,
                dayOfMonthZero % DAYS_PER_WEEK + 1,
                lunarDayZero + 1,
                LunarPhase.fromDayZero(lunarDayZero),
                seasonForPhase(yearProgress),
                yearProgress
        );
    }

    /** Returns the calendar date at a longitude's local solar midnight boundary. */
    public static CalendarDate calendarAt(long worldTimeTicks, double longitudeDegrees) {
        double longitude = clamp(longitudeDegrees, -180.0, 180.0);
        long localOffsetTicks = Math.round(longitude / 360.0 * TICKS_PER_DAY);
        return calendarAt(worldTimeTicks + localOffsetTicks);
    }

    /** Uses elapsed world age for the date and mutable vanilla day-time for local midnight. */
    public static CalendarDate calendarAt(long calendarTicks, long timeOfDayTicks, double longitudeDegrees) {
        double longitude = clamp(longitudeDegrees, -180.0, 180.0);
        long elapsedDay = Math.floorDiv(calendarTicks, TICKS_PER_DAY);
        long dayTime = Math.floorMod(timeOfDayTicks, TICKS_PER_DAY);
        long localOffsetTicks = Math.round(longitude / 360.0 * TICKS_PER_DAY);
        return calendarAt(elapsedDay * TICKS_PER_DAY + dayTime + localOffsetTicks);
    }

    /** Latitude is north-positive degrees in [-90, 90]. Longitude is east-positive degrees in [-180, 180]. */
    public static SolarPosition solarAt(long worldTimeTicks, double latitudeDegrees) {
        return solarAt(worldTimeTicks, worldTimeTicks, latitudeDegrees, 0.0);
    }

    /** Latitude/longitude solar position with one source for elapsed date and one for mutable daily time. */
    public static SolarPosition solarAt(long calendarTicks, long timeOfDayTicks,
                                        double latitudeDegrees, double longitudeDegrees) {
        return solarAtInternal(calendarTicks, timeOfDayTicks, latitudeDegrees, longitudeDegrees);
    }

    /**
     * Computes the Sun and Moon at a geographic position. Minecraft time is
     * treated as dawn at 0 ticks, noon at 6,000 ticks, sunset at 12,000,
     * and midnight at 18,000; longitude shifts only the local daily phase.
     */
    public static SolarPosition solarAt(long worldTimeTicks, double latitudeDegrees, double longitudeDegrees) {
        return solarAtInternal(worldTimeTicks, worldTimeTicks, latitudeDegrees, longitudeDegrees);
    }

    private static SolarPosition solarAtInternal(long calendarTicks, long timeOfDayTicks,
                                                  double latitudeDegrees, double longitudeDegrees) {
        double latitude = clamp(latitudeDegrees, -90.0, 90.0);
        double longitude = clamp(longitudeDegrees, -180.0, 180.0);
        double latitudeRadians = Math.toRadians(latitude);
        double yearProgress = calendarAt(calendarTicks).yearProgress();
        double localDayProgress = fraction(dayProgress(timeOfDayTicks) + longitude / 360.0);
        double localSolarTimeHours = fraction(localDayProgress + 0.25) * 24.0;
        double declinationRadians = AXIAL_TILT_RADIANS * Math.sin(TAU * yearProgress);
        double hourAngleRadians = TAU * (localDayProgress - 0.25);
        double elevationSin = Math.sin(latitudeRadians) * Math.sin(declinationRadians)
                + Math.cos(latitudeRadians) * Math.cos(declinationRadians) * Math.cos(hourAngleRadians);
        double solarElevationRadians = Math.asin(clamp(elevationSin, -1.0, 1.0));

        double sunsetCosine = -Math.tan(latitudeRadians) * Math.tan(declinationRadians);
        double sunsetHourAngle;
        boolean polarDay;
        boolean polarNight;
        if (sunsetCosine <= -1.0) {
            sunsetHourAngle = Math.PI;
            polarDay = true;
            polarNight = false;
        } else if (sunsetCosine >= 1.0) {
            sunsetHourAngle = 0.0;
            polarDay = false;
            polarNight = true;
        } else {
            sunsetHourAngle = Math.acos(sunsetCosine);
            polarDay = false;
            polarNight = false;
        }

        double daylightFraction = sunsetHourAngle / Math.PI;
        double dailySolarEnergy = dailySolarEnergy(latitudeRadians, declinationRadians, sunsetHourAngle);
        double equinoxEnergy = dailySolarEnergy(latitudeRadians, 0.0, sunsetHourAngleFor(latitudeRadians, 0.0));
        double solarHeatSignal = clamp(
                (dailySolarEnergy - equinoxEnergy) / Math.max(equinoxEnergy, 0.05),
                -1.0,
                1.0
        );
        double absoluteLatitude = Math.abs(latitude) / 90.0;
        double seasonalStrength = smoothstep(clamp((absoluteLatitude - 0.08) / 0.72, 0.0, 1.0));
        double equatorialWetness = 0.5 + 0.5 * Math.cos(TAU * 2.0 * yearProgress);
        int lunarDayZero = (int) Math.floorMod(Math.floorDiv(calendarTicks, TICKS_PER_DAY), DAYS_PER_LUNAR_CYCLE);
        double lunarProgress = (lunarDayZero + localDayProgress) / (double) DAYS_PER_LUNAR_CYCLE;
        double lunarPhaseRadians = TAU * lunarProgress;
        double lunarDeclinationRadians = Math.toRadians(5.14) * Math.sin(lunarPhaseRadians);
        double lunarHourAngleRadians = hourAngleRadians + lunarPhaseRadians;
        double lunarElevationSin = Math.sin(latitudeRadians) * Math.sin(lunarDeclinationRadians)
                + Math.cos(latitudeRadians) * Math.cos(lunarDeclinationRadians) * Math.cos(lunarHourAngleRadians);
        double lunarElevationRadians = Math.asin(clamp(lunarElevationSin, -1.0, 1.0));
        double lunarIllumination = 0.5 - 0.5 * Math.cos(lunarPhaseRadians);
        double sunLight = clamp((Math.sin(solarElevationRadians) + 0.08) / 1.08, 0.0, 1.0);
        double moonLight = lunarIllumination
                * clamp((Math.sin(lunarElevationRadians) + 0.04) / 1.04, 0.0, 1.0)
                * 0.18;
        double skyLightFactor = Math.max(sunLight, moonLight);
        double localPhase = latitude < 0.0 ? yearProgress + 0.5 : yearProgress;

        return new SolarPosition(
                latitude,
                longitude,
                localDayProgress,
                localSolarTimeHours,
                Math.toDegrees(declinationRadians),
                solarElevationRadians,
                daylightFraction,
                dailySolarEnergy,
                solarHeatSignal,
                seasonalStrength,
                equatorialWetness,
                Math.toDegrees(lunarDeclinationRadians),
                lunarElevationRadians,
                lunarIllumination,
                skyLightFactor,
                polarDay,
                polarNight,
                seasonForPhase(localPhase)
        );
    }

    private static double sunsetHourAngleFor(double latitudeRadians, double declinationRadians) {
        double cosine = -Math.tan(latitudeRadians) * Math.tan(declinationRadians);
        if (cosine <= -1.0) {
            return Math.PI;
        }
        if (cosine >= 1.0) {
            return 0.0;
        }
        return Math.acos(cosine);
    }

    private static double dailySolarEnergy(double latitudeRadians, double declinationRadians, double sunsetHourAngle) {
        double energy = (
                sunsetHourAngle * Math.sin(latitudeRadians) * Math.sin(declinationRadians)
                        + Math.cos(latitudeRadians) * Math.cos(declinationRadians) * Math.sin(sunsetHourAngle)
        ) / Math.PI;
        return Math.max(0.0, energy);
    }

    private static Season seasonForPhase(double phase) {
        double normalized = phase - Math.floor(phase);
        int seasonIndex = (int) Math.floor(normalized * SEASONS_PER_YEAR);
        return Season.values()[Math.min(SEASONS_PER_YEAR - 1, seasonIndex)];
    }

    private static double dayProgress(long worldTimeTicks) {
        return Math.floorMod(worldTimeTicks, TICKS_PER_DAY) / (double) TICKS_PER_DAY;
    }

    private static double fraction(double value) {
        return value - Math.floor(value);
    }

    private static double smoothstep(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
