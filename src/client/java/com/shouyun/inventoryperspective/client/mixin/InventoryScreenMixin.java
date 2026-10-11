package com.shouyun.inventoryperspective.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.screen.WorldInventoryRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapMethod(method = "mouseClicked")
    private boolean inventoryPerspective$click(double x, double y, int button, Operation<Boolean> original) {
        if (!InventoryCameraController.isInventoryActive(minecraft)) return original.call(x, y, button);
        var point = WorldInventoryRenderer.pick(x, y);
        return point != null && original.call(point.x(), point.y(), button);
    }

    @WrapMethod(method = "mouseReleased")
    private boolean inventoryPerspective$release(double x, double y, int button, Operation<Boolean> original) {
        if (!InventoryCameraController.isInventoryActive(minecraft)) return original.call(x, y, button);
        var point = WorldInventoryRenderer.pick(x, y);
        // Finish a drag over blank chrome if blocked, without dropping the held stack into the world.
        return point == null ? original.call((double) leftPos + imageWidth - 4, (double) topPos + 4, button)
                : original.call(point.x(), point.y(), button);
    }
}
