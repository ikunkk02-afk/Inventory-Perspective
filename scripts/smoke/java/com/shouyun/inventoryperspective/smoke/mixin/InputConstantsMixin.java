package com.shouyun.inventoryperspective.smoke.mixin;

import com.shouyun.inventoryperspective.smoke.ClientSmoke;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies physical key states to the real InvMove polling path; no movement logic is replaced. */
@Mixin(InputConstants.class)
public class InputConstantsMixin {
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void simulatedKey(long window, int key, CallbackInfoReturnable<Boolean> cir) {
        if (ClientSmoke.simulatingKeys) cir.setReturnValue(ClientSmoke.keys.contains(key));
    }
}
