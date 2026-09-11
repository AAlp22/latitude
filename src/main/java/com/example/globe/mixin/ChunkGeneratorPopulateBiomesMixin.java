package com.example.globe.mixin;

import com.example.globe.GlobeMod;
import com.example.globe.world.LatitudeBiomeSource;
import com.example.globe.world.LatitudeBiomes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseChunkGenerator.class)
public abstract class ChunkGeneratorPopulateBiomesMixin {
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

    @Unique
    private final ThreadLocal<BiomeSource> globe$populationBiomeSource = new ThreadLocal<>();

    @Shadow
    public abstract boolean matchesSettings(RegistryKey<ChunkGeneratorSettings> settings);

    @Inject(
            method = "populateBiomes(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)V",
            at = @At("HEAD")
    )
    private void globe$prepareAuthoritativeBiomeSource(Blender blender, NoiseConfig noiseConfig,
                                                        StructureAccessor structureAccessor, Chunk chunk,
                                                        CallbackInfo ci) {
        this.globe$populationBiomeSource.remove();
        ChunkGeneratorBiomeSourceAccessor sourceAccessor = (ChunkGeneratorBiomeSourceAccessor) (Object) this;
        BiomeSource current = sourceAccessor.globe$getBiomeSource();
        if (current instanceof LatitudeBiomeSource || !globe$isLatitudeSettings()) {
            return;
        }

        if (structureAccessor != null) {
            Registry<Biome> registry = structureAccessor.getRegistryManager().get(RegistryKeys.BIOME);
            LatitudeBiomes.setActiveBiomeRegistry(registry);
        }

        BiomeSource original = current;
        this.globe$populationBiomeSource.set(new LatitudeBiomeSource(
                original,
                original::getBiomes,
                globe$borderRadiusBlocks(),
                (NoiseChunkGenerator) (Object) this,
                noiseConfig,
                chunk.getHeightLimitView()));
        if (Boolean.getBoolean("latitude.debugWorldgenPath")) {
            GlobeMod.LOGGER.info("[Latitude] selected a runtime biome source for vanilla biome population without mutating generator settings");
        }
    }

    @Inject(
            method = "populateBiomes(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)V",
            at = @At("TAIL")
    )
    private void globe$clearPopulationBiomeSource(Blender blender, NoiseConfig noiseConfig,
                                                    StructureAccessor structureAccessor, Chunk chunk,
                                                    CallbackInfo ci) {
        this.globe$populationBiomeSource.remove();
    }

    @Redirect(
            method = "populateBiomes(Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/gen/chunk/NoiseChunkGenerator;biomeSource:Lnet/minecraft/world/biome/source/BiomeSource;"
            )
    )
    private BiomeSource globe$usePopulationBiomeSource(NoiseChunkGenerator generator) {
        BiomeSource runtimeSource = this.globe$populationBiomeSource.get();
        return runtimeSource != null ? runtimeSource : generator.getBiomeSource();
    }

    private boolean globe$isLatitudeSettings() {
        return this.matchesSettings(GLOBE_SETTINGS_SMALL)
                || this.matchesSettings(GLOBE_SETTINGS_MEDIUM)
                || this.matchesSettings(GLOBE_SETTINGS_REGULAR)
                || this.matchesSettings(GLOBE_SETTINGS_LARGE)
                || this.matchesSettings(GLOBE_SETTINGS_MASSIVE);
    }

    private int globe$borderRadiusBlocks() {
        if (this.matchesSettings(GLOBE_SETTINGS_SMALL)) {
            return 12_500;
        }
        if (this.matchesSettings(GLOBE_SETTINGS_MEDIUM)) {
            return 25_000;
        }
        if (this.matchesSettings(GLOBE_SETTINGS_REGULAR)) {
            return 50_000;
        }
        if (this.matchesSettings(GLOBE_SETTINGS_LARGE)) {
            return 100_000;
        }
        if (this.matchesSettings(GLOBE_SETTINGS_MASSIVE)) {
            return 200_000;
        }
        return GlobeMod.BORDER_RADIUS;
    }
}
