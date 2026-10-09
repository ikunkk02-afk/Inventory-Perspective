package com.shouyun.inventoryperspective.client.mixin;

import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.screen.WorldInventoryRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"), cancellable = true)
    private void inventoryPerspective$worldOnly(GuiGraphics graphics, int x, int y, float delta, CallbackInfo ci) {
        if (InventoryCameraController.isInventoryActive(Minecraft.getInstance())
                && !WorldInventoryRenderer.renderingTexture()) ci.cancel();
    }

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void inventoryPerspective$clearWorld(GuiGraphics graphics, CallbackInfo ci) {
        if (WorldInventoryRenderer.renderingTexture() || InventoryCameraController.isInventoryActive(Minecraft.getInstance())) ci.cancel();
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void inventoryPerspective$noBlur(float delta, CallbackInfo ci) {
        if (WorldInventoryRenderer.renderingTexture() || InventoryCameraController.isInventoryActive(Minecraft.getInstance())) ci.cancel();
    }
}
