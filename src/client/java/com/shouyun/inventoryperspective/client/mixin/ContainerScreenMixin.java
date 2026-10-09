package com.shouyun.inventoryperspective.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.screen.WorldInventoryRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @WrapMethod(method = "mouseDragged")
    private boolean inventoryPerspective$drag(double x, double y, int button, double dx, double dy,
                                              Operation<Boolean> original) {
        if (!((Object) this instanceof InventoryScreen)
                || !InventoryCameraController.isInventoryActive(Minecraft.getInstance())) {
            return original.call(x, y, button, dx, dy);
        }
        var point = WorldInventoryRenderer.pick(x, y);
        var previous = WorldInventoryRenderer.pick(x - dx, y - dy);
        return point != null && original.call(point.x(), point.y(), button,
                previous == null ? 0.0 : point.x() - previous.x(),
                previous == null ? 0.0 : point.y() - previous.y());
    }
}
