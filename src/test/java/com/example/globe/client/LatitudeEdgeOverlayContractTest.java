package com.example.globe.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeEdgeOverlayContractTest {
    @Test
    void eastWestOverlayIsOptInByDefault() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/example/globe/client/GlobeClientState.java"));

        assertTrue(source.contains(
                "DEBUG_EW_WALL = Boolean.parseBoolean(System.getProperty(\"latitude.debugEwWall\", \"false\"));"));
    }
}
