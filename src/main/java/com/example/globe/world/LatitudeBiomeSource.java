package com.example.globe.world;

import com.example.globe.mixin.BiomeSourceAccessor;
import com.mojang.serialization.Codec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LatitudeBiomeSource extends BiomeSource {
    private final BiomeSource original;
    private final Supplier<Collection<RegistryEntry<Biome>>> biomes;
    private final int borderRadiusBlocks;
    private final NoiseChunkGenerator generator;
    private final NoiseConfig noiseConfig;
    private final HeightLimitView heightView;

    public LatitudeBiomeSource(BiomeSource original, Supplier<Collection<RegistryEntry<Biome>>> biomes, int borderRadiusBlocks) {
        this(original, biomes, borderRadiusBlocks, null, null, null);
    }

    public LatitudeBiomeSource(BiomeSource original, Supplier<Collection<RegistryEntry<Biome>>> biomes,
                               int borderRadiusBlocks, NoiseChunkGenerator generator,
                               NoiseConfig noiseConfig, HeightLimitView heightView) {
        this.original = original;
        this.biomes = biomes;
        this.borderRadiusBlocks = borderRadiusBlocks;
        this.generator = generator;
        this.noiseConfig = noiseConfig;
        this.heightView = heightView;
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
        return biomes.get().stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        RegistryEntry<Biome> base = original.getBiome(x, y, z, sampler);
        int blockX = x << 2;
        int blockZ = z << 2;
        int blockY = y << 2;
        Collection<RegistryEntry<Biome>> pool = biomes.get();
        String callerContext = generator != null ? "POPULATE" : "SOURCE";
        return LatitudeBiomes.pick(pool, base, blockX, blockZ, blockY, borderRadiusBlocks, sampler,
                callerContext, generator, noiseConfig, heightView);
    }
}
