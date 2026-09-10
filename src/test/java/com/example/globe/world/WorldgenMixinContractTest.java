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
    void generatorGetterDoesNotInstallWrapperDuringSettingsSerialization() throws IOException {
        Path sourcePath = Path.of("src/main/java/com/example/globe/mixin/ChunkGeneratorBiomeSourceMixin.java");
        String source = Files.readString(sourcePath);

        assertFalse(source.contains("method = \"getBiomeSource\""));
        assertFalse(source.contains("globe$returnWrappedBiomeSource"));
    }
}
