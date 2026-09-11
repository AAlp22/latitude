package com.example.globe.mixin;

import com.example.globe.world.LatitudeStructureBiomeSourceProvider;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Structure.class)
public abstract class StructureBiomeSourceMixin {
    @Redirect(
            method = "isBiomeValid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;getBiomeSource()Lnet/minecraft/world/biome/source/BiomeSource;"
            )
    )
    private static BiomeSource globe$useStructureBiomeSource(ChunkGenerator generator) {
        if (generator instanceof LatitudeStructureBiomeSourceProvider provider) {
            BiomeSource runtime = provider.globe$getStructureBiomeSource();
            if (runtime != null) {
                return runtime;
            }
        }
        return generator.getBiomeSource();
    }
}
