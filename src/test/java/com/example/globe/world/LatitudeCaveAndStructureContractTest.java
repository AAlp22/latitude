package com.example.globe.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeCaveAndStructureContractTest {
    private static final Path MAIN_ROOT = Path.of("src/main/java/com/example/globe");

    @Test
    void latitudeSourcePreservesTheRequestedVerticalNoiseSample() throws IOException {
        String source = Files.readString(MAIN_ROOT.resolve("world/LatitudeBiomeSource.java"));

        assertTrue(source.contains("original.getBiome(x, y, z, sampler)"));
        assertFalse(source.contains("original.getBiome(x, 0, z, sampler)"));
        assertTrue(source.contains("LatitudeCaveBiomeRules.isCaveBiome(base)"));
        assertTrue(source.contains("return biomes.get().stream()"));
        assertFalse(source.contains("completeBiomePool(biomes.get())"));
    }

    @Test
    void undergroundCaveBiomePreservationUsesTheSurfaceBoundary() {
        assertTrue(LatitudeCaveBiomeRules.shouldPreserveUndergroundCaveBiome(
                "minecraft:lush_caves", -27, 96));
        assertTrue(LatitudeCaveBiomeRules.shouldPreserveUndergroundCaveBiome(
                "minecraft:dripstone_caves", 64, 96));
        assertTrue(LatitudeCaveBiomeRules.shouldPreserveUndergroundCaveBiome(
                "minecraft:deep_dark", -64, 96));
        assertTrue(LatitudeCaveBiomeRules.shouldPreserveUndergroundCaveBiome(
                "minecraft:lush_caves", 80, 96));
        assertFalse(LatitudeCaveBiomeRules.shouldPreserveUndergroundCaveBiome(
                "minecraft:lush_caves", 96, 96));
        assertFalse(LatitudeCaveBiomeRules.shouldPreserveUndergroundCaveBiome(
                "minecraft:plains", -27, 96));
    }

    @Test
    void structureValidationUsesASeparateRuntimeSource() throws IOException {
        String generatorSource = Files.readString(
                MAIN_ROOT.resolve("mixin/ChunkGeneratorStructureBiomeSourceMixin.java"));
        String structureSource = Files.readString(
                MAIN_ROOT.resolve("mixin/StructureBiomeSourceMixin.java"));
        String manifest = Files.readString(Path.of("src/main/resources/globe.mixins.json"));

        assertTrue(generatorSource.contains("@Mixin(ChunkGenerator.class)"));
        assertTrue(generatorSource.contains("trySetStructureStart"));
        assertTrue(generatorSource.contains("new LatitudeBiomeSource"));
        assertTrue(structureSource.contains("@Mixin(Structure.class)"));
        assertTrue(structureSource.contains("getBiomeSource()"));
        assertTrue(manifest.contains("ChunkGeneratorStructureBiomeSourceMixin"));
        assertTrue(manifest.contains("StructureBiomeSourceMixin"));
    }

    @Test
    void runtimeProviderInterfaceIsOutsideTheMixinPackage() throws IOException {
        Path provider = MAIN_ROOT.resolve("world/LatitudeStructureBiomeSourceProvider.java");
        Path oldProvider = MAIN_ROOT.resolve("mixin/LatitudeStructureBiomeSourceProvider.java");
        String generatorSource = Files.readString(
                MAIN_ROOT.resolve("mixin/ChunkGeneratorStructureBiomeSourceMixin.java"));
        String structureSource = Files.readString(
                MAIN_ROOT.resolve("mixin/StructureBiomeSourceMixin.java"));

        assertTrue(Files.exists(provider));
        assertFalse(Files.exists(oldProvider));
        assertTrue(generatorSource.contains(
                "import com.example.globe.world.LatitudeStructureBiomeSourceProvider;"));
        assertTrue(structureSource.contains(
                "import com.example.globe.world.LatitudeStructureBiomeSourceProvider;"));
    }

    @Test
    void structureRuntimeSourceDoesNotReplaceSerializedGeneratorSource() throws IOException {
        String source = Files.readString(
                MAIN_ROOT.resolve("mixin/ChunkGeneratorStructureBiomeSourceMixin.java"));

        assertTrue(source.contains("private BiomeSource biomeSource;"));
        assertTrue(source.contains("globe$structureBiomeSource"));
        assertFalse(source.contains("globe$setBiomeSource"));
    }
}
