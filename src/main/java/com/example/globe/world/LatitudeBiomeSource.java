package com.example.globe.world;

import com.example.globe.mixin.BiomeSourceAccessor;
import com.mojang.serialization.Codec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LatitudeBiomeSource extends BiomeSource {
    private final BiomeSource original;
    private final Supplier<Collection<RegistryEntry<Biome>>> biomes;
    private final int borderRadiusBlocks;

    public LatitudeBiomeSource(BiomeSource original, Supplier<Collection<RegistryEntry<Biome>>> biomes, int borderRadiusBlocks) {
        this.original = original;
        this.biomes = biomes;
        this.borderRadiusBlocks = borderRadiusBlocks;
    }

    public BiomeSource original() {
        return original;
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        @SuppressWarnings("unchecked")
        Codec<? extends BiomeSource> delegate = ((BiomeSourceAccessor) original).globe$invokeGetCodec();
        // Keep the registered concrete codec. The wrapper is never serialized as a
        // standalone biome-source type; it is installed only after generator settings load.
        return delegate;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        // StructurePlacementCalculator uses this set as a possible-biome prefilter.
        // The active registry is broader than the source's actual noise entries.
        return biomes.get().stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        RegistryEntry<Biome> base = original.getBiome(x, y, z, sampler);
        int blockX = x << 2;
        int blockZ = z << 2;
        int blockY = y << 2;
        Collection<RegistryEntry<Biome>> pool = biomes.get();
        if (LatitudeCaveBiomeRules.isCaveBiome(base)) {
            return base;
        }
        return LatitudeBiomes.pick(pool, base, blockX, blockZ, blockY, borderRadiusBlocks, sampler, "SOURCE", null, null, null);
    }
}
