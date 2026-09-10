package com.example.globe.mixin.client;

import com.example.globe.world.LatitudeSeasonBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldLatitudeSkyMixin {
    @Inject(method = "getSkyBrightness", at = @At("HEAD"), cancellable = true)
    private void latitude$localSkyBrightness(float tickDelta, CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = (ClientWorld) (Object) this;
        if (client.world != world || client.player == null
                || !LatitudeSeasonBridge.isLatitudeWorld(world)) {
            return;
        }

        cir.setReturnValue(LatitudeSeasonBridge.localSkyBrightness(world, client.player.getBlockPos()));
    }
}
