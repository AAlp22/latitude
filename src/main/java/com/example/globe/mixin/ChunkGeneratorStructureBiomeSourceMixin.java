package com.example.globe.mixin;

import com.example.globe.GlobeMod;
import com.example.globe.world.LatitudeBiomeSource;
import com.example.globe.world.LatitudeBiomes;
import com.example.globe.world.LatitudeStructureBiomeSourceProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorStructureBiomeSourceMixin implements LatitudeStructureBiomeSourceProvider {
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_SMALL =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.of("globe", "overworld_small"));
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_MEDIUM =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.of("globe", "overworld_medium"));
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_REGULAR =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.of("globe", "overworld_regular"));
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_LARGE =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.of("globe", "overworld_large"));
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_MASSIVE =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.of("globe", "overworld_massive"));

    @Shadow
    @Final
    private BiomeSource biomeSource;

    @Unique
    private BiomeSource globe$structureBiomeSource;

    @Redirect(
            method = "trySetStructureStart",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;biomeSource:Lnet/minecraft/world/biome/source/BiomeSource;"
            )
    )
    private BiomeSource globe$useStructureBiomeSource(ChunkGenerator generator) {
        return globe$getStructureBiomeSource();
    }

    @Override
    @Unique
    public BiomeSource globe$getStructureBiomeSource() {
        if (!((Object) this instanceof NoiseChunkGenerator noise) || !globe$isLatitudeSettings(noise)) {
            return this.biomeSource;
        }
        if (this.globe$structureBiomeSource == null) {
            BiomeSource original = this.biomeSource;
            this.globe$structureBiomeSource = new LatitudeBiomeSource(
                    original,
                    original::getBiomes,
                    globe$borderRadiusBlocks(noise));
            if (Boolean.getBoolean("latitude.debugWorldgenPath")) {
                GlobeMod.LOGGER.info("[Latitude] structure validation source enabled without replacing serialized biome source");
            }
        }
        return this.globe$structureBiomeSource;
    }

    @Unique
    private static boolean globe$isLatitudeSettings(NoiseChunkGenerator noise) {
        return noise.matchesSettings(GLOBE_SETTINGS_SMALL)
                || noise.matchesSettings(GLOBE_SETTINGS_MEDIUM)
                || noise.matchesSettings(GLOBE_SETTINGS_REGULAR)
                || noise.matchesSettings(GLOBE_SETTINGS_LARGE)
                || noise.matchesSettings(GLOBE_SETTINGS_MASSIVE);
    }

    @Unique
    private static int globe$borderRadiusBlocks(NoiseChunkGenerator noise) {
        if (noise.matchesSettings(GLOBE_SETTINGS_SMALL)) {
            return 12_500;
        }
        if (noise.matchesSettings(GLOBE_SETTINGS_MEDIUM)) {
            return 25_000;
        }
        if (noise.matchesSettings(GLOBE_SETTINGS_REGULAR)) {
            return 50_000;
        }
        if (noise.matchesSettings(GLOBE_SETTINGS_LARGE)) {
            return 100_000;
        }
        if (noise.matchesSettings(GLOBE_SETTINGS_MASSIVE)) {
            return 200_000;
        }
        return GlobeMod.BORDER_RADIUS;
    }
}
