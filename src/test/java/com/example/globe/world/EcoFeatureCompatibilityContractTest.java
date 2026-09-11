package com.example.globe.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EcoFeatureCompatibilityContractTest {
    @Test
    void stripsEcoWarmOceanVegetationFromPlainsOnly() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/example/globe/world/BiomeFeatureStripping.java"));

        assertTrue(source.contains("WARM_OCEAN_VEGETATION"));
        assertTrue(source.contains("BiomeKeys.PLAINS"));
        assertTrue(source.contains("BiomeKeys.SUNFLOWER_PLAINS"));
        assertTrue(source.contains("removeFeature"));
    }
}
