package fnn.stupidblockplacement.mixin.client;

import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.class)
public abstract class BlockModelRendererMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private void stupidBlockPlacement$forceFaceNextToInvisibleBlock(
            BlockAndTintGetter level,
            BlockState state,
            Direction direction,
            BlockPos neighborPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (PlacementAnimationManager.isInvisible(neighborPos)) {
            cir.setReturnValue(true);
        }
    }
}
