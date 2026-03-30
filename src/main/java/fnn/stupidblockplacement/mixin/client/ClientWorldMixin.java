package fnn.stupidblockplacement.mixin.client;

import fnn.stupidblockplacement.DebugUtil;
import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void stupidBlockPlacement$startAnimationOnActualStateChange(
            BlockPos pos,
            BlockState state,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            DebugUtil.log("ClientLevel setBlock " + pos + " -> " + state.getBlock());
            PlacementAnimationManager.onClientBlockUpdated((ClientLevel) (Object) this, pos, state);
        }
    }
}
