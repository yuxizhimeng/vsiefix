package com.example.vsiefix.mixin;

import com.example.vsiefix.PreviewGuard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.2.0: the spark profile showed that Create renders a blueprint preview's block entities via
 * Create's own BlockEntityRenderHelper -> AlwaysRenderGeoBlockRenderer.render (this class is the
 * shared base of every vsie block renderer), which never passes through the vanilla
 * BlockEntityRenderDispatcher hook added in v1.1.0. Skip those renders inside preview/virtual
 * levels only - real worlds are untouched.
 */
@Mixin(targets = "com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer", remap = false)
public abstract class AlwaysRenderGeoBlockRendererMixin {

    @Unique
    private static int vsiefix$skippedRenders;

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true)
    private void vsiefix$skipPreviewRender(BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                           CallbackInfo ci) {
        if (blockEntity == null) {
            return;
        }
        if (!PreviewGuard.isPreview(blockEntity.getLevel())) {
            return;
        }
        vsiefix$skippedRenders++;
        if (vsiefix$skippedRenders <= 3 || vsiefix$skippedRenders % 2000 == 0) {
            System.out.println("[vsiefix] skipped VSIE preview render #" + vsiefix$skippedRenders
                    + " (" + blockEntity.getClass().getSimpleName() + ")");
        }
        ci.cancel();
    }
}
