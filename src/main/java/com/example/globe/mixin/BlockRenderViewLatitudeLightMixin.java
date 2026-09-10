package com.example.globe.mixin;

import com.example.globe.world.LatitudeSeasonBridge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderView.class)
public interface BlockRenderViewLatitudeLightMixin {
    @Inject(method = "getLightLevel", at = @At("HEAD"), cancellable = true)
    private void latitude$localSkyLight(LightType type, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (type != LightType.SKY || !(self instanceof World world)
                || !LatitudeSeasonBridge.isLatitudeWorld(world)) {
            return;
        }

        int storedSky = world.getLightingProvider().get(LightType.SKY).getLightLevel(pos);
        cir.setReturnValue(LatitudeSeasonBridge.effectiveSkyLight(world, pos, storedSky));
    }

    @Inject(method = "getBaseLightLevel", at = @At("HEAD"), cancellable = true)
    private void latitude$localBaseLight(BlockPos pos, int ambientDarkness,
                                         CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (!(self instanceof World world) || !LatitudeSeasonBridge.isLatitudeWorld(world)) {
            return;
        }

        int storedSky = world.getLightingProvider().get(LightType.SKY).getLightLevel(pos);
        int storedBlock = world.getLightingProvider().get(LightType.BLOCK).getLightLevel(pos);
        cir.setReturnValue(LatitudeSeasonBridge.effectiveBaseLight(
                world, pos, storedSky, storedBlock, ambientDarkness));
    }
}
