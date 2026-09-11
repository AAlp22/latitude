package com.example.globe.world;

import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Removes only known cross-biome feature entries from Eco's replacement biome JSONs.
 * Vanilla land generation is left intact; the cleanup is keyed by the biome that owns
 * the generation settings rather than by feature namespace alone.
 */
public final class BiomeFeatureStripping {
    private static final Logger LOGGER = LoggerFactory.getLogger("globe");

    private static final String LUSH_CAVES = "minecraft:lush_caves";
    private static final String DRIPSTONE_CAVES = "minecraft:dripstone_caves";
    private static final String DEEP_DARK = "minecraft:deep_dark";
    private static final String WARM_OCEAN = "minecraft:warm_ocean";

    private static final Set<String> OCEAN_BIOMES = Set.of(
            "minecraft:ocean",
            "minecraft:deep_ocean",
            "minecraft:cold_ocean",
            "minecraft:deep_cold_ocean",
            "minecraft:frozen_ocean",
            "minecraft:deep_frozen_ocean",
            "minecraft:lukewarm_ocean",
            "minecraft:deep_lukewarm_ocean",
            WARM_OCEAN);

    private static final Set<String> WARM_SHALLOW_OCEANS = Set.of(
            WARM_OCEAN,
            "minecraft:lukewarm_ocean");
    private static final Set<String> WARM_KELP_OCEANS = Set.of(
            WARM_OCEAN,
            "minecraft:lukewarm_ocean",
            "minecraft:deep_lukewarm_ocean");
    private static final Set<String> DEEP_WARM_SEAGRASS_OCEANS = Set.of(
            "minecraft:deep_lukewarm_ocean");

    /* Eco's ocean additions are deliberately restored to the vanilla ocean baseline. */
    private static final Set<String> OCEAN_ECO_FEATURES = Set.of(
            "eco:amethyst_spiral",
            "eco:bushes_super_dense",
            "eco:cave_brain_coral",
            "eco:cave_bubble_coral",
            "eco:cave_ceiling_shrubbery",
            "eco:cave_fire_coral",
            "eco:cave_kelp_warm",
            "eco:cave_leaf_vines",
            "eco:cave_pickle",
            "eco:cave_seagrass_warm",
            "eco:cave_slime_vines",
            "eco:cave_tube_coral",
            "eco:ceiling_dripstone",
            "eco:dense_dark_prismarine_water_fall",
            "eco:dense_powdered_snow_fall",
            "eco:hanging_sea_lantern",
            "eco:large_pumpkin",
            "eco:lush_spore_blossom",
            "eco:patch_snow_layer",
            "eco:prismarine_pool_with_dripleaves",
            "eco:powdered_snow_pool",
            "eco:snowy_pool_with_ice",
            "eco:sparse_aquatic_vines",
            "eco:sparse_dark_prismarine_pillar",
            "eco:sparse_packed_ice_pillar",
            "eco:sparse_prismarine_pillar",
            "eco:sparse_thin_blue_ice_pillar",
            "eco:thin_slime_pillar",
            "eco:underwater_blue_ice",
            "eco:underwater_clay",
            "eco:underwater_diorite",
            "eco:underwater_noise_andesite_rocks",
            "eco:underwater_noise_calcite_rocks",
            "eco:underwater_noise_granite_rocks",
            "eco:underwater_noise_ice_rocks",
            "eco:underwater_packed_mud");

    private static final Set<String> CAVE_VEGETATION_FEATURES = Set.of(
            "eco:cave_ceiling_shrubbery",
            "eco:cave_leaf_vines",
            "eco:cave_slime_vines",
            "eco:lush_cave_vines",
            "eco:lush_caves_vegetation",
            "eco:lush_spore_blossom",
            "eco:sparse_lush_caves_vegetation",
            "minecraft:cave_vines",
            "minecraft:classic_vines_cave_feature",
            "minecraft:lush_caves_ceiling_vegetation",
            "minecraft:lush_caves_clay",
            "minecraft:lush_caves_vegetation",
            "minecraft:rooted_azalea_tree",
            "minecraft:spore_blossom");

    private static final Set<String> SCULK_FEATURES = Set.of(
            "eco:sculk_patch_deep_dark",
            "eco:sculk_vein",
            "minecraft:sculk_patch_deep_dark",
            "minecraft:sculk_vein");

    private static final Set<String> DRIPSTONE_FEATURES = Set.of(
            "eco:ceiling_dripstone",
            "eco:dripstone_cluster",
            "eco:large_dripstone",
            "eco:pointed_dripstone",
            "minecraft:dripstone_cluster",
            "minecraft:large_dripstone",
            "minecraft:pointed_dripstone");

