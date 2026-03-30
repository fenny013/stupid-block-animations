package fnn.stupidblockplacement.mixin.client;

import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void stupidBlockPlacement$hideInvisibleBlockDuringChunkBuild(
            BlockPos pos,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (PlacementAnimationManager.isInvisible(pos)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
