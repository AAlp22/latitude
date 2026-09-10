package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeSeamCoordinateFrameTest {
    @Test
    void canonicalXUsesTheCenteredHalfOpenLongitudeInterval() {
        LatitudeSeamCoordinateFrame frame = new LatitudeSeamCoordinateFrame(360.0, 100.0);

        assertEquals(-80.0, frame.canonicalX(280.0), 1.0e-9);
        assertEquals(-80.0, frame.canonicalX(640.0), 1.0e-9);
        assertEquals(100.0, frame.canonicalX(100.0), 1.0e-9);
        assertEquals(-80.0, frame.westBoundary(), 1.0e-9);
        assertEquals(280.0, frame.eastBoundary(), 1.0e-9);
    }

    @Test
    void entityImageUsesTheNearestPeriodicCopyAcrossTheSeam() {
        LatitudeSeamCoordinateFrame frame = new LatitudeSeamCoordinateFrame(25_000.0, 0.0);

        LatitudeSeamCoordinateFrame.Image image = frame.imageForViewer(-12_499.0, 12_499.0);

        assertEquals(-12_501.0, image.worldX(), 1.0e-9);
        assertEquals(-25_000.0, image.translationBlocks(), 1.0e-9);
        assertEquals(-2.0, image.relativeOffset(), 1.0e-9);
        assertEquals(-1L, image.turns());
    }

    @Test
    void entityImagePreservesTheNativeCopyWhenItIsAlreadyNearest() {
        LatitudeSeamCoordinateFrame frame = new LatitudeSeamCoordinateFrame(25_000.0, 0.0);

        LatitudeSeamCoordinateFrame.Image image = frame.imageForViewer(100.0, 103.5);

        assertEquals(103.5, image.worldX(), 1.0e-9);
        assertEquals(0.0, image.translationBlocks(), 1.0e-9);
        assertEquals(3.5, image.relativeOffset(), 1.0e-9);
        assertEquals(0L, image.turns());
    }

    @Test
    void blockImageUsesAnExactIntegerTranslationWhenTheCircumferenceIsIntegral() {
        LatitudeSeamCoordinateFrame frame = new LatitudeSeamCoordinateFrame(25_000.0, 0.0);

        LatitudeSeamCoordinateFrame.BlockImage image = frame.blockImageForViewer(-12_499, 12_499);

        assertTrue(image.supported());
        assertEquals(-12_501L, image.blockX());
        assertEquals(-25_000L, image.translationBlocks());
        assertEquals(-1L, image.turns());
        assertEquals(LatitudeSeamCoordinateFrame.Rejection.NONE, image.rejection());
    }

    @Test
    void alignedChunkGridCanMapWholeChunksAcrossTheSeam() {
        LatitudeSeamCoordinateFrame frame = new LatitudeSeamCoordinateFrame(100_000.0, 0.0);

        LatitudeSeamCoordinateFrame.ChunkGrid grid = frame.chunkGrid();
        LatitudeSeamCoordinateFrame.ChunkImage image = frame.chunkImageForViewer(-3_125, 3_124);

        assertTrue(grid.aligned());
        assertEquals(6_250L, grid.circumferenceChunks());
        assertTrue(image.supported());
        assertEquals(-3_126L, image.chunkX());
        assertEquals(-6_250L, image.translationChunks());
        assertEquals(-1L, image.turns());
        assertEquals(LatitudeSeamCoordinateFrame.Rejection.NONE, image.rejection());
    }

    @Test
    void unalignedChunkGridIsRejectedInsteadOfSplittingEdgeChunks() {
        LatitudeSeamCoordinateFrame frame = new LatitudeSeamCoordinateFrame(50_000.0, 0.0);

        LatitudeSeamCoordinateFrame.ChunkGrid grid = frame.chunkGrid();
        LatitudeSeamCoordinateFrame.ChunkImage image = frame.chunkImageForViewer(-1_563, 1_562);

        assertFalse(grid.aligned());
        assertEquals(0L, grid.circumferenceChunks());
        assertEquals(LatitudeSeamCoordinateFrame.Rejection.UNALIGNED_BORDER, grid.rejection());
        assertFalse(image.supported());
        assertEquals(LatitudeSeamCoordinateFrame.Rejection.UNALIGNED_BORDER, image.rejection());
    }

    @Test
    void invalidFrameDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new LatitudeSeamCoordinateFrame(0.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new LatitudeSeamCoordinateFrame(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new LatitudeSeamCoordinateFrame(100.0, Double.POSITIVE_INFINITY));
    }
}
