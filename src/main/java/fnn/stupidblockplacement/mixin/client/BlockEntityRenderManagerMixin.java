package fnn.stupidblockplacement.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import fnn.stupidblockplacement.animation.PlacementAnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderManagerMixin {
    @Unique
    private static final ThreadLocal<Boolean> stupidBlockPlacement$applied =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "submit", at = @At("HEAD"))
    private void stupidBlockPlacement$applyAnimation(
            BlockEntityRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitter,
            CameraRenderState camera,
            CallbackInfo ci
    ) {
        if (renderState == null || renderState.blockPos == null || renderState instanceof ChestRenderState) {
            stupidBlockPlacement$applied.set(false);
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            stupidBlockPlacement$applied.set(false);
            return;
        }

        PlacementAnimationState animation = PlacementAnimationManager.getForRender(client.level, renderState.blockPos);
        if (animation == null) {
            stupidBlockPlacement$applied.set(false);
            return;
        }

        float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        poseStack.pushPose();
        animation.applyLocal(poseStack, PlacementAnimationManager.getClientTicks(), tickDelta);
        stupidBlockPlacement$applied.set(true);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void stupidBlockPlacement$popAnimation(
            BlockEntityRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitter,
            CameraRenderState camera,
            CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(stupidBlockPlacement$applied.get())) {
            poseStack.popPose();
        }
        stupidBlockPlacement$applied.set(false);
    }
}
