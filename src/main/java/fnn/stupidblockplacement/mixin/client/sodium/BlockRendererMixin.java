package fnn.stupidblockplacement.mixin.client.sodium;

import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Restriction(require = @Condition("sodium"))
@Mixin(BlockRenderer.class)
public class BlockRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void stupidBlockPlacement$hideInvisibleBlock(
            BlockStateModel model,
            BlockState state,
            BlockPos pos,
            BlockPos origin,
            CallbackInfo ci
    ) {
        if (PlacementAnimationManager.isInvisible(pos)) {
            ci.cancel();
        }
    }
}
