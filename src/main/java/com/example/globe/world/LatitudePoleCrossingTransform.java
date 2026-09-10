package com.example.globe.world;

/**
 * Tangent approximation for the folded atlas at either pole.
 */
public final class LatitudePoleCrossingTransform {
    private static final double DEGREES_PER_CIRCLE = 360.0;
    private static final double POLE_REFLECTION_YAW_OFFSET = 180.0;

    private LatitudePoleCrossingTransform() {
    }

    public static Motion transform(double xVelocity, double meridionalVelocity, double yawDegrees) {
        requireFinite(xVelocity, "xVelocity");
        requireFinite(meridionalVelocity, "meridionalVelocity");
        requireFinite(yawDegrees, "yawDegrees");

        return new Motion(
                xVelocity,
                -meridionalVelocity,
                positiveModulo(POLE_REFLECTION_YAW_OFFSET - yawDegrees, DEGREES_PER_CIRCLE));
    }

    private static double positiveModulo(double value, double modulus) {
        return value - Math.floor(value / modulus) * modulus;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    public record Motion(
            double xVelocity,
            double meridionalVelocity,
            double yawDegrees
    ) {
    }
}
