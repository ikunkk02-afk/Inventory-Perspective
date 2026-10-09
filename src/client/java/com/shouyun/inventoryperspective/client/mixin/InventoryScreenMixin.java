package com.shouyun.inventoryperspective.client.mixin;

import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    @Shadow @Final private RecipeBookComponent recipeBookComponent;
    @Unique private ImageButton inventoryPerspective$recipeButton;

    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void inventoryPerspective$init(CallbackInfo ci) {
        inventoryPerspective$recipeButton = children().stream().filter(ImageButton.class::isInstance)
                .map(ImageButton.class::cast).findFirst().orElse(null);
        inventoryPerspective$layout();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void inventoryPerspective$render(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        inventoryPerspective$layout();
    }

    @Unique
    private void inventoryPerspective$layout() {
        if (minecraft == null || minecraft.screen != (Object) this) return;
        int original = recipeBookComponent.updateScreenPosition(width, imageWidth);
        leftPos = original;
        if (InventoryCameraController.isInventoryActive(minecraft) && !recipeBookComponent.isVisible() && width >= imageWidth + 76) {
            boolean right = InventoryPerspectiveConfig.get().inventoryOnRight;
            int target = Mth.clamp(Math.round(width * (right ? 0.72F : 0.28F) - imageWidth / 2F), 8, width - imageWidth - 38);
            int slide = (int) Math.round(24 * (1 - InventoryCameraController.progress()));
            leftPos = Mth.clamp(target + (right ? -slide : slide), 8, width - imageWidth - 38);
        }
        if (inventoryPerspective$recipeButton != null) {
            inventoryPerspective$recipeButton.setX(leftPos + 104);
        }
    }
}
