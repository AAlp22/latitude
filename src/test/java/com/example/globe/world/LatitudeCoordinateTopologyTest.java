package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
