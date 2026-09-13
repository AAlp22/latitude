package com.example.globe.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobePresetRadiusTest {
    @Test
    void radiiAndLabelsAgreeAcrossEveryGate() throws Exception {
        Path src = Path.of("src/main/java/com/example/globe");
        String size = Files.readString(src.resolve("client/GlobeWorldSize.java"));
        String mod = Files.readString(src.resolve("GlobeMod.java"));
        String mixin = Files.readString(src.resolve("mixin/ChunkGeneratorBiomeSourceMixin.java"));
        String[][] pairs = {
                {"25000", "50,000"},
                {"50000", "100,000"},
                {"100000", "200,000"},
                {"200000", "400,000"},
                {"400000", "800,000"}
        };
        for (String[] pair : pairs) {
            assertTrue(size.contains("            " + pair[0]), "GlobeWorldSize radius " + pair[0]);
            assertTrue(size.contains("(" + pair[1] + " x " + pair[1] + ")"), "label " + pair[1]);
            assertTrue(mixin.contains("return " + pair[0] + ";"), "mixin gate " + pair[0]);
        }
        assertTrue(mod.contains("BORDER_RADIUS = 100000;"), "BORDER_RADIUS fallback");
        assertTrue(mod.contains("return 400000;"), "GlobeMod gate 400000");
    }
}
