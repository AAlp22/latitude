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

    public MovementResult mapMovement(double currentXBlocks, double currentMeridionalBlocks,
                                      double targetXBlocks, double targetMeridionalBlocks,
                                      double maxCrossingStepBlocks) {
        requireFinite(currentXBlocks, "currentXBlocks");
        requireFinite(currentMeridionalBlocks, "currentMeridionalBlocks");
        requireFinite(targetXBlocks, "targetXBlocks");
        requireFinite(targetMeridionalBlocks, "targetMeridionalBlocks");
        requirePositiveFinite(maxCrossingStepBlocks, "maxCrossingStepBlocks");

        boolean currentXCanonical = currentXBlocks >= -halfCircumferenceBlocks
                && currentXBlocks < halfCircumferenceBlocks;
        boolean currentMeridionalCanonical = currentMeridionalBlocks >= 0.0
                && currentMeridionalBlocks <= poleToPoleBlocks;
        boolean crossesLongitude = targetXBlocks < -halfCircumferenceBlocks
                || targetXBlocks >= halfCircumferenceBlocks;
        boolean crossesPole = targetMeridionalBlocks < 0.0
                || targetMeridionalBlocks > poleToPoleBlocks;

        boolean nearLongitudeBoundary = currentXCanonical
                && (currentXBlocks - (-halfCircumferenceBlocks) <= maxCrossingStepBlocks
                || halfCircumferenceBlocks - currentXBlocks <= maxCrossingStepBlocks);
        boolean nearPoleBoundary = currentMeridionalCanonical
                && (currentMeridionalBlocks <= maxCrossingStepBlocks
                || poleToPoleBlocks - currentMeridionalBlocks <= maxCrossingStepBlocks);
        boolean smallHorizontalStep = Math.hypot(
                targetXBlocks - currentXBlocks,
                targetMeridionalBlocks - currentMeridionalBlocks) <= maxCrossingStepBlocks;

        if (crossesLongitude && crossesPole) {
            return MovementResult.rejected(Rejection.MULTIPLE_SEAMS);
        }
        if ((crossesLongitude && !nearLongitudeBoundary)
                || (crossesPole && !nearPoleBoundary)
                || ((crossesLongitude || crossesPole) && !smallHorizontalStep)) {
            return MovementResult.rejected(Rejection.OUT_OF_BOUNDS);
        }

        return MovementResult.accepted(
                map(targetXBlocks, targetMeridionalBlocks), crossesLongitude, crossesPole);
    }

    public MovementResult mapMovementWithLongitudeTrigger(double currentXBlocks, double currentMeridionalBlocks,
                                                           double targetXBlocks, double targetMeridionalBlocks,
                                                           double maxCrossingStepBlocks,
                                                           double triggerMarginBlocks) {
        requirePositiveFinite(triggerMarginBlocks, "triggerMarginBlocks");
        if (triggerMarginBlocks >= halfCircumferenceBlocks) {
            throw new IllegalArgumentException("triggerMarginBlocks must be smaller than half the circumference");
        }

        boolean targetAlreadyOutside = targetXBlocks < -halfCircumferenceBlocks
                || targetXBlocks >= halfCircumferenceBlocks;
        if (targetAlreadyOutside) {
            return mapMovement(currentXBlocks, currentMeridionalBlocks,
                    targetXBlocks, targetMeridionalBlocks, maxCrossingStepBlocks);
        }

        boolean movingEastIntoTrigger = targetXBlocks > currentXBlocks
                && targetXBlocks >= halfCircumferenceBlocks - triggerMarginBlocks;
        boolean movingWestIntoTrigger = targetXBlocks < currentXBlocks
                && targetXBlocks < -halfCircumferenceBlocks + triggerMarginBlocks;
        if (!movingEastIntoTrigger && !movingWestIntoTrigger) {
            return mapMovement(currentXBlocks, currentMeridionalBlocks,
                    targetXBlocks, targetMeridionalBlocks, maxCrossingStepBlocks);
        }

        double virtualTargetX = movingEastIntoTrigger
                ? targetXBlocks + triggerMarginBlocks
                : targetXBlocks - triggerMarginBlocks;
        return mapMovement(currentXBlocks, currentMeridionalBlocks,
                virtualTargetX, targetMeridionalBlocks, maxCrossingStepBlocks);
    }

    public MovementResult mapMovementWithPoleTrigger(double currentXBlocks, double currentMeridionalBlocks,
                                                      double targetXBlocks, double targetMeridionalBlocks,
                                                      double maxCrossingStepBlocks,
                                                      double triggerMarginBlocks) {
        requirePositiveFinite(triggerMarginBlocks, "triggerMarginBlocks");
        if (triggerMarginBlocks >= poleToPoleBlocks) {
            throw new IllegalArgumentException("triggerMarginBlocks must be smaller than the pole-to-pole span");
        }

        boolean targetAlreadyOutside = targetMeridionalBlocks < 0.0
                || targetMeridionalBlocks > poleToPoleBlocks;
        if (targetAlreadyOutside) {
            return mapMovement(currentXBlocks, currentMeridionalBlocks,
                    targetXBlocks, targetMeridionalBlocks, maxCrossingStepBlocks);
        }

        boolean movingNorthIntoTrigger = targetMeridionalBlocks < currentMeridionalBlocks
                && targetMeridionalBlocks < triggerMarginBlocks;
        boolean movingSouthIntoTrigger = targetMeridionalBlocks > currentMeridionalBlocks
                && targetMeridionalBlocks > poleToPoleBlocks - triggerMarginBlocks;
        if (!movingNorthIntoTrigger && !movingSouthIntoTrigger) {
            return mapMovement(currentXBlocks, currentMeridionalBlocks,
                    targetXBlocks, targetMeridionalBlocks, maxCrossingStepBlocks);
        }

        double virtualTargetMeridional = movingNorthIntoTrigger
                ? targetMeridionalBlocks - triggerMarginBlocks
                : targetMeridionalBlocks + triggerMarginBlocks;
        return mapMovement(currentXBlocks, currentMeridionalBlocks,
                targetXBlocks, virtualTargetMeridional, maxCrossingStepBlocks);
    }

    public MovementResult mapMovementWithTriggers(double currentXBlocks, double currentMeridionalBlocks,
                                                   double targetXBlocks, double targetMeridionalBlocks,
                                                   double maxCrossingStepBlocks,
                                                   double longitudeTriggerMarginBlocks,
                                                   double poleTriggerMarginBlocks) {
        requirePositiveFinite(longitudeTriggerMarginBlocks, "longitudeTriggerMarginBlocks");
        requirePositiveFinite(poleTriggerMarginBlocks, "poleTriggerMarginBlocks");
        if (longitudeTriggerMarginBlocks >= halfCircumferenceBlocks) {
            throw new IllegalArgumentException("longitudeTriggerMarginBlocks must be smaller than half the circumference");
        }
        if (poleTriggerMarginBlocks >= poleToPoleBlocks) {
            throw new IllegalArgumentException("poleTriggerMarginBlocks must be smaller than the pole-to-pole span");
        }

        boolean targetAlreadyOutside = targetXBlocks < -halfCircumferenceBlocks
                || targetXBlocks >= halfCircumferenceBlocks
                || targetMeridionalBlocks < 0.0
                || targetMeridionalBlocks > poleToPoleBlocks;
        if (targetAlreadyOutside) {
            return mapMovement(currentXBlocks, currentMeridionalBlocks,
                    targetXBlocks, targetMeridionalBlocks, maxCrossingStepBlocks);
        }

        boolean movingEastIntoTrigger = targetXBlocks > currentXBlocks
                && targetXBlocks >= halfCircumferenceBlocks - longitudeTriggerMarginBlocks;
        boolean movingWestIntoTrigger = targetXBlocks < currentXBlocks
                && targetXBlocks < -halfCircumferenceBlocks + longitudeTriggerMarginBlocks;
        boolean movingNorthIntoTrigger = targetMeridionalBlocks < currentMeridionalBlocks
                && targetMeridionalBlocks < poleTriggerMarginBlocks;
        boolean movingSouthIntoTrigger = targetMeridionalBlocks > currentMeridionalBlocks
                && targetMeridionalBlocks > poleToPoleBlocks - poleTriggerMarginBlocks;

        boolean longitudeTrigger = movingEastIntoTrigger || movingWestIntoTrigger;
        boolean poleTrigger = movingNorthIntoTrigger || movingSouthIntoTrigger;
        if (longitudeTrigger && poleTrigger) {
            return MovementResult.rejected(Rejection.MULTIPLE_SEAMS);
        }
        if (!longitudeTrigger && !poleTrigger) {
            return mapMovement(currentXBlocks, currentMeridionalBlocks,
                    targetXBlocks, targetMeridionalBlocks, maxCrossingStepBlocks);
        }

        double virtualTargetX = movingEastIntoTrigger
                ? targetXBlocks + longitudeTriggerMarginBlocks
                : movingWestIntoTrigger
                ? targetXBlocks - longitudeTriggerMarginBlocks
                : targetXBlocks;
        double virtualTargetMeridional = movingNorthIntoTrigger
                ? targetMeridionalBlocks - poleTriggerMarginBlocks
                : movingSouthIntoTrigger
                ? targetMeridionalBlocks + poleTriggerMarginBlocks
                : targetMeridionalBlocks;
        return mapMovement(currentXBlocks, currentMeridionalBlocks,
                virtualTargetX, virtualTargetMeridional, maxCrossingStepBlocks);
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

    public enum Rejection {
        NONE,
        OUT_OF_BOUNDS,
        MULTIPLE_SEAMS
    }

    public record MovementResult(
            boolean accepted,
            boolean crossedLongitude,
            boolean crossedPole,
            Coordinate coordinate,
            Rejection rejection
    ) {
        private static MovementResult accepted(Coordinate coordinate, boolean crossedLongitude, boolean crossedPole) {
            return new MovementResult(true, crossedLongitude, crossedPole, coordinate, Rejection.NONE);
        }

        private static MovementResult rejected(Rejection rejection) {
            return new MovementResult(false, false, false, null, rejection);
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
