package com.example.vsiefix.mixin;

import com.example.vsiefix.PreviewGuard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.1.0: the remaining cost is rendering. vsie's blocks are GeckoLib animated blocks, and Create's
 * schematic preview renders them every frame. Skip those block entities inside preview/virtual
 * levels only (they simply become invisible in the preview, exactly like removing them from the
 * blueprint by hand - real worlds are untouched).
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Unique
    private static int vsiefix$skipped;

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"), cancellable = true)
    private void vsiefix$skipPreviewRender(BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                           MultiBufferSource bufferSource, CallbackInfo ci) {
        if (blockEntity == null) {
            return;
        }
        String name = blockEntity.getClass().getName();
        if (!name.startsWith("com.kodu16.vsie")) {
            return;
        }
        if (!PreviewGuard.isPreview(blockEntity.getLevel())) {
            return;
        }
        vsiefix$skipped++;
        if (vsiefix$skipped % 2000 == 0) {
            System.out.println("[vsiefix] skipped " + vsiefix$skipped
                    + " VSIE block-entity renders inside preview levels (last=" + name + ")");
        }
        ci.cancel();
    }
}
