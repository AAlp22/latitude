package com.example.globe.world;

/** Converts geographic solar declination into the Minecraft sky-matrix sign convention. */
public final class LatitudeCelestialGeometry {
    private LatitudeCelestialGeometry() {
    }

    public static double sunDeclinationRotationDegrees(LatitudeCalendarMath.SolarPosition solar) {
        if (solar == null) {
            throw new IllegalArgumentException("solar must not be null");
        }
        double declination = Math.max(
                -LatitudeCalendarMath.AXIAL_TILT_DEGREES,
                Math.min(LatitudeCalendarMath.AXIAL_TILT_DEGREES, solar.solarDeclinationDegrees())
        );
        return -declination;
    }
}
