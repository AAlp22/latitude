package com.example.globe.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeJjThunderHybridContractTest {
    private static final Path PACK = Path.of("datapacks/latitude-jjthunder-hybrid");

    @Test
    void hybridPackKeepsJjHeightAndGlobeRouterContract() throws IOException {
        String dimensionType = Files.readString(PACK.resolve(
                "data/minecraft/dimension_type/overworld.json"));
        assertTrue(dimensionType.contains("\"height\": 2096"));
        assertTrue(dimensionType.contains("\"min_y\": -64"));
        assertTrue(dimensionType.contains("\"logical_height\": 384"));

        for (String size : new String[]{"small", "medium", "regular", "large", "massive"}) {
            String settings = Files.readString(PACK.resolve(
                    "data/globe/worldgen/noise_settings/overworld_" + size + ".json"));
            assertTrue(settings.contains("\"height\": 2096"), size);
            assertTrue(settings.contains("\"size_vertical\": 4"), size);
            assertTrue(settings.contains("\"temperature\": \"globe:overworld/noise_router/temperature\""), size);
            assertTrue(settings.contains("\"vegetation\": \"globe:overworld/noise_router/vegetation\""), size);
            assertTrue(settings.contains("\"continents\": \"globe:overworld/noise_router/continents\""), size);
            assertTrue(settings.contains("\"erosion\": \"globe:overworld/noise_router/erosion\""), size);
            assertTrue(settings.contains("\"depth\": \"globe:overworld/noise_router/depth\""), size);
            assertTrue(settings.contains("\"ridges\": \"globe:overworld/noise_router/ridges\""), size);
            assertTrue(settings.contains("\"final_density\": \"globe:hybrid/final_density\""), size);
        }
    }

    @Test
    void boundedOceanMergeDoesNotUseGlobeFullVerticalTerrainAsAClamp() throws IOException {
        String finalDensity = Files.readString(PACK.resolve(
                "data/globe/worldgen/density_function/hybrid/final_density.json"));
        assertTrue(finalDensity.contains("globe:overworld/noise_router/continents"));
        assertTrue(finalDensity.contains("minecraft:new_combination/interpolated"));
        assertTrue(finalDensity.contains("\"from_y\": 62"));
        assertTrue(finalDensity.contains("\"to_y\": 63"));
        assertTrue(finalDensity.contains("\"from_value\": 1024.0"));
        assertTrue(finalDensity.contains("\"to_value\": -1024.0"));
        assertFalse(finalDensity.contains("globe:base_terrain"));
    }

    @Test
    void hybridPackDoesNotReplaceGlobePresetWithJjDefaultOverworldFiles() {
        assertFalse(Files.exists(PACK.resolve("data/minecraft/dimension/overworld.json")));
        assertFalse(Files.exists(PACK.resolve(
                "data/minecraft/worldgen/noise_settings/overworld.json")));
        assertFalse(Files.exists(PACK.resolve("data/minecraft/worldgen/structure")));
        assertFalse(Files.exists(PACK.resolve("data/minecraft/worldgen/structure_set")));
    }

    @Test
    void hybridPackContainsTheSelectedJjWorldgenCategories() throws IOException {
        assertEquals(35, countJson("data/minecraft/worldgen/biome"));
        assertEquals(3, countJson("data/minecraft/worldgen/configured_carver"));
        assertEquals(4, countJson("data/minecraft/worldgen/configured_feature"));
        assertEquals(162, countJson("data/minecraft/worldgen/density_function"));
        assertEquals(5, countJson("data/minecraft/worldgen/noise"));
        assertEquals(33, countJson("data/minecraft/worldgen/placed_feature"));
    }

    @Test
    void continentDependencyIsGlobeOwnedBeforeJjContinentalnessAugmentation() throws IOException {
        String continentalness = Files.readString(PACK.resolve(
                "data/minecraft/worldgen/density_function/biome/overworld/continentalness.json"));
        assertTrue(continentalness.contains("globe:overworld/noise_router/continents"));
    }

    private static long countJson(String relativeDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(PACK.resolve(relativeDirectory))) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .count();
        }
    }
}
