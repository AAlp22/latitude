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
        assertTrue(source.contains("requested world preset"));
        assertTrue(source.contains("selectedDimensionKey"));
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

    @Test
    void launcherKeepsTheSelectedPresetWhenDataPacksOverrideDimensionIds() throws IOException {
        String source = Files.readString(LAUNCHER);

        // A data pack shipping data/minecraft/dimension/overworld.json must not win the merge.
        assertTrue(source.contains("mergePreservingSelected"));
        assertTrue(source.contains("Data-pack dimension"));
        assertTrue(source.contains("shadowed by the selected preset"));

        int finalValidationCall = source.indexOf("if (isLatitude && !validateFinalDimensionsConfig");
        int session = source.indexOf("createSessionWithoutSymlinkCheck");
        assertTrue(finalValidationCall >= 0);
        assertTrue(session >= 0);
        assertTrue(finalValidationCall < session);
        assertTrue(source.contains(
                "Aborting world start because the final dimensions config is not Latitude-owned"));
    }
}
