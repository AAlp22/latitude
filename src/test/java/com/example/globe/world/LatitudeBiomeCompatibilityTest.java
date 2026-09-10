package com.example.globe.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeBiomeCompatibilityTest {
    @Test
    void preservesEcoFloatingRiverInWarmBands() {
        assertTrue(LatitudeBiomeCompatibility.shouldPreserveEcoFloatingRiver("eco:floating_river", false));
    }

    @Test
    void doesNotPreserveEcoFloatingRiverInColdBands() {
        assertFalse(LatitudeBiomeCompatibility.shouldPreserveEcoFloatingRiver("eco:floating_river", true));
    }

    @Test
    void doesNotTreatOtherRiverIdsAsEcoFloatingRiver() {
        assertFalse(LatitudeBiomeCompatibility.shouldPreserveEcoFloatingRiver("minecraft:river", false));
    }
}
