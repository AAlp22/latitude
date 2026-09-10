package com.example.globe.client;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public enum GlobeWorldSize {
    SMALL(
            Text.literal("Small (25,000 x 25,000)"),
            Identifier.of("globe", "globe_small"),
            12500
    ),
    MEDIUM(
            Text.literal("Medium (50,000 x 50,000)"),
            Identifier.of("globe", "globe_medium"),
            25000
    ),
    REGULAR(
            Text.literal("Regular (100,000 x 100,000)"),
            Identifier.of("globe", "globe_regular"),
            50000
    ),
    LARGE(
            Text.literal("Large (200,000 x 200,000)"),
            Identifier.of("globe", "globe_large"),
            100000
    ),
    MASSIVE(
            Text.literal("Massive (400,000 x 400,000)"),
            Identifier.of("globe", "globe_massive"),
            200000
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
