package com.example.globe.world;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.feature.PlacedFeature;

public final class BiomeFeatureStripping {
    private static final Logger LOGGER = LoggerFactory.getLogger("globe");
    private static final Identifier STRIP_NON_WARM_OCEAN_ID = Identifier.of("globe", "strip_non_warm_ocean_features");
    private static final Identifier STRIP_NON_LUSH_CAVE_ID = Identifier.of("globe", "strip_non_lush_cave_features");
    private static final Identifier STRIP_NON_DRIPSTONE_CAVE_ID = Identifier.of("globe", "strip_non_dripstone_cave_features");
    private static final Identifier STRIP_NON_DEEP_DARK_ID = Identifier.of("globe", "strip_non_deep_dark_features");
    private static final Identifier STRIP_NON_WARM_AQUATIC_ID = Identifier.of("globe", "strip_non_warm_aquatic_features");
    private static final Identifier STRIP_NON_COLD_OCEAN_ID = Identifier.of("globe", "strip_non_cold_ocean_features");

    private static final RegistryKey<PlacedFeature> WARM_OCEAN_VEGETATION = vanillaFeature("warm_ocean_vegetation");
    private static final RegistryKey<PlacedFeature> SEA_PICKLE = vanillaFeature("sea_pickle");
    private static final RegistryKey<PlacedFeature> LUSH_CAVES_CEILING_VEGETATION = vanillaFeature("lush_caves_ceiling_vegetation");
    private static final RegistryKey<PlacedFeature> CAVE_VINES = vanillaFeature("cave_vines");
    private static final RegistryKey<PlacedFeature> CLASSIC_VINES_CAVE_FEATURE = vanillaFeature("classic_vines_cave_feature");
    private static final RegistryKey<PlacedFeature> LARGE_DRIPSTONE = vanillaFeature("large_dripstone");
    private static final RegistryKey<PlacedFeature> DRIPSTONE_CLUSTER = vanillaFeature("dripstone_cluster");
    private static final RegistryKey<PlacedFeature> POINTED_DRIPSTONE = vanillaFeature("pointed_dripstone");
    private static final RegistryKey<PlacedFeature> SCULK_VEIN = vanillaFeature("sculk_vein");
    private static final RegistryKey<PlacedFeature> SCULK_PATCH_DEEP_DARK = vanillaFeature("sculk_patch_deep_dark");
    private static final RegistryKey<PlacedFeature> SEAGRASS_WARM = vanillaFeature("seagrass_warm");
    private static final RegistryKey<PlacedFeature> KELP_WARM = vanillaFeature("kelp_warm");
    private static final RegistryKey<PlacedFeature> KELP_COLD = vanillaFeature("kelp_cold");

    private static final List<RegistryKey<PlacedFeature>> NON_LUSH_CAVE_FEATURES = List.of(
            LUSH_CAVES_CEILING_VEGETATION,
            CAVE_VINES,
            CLASSIC_VINES_CAVE_FEATURE
    );
    private static final List<RegistryKey<PlacedFeature>> NON_DRIPSTONE_CAVE_FEATURES = List.of(
            LARGE_DRIPSTONE,
            DRIPSTONE_CLUSTER,
            POINTED_DRIPSTONE
    );
    private static final List<RegistryKey<PlacedFeature>> NON_DEEP_DARK_FEATURES = List.of(
            SCULK_VEIN,
            SCULK_PATCH_DEEP_DARK
    );
    private static final List<RegistryKey<PlacedFeature>> NON_WARM_AQUATIC_FEATURES = List.of(
            SEAGRASS_WARM,
            KELP_WARM
    );

    private BiomeFeatureStripping() {
    }

    public static void init() {
        if (Boolean.getBoolean("latitude.disableFeatureStripping")) {
            LOGGER.info("[Latitude] Biome feature stripping disabled by system property.");
            return;
        }

        BiomeModifications.create(STRIP_NON_WARM_OCEAN_ID)
                .add(ModificationPhase.REMOVALS,
                        ctx -> !ctx.getBiomeKey().equals(BiomeKeys.WARM_OCEAN),
                        BiomeFeatureStripping::stripNonWarmOceanFeatures);
        BiomeModifications.create(STRIP_NON_LUSH_CAVE_ID)
                .add(ModificationPhase.REMOVALS,
                        ctx -> !ctx.getBiomeKey().equals(BiomeKeys.LUSH_CAVES),
                        ctx -> stripFeatures(ctx, NON_LUSH_CAVE_FEATURES));
        BiomeModifications.create(STRIP_NON_DRIPSTONE_CAVE_ID)
                .add(ModificationPhase.REMOVALS,
                        ctx -> !ctx.getBiomeKey().equals(BiomeKeys.DRIPSTONE_CAVES),
                        ctx -> stripFeatures(ctx, NON_DRIPSTONE_CAVE_FEATURES));
        BiomeModifications.create(STRIP_NON_DEEP_DARK_ID)
                .add(ModificationPhase.REMOVALS,
                        ctx -> !ctx.getBiomeKey().equals(BiomeKeys.DEEP_DARK),
                        ctx -> stripFeatures(ctx, NON_DEEP_DARK_FEATURES));
        BiomeModifications.create(STRIP_NON_WARM_AQUATIC_ID)
                .add(ModificationPhase.REMOVALS,
                        ctx -> !ctx.getBiomeKey().equals(BiomeKeys.WARM_OCEAN)
                                && !ctx.getBiomeKey().equals(BiomeKeys.LUKEWARM_OCEAN),
                        ctx -> stripFeatures(ctx, NON_WARM_AQUATIC_FEATURES));
        BiomeModifications.create(STRIP_NON_COLD_OCEAN_ID)
                .add(ModificationPhase.REMOVALS,
                        ctx -> !isColdKelpBiome(ctx.getBiomeKey()),
                        ctx -> stripFeatures(ctx, List.of(KELP_COLD)));
    }

    private static void stripNonWarmOceanFeatures(BiomeModificationContext ctx) {
        stripFeatures(ctx, List.of(WARM_OCEAN_VEGETATION, SEA_PICKLE));
    }

    private static void stripFeatures(BiomeModificationContext ctx, List<RegistryKey<PlacedFeature>> features) {
        int removed = 0;
        for (RegistryKey<PlacedFeature> feature : features) {
            if (ctx.getGenerationSettings().removeFeature(feature)) {
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.info("[Latitude] Removed {} misplaced biome feature(s)", removed);
        }
    }

    private static boolean isColdKelpBiome(RegistryKey<net.minecraft.world.biome.Biome> biomeKey) {
        return biomeKey.equals(BiomeKeys.COLD_OCEAN)
                || biomeKey.equals(BiomeKeys.DEEP_COLD_OCEAN)
                || biomeKey.equals(BiomeKeys.OCEAN)
                || biomeKey.equals(BiomeKeys.DEEP_OCEAN);
    }

    private static RegistryKey<PlacedFeature> vanillaFeature(String path) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of("minecraft", path));
    }
}
