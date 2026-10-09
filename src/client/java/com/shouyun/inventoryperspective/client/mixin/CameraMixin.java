package com.shouyun.inventoryperspective.client.mixin;

import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow private boolean detached;
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void inventoryPerspective$shoulderCamera(BlockGetter level, Entity entity, boolean thirdPerson,
                                                     boolean mirrored, float tickDelta, CallbackInfo ci) {
        if (!InventoryCameraController.controlsCamera()) return;
        double progress = InventoryCameraController.progress();
        CameraType previous = InventoryCameraController.previousPerspective();
        float yaw = entity.getViewYRot(tickDelta);
        float pitch = entity.getViewXRot(tickDelta);
        float previousYaw = yaw + (previous.isMirrored() ? 180 : 0);
        float previousPitch = previous.isMirrored() ? -pitch : pitch;
        float scale = entity instanceof LivingEntity living ? living.getScale() : 1;
        Vec3 anchor = new Vec3(Mth.lerp(tickDelta, entity.xo, entity.getX()),
                Mth.lerp(tickDelta, entity.yo, entity.getY()) + Mth.lerp(tickDelta, eyeHeightOld, eyeHeight),
                Mth.lerp(tickDelta, entity.zo, entity.getZ()));
        Vec3 originalOffset = previous.isFirstPerson() ? Vec3.ZERO
                : Vec3.directionFromRotation(previousPitch, previousYaw).scale(-4 * scale);
        // Positive camera-right offset puts the player on the left of the picture.
        double side = InventoryPerspectiveConfig.get().inventoryOnRight ? 0.95 : -0.95;
        var window = Minecraft.getInstance().getWindow();
        side *= Math.min(1, (double) window.getWidth() / window.getHeight() / 1.6);
        double radians = Math.toRadians(yaw);
        Vec3 shoulderOffset = Vec3.directionFromRotation(18, yaw).scale(-3.2 * scale)
                .add(-Math.cos(radians) * side * scale, 0.2 * scale, -Math.sin(radians) * side * scale);
        Vec3 offset = originalOffset.lerp(shoulderOffset, progress);
        setRotation(Mth.rotLerp((float) progress, previousYaw, yaw), Mth.lerp((float) progress, previousPitch, 18));
        setPosition(anchor.add(inventoryPerspective$clipOffset(level, entity, anchor, offset)));
        detached = progress > 0.23 || !previous.isFirstPerson();
    }

    @Unique
    private static Vec3 inventoryPerspective$clipOffset(BlockGetter level, Entity entity, Vec3 anchor, Vec3 offset) {
        double distance = offset.length();
        if (distance < 0.0001) return offset;
        double fraction = 1;
        // Sweep all eight near-camera corners, including the lateral shoulder offset.
        for (int i = 0; i < 8; i++) {
            Vec3 start = anchor.add((i & 1) == 0 ? -0.1 : 0.1,
                    (i & 2) == 0 ? -0.1 : 0.1, (i & 4) == 0 ? -0.1 : 0.1);
            HitResult hit = level.clip(new ClipContext(start, start.add(offset),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.MISS) {
                fraction = Math.min(fraction, Math.max(0, (start.distanceTo(hit.getLocation()) - 0.05) / distance));
            }
        }
        return offset.scale(fraction);
    }
}
