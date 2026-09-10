package com.example.globe.mixin;

import com.example.globe.world.LatitudeSeasonBridge;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldView.class)
public interface WorldViewLatitudeContextMixin {
    @Inject(method = "getBiome", at = @At("RETURN"))
    private void latitude$rememberBiomeQuery(BlockPos pos,
                                             CallbackInfoReturnable<RegistryEntry<Biome>> cir) {
        Object self = this;
        if (self instanceof World world && LatitudeSeasonBridge.isLatitudeWorld(world)) {
            LatitudeSeasonBridge.rememberBiomeQuery(world, pos);
        }
    }
}
