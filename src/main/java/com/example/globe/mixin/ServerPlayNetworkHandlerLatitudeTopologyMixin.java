package com.example.globe.mixin;

import com.example.globe.GlobeMod;
import com.example.globe.world.LatitudeLongitudeMovePlanner;
import com.example.globe.world.LatitudePoleCrossingTransform;
import com.example.globe.world.LatitudeWorldTopologyMapper;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerLatitudeTopologyMixin {
    @Unique
    private static final String latitude$ENABLE_LONGITUDE_PROPERTY = "latitude.experimentalLongitudeLoop";

    @Unique
    private static final String latitude$ENABLE_POLE_PROPERTY = "latitude.experimentalPoleTraversal";

    @Unique
    private static final double latitude$MAX_CROSSING_STEP_BLOCKS = 4.0;

    @Unique
    private static final double latitude$TRIGGER_MARGIN_BLOCKS = 1.0;

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    public abstract void requestTeleport(double x, double y, double z, float yaw, float pitch);

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void latitude$interceptLongitudeSeam(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        boolean longitudeEnabled = Boolean.getBoolean(latitude$ENABLE_LONGITUDE_PROPERTY);
        boolean poleEnabled = Boolean.getBoolean(latitude$ENABLE_POLE_PROPERTY);
        if ((!longitudeEnabled && !poleEnabled)
                || packet == null
                || !packet.changesPosition()) {
            return;
        }

        ServerPlayerEntity currentPlayer = player;
        if (currentPlayer == null
                || currentPlayer.isRemoved()
                || currentPlayer.isSpectator()
                || currentPlayer.hasVehicle()
                || currentPlayer.hasPassengers()
                || currentPlayer.isSleeping()
                || currentPlayer.isInTeleportationState()) {
            return;
        }

        if (!(currentPlayer.getEntityWorld() instanceof ServerWorld world)
                || !GlobeMod.isGlobeOverworld(world)) {
            return;
        }

        WorldBorder border = world.getWorldBorder();
        double borderDiameter = border.getSize();
        if (!Double.isFinite(borderDiameter) || borderDiameter <= 0.0) {
            return;
        }

        double targetX = packet.getX(currentPlayer.getX());
        double targetY = packet.getY(currentPlayer.getY());
        double targetZ = packet.getZ(currentPlayer.getZ());
        float targetYaw = packet.getYaw(currentPlayer.getYaw());
        float targetPitch = packet.getPitch(currentPlayer.getPitch());
        if (!Double.isFinite(targetX)
                || !Double.isFinite(targetY)
                || !Double.isFinite(targetZ)
                || !Float.isFinite(targetYaw)
                || !Float.isFinite(targetPitch)) {
            return;
        }
        if (Math.abs(targetY - currentPlayer.getY()) > latitude$MAX_CROSSING_STEP_BLOCKS) {
            return;
        }

        LatitudeLongitudeMovePlanner planner = new LatitudeLongitudeMovePlanner(
                borderDiameter,
                border.getCenterX(),
                border.getCenterZ(),
                latitude$MAX_CROSSING_STEP_BLOCKS,
                latitude$TRIGGER_MARGIN_BLOCKS);
        LatitudeLongitudeMovePlanner.Plan plan = planner.planWithPoleTriggers(
                currentPlayer.getX(),
                currentPlayer.getZ(),
                targetX,
                targetZ);
        if (!plan.intercept()
                || (plan.movement().crossedLongitude() && !longitudeEnabled)
                || (plan.movement().crossedPole() && !poleEnabled)) {
            return;
        }

        LatitudeWorldTopologyMapper.MappedPosition position = plan.position();
        if (position == null || !currentPlayer.doesNotCollide(position.worldX(), targetY, position.worldZ())) {
            return;
        }

        Vec3d velocity = currentPlayer.getVelocity();
        float effectiveYaw = targetYaw;
        if (plan.movement().crossedPole()) {
            LatitudePoleCrossingTransform.Motion transformed = LatitudePoleCrossingTransform.transform(
                    velocity.x,
                    velocity.z,
                    targetYaw);
            effectiveYaw = (float) transformed.yawDegrees();
            velocity = new Vec3d(transformed.xVelocity(), velocity.y, transformed.meridionalVelocity());
        }

        requestTeleport(position.worldX(), targetY, position.worldZ(), effectiveYaw, targetPitch);
        world.getChunkManager().updatePosition(currentPlayer);
        currentPlayer.setVelocity(velocity);
        ci.cancel();
    }
}
