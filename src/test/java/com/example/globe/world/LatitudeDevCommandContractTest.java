package com.example.globe.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeDevCommandContractTest {
    @Test
    void dayOfYearCommandUpdatesCalendarTimeAndPreservesTimeOfDay() throws IOException {
        Path sourcePath = Path.of("src/main/java/com/example/globe/LatitudeDevCommands.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("Math.floorDiv(world.getTime(), LatitudeCalendarMath.TICKS_PER_DAY)"));
        assertTrue(source.contains("properties.setTime(target)"));
        assertTrue(source.contains("properties.setTimeOfDay(tickInDay)"));
        assertFalse(source.contains("return setTimeOfDay(ctx, target, \"dayofyear=\" + day)"));
    }
}
