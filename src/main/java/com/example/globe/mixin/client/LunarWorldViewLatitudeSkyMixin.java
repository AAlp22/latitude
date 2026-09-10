package com.example.globe.mixin.client;

import com.example.globe.world.LatitudeSeasonBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.LunarWorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LunarWorldView.class)
public interface LunarWorldViewLatitudeSkyMixin {
    @Inject(method = "getSkyAngle(F)F", at = @At("HEAD"), cancellable = true)
    private void latitude$localSkyAngle(float tickDelta, CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        Object self = this;
        if (!(self instanceof ClientWorld world) || client.world != world || client.player == null
                || !LatitudeSeasonBridge.isLatitudeWorld(world)) {
            return;
        }

        cir.setReturnValue(LatitudeSeasonBridge.localSkyAngle(world, client.player.getX(), client.player.getZ()));
    }

    @Inject(method = "getMoonPhase()I", at = @At("HEAD"), cancellable = true)
    private void latitude$localMoonPhase(CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (!(self instanceof ClientWorld world) || !LatitudeSeasonBridge.isLatitudeWorld(world)) {
            return;
        }
        cir.setReturnValue(LatitudeSeasonBridge.localMoonPhase(world));
    }
}
