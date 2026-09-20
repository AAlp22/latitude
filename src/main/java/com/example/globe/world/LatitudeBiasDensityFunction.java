package com.example.globe.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Adds a latitude-dependent bias to a density function. The weight rises smoothly from
 * {@code start} to {@code end} of |z| / active globe radius, so polar caps can be pushed
 * toward land regardless of the continent noise. Pure-noise shaping cannot read latitude;
 * this is the code-side hook for the guaranteed-poles feature.
 */
public record LatitudeBiasDensityFunction(RegistryEntry<DensityFunction> input, double amplitude,
                                          double startFraction, double endFraction) implements DensityFunction {
    public static final Codec<LatitudeBiasDensityFunction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DensityFunction.REGISTRY_ENTRY_CODEC.fieldOf("input").forGetter(LatitudeBiasDensityFunction::input),
            Codec.DOUBLE.fieldOf("amplitude").forGetter(LatitudeBiasDensityFunction::amplitude),
            Codec.DOUBLE.optionalFieldOf("start", 0.72).forGetter(LatitudeBiasDensityFunction::startFraction),
            Codec.DOUBLE.optionalFieldOf("end", 0.92).forGetter(LatitudeBiasDensityFunction::endFraction)
    ).apply(instance, LatitudeBiasDensityFunction::new));

    @Override
    public double sample(NoisePos pos) {
        return input.value().sample(pos) + amplitude * polarWeight(pos.blockZ());
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new LatitudeBiasDensityFunction(
                RegistryEntry.of(input.value().apply(visitor)), amplitude, startFraction, endFraction));
    }

    @Override
    public double minValue() {
        return input.value().minValue() + Math.min(0.0, amplitude);
    }

    @Override
    public double maxValue() {
        return input.value().maxValue() + Math.max(0.0, amplitude);
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CodecHolder.of(CODEC);
    }

    private double polarWeight(int blockZ) {
        int radius = LatitudeBiomes.ACTIVE_RADIUS_BLOCKS;
        if (radius <= 0) {
            return 0.0;
        }
        double latFraction = Math.abs((double) blockZ) / (double) radius;
        if (endFraction <= startFraction) {
            return latFraction >= endFraction ? 1.0 : 0.0;
        }
        double t = (latFraction - startFraction) / (endFraction - startFraction);
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }
}
