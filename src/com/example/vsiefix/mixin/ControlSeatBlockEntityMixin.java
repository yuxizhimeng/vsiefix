package com.example.vsiefix.mixin;

import com.example.vsiefix.PreviewGuard;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * vsie's control seat resolves its linked peripherals from the block entity read/write path (that is
 * where its "VSIE-LINK-TRACE ... READ_APPLY / WRITE" log lines come from). Create's blueprint
 * preview world keeps rebuilding these block entities, so the whole chain runs again and again in a
 * fake world. We simply skip it there; real levels are untouched.
 */
@Mixin(targets = "com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity", remap = false)
public abstract class ControlSeatBlockEntityMixin {

    private boolean vsiefix$inPreview() {
        return PreviewGuard.isPreview(((BlockEntity) (Object) this).getLevel());
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void vsiefix$skipRead(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (this.vsiefix$inPreview()) {
            ci.cancel();
        }
    }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void vsiefix$skipWrite(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (this.vsiefix$inPreview()) {
            ci.cancel();
        }
    }

    @Inject(method = "addLinkedPeripheral(Lnet/minecraft/world/phys/Vec3;I)V",
            at = @At("HEAD"), cancellable = true)
    private void vsiefix$skipAdd(Vec3 pos, int type, CallbackInfo ci) {
        if (this.vsiefix$inPreview()) {
            ci.cancel();
        }
    }

    @Inject(method = "removeLinkedPeripheral(Lnet/minecraft/world/phys/Vec3;I)V",
            at = @At("HEAD"), cancellable = true)
    private void vsiefix$skipRemove(Vec3 pos, int type, CallbackInfo ci) {
        if (this.vsiefix$inPreview()) {
            ci.cancel();
        }
    }

    @Inject(method = "removeLinkedPeripheralImmediately(Lnet/minecraft/world/phys/Vec3;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void vsiefix$skipRemoveImmediately(Vec3 pos, int type, CallbackInfoReturnable<Boolean> cir) {
        if (this.vsiefix$inPreview()) {
            cir.setReturnValue(false);
        }
    }
}
