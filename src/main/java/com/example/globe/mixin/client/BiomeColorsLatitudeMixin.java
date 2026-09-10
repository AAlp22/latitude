package com.example.globe.mixin.client;

import com.example.globe.world.LatitudeSeasonBridge;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class BiomeColorsLatitudeMixin {
    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private static void latitude$grassColor(BlockRenderView world, BlockPos pos,
                                            CallbackInfoReturnable<Integer> cir) {
        if (world instanceof World actualWorld && LatitudeSeasonBridge.isLatitudeWorld(actualWorld)) {
            cir.setReturnValue(LatitudeSeasonBridge.localBiomeColor(
                    actualWorld, pos, cir.getReturnValueI(), false));
        }
    }

    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private static void latitude$foliageColor(BlockRenderView world, BlockPos pos,
                                              CallbackInfoReturnable<Integer> cir) {
        if (world instanceof World actualWorld && LatitudeSeasonBridge.isLatitudeWorld(actualWorld)) {
            cir.setReturnValue(LatitudeSeasonBridge.localBiomeColor(
                    actualWorld, pos, cir.getReturnValueI(), true));
        }
    }
}
