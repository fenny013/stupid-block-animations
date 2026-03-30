package fnn.stupidblockplacement.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import fnn.stupidblockplacement.animation.PlacementAnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
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
public abstract class WorldRendererOutlineMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private static final ThreadLocal<Boolean> stupidBlockPlacement$applied =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "renderHitOutline", at = @At("HEAD"))
    private void stupidBlockPlacement$applyOutlineAnimation(
            PoseStack poseStack,
            VertexConsumer builder,
            double camX,
            double camY,
            double camZ,
            BlockOutlineRenderState state,
            int color,
            float width,
            CallbackInfo ci
    ) {
        if (this.minecraft.level == null || state == null) {
            stupidBlockPlacement$applied.set(false);
            return;
        }

        BlockPos pos = state.pos();
        BlockState blockState = this.minecraft.level.getBlockState(pos);
        Vec3 offset = blockState.getOffset(pos);
        PlacementAnimationState animation = PlacementAnimationManager.getVisualAnimation(this.minecraft.level, pos);
        if (animation == null) {
            stupidBlockPlacement$applied.set(false);
            return;
        }

        float tickDelta = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
        poseStack.translate(offset.x, offset.y, offset.z);
        animation.applyLocal(poseStack, PlacementAnimationManager.getClientTicks(), tickDelta);
        poseStack.translate(-offset.x, -offset.y, -offset.z);
        poseStack.translate(-(pos.getX() - camX), -(pos.getY() - camY), -(pos.getZ() - camZ));
        stupidBlockPlacement$applied.set(true);
    }

    @Inject(method = "renderHitOutline", at = @At("RETURN"))
    private void stupidBlockPlacement$popOutlineAnimation(
            PoseStack poseStack,
            VertexConsumer builder,
            double camX,
            double camY,
            double camZ,
            BlockOutlineRenderState state,
            int color,
            float width,
            CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(stupidBlockPlacement$applied.get())) {
            poseStack.popPose();
        }
        stupidBlockPlacement$applied.set(false);
    }
}
