package com.shouyun.inventoryperspective.client.mixin;

import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void inventoryPerspective$clearWorld(GuiGraphics graphics, CallbackInfo ci) {
        if (InventoryCameraController.isInventoryActive(Minecraft.getInstance())) ci.cancel();
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void inventoryPerspective$noBlur(float delta, CallbackInfo ci) {
        if (InventoryCameraController.isInventoryActive(Minecraft.getInstance())) ci.cancel();
    }
}
