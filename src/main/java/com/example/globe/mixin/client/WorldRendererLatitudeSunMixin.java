package com.example.globe.mixin.client;

import com.example.globe.world.LatitudeCelestialGeometry;
import com.example.globe.world.LatitudeSeasonBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererLatitudeSunMixin {
    @Inject(
            method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V",
                    ordinal = 0
            )
    )
    private void latitude$tiltSun(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta,
                                  Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        matrices.push();

        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || client.player == null || !LatitudeSeasonBridge.isLatitudeWorld(world)) {
            return;
        }

        double rotationDegrees = LatitudeCelestialGeometry.sunDeclinationRotationDegrees(
                LatitudeSeasonBridge.at(world, client.player.getX(), client.player.getZ()).solar());
        if (Math.abs(rotationDegrees) <= 1.0e-9) {
            return;
        }

        latitude$leftMultiplyPositionRotation(matrices, (float) Math.toRadians(rotationDegrees));
    }

    @Inject(
            method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V",
                    ordinal = 1
            )
    )
    private void latitude$restoreSunMatrix(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta,
                                           Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        matrices.pop();
    }

    @Unique
    private static void latitude$leftMultiplyPositionRotation(MatrixStack matrices, float radians) {
        Matrix4f position = matrices.peek().getPositionMatrix();
        // MatrixStack.multiply post-multiplies; this left product tilts the final sky position.
        Matrix4f tilted = new Matrix4f().rotationX(radians).mul(new Matrix4f(position));
        position.set(tilted);
    }
}
