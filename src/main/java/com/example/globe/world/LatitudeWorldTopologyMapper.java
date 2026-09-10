package com.example.globe.world;

/**
 * Adapts the centered Minecraft world-border coordinates to the folded topology contract.
 */
public final class LatitudeWorldTopologyMapper {
    private final LatitudeCoordinateTopology topology;
    private final double centerX;
    private final double centerZ;
    private final double halfPoleSpanBlocks;

    public LatitudeWorldTopologyMapper(double borderDiameterBlocks, double centerX, double centerZ) {
        requirePositiveFinite(borderDiameterBlocks, "borderDiameterBlocks");
        requireFinite(centerX, "centerX");
        requireFinite(centerZ, "centerZ");
        this.topology = new LatitudeCoordinateTopology(borderDiameterBlocks, borderDiameterBlocks);
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.halfPoleSpanBlocks = borderDiameterBlocks * 0.5;
    }

    public MappedPosition map(double worldX, double worldZ) {
        LatitudeCoordinateTopology.Coordinate logical = topology.map(
                worldX - centerX,
                toMeridional(worldZ));
        return toWorldPosition(logical);
    }

    public MovementResult mapMovement(double currentWorldX, double currentWorldZ,
                                      double targetWorldX, double targetWorldZ,
                                      double maxCrossingStepBlocks,
                                      double triggerMarginBlocks) {
        LatitudeCoordinateTopology.MovementResult logical = topology.mapMovementWithLongitudeTrigger(
                currentWorldX - centerX,
                toMeridional(currentWorldZ),
                targetWorldX - centerX,
                toMeridional(targetWorldZ),
                maxCrossingStepBlocks,
                triggerMarginBlocks);

        MappedPosition position = logical.coordinate() == null
                ? null
                : toWorldPosition(logical.coordinate());
        return new MovementResult(
                logical.accepted(),
                logical.crossedLongitude(),
                logical.crossedPole(),
                position,
                logical.rejection());
    }

    public MovementResult mapMovementWithTriggers(double currentWorldX, double currentWorldZ,
                                                   double targetWorldX, double targetWorldZ,
                                                   double maxCrossingStepBlocks,
                                                   double longitudeTriggerMarginBlocks,
                                                   double poleTriggerMarginBlocks) {
        LatitudeCoordinateTopology.MovementResult logical = topology.mapMovementWithTriggers(
                currentWorldX - centerX,
                toMeridional(currentWorldZ),
                targetWorldX - centerX,
                toMeridional(targetWorldZ),
                maxCrossingStepBlocks,
                longitudeTriggerMarginBlocks,
                poleTriggerMarginBlocks);

        MappedPosition position = logical.coordinate() == null
                ? null
                : toWorldPosition(logical.coordinate());
        return new MovementResult(
                logical.accepted(),
                logical.crossedLongitude(),
                logical.crossedPole(),
                position,
                logical.rejection());
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public double borderDiameterBlocks() {
        return topology.circumferenceBlocks();
    }

    private double toMeridional(double worldZ) {
        return worldZ - centerZ + halfPoleSpanBlocks;
    }

    private MappedPosition toWorldPosition(LatitudeCoordinateTopology.Coordinate logical) {
        return new MappedPosition(
                centerX + logical.wrappedXBlocks(),
                centerZ - halfPoleSpanBlocks + logical.meridionalBlocks(),
                logical);
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

    public record MappedPosition(
            double worldX,
            double worldZ,
            LatitudeCoordinateTopology.Coordinate logical
    ) {
    }

    public record MovementResult(
            boolean accepted,
            boolean crossedLongitude,
            boolean crossedPole,
            MappedPosition position,
            LatitudeCoordinateTopology.Rejection rejection
    ) {
    }
}
