package com.example.globe.world;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.Set;

public final class LatitudeCaveBiomeRules {
    private static final Set<String> CAVE_BIOME_IDS = Set.of(
            "minecraft:dripstone_caves",
            "minecraft:lush_caves",
            "minecraft:deep_dark");

    private LatitudeCaveBiomeRules() {
    }

    public static boolean isCaveBiome(RegistryEntry<Biome> entry) {
        return entry != null
                && entry.getKey()
                .map(key -> isCaveBiome(key.getValue().toString()))
                .orElse(false);
    }

    public static boolean isCaveBiome(String biomeIdentifier) {
        return CAVE_BIOME_IDS.contains(biomeIdentifier);
    }

    public static boolean shouldPreserveUndergroundCaveBiome(String biomeIdentifier,
                                                               int blockY,
                                                               int columnDecisionY) {
        return blockY < columnDecisionY && isCaveBiome(biomeIdentifier);
    }

    public static boolean shouldPreserveCaveBiome(RegistryEntry<Biome> entry) {
        return isCaveBiome(entry);
    }

}
