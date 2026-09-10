package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeWorldTopologyMapperTest {
    private static final double BORDER_DIAMETER_BLOCKS = 360.0;
    private static final double CENTER_X = 100.0;
    private static final double CENTER_Z = 50.0;

    @Test
    void eastwardSeamMapsBackIntoTheExistingCenteredWorld() {
        LatitudeWorldTopologyMapper mapper = new LatitudeWorldTopologyMapper(
                BORDER_DIAMETER_BLOCKS, CENTER_X, CENTER_Z);

        LatitudeWorldTopologyMapper.MovementResult result = mapper.mapMovement(
                278.5, 50.0, 279.5, 50.0, 4.0, 1.0);

        assertTrue(result.accepted());
        assertTrue(result.crossedLongitude());
        assertFalse(result.crossedPole());
        assertEquals(-79.5, result.position().worldX(), 1.0e-9);
        assertEquals(50.0, result.position().worldZ(), 1.0e-9);
        assertEquals(180.0, result.position().logical().meridionalBlocks(), 1.0e-9);
    }

    @Test
    void northwardCrossingKeepsThePlayerNearTheNorthPole() {
        LatitudeWorldTopologyMapper mapper = new LatitudeWorldTopologyMapper(
                BORDER_DIAMETER_BLOCKS, CENTER_X, CENTER_Z);

        LatitudeWorldTopologyMapper.MovementResult result = mapper.mapMovement(
                112.0, -129.0, 113.0, -131.0, 4.0, 1.0);

        assertTrue(result.accepted());
        assertFalse(result.crossedLongitude());
        assertTrue(result.crossedPole());
        assertEquals(-67.0, result.position().worldX(), 1.0e-9);
        assertEquals(-129.0, result.position().worldZ(), 1.0e-9);
        assertEquals(1.0, result.position().logical().meridionalBlocks(), 1.0e-9);
    }
}
