package com.shouyun.inventoryperspective.client.mixin;

import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void inventoryPerspective$screenChanged(Screen screen, CallbackInfo ci) {
        InventoryCameraController.update((Minecraft) (Object) this);
    }
}
