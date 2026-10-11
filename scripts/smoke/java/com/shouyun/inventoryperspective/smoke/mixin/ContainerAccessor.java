package com.shouyun.inventoryperspective.smoke.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerAccessor {
    @Accessor("leftPos") int smokeLeft();
    @Accessor("topPos") int smokeTop();
    @Accessor("hoveredSlot") Slot smokeHovered();
}
