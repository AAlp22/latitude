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
}
