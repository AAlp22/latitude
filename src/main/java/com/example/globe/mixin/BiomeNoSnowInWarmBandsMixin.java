package com.example.globe.mixin;

import com.example.globe.GlobeMod;
import com.example.globe.util.LatitudeBands;
import com.example.globe.world.LatitudeSeasonBridge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class BiomeNoSnowInWarmBandsMixin {

    private static boolean globe$isWarmBand(int z) {
        LatitudeBands.Band band = LatitudeBands.fromAbsoluteLatitudeDeg(
                Math.abs((double) z) * 90.0 / Math.max(1, GlobeMod.BORDER_RADIUS));
        return band == LatitudeBands.Band.TROPICAL
                || band == LatitudeBands.Band.SUBTROPICAL
                || band == LatitudeBands.Band.TEMPERATE;
    }

    @Inject(method = "getTemperature", at = @At("RETURN"), cancellable = true)
    private void latitude$applyLocalTemperature(BlockPos pos, CallbackInfoReturnable<Float> cir) {
        World world = LatitudeSeasonBridge.contextWorldFor(pos);
        if (world != null) {
            cir.setReturnValue(LatitudeSeasonBridge.adjustedTemperature(world, pos, cir.getReturnValueF()));
        }
    }

    @Inject(method = "doesNotSnow", at = @At("HEAD"), cancellable = true)
    private void globe$blockSnowInWarmBands(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        World world = LatitudeSeasonBridge.contextWorldFor(pos);
        if (world != null) {
            cir.setReturnValue(LatitudeSeasonBridge.doesNotSnow(
                    (Biome) (Object) this, world, pos));
            return;
        }

        if (globe$isWarmBand(pos.getZ())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true)
    private void globe$useLocalPrecipitation(BlockPos pos,
                                              CallbackInfoReturnable<Biome.Precipitation> cir) {
        World world = LatitudeSeasonBridge.contextWorldFor(pos);
        if (world != null) {
            cir.setReturnValue(LatitudeSeasonBridge.precipitationFor(
                    (Biome) (Object) this, world, pos));
            return;
        }

        if (globe$isWarmBand(pos.getZ())) {
            Biome self = (Biome) (Object) this;
            cir.setReturnValue(self.hasPrecipitation()
                    ? Biome.Precipitation.RAIN
                    : Biome.Precipitation.NONE);
        }
    }
}
