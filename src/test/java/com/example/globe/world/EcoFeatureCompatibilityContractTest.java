package com.example.globe.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcoFeatureCompatibilityContractTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/example/globe");

    @Test
    void stripsWarmOceanVegetationFromEveryNonWarmOceanBiome() throws IOException {
        String source = Files.readString(SOURCE_ROOT.resolve("world/BiomeFeatureStripping.java"));

        assertTrue(source.contains("WARM_OCEAN_VEGETATION"));
        assertTrue(source.contains("BiomeKeys.WARM_OCEAN"));
        assertTrue(source.contains("!ctx.getBiomeKey().equals(BiomeKeys.WARM_OCEAN)"));
        assertTrue(source.contains("removeFeature"));
    }

    @Test
    void stripsEcoPlacedFeaturesAtTheGenerationSettingsBoundary() throws IOException {
        String source = Files.readString(SOURCE_ROOT.resolve("mixin/GenerationSettingsEcoFeatureFilterMixin.java"));

        assertTrue(source.contains("@Mixin(GenerationSettings.class)"));
        assertTrue(source.contains("RegistryEntryList<PlacedFeature>"));
        assertTrue(source.contains("RegistryEntryList.of(filteredEntries)"));
        assertFalse(source.contains("List<List<RegistryEntry<PlacedFeature>>>"));
        assertTrue(source.contains("RegistryKey::getValue"));
        assertTrue(source.contains("Identifier::getNamespace"));
        assertTrue(source.contains("ECO_NAMESPACE"));
        assertTrue(source.contains("features"));
    }

    @Test
    void gatesSpecialVanillaFeaturesToTheirOwningBiomesWithoutReflection() throws IOException {
        String source = Files.readString(SOURCE_ROOT.resolve("world/BiomeFeatureStripping.java"));
        String manifest = Files.readString(Path.of("src/main/resources/globe.mixins.json"));

        assertTrue(source.contains("BiomeKeys.LUSH_CAVES"));
        assertTrue(source.contains("BiomeKeys.DRIPSTONE_CAVES"));
        assertTrue(source.contains("BiomeKeys.DEEP_DARK"));
        assertTrue(source.contains("vanillaFeature(\"lush_caves_ceiling_vegetation\")"));
        assertTrue(source.contains("vanillaFeature(\"large_dripstone\")"));
        assertTrue(source.contains("vanillaFeature(\"sculk_patch_deep_dark\")"));
        assertTrue(source.contains("Identifier.of(\"minecraft\", path)"));
        assertFalse(source.contains("getMethod(\"getFeatures\")"));
        assertTrue(manifest.contains("GenerationSettingsEcoFeatureFilterMixin"));
    }
}
