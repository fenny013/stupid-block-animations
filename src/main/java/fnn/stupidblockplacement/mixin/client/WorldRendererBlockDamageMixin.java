package fnn.stupidblockplacement.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import fnn.stupidblockplacement.animation.PlacementAnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererBlockDamageMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private static final ThreadLocal<Boolean> stupidBlockPlacement$applied =
            ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "submitBlockDestroyAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitBreakingBlockModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;JI)V"
            )
    )
    private void stupidBlockPlacement$applyDamageAnimation(
            PoseStack poseStack,
            SubmitNodeCollector submitter,
            LevelRenderState levelRenderState,
            CallbackInfo ci,
            @Local BlockBreakingRenderState state
    ) {
        if (this.minecraft.level == null || state == null) {
            stupidBlockPlacement$applied.set(false);
            return;
        }

        BlockPos pos = state.blockPos();
        PlacementAnimationState animation = PlacementAnimationManager.getBreaking(pos);
        if (animation == null) {
            stupidBlockPlacement$applied.set(false);
            return;
        }

        BlockState blockState = state.blockState();
        Vec3 offset = blockState.getOffset(pos);
        float tickDelta = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        animation.applyLocal(poseStack, PlacementAnimationManager.getClientTicks(), tickDelta);
        poseStack.translate(-offset.x, -offset.y, -offset.z);
        stupidBlockPlacement$applied.set(true);
    }

    @Inject(
            method = "submitBlockDestroyAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitBreakingBlockModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;JI)V",
                    shift = At.Shift.AFTER
            )
    )
    private void stupidBlockPlacement$popDamageAnimation(
            PoseStack poseStack,
            SubmitNodeCollector submitter,
            LevelRenderState levelRenderState,
            CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(stupidBlockPlacement$applied.get())) {
            poseStack.popPose();
        }
        stupidBlockPlacement$applied.set(false);
    }
}
