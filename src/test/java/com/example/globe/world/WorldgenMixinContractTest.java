package com.example.globe.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldgenMixinContractTest {
    @Test
    void noisePopulationMixinUsesTheSuperclassBiomeSourceAccessor() throws IOException {
        Path sourcePath = Path.of("src/main/java/com/example/globe/mixin/ChunkGeneratorPopulateBiomesMixin.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("ChunkGeneratorBiomeSourceAccessor"));
        assertFalse(source.contains("private BiomeSource biomeSource;"));
    }

    @Test
    void latitudeSourceKeepsTheRegisteredConcreteCodec() throws IOException {
        Path sourcePath = Path.of("src/main/java/com/example/globe/world/LatitudeBiomeSource.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("return delegate;"));
        assertFalse(source.contains("delegate.xmap("));
    }

    @Test
    void latitudeCaveRepairUsesRequestedYAndPreservesEveryUndergroundVanillaBiome() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/com/example/globe/world/LatitudeBiomeSource.java"));
        String selector = Files.readString(
                Path.of("src/main/java/com/example/globe/world/LatitudeBiomes.java"));
        String normalizedSelector = selector.replaceAll("\\s+", " ");

        assertTrue(source.contains("original.getBiome(x, y, z, sampler)"));
        assertFalse(source.contains("original.getBiome(x, 0, z, sampler)"));
        assertTrue(normalizedSelector.contains("if (blockY < columnDecisionY - 16) { return base; }"));
        assertFalse(normalizedSelector.contains("blockY < columnDecisionY - 16 && isVanillaCaveBiome(base)"));
    }

    @Test
    void legacyGeneratorWrapperMixinIsNotRegistered() throws IOException {
        Path mixinPath = Path.of("src/main/resources/globe.mixins.json");
        String mixins = Files.readString(mixinPath);

        assertFalse(mixins.contains("\"ChunkGeneratorBiomeSourceMixin\""));
    }

    @Test
    void noisePopulationUsesASeparateRuntimeSourceWithoutMutatingGeneratorState() throws IOException {
        Path sourcePath = Path.of("src/main/java/com/example/globe/mixin/ChunkGeneratorPopulateBiomesMixin.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("globe$populationBiomeSource"));
        assertTrue(source.contains("@Redirect"));
        assertTrue(source.contains("NoiseChunkGenerator;biomeSource:Lnet/minecraft/world/biome/source/BiomeSource;"));
        assertFalse(source.contains("globe$setBiomeSource"));
    }

    @Test
    void populationSourceUsesTheCurrentChunkHeightContext() throws IOException {
        String mixin = Files.readString(
                Path.of("src/main/java/com/example/globe/mixin/ChunkGeneratorPopulateBiomesMixin.java"));
        String source = Files.readString(
                Path.of("src/main/java/com/example/globe/world/LatitudeBiomeSource.java"));

        assertTrue(mixin.contains("chunk.getHeightLimitView()"));
        assertTrue(mixin.contains("ThreadLocal<BiomeSource>"));
        assertTrue(mixin.contains("globe$populationBiomeSource.remove()"));
        assertTrue(source.contains("NoiseConfig noiseConfig"));
        assertTrue(source.contains("callerContext = generator != null ? \"POPULATE\" : \"SOURCE\""));
    }

    @Test
    void latitudeSourceDoesNotAdvertiseTheFullActiveRegistry() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/com/example/globe/world/LatitudeBiomeSource.java"));

        assertTrue(source.contains("return biomes.get().stream();"));
        assertFalse(source.contains("completeBiomePool"));
    }

    @Test
    void ecoFeatureIsolationIsBiomeScoped() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/com/example/globe/world/BiomeFeatureStripping.java"));

        assertTrue(source.contains("OCEAN_ECO_FEATURES"));
        assertTrue(source.contains("SCULK_FEATURES"));
        assertTrue(source.contains("DRIPSTONE_FEATURES"));
        assertTrue(source.contains("CAVE_VEGETATION_FEATURES"));
        assertTrue(source.contains("!DEEP_DARK.equals(ctx.getBiomeKey().getValue().toString())"));
        assertTrue(source.contains("!DRIPSTONE_CAVES.equals(ctx.getBiomeKey().getValue().toString())"));
        assertTrue(source.contains("!LUSH_CAVES.equals(ctx.getBiomeKey().getValue().toString())"));
        assertFalse(source.contains("featureId.startsWith(\"eco:\")"));
    }

    @Test
    void oceanAquaticFeaturesKeepTheirVanillaTemperatureOwners() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/com/example/globe/world/BiomeFeatureStripping.java"));

        assertTrue(source.contains("isolate_seagrass_warm"));
        assertTrue(source.contains("isolate_seagrass_deep_warm"));
        assertTrue(source.contains("isolate_kelp_warm"));
        assertTrue(source.contains("isolate_sea_pickle"));
        assertTrue(source.contains("minecraft:seagrass_deep_warm"));
        assertTrue(source.contains("minecraft:kelp_warm"));
        assertTrue(source.contains("minecraft:sea_pickle"));
        assertTrue(source.contains("minecraft:seagrass_cold"));
        assertTrue(source.contains("minecraft:seagrass_deep_cold"));
    }
}
