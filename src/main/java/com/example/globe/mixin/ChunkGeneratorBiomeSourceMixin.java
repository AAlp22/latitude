package com.example.globe.mixin;

import com.example.globe.GlobeMod;
import com.example.globe.world.LatitudeBiomeSource;
import com.example.globe.world.LatitudeBiomes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorBiomeSourceMixin {
    private static final boolean DEBUG_WORLDGEN_PATH =
            Boolean.getBoolean("latitude.debugWorldgenPath");

    private static final java.util.concurrent.atomic.AtomicBoolean DEBUG_WRAP_GATE_REJECT_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static final java.util.concurrent.atomic.AtomicBoolean DEBUG_WRAP_SUCCESS_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static final java.util.concurrent.atomic.AtomicBoolean DEBUG_BIOLITH_HANDOFF_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private static final String GLOBE_SETTINGS_CHECKED =
            "globe:overworld_small|globe:overworld_medium|globe:overworld_regular|globe:overworld_large|globe:overworld_massive";

    private static final Identifier GLOBE_SETTINGS_SMALL_ID = Identifier.of("globe", "overworld_small");
    private static final Identifier GLOBE_SETTINGS_MEDIUM_ID = Identifier.of("globe", "overworld_medium");
    private static final Identifier GLOBE_SETTINGS_REGULAR_ID = Identifier.of("globe", "overworld_regular");
    private static final Identifier GLOBE_SETTINGS_LARGE_ID = Identifier.of("globe", "overworld_large");
    private static final Identifier GLOBE_SETTINGS_MASSIVE_ID = Identifier.of("globe", "overworld_massive");

    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_SMALL_KEY =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, GLOBE_SETTINGS_SMALL_ID);
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_MEDIUM_KEY =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, GLOBE_SETTINGS_MEDIUM_ID);
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_REGULAR_KEY =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, GLOBE_SETTINGS_REGULAR_ID);
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_LARGE_KEY =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, GLOBE_SETTINGS_LARGE_ID);
    private static final RegistryKey<ChunkGeneratorSettings> GLOBE_SETTINGS_MASSIVE_KEY =
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, GLOBE_SETTINGS_MASSIVE_ID);
    @Shadow
    @Final
    @Mutable
    private BiomeSource biomeSource;

    @org.spongepowered.asm.mixin.Unique
    private BiomeSource globe$wrappedBiomeSource;

    @Inject(method = "<init>(Lnet/minecraft/world/biome/source/BiomeSource;)V", at = @At("TAIL"), require = 0)
    private void globe$wrapBiomeSource(BiomeSource biomeSource, CallbackInfo ci) {
        globe$maybeWrapBiomeSource();
        globe$installWrappedFieldIfSafe();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/biome/source/BiomeSource;Ljava/util/function/Function;)V", at = @At("TAIL"), require = 0)
    private void globe$wrapBiomeSource(BiomeSource biomeSource, java.util.function.Function<?, ?> settingsLookup, CallbackInfo ci) {
        globe$maybeWrapBiomeSource();
        globe$installWrappedFieldIfSafe();
    }

    @Inject(
            method = "populateBiomes(Ljava/util/concurrent/Executor;Lnet/minecraft/world/gen/noise/NoiseConfig;Lnet/minecraft/world/gen/chunk/Blender;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD")
    )
    private void globe$installBeforeBiomePopulation(Executor executor, NoiseConfig noiseConfig, Blender blender,
                                                     StructureAccessor structureAccessor, Chunk chunk,
                                                     CallbackInfoReturnable<CompletableFuture<Chunk>> cir) {
        if (structureAccessor != null && globe$isAnyGlobeSettings()) {
            Registry<Biome> registry = structureAccessor.getRegistryManager().get(RegistryKeys.BIOME);
            LatitudeBiomes.setActiveBiomeRegistry(registry);
        }
        globe$maybeWrapBiomeSource();
        globe$installWrappedFieldIfSafe();
    }

    /**
     * Biolith assigns the dimension type through ChunkGenerator.getBiomeSource() from
     * DimensionOptions' constructor. Keep returning the original MultiNoise source until
     * that handoff is complete; otherwise Biolith annotates only this wrapper and its
     * later MultiNoise hook sees a null dimension type on the original source.
     */
    private boolean globe$canExposeWrappedBiomeSource() {
        try {
            Class<?> biolithSourceApi = Class.forName("com.terraformersmc.biolith.impl.biome.InterfaceBiomeSource");
            if (!biolithSourceApi.isInstance(this.biomeSource)) {
                return true;
            }

            java.lang.reflect.Method getDimensionType =
                    biolithSourceApi.getMethod("biolith$getDimensionType");
            boolean ready = getDimensionType.invoke(this.biomeSource) != null;
            if (!ready && DEBUG_WORLDGEN_PATH && DEBUG_BIOLITH_HANDOFF_LOGGED.compareAndSet(false, true)) {
                GlobeMod.LOGGER.info("[Latitude] delaying wrapped biome source until Biolith dimension-type handoff completes");
            }
            return ready;
        } catch (ClassNotFoundException ignored) {
            // Biolith is optional; without it there is no handoff to wait for.
            return true;
        } catch (ReflectiveOperationException | SecurityException e) {
            // Fail closed: exposing the wrapper without the handoff recreates Biolith's NPE.
            if (DEBUG_WORLDGEN_PATH && DEBUG_BIOLITH_HANDOFF_LOGGED.compareAndSet(false, true)) {
                GlobeMod.LOGGER.warn("[Latitude] unable to inspect Biolith dimension-type handoff; keeping original biome source", e);
            }
            return false;
        }
    }

    private void globe$installWrappedFieldIfSafe() {
        if (this.globe$wrappedBiomeSource != null
                && !(this.biomeSource instanceof LatitudeBiomeSource)
                && globe$canExposeWrappedBiomeSource()) {
            this.biomeSource = this.globe$wrappedBiomeSource;
        }
    }

    private void globe$maybeWrapBiomeSource() {
        if (this.biomeSource instanceof LatitudeBiomeSource || this.globe$wrappedBiomeSource instanceof LatitudeBiomeSource) {
            return;
        }
        if (!((Object) this instanceof NoiseChunkGenerator)) {
            if (DEBUG_WORLDGEN_PATH && DEBUG_WRAP_GATE_REJECT_LOGGED.compareAndSet(false, true)) {
                GlobeMod.LOGGER.info("[Latitude] biomeSource wrap gate reject: generator is not NoiseChunkGenerator action=not wrapping biome source");
            }
            return;
        }
        if (!globe$isAnyGlobeSettings()) {
            if (DEBUG_WORLDGEN_PATH && DEBUG_WRAP_GATE_REJECT_LOGGED.compareAndSet(false, true)) {
                GlobeMod.LOGGER.info("[Latitude] biomeSource wrap gate reject: settings not Globe preset checked={} matched={} settingsReady={} action=not wrapping biome source",
                        GLOBE_SETTINGS_CHECKED, globe$matchedSettingsLabel(), globe$hasResolvedSettings());
            }
            return;
        }
        BiomeSource original = this.biomeSource;
        java.util.function.Supplier<java.util.Collection<net.minecraft.registry.entry.RegistryEntry<Biome>>> biomes =
                () -> original.getBiomes();
        int borderRadiusBlocks = globe$borderRadiusBlocks();
        // Ensure structure placement and surface rules see the same Latitude biome override as terrain.
        this.globe$wrappedBiomeSource = new LatitudeBiomeSource(original, biomes, borderRadiusBlocks);
        if (DEBUG_WORLDGEN_PATH && DEBUG_WRAP_SUCCESS_LOGGED.compareAndSet(false, true)) {
            GlobeMod.LOGGER.info("[Latitude] Worldgen path active: wrapped ChunkGenerator biomeSource settings={} checked={} radius={} action=using LatitudeBiomeSource",
                    globe$matchedSettingsLabel(), GLOBE_SETTINGS_CHECKED, borderRadiusBlocks);
        }
    }

    private boolean globe$isAnyGlobeSettings() {
        if (!((Object) this instanceof NoiseChunkGenerator noise)) {
            return false;
        }
        // ChunkGenerator/NoiseChunkGenerator settings are not initialized yet in some constructor paths.
        // Never call matchesSettings() until settings is non-null.
        if (!((Object) this instanceof NoiseChunkGeneratorAccessor accessor)) {
            return false;
        }
        if (accessor.globe$getSettings() == null) {
            return false;
        }
        return noise.matchesSettings(GLOBE_SETTINGS_SMALL_KEY)
                || noise.matchesSettings(GLOBE_SETTINGS_MEDIUM_KEY)
                || noise.matchesSettings(GLOBE_SETTINGS_REGULAR_KEY)
                || noise.matchesSettings(GLOBE_SETTINGS_LARGE_KEY)
                || noise.matchesSettings(GLOBE_SETTINGS_MASSIVE_KEY);
    }

    private boolean globe$hasResolvedSettings() {
        if (!((Object) this instanceof NoiseChunkGeneratorAccessor accessor)) {
            return false;
        }
        return accessor.globe$getSettings() != null;
    }

    private String globe$matchedSettingsLabel() {
        if (!((Object) this instanceof NoiseChunkGenerator noise)) {
            return "not_noise_generator";
        }
        if (!globe$hasResolvedSettings()) {
            return "settings_unresolved";
        }
        if (noise.matchesSettings(GLOBE_SETTINGS_SMALL_KEY)) return "overworld_small";
        if (noise.matchesSettings(GLOBE_SETTINGS_MEDIUM_KEY)) return "overworld_medium";
        if (noise.matchesSettings(GLOBE_SETTINGS_REGULAR_KEY)) return "overworld_regular";
        if (noise.matchesSettings(GLOBE_SETTINGS_LARGE_KEY)) return "overworld_large";
        if (noise.matchesSettings(GLOBE_SETTINGS_MASSIVE_KEY)) return "overworld_massive";
        return "unknown";
    }

    private int globe$borderRadiusBlocks() {
        if (!((Object) this instanceof NoiseChunkGenerator noise)) {
            return GlobeMod.BORDER_RADIUS;
        }
        if (noise.matchesSettings(GLOBE_SETTINGS_SMALL_KEY)) return 12500;
        if (noise.matchesSettings(GLOBE_SETTINGS_MEDIUM_KEY)) return 25000;
        if (noise.matchesSettings(GLOBE_SETTINGS_REGULAR_KEY)) return 50000;
        if (noise.matchesSettings(GLOBE_SETTINGS_LARGE_KEY)) return 100000;
        if (noise.matchesSettings(GLOBE_SETTINGS_MASSIVE_KEY)) return 200000;
        return GlobeMod.BORDER_RADIUS;
    }
}
