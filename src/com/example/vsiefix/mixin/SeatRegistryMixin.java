package com.example.vsiefix.mixin;

import com.example.vsiefix.PreviewGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * vsie also registers control seats in a global registry (with Sable sub-level lookups). Doing that
 * for preview world block entities is pointless work, so skip it there as well.
 */
@Mixin(targets = "com.kodu16.vsie.content.controlseat.server.SeatRegistry", remap = false)
public abstract class SeatRegistryMixin {

    @Inject(method = "registerOrUpdateControlSeat(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ldev/ryanhcode/sable/sublevel/SubLevel;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void vsiefix$skipPreview(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (PreviewGuard.isPreview(level)) {
            cir.setReturnValue(false);
        }
    }
}
