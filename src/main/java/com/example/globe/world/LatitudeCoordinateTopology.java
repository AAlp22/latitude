package com.example.globe.world;

/**
 * Pure folded latitude/longitude mapping for the future physical topology.
 * The meridional coordinate starts at the North Pole and increases southward.
 */
public final class LatitudeCoordinateTopology {
    private static final double DEGREES_PER_CIRCLE = 360.0;
    private static final double DEGREES_PER_HALF_CIRCLE = 180.0;

    private final double circumferenceBlocks;
    private final double poleToPoleBlocks;
    private final double halfCircumferenceBlocks;

    public LatitudeCoordinateTopology(double circumferenceBlocks, double poleToPoleBlocks) {
        requirePositiveFinite(circumferenceBlocks, "circumferenceBlocks");
        requirePositiveFinite(poleToPoleBlocks, "poleToPoleBlocks");
        this.circumferenceBlocks = circumferenceBlocks;
        this.poleToPoleBlocks = poleToPoleBlocks;
        this.halfCircumferenceBlocks = circumferenceBlocks * 0.5;
    }

    public Coordinate map(double xBlocks, double meridionalBlocks) {
        requireFinite(xBlocks, "xBlocks");
        requireFinite(meridionalBlocks, "meridionalBlocks");

        double foldedMeridional = foldMeridional(meridionalBlocks);
        boolean northPole = foldedMeridional == 0.0;
        boolean southPole = foldedMeridional == poleToPoleBlocks;

        double wrappedX;
        double longitude;
        if (northPole || southPole) {
            wrappedX = 0.0;
            longitude = 0.0;
        } else {
            long meridionalBand = (long) Math.floor(meridionalBlocks / poleToPoleBlocks);
            double shiftedX = xBlocks + (Math.floorMod(meridionalBand, 2L) == 0L
                    ? 0.0
                    : halfCircumferenceBlocks);
            wrappedX = wrapX(shiftedX);
            longitude = wrappedX / circumferenceBlocks * DEGREES_PER_CIRCLE;
        }

        double latitude = DEGREES_PER_HALF_CIRCLE * 0.5
                - foldedMeridional / poleToPoleBlocks * DEGREES_PER_HALF_CIRCLE;
        return new Coordinate(wrappedX, foldedMeridional, latitude, longitude, northPole, southPole);
    }

    public double wrapX(double xBlocks) {
        requireFinite(xBlocks, "xBlocks");
        return positiveModulo(xBlocks + halfCircumferenceBlocks, circumferenceBlocks)
                - halfCircumferenceBlocks;
    }

    public double circumferenceBlocks() {
        return circumferenceBlocks;
    }

    public double poleToPoleBlocks() {
        return poleToPoleBlocks;
    }

    private double foldMeridional(double meridionalBlocks) {
        double period = poleToPoleBlocks * 2.0;
        double cyclePosition = positiveModulo(meridionalBlocks, period);
        return cyclePosition <= poleToPoleBlocks
                ? cyclePosition
                : period - cyclePosition;
    }

    private static double positiveModulo(double value, double modulus) {
        return value - Math.floor(value / modulus) * modulus;
    }

    private static void requirePositiveFinite(double value, String name) {
        requireFinite(value, name);
        if (value <= 0.0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    public record Coordinate(
            double wrappedXBlocks,
            double meridionalBlocks,
            double latitudeDegrees,
            double longitudeDegrees,
            boolean northPole,
            boolean southPole
    ) {
    }
}
