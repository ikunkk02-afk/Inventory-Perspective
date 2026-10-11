package com.shouyun.inventoryperspective.client.camera;

import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;

/** Owns the temporary perspective only while the survival inventory is open or returning. */
public final class InventoryCameraController {
    private static CameraType previousPerspective;
    private static ClientLevel previousLevel;
    private static Object previousPlayer;
    private static boolean opening;
    private static double from;
    private static double target;
    private static long started;
    private static long duration;

    private InventoryCameraController() {}

    public static boolean isInventoryActive(Minecraft client) {
        return InventoryPerspectiveConfig.get().enabled && client.screen instanceof InventoryScreen
                && client.player != null && client.level != null && client.gameMode != null
                && !client.gameMode.hasInfiniteItems() && !client.player.isDeadOrDying()
                && !client.player.isSleeping() && client.getCameraEntity() == client.player;
    }

    public static void update(Minecraft client) {
        if (previousPerspective != null && (client.level != previousLevel || client.player != previousPlayer
                || client.player == null || client.player.isDeadOrDying() || client.player.isSleeping()
                || client.getCameraEntity() != client.player || !InventoryPerspectiveConfig.get().enabled
                || client.options.getCameraType() != CameraType.THIRD_PERSON_BACK)) {
            reset(client);
        }
        boolean active = isInventoryActive(client);
        if (active && !opening) {
            if (previousPerspective == null) {
                previousPerspective = client.options.getCameraType();
                previousLevel = client.level;
                previousPlayer = client.player;
                from = target = 0;
            }
            opening = true;
            animateTo(1);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!active && opening) {
            opening = false;
            if (client.screen != null) {
                reset(client);
            } else {
                animateTo(0);
            }
        }
        if (previousPerspective != null && !opening && (client.screen != null || rawProgress() <= 0)) {
            reset(client);
        }
    }

    private static void animateTo(double value) {
        from = rawProgress();
        target = value;
        started = System.nanoTime();
        duration = (long) (InventoryPerspectiveConfig.get().transitionMillis * 1_000_000L * Math.abs(target - from));
    }

    private static double rawProgress() {
        if (duration == 0) return target;
        double fraction = Math.min(1, Math.max(0, (double) (System.nanoTime() - started) / duration));
        return from + (target - from) * fraction;
    }

    public static double progress() {
        double value = rawProgress();
        return value * value * (3 - 2 * value);
    }

    public static boolean controlsCamera() {
        return previousPerspective != null;
    }

    public static CameraType previousPerspective() {
        return previousPerspective;
    }

    private static void reset(Minecraft client) {
        if (previousPerspective != null && client.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            client.options.setCameraType(previousPerspective);
        }
        previousPerspective = null;
        previousLevel = null;
        previousPlayer = null;
        opening = false;
        from = target = 0;
        duration = 0;
    }
}
