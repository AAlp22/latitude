package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatitudePoleCrossingTransformTest {
    @Test
    void northwardVelocityBecomesSouthwardAfterTheFold() {
        LatitudePoleCrossingTransform.Motion motion = LatitudePoleCrossingTransform.transform(
                0.0, -0.8, 0.0f);

        assertEquals(0.0, motion.xVelocity(), 1.0e-9);
        assertEquals(0.8, motion.meridionalVelocity(), 1.0e-9);
        assertEquals(180.0, motion.yawDegrees(), 1.0e-9);
    }

    @Test
    void diagonalVelocityReflectsOnlyItsMeridionalComponent() {
        LatitudePoleCrossingTransform.Motion motion = LatitudePoleCrossingTransform.transform(
                0.35, -0.65, 37.0f);

        assertEquals(0.35, motion.xVelocity(), 1.0e-9);
        assertEquals(0.65, motion.meridionalVelocity(), 1.0e-9);
        assertEquals(143.0, motion.yawDegrees(), 1.0e-9);
    }

    @Test
    void eastwardAndWestwardComponentsRemainInTheSameAtlasDirection() {
        LatitudePoleCrossingTransform.Motion east = LatitudePoleCrossingTransform.transform(
                0.8, 0.0, -90.0f);
        LatitudePoleCrossingTransform.Motion west = LatitudePoleCrossingTransform.transform(
                -0.8, 0.0, 90.0f);

        assertEquals(0.8, east.xVelocity(), 1.0e-9);
        assertEquals(0.0, east.meridionalVelocity(), 1.0e-9);
        assertEquals(270.0, east.yawDegrees(), 1.0e-9);
        assertEquals(-0.8, west.xVelocity(), 1.0e-9);
        assertEquals(0.0, west.meridionalVelocity(), 1.0e-9);
        assertEquals(90.0, west.yawDegrees(), 1.0e-9);
    }
}
