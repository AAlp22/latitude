package com.example.globe.world;

/**
 * Periodic X-coordinate frame for future seam-aware queries and packet adapters.
 * It does not mutate Minecraft coordinates by itself.
 */
public final class LatitudeSeamCoordinateFrame {
    private static final double EPSILON = 1.0e-9;
    private static final long CHUNK_SIZE_BLOCKS = 16L;

    private final double circumferenceBlocks;
    private final double centerX;
    private final double halfCircumferenceBlocks;

    public LatitudeSeamCoordinateFrame(double circumferenceBlocks, double centerX) {
        requirePositiveFinite(circumferenceBlocks, "circumferenceBlocks");
        requireFinite(centerX, "centerX");
        this.circumferenceBlocks = circumferenceBlocks;
        this.centerX = centerX;
        this.halfCircumferenceBlocks = circumferenceBlocks * 0.5;
    }

    public double canonicalX(double worldX) {
        requireFinite(worldX, "worldX");
        return westBoundary() + positiveModulo(worldX - westBoundary(), circumferenceBlocks);
    }

    public Image imageForViewer(double observerX, double targetX) {
        requireFinite(observerX, "observerX");
        requireFinite(targetX, "targetX");

        long turns = translationTurns(targetX - observerX, circumferenceBlocks);
        double translationBlocks = turns * circumferenceBlocks;
        double imageX = targetX + translationBlocks;
        return new Image(imageX, turns, translationBlocks, imageX - observerX);
    }

    public BlockImage blockImageForViewer(int observerBlockX, int targetBlockX) {
        long integralCircumference = integralCircumference();
        if (integralCircumference <= 0L) {
            return BlockImage.rejected(Rejection.NON_INTEGRAL_CIRCUMFERENCE);
        }

        long turns = translationTurns((double) targetBlockX - observerBlockX, integralCircumference);
        try {
            long translationBlocks = Math.multiplyExact(turns, integralCircumference);
            long blockX = Math.addExact(targetBlockX, translationBlocks);
            return new BlockImage(true, blockX, turns, translationBlocks, Rejection.NONE);
        } catch (ArithmeticException e) {
            return BlockImage.rejected(Rejection.RANGE_OVERFLOW);
        }
    }

    public ChunkGrid chunkGrid() {
        double west = westBoundary();
        double east = eastBoundary();
        if (!isChunkBoundary(west) || !isChunkBoundary(east)) {
            return new ChunkGrid(false, 0L, Rejection.UNALIGNED_BORDER);
        }

        double circumferenceChunks = circumferenceBlocks / CHUNK_SIZE_BLOCKS;
        if (!isIntegral(circumferenceChunks)) {
            return new ChunkGrid(false, 0L, Rejection.NON_INTEGRAL_CIRCUMFERENCE);
        }

        long period = Math.round(circumferenceChunks);
        if (period <= 0L) {
            return new ChunkGrid(false, 0L, Rejection.RANGE_OVERFLOW);
        }
        return new ChunkGrid(true, period, Rejection.NONE);
    }

    public ChunkImage chunkImageForViewer(int observerChunkX, int targetChunkX) {
        ChunkGrid grid = chunkGrid();
        if (!grid.aligned()) {
            return ChunkImage.rejected(grid.rejection());
        }

        long delta = (long) targetChunkX - observerChunkX;
        long turns = translationTurns(delta, grid.circumferenceChunks());
        try {
            long translationChunks = Math.multiplyExact(turns, grid.circumferenceChunks());
            long chunkX = Math.addExact(targetChunkX, translationChunks);
            return new ChunkImage(true, chunkX, turns, translationChunks, Rejection.NONE);
        } catch (ArithmeticException e) {
            return ChunkImage.rejected(Rejection.RANGE_OVERFLOW);
        }
    }

    public double circumferenceBlocks() {
        return circumferenceBlocks;
    }

    public double centerX() {
        return centerX;
    }

    public double westBoundary() {
        return centerX - halfCircumferenceBlocks;
    }

    public double eastBoundary() {
        return centerX + halfCircumferenceBlocks;
    }

    private long integralCircumference() {
        if (!isIntegral(circumferenceBlocks)
                || circumferenceBlocks > Long.MAX_VALUE) {
            return -1L;
        }
        return Math.round(circumferenceBlocks);
    }

    private static long translationTurns(double delta, double period) {
        try {
            return Math.negateExact(nearestTurns(delta, period));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("seam turn count is outside the supported range", e);
        }
    }

    private static long nearestTurns(double delta, double period) {
        double quotient = Math.floor((delta + period * 0.5) / period);
        if (quotient < Long.MIN_VALUE || quotient > Long.MAX_VALUE) {
            throw new IllegalArgumentException("seam turn count is outside the supported range");
        }
        return (long) quotient;
    }

    private static boolean isChunkBoundary(double coordinate) {
        return isIntegral(coordinate / CHUNK_SIZE_BLOCKS);
    }

    private static boolean isIntegral(double value) {
        return Double.isFinite(value) && Math.abs(value - Math.rint(value)) <= EPSILON;
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
        UNALIGNED_BORDER,
        NON_INTEGRAL_CIRCUMFERENCE,
        RANGE_OVERFLOW
    }

    public record Image(
            double worldX,
            long turns,
            double translationBlocks,
            double relativeOffset
    ) {
    }

    public record BlockImage(
            boolean supported,
            long blockX,
            long turns,
            long translationBlocks,
            Rejection rejection
    ) {
        private static BlockImage rejected(Rejection rejection) {
            return new BlockImage(false, 0L, 0L, 0L, rejection);
        }
    }

    public record ChunkGrid(
            boolean aligned,
            long circumferenceChunks,
            Rejection rejection
    ) {
    }

    public record ChunkImage(
            boolean supported,
            long chunkX,
            long turns,
            long translationChunks,
            Rejection rejection
    ) {
        private static ChunkImage rejected(Rejection rejection) {
            return new ChunkImage(false, 0L, 0L, 0L, rejection);
        }
    }
}
