package com.example.globe.mixin;

import com.example.globe.world.LatitudeBiomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Feature generation intersects the biome set collected from the 3x3 chunk-neighborhood
 * palettes with the biome source's getBiomes(), and the memoized indexed-features list
 * (method_44215) is built from the same call. The serialized generator field must stay the
 * concrete vanilla source (see the serialization contract), but that source only reports
 * vanilla parameter-list biomes, so every tag-bridged pool biome was stripped from the
 * feature set and its placed features (trees, vegetation) never ran. Substitute the
 * decorated pool at these two read sites only.
 *
 * This class MUST stay registered in globe.mixins.json: an unregistered mixin source file
 * compiles and packages fine and then silently does nothing at runtime. The retired
 * ChunkGeneratorBiomeSourceMixin source-wrap experiment stays unregistered on purpose.
 */
@Mixin(net.minecraft.world.gen.chunk.ChunkGenerator.class)
public abstract class ChunkGeneratorFeatureSetMixin {

    @Redirect(
            method = "generateFeatures(Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/StructureAccessor;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/source/BiomeSource;getBiomes()Ljava/util/Set;"),
            require = 1
    )
    private static java.util.Set<net.minecraft.registry.entry.RegistryEntry<Biome>> globe$decoratedFeatureBiomeSet(BiomeSource source) {
        return java.util.Set.copyOf(LatitudeBiomes.completeBiomePool(source.getBiomes()));
    }

    @Redirect(
            method = "method_44215",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/source/BiomeSource;getBiomes()Ljava/util/Set;"),
            require = 1
    )
    private static java.util.Set<net.minecraft.registry.entry.RegistryEntry<Biome>> globe$decoratedIndexedFeatureBiomes(BiomeSource source) {
        return java.util.Set.copyOf(LatitudeBiomes.completeBiomePool(source.getBiomes()));
    }
}
