package com.example.globe.worldgen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The feature biome-set redirects live on a dedicated mixin that must stay registered: an
 * unregistered mixin source file compiles, packages and ships fine, then silently never
 * applies (the failure that shipped once: vanillade-only biome sets stripped every
 * tag-bridged biome from feature placement, so pool biomes generated treeless).
 */
class ChunkGeneratorFeatureSetRegistrationTest {
    private static final Path MANIFEST = Path.of("src/main/resources/globe.mixins.json");
    private static final Path FEATURE_SET_MIXIN = Path.of(
            "src/main/java/com/example/globe/mixin/ChunkGeneratorFeatureSetMixin.java");

    @Test
    void featureSetRedirectsAreRegisteredAndScopedToTheDeclaringClass() throws IOException {
        String manifest = Files.readString(MANIFEST);
        assertTrue(manifest.contains("\"ChunkGeneratorFeatureSetMixin\""),
                "feature biome-set redirects must be registered or pool biomes stay treeless");
        assertFalse(manifest.contains("\"ChunkGeneratorBiomeSourceMixin\""),
                "the retired source-wrap mixin must stay out of the manifest");

        String source = Files.readString(FEATURE_SET_MIXIN);
        assertTrue(source.contains("@Mixin(net.minecraft.world.gen.chunk.ChunkGenerator.class)"),
                "the redirects must target the class that declares generateFeatures/method_44215");
        assertTrue(source.contains("generateFeatures"));
        assertTrue(source.contains("method_44215"));
        assertEquals(2, countOccurrences(source, "BiomeSource;getBiomes()Ljava/util/Set;"));
        assertEquals(2, countOccurrences(source, "completeBiomePool"));
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
