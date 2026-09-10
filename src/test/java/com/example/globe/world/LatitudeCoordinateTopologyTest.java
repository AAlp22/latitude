package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeCoordinateTopologyTest {
    private static final double CIRCUMFERENCE_BLOCKS = 360.0;
    private static final double POLE_TO_POLE_BLOCKS = 180.0;

    @Test
    void northPoleOvershootReflectsIntoNorthernHemisphereOnOppositeLongitude() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(
                CIRCUMFERENCE_BLOCKS,
                POLE_TO_POLE_BLOCKS
        );

        LatitudeCoordinateTopology.Coordinate coordinate = topology.map(12.0, -4.0);

        assertEquals(4.0, coordinate.meridionalBlocks(), 1.0e-9);
        assertEquals(86.0, coordinate.latitudeDegrees(), 1.0e-9);
        assertEquals(-168.0, coordinate.longitudeDegrees(), 1.0e-9);
        assertEquals(-168.0, coordinate.wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void negativeXWrapsIntoCenteredHalfOpenCircumference() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(
                CIRCUMFERENCE_BLOCKS,
                POLE_TO_POLE_BLOCKS
        );

        assertEquals(12.0, topology.wrapX(-348.0), 1.0e-9);
        assertEquals(-180.0, topology.wrapX(180.0), 1.0e-9);
        assertEquals(-1.0, topology.wrapX(-361.0), 1.0e-9);
    }

    @Test
    void southPoleOvershootReflectsIntoSouthernHemisphereOnOppositeLongitude() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(
                CIRCUMFERENCE_BLOCKS,
                POLE_TO_POLE_BLOCKS
        );

        LatitudeCoordinateTopology.Coordinate coordinate = topology.map(-12.0, 184.0);

        assertEquals(176.0, coordinate.meridionalBlocks(), 1.0e-9);
        assertEquals(-86.0, coordinate.latitudeDegrees(), 1.0e-9);
        assertEquals(168.0, coordinate.longitudeDegrees(), 1.0e-9);
        assertEquals(168.0, coordinate.wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void exactPolesUseTheCanonicalOriginLongitude() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(
                CIRCUMFERENCE_BLOCKS,
                POLE_TO_POLE_BLOCKS
        );

        LatitudeCoordinateTopology.Coordinate north = topology.map(137.0, 0.0);
        LatitudeCoordinateTopology.Coordinate south = topology.map(-137.0, POLE_TO_POLE_BLOCKS);

        assertEquals(0.0, north.wrappedXBlocks(), 1.0e-9);
        assertEquals(90.0, north.latitudeDegrees(), 1.0e-9);
        assertEquals(0.0, north.longitudeDegrees(), 1.0e-9);
        assertEquals(0.0, south.wrappedXBlocks(), 1.0e-9);
        assertEquals(-90.0, south.latitudeDegrees(), 1.0e-9);
        assertEquals(0.0, south.longitudeDegrees(), 1.0e-9);
    }

    @Test
    void twoPoleCrossingsRestoreTheOriginalLongitude() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(
                CIRCUMFERENCE_BLOCKS,
                POLE_TO_POLE_BLOCKS
        );

        LatitudeCoordinateTopology.Coordinate coordinate = topology.map(12.0, 2.0 * POLE_TO_POLE_BLOCKS + 4.0);

        assertEquals(4.0, coordinate.meridionalBlocks(), 1.0e-9);
        assertEquals(86.0, coordinate.latitudeDegrees(), 1.0e-9);
        assertEquals(12.0, coordinate.longitudeDegrees(), 1.0e-9);
        assertEquals(12.0, coordinate.wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void invalidDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new LatitudeCoordinateTopology(0.0, POLE_TO_POLE_BLOCKS));
        assertThrows(IllegalArgumentException.class,
                () -> new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, -1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new LatitudeCoordinateTopology(Double.NaN, POLE_TO_POLE_BLOCKS));
    }

    @Test
    void smallEastwardSeamCrossingIsAcceptedAndWrapped() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovement(
                179.0, 90.0, 181.0, 90.0, 4.0);

        assertTrue(result.accepted());
        assertTrue(result.crossedLongitude());
        assertFalse(result.crossedPole());
        assertEquals(-179.0, result.coordinate().wrappedXBlocks(), 1.0e-9);
        assertEquals(90.0, result.coordinate().meridionalBlocks(), 1.0e-9);
    }

    @Test
    void smallOutwardStepInsideTriggerBandIsMappedAcrossLongitudeSeam() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovementWithLongitudeTrigger(
                178.5, 90.0, 179.5, 90.0, 4.0, 1.0);

        assertTrue(result.accepted());
        assertTrue(result.crossedLongitude());
        assertFalse(result.crossedPole());
        assertEquals(-179.5, result.coordinate().wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void smallWestwardStepInsideTriggerBandIsMappedAcrossLongitudeSeam() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovementWithLongitudeTrigger(
                -178.5, 90.0, -179.5, 90.0, 4.0, 1.0);

        assertTrue(result.accepted());
        assertTrue(result.crossedLongitude());
        assertFalse(result.crossedPole());
        assertEquals(179.5, result.coordinate().wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void inwardStepInsideTriggerBandDoesNotWrap() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovementWithLongitudeTrigger(
                179.5, 90.0, 178.5, 90.0, 4.0, 1.0);

        assertTrue(result.accepted());
        assertFalse(result.crossedLongitude());
        assertFalse(result.crossedPole());
        assertEquals(178.5, result.coordinate().wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void oversizedOutOfBoundsJumpIsRejected() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovement(
                179.0, 90.0, 200.0, 90.0, 4.0);

        assertFalse(result.accepted());
        assertEquals(LatitudeCoordinateTopology.Rejection.OUT_OF_BOUNDS, result.rejection());
        assertNull(result.coordinate());
    }

    @Test
    void smallNorthwardPoleCrossingIsAcceptedAndReflected() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovement(
                12.0, 1.0, 13.0, -1.0, 4.0);

        assertTrue(result.accepted());
        assertFalse(result.crossedLongitude());
        assertTrue(result.crossedPole());
        assertEquals(1.0, result.coordinate().meridionalBlocks(), 1.0e-9);
        assertEquals(89.0, result.coordinate().latitudeDegrees(), 1.0e-9);
        assertEquals(-167.0, result.coordinate().wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void smallNorthwardStepInsidePoleTriggerBandIsMappedAcrossThePole() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovementWithPoleTrigger(
                12.0, 1.0, 13.0, 0.5, 4.0, 1.0);

        assertTrue(result.accepted());
        assertFalse(result.crossedLongitude());
        assertTrue(result.crossedPole());
        assertEquals(0.5, result.coordinate().meridionalBlocks(), 1.0e-9);
        assertEquals(-167.0, result.coordinate().wrappedXBlocks(), 1.0e-9);
    }

    @Test
    void simultaneousLongitudeAndPoleCrossingIsRejectedAsAmbiguous() {
        LatitudeCoordinateTopology topology = new LatitudeCoordinateTopology(CIRCUMFERENCE_BLOCKS, POLE_TO_POLE_BLOCKS);

        LatitudeCoordinateTopology.MovementResult result = topology.mapMovement(
                179.0, 1.0, 181.0, -1.0, 4.0);

        assertFalse(result.accepted());
        assertEquals(LatitudeCoordinateTopology.Rejection.MULTIPLE_SEAMS, result.rejection());
        assertNull(result.coordinate());
    }
}
