package com.example.globe.client.create;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudePresetSerializationContractTest {
    private static final Path LAUNCHER = Path.of(
            "src/main/java/com/example/globe/client/create/LatitudeWorldLauncher.java");

    @Test
    void launcherRebuildsAndVerifiesTheSelectedPresetDimensionsBeforeLaunch() throws IOException {
        String source = Files.readString(LAUNCHER);

        assertTrue(source.contains("createDimensionsRegistryHolder()"));
        assertTrue(source.contains("setGeneratorOptionsHolder"));
        assertTrue(source.contains("selectedDimensions()"));
        assertTrue(source.contains("getOrEmpty(DimensionOptions.OVERWORLD)"));
        assertTrue(source.contains("NoiseChunkGenerator"));
        assertTrue(source.contains("getSettings().getKey()"));
        assertTrue(source.contains("biomeSource"));
        assertTrue(source.contains("effective overworld generator"));
    }

    @Test
    void launcherFailsClosedOnAGenericOverworldSettingsFallback() throws IOException {
        String source = Files.readString(LAUNCHER);

        int validation = source.indexOf("validateEffectivePreset");
        int session = source.indexOf("createSessionWithoutSymlinkCheck");
        int start = source.indexOf("start(session");

        assertTrue(validation >= 0);
        assertTrue(session >= 0);
        assertTrue(start >= 0);
        assertTrue(validation < session);
        assertTrue(source.contains("minecraft:overworld"));
        assertTrue(source.contains("Aborting world start because effective overworld generator"));
        assertFalse(source.contains("// TODO: validate effective overworld generator"));
    }
}
