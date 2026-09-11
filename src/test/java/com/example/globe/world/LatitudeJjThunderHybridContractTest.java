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
    void hybridPackKeepsGlobePresetContractAndUsesJjVerticalContract() throws IOException {
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
            assertTrue(settings.contains("globe:overworld/noise_router/continents"), size);
            assertTrue(settings.contains("globe:overworld/noise_router/depth"), size);
            assertTrue(settings.contains("globe:overworld/noise_router/erosion"), size);
            assertTrue(settings.contains("globe:overworld/noise_router/ridges"), size);
            assertTrue(settings.contains("\"argument2\": \"globe:base_terrain\""), size);
        }
    }

    @Test
    void hybridPackDoesNotReplaceGlobePresetWithJjDefaultOverworldFiles() {
        assertFalse(Files.exists(PACK.resolve("data/minecraft/dimension/overworld.json")));
        assertFalse(Files.exists(PACK.resolve(
                "data/minecraft/worldgen/noise_settings/overworld.json")));
    }

    @Test
    void hybridPackContainsSelectedJjWorldgenCategoriesAndNoUnapprovedStructures() throws IOException {
        assertEquals(35, countJson("data/minecraft/worldgen/biome"));
        assertEquals(3, countJson("data/minecraft/worldgen/configured_carver"));
        assertEquals(4, countJson("data/minecraft/worldgen/configured_feature"));
        assertEquals(162, countJson("data/minecraft/worldgen/density_function"));
        assertEquals(5, countJson("data/minecraft/worldgen/noise"));
        assertEquals(33, countJson("data/minecraft/worldgen/placed_feature"));
        assertFalse(Files.exists(PACK.resolve("data/minecraft/worldgen/structure")));
        assertFalse(Files.exists(PACK.resolve("data/minecraft/worldgen/structure_set")));
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
