package fnn.stupidblockplacement.mixin.client;

import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class BlockRenderManagerMixin {
    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void stupidBlockPlacement$skipInvisibleAnimatedBlock(
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState blockState,
            BlockStateModel model,
            long seed,
            CallbackInfo ci
    ) {
        if (!(level instanceof MovingBlockRenderState) && PlacementAnimationManager.isInvisible(pos)) {
            ci.cancel();
        }
    }
}
