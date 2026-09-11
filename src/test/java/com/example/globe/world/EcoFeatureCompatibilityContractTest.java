package com.example.globe.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcoFeatureCompatibilityContractTest {
    @Test
    void stripsWarmOceanVegetationOutsideItsOwner() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/example/globe/world/BiomeFeatureStripping.java"));

        assertTrue(source.contains("isolate_warm_ocean_vegetation"));
        assertTrue(source.contains("minecraft:warm_ocean_vegetation"));
        assertTrue(source.contains("!WARM_OCEAN.equals"));
        assertTrue(source.contains("removeFeature"));
        assertFalse(source.contains("getMethod(\"getFeatures\")"));
    }
}