    /* These are valid in ocean biomes, not in land biomes where Eco also listed them. */
    private static final Set<String> LAND_AQUATIC_FEATURES = Set.of(
            "minecraft:kelp_cold",
            "minecraft:kelp_warm",
            "minecraft:sea_pickle",
            "minecraft:seagrass_cold",
            "minecraft:seagrass_deep_cold",
            "minecraft:seagrass_deep_warm",
            "minecraft:seagrass_deep",
            "minecraft:seagrass_normal",
            "minecraft:seagrass_simple",
            "minecraft:seagrass_warm");

    private BiomeFeatureStripping() {
    }

    public static void init() {
        if (Boolean.getBoolean("latitude.disableFeatureStripping")) {
            LOGGER.info("[Latitude] Biome feature isolation disabled by system property.");
            return;
        }

        registerFamily("isolate_ocean_eco_features",
                ctx -> isVanillaBiome(ctx) && OCEAN_BIOMES.contains(ctx.getBiomeKey().getValue().toString()),
                OCEAN_ECO_FEATURES);
        registerFamily("isolate_cave_vegetation",
                ctx -> isVanillaBiome(ctx) && !LUSH_CAVES.equals(ctx.getBiomeKey().getValue().toString()),
                CAVE_VEGETATION_FEATURES);
        registerFamily("isolate_sculk",
                ctx -> isVanillaBiome(ctx) && !DEEP_DARK.equals(ctx.getBiomeKey().getValue().toString()),
                SCULK_FEATURES);
        registerFamily("isolate_dripstone",
                ctx -> isVanillaBiome(ctx) && !DRIPSTONE_CAVES.equals(ctx.getBiomeKey().getValue().toString()),
                DRIPSTONE_FEATURES);
        registerFamily("isolate_land_aquatic_features",
                ctx -> isVanillaBiome(ctx) && !OCEAN_BIOMES.contains(ctx.getBiomeKey().getValue().toString()),
                LAND_AQUATIC_FEATURES);
        registerFamily("isolate_seagrass_warm",
                ctx -> isOceanBiome(ctx) && !WARM_SHALLOW_OCEANS.contains(biomeId(ctx)),
                Set.of("minecraft:seagrass_warm"));
        registerFamily("isolate_seagrass_deep_warm",
                ctx -> isOceanBiome(ctx) && !DEEP_WARM_SEAGRASS_OCEANS.contains(biomeId(ctx)),
                Set.of("minecraft:seagrass_deep_warm"));
        registerFamily("isolate_kelp_warm",
                ctx -> isOceanBiome(ctx) && !WARM_KELP_OCEANS.contains(biomeId(ctx)),
                Set.of("minecraft:kelp_warm"));
        registerFamily("isolate_sea_pickle",
                ctx -> isOceanBiome(ctx) && !WARM_OCEAN.equals(biomeId(ctx)),
                Set.of("minecraft:sea_pickle"));
        registerFamily("isolate_warm_ocean_vegetation",
                ctx -> isVanillaBiome(ctx) && !WARM_OCEAN.equals(ctx.getBiomeKey().getValue().toString()),
                Set.of("minecraft:warm_ocean_vegetation"));
    }

    private static void registerFamily(String name, Predicate<BiomeSelectionContext> selector,
                                       Set<String> featureIds) {
        BiomeModifications.create(Identifier.of("globe", name))
                .add(ModificationPhase.REMOVALS, selector,
                        ctx -> removeFeatureFamily(ctx, name, featureIds));
    }

    private static void removeFeatureFamily(BiomeModificationContext ctx, String family,
                                             Set<String> featureIds) {
        int removed = 0;
        for (String featureId : featureIds) {
            RegistryKey<PlacedFeature> key = placedFeatureKey(featureId);
            if (ctx.getGenerationSettings().removeFeature(key)) {
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.info("[Latitude] isolated feature family={} removed={} entries", family, removed);
        }
    }

    private static boolean isVanillaBiome(BiomeSelectionContext ctx) {
        return "minecraft".equals(ctx.getBiomeKey().getValue().getNamespace());
    }

    private static boolean isOceanBiome(BiomeSelectionContext ctx) {
        return isVanillaBiome(ctx) && OCEAN_BIOMES.contains(biomeId(ctx));
    }

    private static String biomeId(BiomeSelectionContext ctx) {
        return ctx.getBiomeKey().getValue().toString();
    }

    private static RegistryKey<PlacedFeature> placedFeatureKey(String id) {
        int separator = id.indexOf(':');
        if (separator <= 0 || separator == id.length() - 1) {
            throw new IllegalArgumentException("Invalid placed feature id: " + id);
        }
        Identifier identifier = Identifier.of(id.substring(0, separator), id.substring(separator + 1));
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, identifier);
    }
}
