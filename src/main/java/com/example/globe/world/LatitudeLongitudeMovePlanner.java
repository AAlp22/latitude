package com.example.globe.world;

/**
 * Selects which mapped movements are safe for the first player-only prototype.
 */
public final class LatitudeLongitudeMovePlanner {
    private final LatitudeWorldTopologyMapper mapper;
    private final double maxCrossingStepBlocks;
    private final double triggerMarginBlocks;

    public LatitudeLongitudeMovePlanner(double borderDiameterBlocks, double centerX, double centerZ,
                                        double maxCrossingStepBlocks, double triggerMarginBlocks) {
        requirePositiveFinite(maxCrossingStepBlocks, "maxCrossingStepBlocks");
        requirePositiveFinite(triggerMarginBlocks, "triggerMarginBlocks");
        this.mapper = new LatitudeWorldTopologyMapper(borderDiameterBlocks, centerX, centerZ);
        this.maxCrossingStepBlocks = maxCrossingStepBlocks;
        this.triggerMarginBlocks = triggerMarginBlocks;
    }

    public Plan plan(double currentWorldX, double currentWorldZ,
                     double targetWorldX, double targetWorldZ) {
        LatitudeWorldTopologyMapper.MovementResult movement = mapper.mapMovement(
                currentWorldX,
                currentWorldZ,
                targetWorldX,
                targetWorldZ,
                maxCrossingStepBlocks,
                triggerMarginBlocks);

        boolean intercept = movement.accepted()
                && movement.crossedLongitude()
                && !movement.crossedPole();
        return new Plan(intercept, intercept ? movement.position() : null, movement);
    }

    public Plan planWithPoleTriggers(double currentWorldX, double currentWorldZ,
                                     double targetWorldX, double targetWorldZ) {
        LatitudeWorldTopologyMapper.MovementResult movement = mapper.mapMovementWithTriggers(
                currentWorldX,
                currentWorldZ,
                targetWorldX,
                targetWorldZ,
                maxCrossingStepBlocks,
                triggerMarginBlocks,
                triggerMarginBlocks);

        boolean intercept = movement.accepted()
                && (movement.crossedLongitude() || movement.crossedPole());
        return new Plan(intercept, intercept ? movement.position() : null, movement);
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and greater than zero");
        }
    }

    public record Plan(
            boolean intercept,
            LatitudeWorldTopologyMapper.MappedPosition position,
            LatitudeWorldTopologyMapper.MovementResult movement
    ) {
    }
}
