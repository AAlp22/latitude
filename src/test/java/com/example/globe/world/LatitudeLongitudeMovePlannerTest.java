package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeLongitudeMovePlannerTest {
    private static final double BORDER_DIAMETER_BLOCKS = 360.0;

    @Test
    void plannerInterceptsOnlyTheSmallEastWestSeamCrossing() {
        LatitudeLongitudeMovePlanner planner = new LatitudeLongitudeMovePlanner(
                BORDER_DIAMETER_BLOCKS, 100.0, 50.0, 4.0, 1.0);

        LatitudeLongitudeMovePlanner.Plan plan = planner.plan(
                278.5, 50.0, 279.5, 50.0);

        assertTrue(plan.intercept());
        assertEquals(-79.5, plan.position().worldX(), 1.0e-9);
        assertEquals(50.0, plan.position().worldZ(), 1.0e-9);
    }

    @Test
    void plannerLeavesPoleCrossingsForTheLaterTangentAwareStep() {
        LatitudeLongitudeMovePlanner planner = new LatitudeLongitudeMovePlanner(
                BORDER_DIAMETER_BLOCKS, 100.0, 50.0, 4.0, 1.0);

        LatitudeLongitudeMovePlanner.Plan plan = planner.plan(
                112.0, -129.0, 113.0, -131.0);

        assertFalse(plan.intercept());
        assertNull(plan.position());
        assertTrue(plan.movement().crossedPole());
    }

    @Test
    void poleEnabledPlannerInterceptsTheSmallPoleTriggerCrossing() {
        LatitudeLongitudeMovePlanner planner = new LatitudeLongitudeMovePlanner(
                BORDER_DIAMETER_BLOCKS, 100.0, 50.0, 4.0, 1.0);

        LatitudeLongitudeMovePlanner.Plan plan = planner.planWithPoleTriggers(
                112.0, -129.0, 113.0, -129.5);

        assertTrue(plan.intercept());
        assertTrue(plan.movement().crossedPole());
        assertEquals(-67.0, plan.position().worldX(), 1.0e-9);
        assertEquals(-129.5, plan.position().worldZ(), 1.0e-9);
    }

    @Test
    void poleEnabledPlannerRejectsACombinedLongitudeAndPoleTrigger() {
        LatitudeLongitudeMovePlanner planner = new LatitudeLongitudeMovePlanner(
                BORDER_DIAMETER_BLOCKS, 100.0, 50.0, 4.0, 1.0);

        LatitudeLongitudeMovePlanner.Plan plan = planner.planWithPoleTriggers(
                278.5, -129.0, 279.5, -129.5);

        assertFalse(plan.intercept());
        assertNull(plan.position());
        assertEquals(LatitudeCoordinateTopology.Rejection.MULTIPLE_SEAMS, plan.movement().rejection());
    }
}
