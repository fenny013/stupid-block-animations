package fnn.stupidblockplacement.mixin.client;

import fnn.stupidblockplacement.DebugUtil;
import fnn.stupidblockplacement.StupidBlockPlacementClient;
import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean stupidBlockPlacement$tracking;
    @Unique
    private BlockPos stupidBlockPlacement$targetPos;
    @Unique
    private Block stupidBlockPlacement$expectedBlock;
    @Unique
    private BlockPos stupidBlockPlacement$breakingPos;
    @Unique
    private Direction stupidBlockPlacement$breakingFace;
    @Unique
    private long stupidBlockPlacement$breakingStartedAt = -1L;

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void stupidBlockPlacement$captureBeforePlacement(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ClientLevel world = this.minecraft.level;
        if (world == null) {
            this.stupidBlockPlacement$tracking = false;
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            this.stupidBlockPlacement$tracking = false;
            return;
        }

        BlockPos clickedPos = hitResult.getBlockPos().immutable();
        BlockState clickedState = world.getBlockState(clickedPos);
        BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hitResult);

        BlockPos targetPos = clickedState.canBeReplaced(context)
                ? clickedPos
                : clickedPos.relative(hitResult.getDirection()).immutable();

        this.stupidBlockPlacement$tracking = true;
        this.stupidBlockPlacement$targetPos = targetPos;
        this.stupidBlockPlacement$expectedBlock = blockItem.getBlock();

        PlacementAnimationManager.markPlacementAttempt(
                this.stupidBlockPlacement$targetPos,
                this.stupidBlockPlacement$expectedBlock,
                hitResult.getDirection()
        );

        DebugUtil.log("mark placement target=" + this.stupidBlockPlacement$targetPos + " expected=" + this.stupidBlockPlacement$expectedBlock);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void stupidBlockPlacement$finishPlacement(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!this.stupidBlockPlacement$tracking) {
            return;
        }

        this.stupidBlockPlacement$tracking = false;
        InteractionResult result = cir.getReturnValue();
        DebugUtil.log("useItemOn result=" + result);

        if (result == null || !result.consumesAction()) {
            PlacementAnimationManager.cancelPlacementAttempt(this.stupidBlockPlacement$targetPos);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("RETURN"))
    private void stupidBlockPlacement$beginBreakingTracking(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        ClientLevel world = this.minecraft.level;
        if (world == null) {
            return;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        this.stupidBlockPlacement$breakingPos = pos.immutable();
        this.stupidBlockPlacement$breakingFace = direction;
        this.stupidBlockPlacement$breakingStartedAt = PlacementAnimationManager.getClientTicks();
    }

    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void stupidBlockPlacement$updateBreakingAnimation(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ClientLevel world = this.minecraft.level;
        if (world == null) {
            stupidBlockPlacement$resetBreakingTracking();
            return;
        }

        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            if (this.stupidBlockPlacement$breakingPos != null) {
                PlacementAnimationManager.cancelBreakingNow(world, this.stupidBlockPlacement$breakingPos);
            }
            stupidBlockPlacement$resetBreakingTracking();
            return;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            if (this.stupidBlockPlacement$breakingPos != null) {
                PlacementAnimationManager.cancelBreakingNow(world, this.stupidBlockPlacement$breakingPos);
            }
            stupidBlockPlacement$resetBreakingTracking();
            return;
        }

        BlockPos immutablePos = pos.immutable();
        if (!immutablePos.equals(this.stupidBlockPlacement$breakingPos)) {
            if (this.stupidBlockPlacement$breakingPos != null) {
                PlacementAnimationManager.cancelBreakingNow(world, this.stupidBlockPlacement$breakingPos);
            }

            this.stupidBlockPlacement$breakingPos = immutablePos;
            this.stupidBlockPlacement$breakingFace = direction;
            this.stupidBlockPlacement$breakingStartedAt = PlacementAnimationManager.getClientTicks();
            return;
        }

        long age = PlacementAnimationManager.getClientTicks() - this.stupidBlockPlacement$breakingStartedAt;
        if (age >= StupidBlockPlacementClient.CONFIG.breakingStartDelayTicks) {
            PlacementAnimationManager.onBreakingProgress(
                    world,
                    this.stupidBlockPlacement$breakingPos,
                    state,
                    this.stupidBlockPlacement$breakingFace
            );
        }
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void stupidBlockPlacement$cancelBreakingAnimation(CallbackInfo ci) {
        ClientLevel world = this.minecraft.level;
        if (world != null && this.stupidBlockPlacement$breakingPos != null) {
            PlacementAnimationManager.stopBreaking(world, this.stupidBlockPlacement$breakingPos);
        }
        stupidBlockPlacement$resetBreakingTracking();
    }

    @Unique
    private void stupidBlockPlacement$resetBreakingTracking() {
        this.stupidBlockPlacement$breakingPos = null;
        this.stupidBlockPlacement$breakingFace = null;
        this.stupidBlockPlacement$breakingStartedAt = -1L;
    }
}
