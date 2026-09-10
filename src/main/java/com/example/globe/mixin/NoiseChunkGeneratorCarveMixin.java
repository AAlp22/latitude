package com.example.globe.mixin;

import com.example.globe.GlobeRegions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseChunkGenerator.class)
public class NoiseChunkGeneratorCarveMixin {
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_SMALL_KEY = RegistryKey.of(
            RegistryKeys.CHUNK_GENERATOR_SETTINGS,
            Identifier.of("globe", "overworld_small")
    );
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_MEDIUM_KEY = RegistryKey.of(
            RegistryKeys.CHUNK_GENERATOR_SETTINGS,
            Identifier.of("globe", "overworld_medium")
    );
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_REGULAR_KEY = RegistryKey.of(
            RegistryKeys.CHUNK_GENERATOR_SETTINGS,
            Identifier.of("globe", "overworld_regular")
    );
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_LARGE_KEY = RegistryKey.of(
            RegistryKeys.CHUNK_GENERATOR_SETTINGS,
            Identifier.of("globe", "overworld_large")
    );
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_MASSIVE_KEY = RegistryKey.of(
            RegistryKeys.CHUNK_GENERATOR_SETTINGS,
            Identifier.of("globe", "overworld_massive")
    );

    @Inject(
            method = "carve(Lnet/minecraft/world/ChunkRegion;JLnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/biome/source/BiomeAccess;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/GenerationStep$Carver;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void globe$disableCarversInPolarCap(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                                               StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carver, CallbackInfo ci) {
        NoiseChunkGenerator self = (NoiseChunkGenerator) (Object) this;
        if (!self.matchesSettings(GLOBE_SETTINGS_SMALL_KEY)
                && !self.matchesSettings(GLOBE_SETTINGS_MEDIUM_KEY)
                && !self.matchesSettings(GLOBE_SETTINGS_REGULAR_KEY)
                && !self.matchesSettings(GLOBE_SETTINGS_LARGE_KEY)
                && !self.matchesSettings(GLOBE_SETTINGS_MASSIVE_KEY)) {
            return;
        }

        int centerZ = chunk.getPos().getStartZ() + 8;
        if (Math.abs(centerZ) >= GlobeRegions.POLAR_CAP_START) {
            ci.cancel();
        }
    }
}
