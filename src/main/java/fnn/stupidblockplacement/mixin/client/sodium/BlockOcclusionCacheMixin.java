package fnn.stupidblockplacement.mixin.client.sodium;

import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Restriction(require = @Condition("sodium"))
@Mixin(AbstractBlockRenderContext.class)
public abstract class BlockOcclusionCacheMixin {
    @Shadow
    protected BlockPos pos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void stupidBlockPlacement$forceFaceNearInvisibleBlock(
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (this.pos != null && PlacementAnimationManager.isInvisible(this.pos.relative(direction))) {
            cir.setReturnValue(true);
        }
    }
}
