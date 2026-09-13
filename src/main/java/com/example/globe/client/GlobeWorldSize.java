package com.example.globe.client;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public enum GlobeWorldSize {
    SMALL(
            Text.literal("Small (50,000 x 50,000)"),
            Identifier.of("globe", "globe_small"),
            25000
    ),
    MEDIUM(
            Text.literal("Medium (100,000 x 100,000)"),
            Identifier.of("globe", "globe_medium"),
            50000
    ),
    REGULAR(
            Text.literal("Regular (200,000 x 200,000)"),
            Identifier.of("globe", "globe_regular"),
            100000
    ),
    LARGE(
            Text.literal("Large (400,000 x 400,000)"),
            Identifier.of("globe", "globe_large"),
            200000
    ),
    MASSIVE(
            Text.literal("Massive (800,000 x 800,000)"),
            Identifier.of("globe", "globe_massive"),
            400000
    );

    public final Text label;
    public final Identifier worldPresetId;
    public final int borderRadiusBlocks;

    GlobeWorldSize(Text label, Identifier worldPresetId, int borderRadiusBlocks) {
        this.label = label;
        this.worldPresetId = worldPresetId;
        this.borderRadiusBlocks = borderRadiusBlocks;
    }
}
