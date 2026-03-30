package fnn.stupidblockplacement.animation;

import fnn.stupidblockplacement.StupidBlockPlacementClient;
import fnn.stupidblockplacement.config.StupidBlockPlacementConfig;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class PlacementAnimationState {
    private static final int END_HOLD_TICKS = 1;
    private final BlockPos pos;
    private BlockState originalState;
    private List<BlockStateModelPart> parts;
    private final Direction face;
    private final long startTick;
    private final int durationTicks;
    private final int horizontalSign;
    private final int verticalSign;
    private Long releaseTick;
    private final AnimationKind kind;
    private long lastRefreshTick;
    private Long breakingReleaseStartTick;
    private int breakingReleaseDurationTicks;
    private float releaseStartHorizontalAngle;
    private float releaseStartVerticalAngle;

    private Long handoffTick;
    public PlacementAnimationState(
            BlockPos pos,
            BlockState originalState,
            Direction face,
            long startTick,
            int durationTicks,
            int horizontalSign,
            int verticalSign,
            AnimationKind kind
    ) {
        this.pos = pos.immutable();
        this.originalState = originalState;
        this.face = face;
        this.startTick = startTick;
        this.durationTicks = Math.max(1, durationTicks);
        this.horizontalSign = horizontalSign;
        this.verticalSign = verticalSign;
        this.kind = kind;
        this.lastRefreshTick = startTick;
        this.parts = collectParts(this.pos, this.originalState);
    }
    private float[] getBreakingAngles(long currentTick, float tickDelta) {
        StupidBlockPlacementConfig config = StupidBlockPlacementClient.CONFIG;

        float age = (currentTick - this.startTick) + tickDelta;
        float loopTicks = Math.max(1, config.breakingLoopTicks);
        float loopT = (age % loopTicks) / loopTicks;

        float horizontalOscillation =
                (float) Math.sin(config.breakingHorizontalCycles * (float) (Math.PI * 2.0) * loopT);
        float verticalOscillation =
                (float) Math.sin(config.breakingVerticalCycles * (float) (Math.PI * 2.0) * loopT);

        float amplitude = config.breakingEasing.apply(loopT);

        float horizontalAngle = config.breakingHorizontalMaxAngle * horizontalOscillation * amplitude;
        float verticalAngle = config.breakingVerticalMaxAngle * verticalOscillation * amplitude;

        if (config.breakingRandomizeDirection) {
            horizontalAngle *= this.horizontalSign;
            verticalAngle *= this.verticalSign;
        }

        return new float[]{horizontalAngle, verticalAngle};
    }
    private static List<BlockStateModelPart> collectParts(BlockPos pos, BlockState state) {
        Minecraft client = Minecraft.getInstance();
        BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(state);
        RandomSource random = RandomSource.create(state.getSeed(pos));
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);
        return parts;
    }
    public void beginHandoff(long currentTick) {
        if (this.handoffTick == null) {
            this.handoffTick = currentTick + 2;
        }
    }

    public boolean isInHandoff() {
        return this.handoffTick != null;
    }

    public boolean isHandoffFinished(long currentTick) {
        return this.handoffTick != null && currentTick >= this.handoffTick;
    }
    public void updateState(BlockState newState) {
        if (newState == null || newState.isAir()) {
            return;
        }
        if (!newState.is(this.originalState.getBlock())) {
            return;
        }

        this.originalState = newState;
        this.parts = collectParts(this.pos, this.originalState);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public BlockState originalState() {
        return this.originalState;
    }

    public List<BlockStateModelPart> parts() {
        return this.parts;
    }

    public boolean usesCustomWorldRender() {
        return this.originalState.getRenderShape() == RenderShape.MODEL
                && !(this.originalState.getBlock() instanceof ChestBlock);
    }

    public void applyLocal(PoseStack matrices, long currentTick, float tickDelta) {
        switch (this.kind) {
            case PLACE -> applyPlacement(matrices, currentTick, tickDelta);
            case BREAK -> applyBreaking(matrices, currentTick, tickDelta);
        }
    }

    private void applyPlacement(PoseStack matrices, long currentTick, float tickDelta) {
        StupidBlockPlacementConfig config = StupidBlockPlacementClient.CONFIG;
        if (!config.enablePlacementAnimation) {
            return;
        }

        float t = ((currentTick - this.startTick) + tickDelta) / (float) this.durationTicks;
        if (t <= 0.0F) {
            return;
        }
        t = Math.min(t, 1.0F);

        float horizontalOscillation = (float) Math.sin(config.horizontalCycles * Math.PI * t);
        float verticalOscillation = (float) Math.sin(config.verticalCycles * Math.PI * t);
        float decay = config.easing.apply(t);

        float horizontalAngle = config.horizontalMaxAngle * horizontalOscillation * decay;
        float verticalAngle = config.verticalMaxAngle * verticalOscillation * decay;

        if (config.randomizeDirection) {
            horizontalAngle *= this.horizontalSign;
            verticalAngle *= this.verticalSign;
        }

        applyRotation(matrices, horizontalAngle, verticalAngle);
    }

    private void applyBreaking(PoseStack matrices, long currentTick, float tickDelta) {
        StupidBlockPlacementConfig config = StupidBlockPlacementClient.CONFIG;
        if (!config.enableBreakingAnimation) {
            return;
        }

        float horizontalAngle;
        float verticalAngle;

        if (this.breakingReleaseStartTick != null) {
            float t = ((currentTick - this.breakingReleaseStartTick) + tickDelta)
                    / (float) Math.max(1, this.breakingReleaseDurationTicks);
            t = Math.min(Math.max(t, 0.0F), 1.0F);

            float k = 1.0F - t;

            horizontalAngle = this.releaseStartHorizontalAngle * k;
            verticalAngle = this.releaseStartVerticalAngle * k;
        } else {
            float[] angles = getBreakingAngles(currentTick, tickDelta);
            horizontalAngle = angles[0];
            verticalAngle = angles[1];
        }

        applyRotation(matrices, horizontalAngle, verticalAngle);
    }
    public void beginBreakingRelease(long currentTick) {
        if (this.kind != AnimationKind.BREAK || this.breakingReleaseStartTick != null) {
            return;
        }

        float[] angles = getBreakingAngles(currentTick, 0.0F);
        this.releaseStartHorizontalAngle = angles[0];
        this.releaseStartVerticalAngle = angles[1];
        this.breakingReleaseStartTick = currentTick;
        this.breakingReleaseDurationTicks = Math.max(1, StupidBlockPlacementClient.CONFIG.breakingReturnTicks);
    }

    public boolean isBreakingReleasing() {
        return this.breakingReleaseStartTick != null;
    }

    public boolean isBreakingReleaseFinished(long currentTick) {
        if (this.breakingReleaseStartTick == null) {
            return false;
        }
        return currentTick - this.breakingReleaseStartTick >= this.breakingReleaseDurationTicks + END_HOLD_TICKS;
    }

    private void applyRotation(PoseStack matrices, float horizontalAngle, float verticalAngle) {
        AxisPair axes = AxisPair.fromFace(this.face);
        Vec3 pivot = this.getPivot();

        matrices.translate(pivot.x, pivot.y, pivot.z);
        matrices.mulPose(axes.horizontalAxis.rotationDegrees(verticalAngle));
        matrices.mulPose(axes.verticalAxis.rotationDegrees(horizontalAngle));
        matrices.translate(-pivot.x, -pivot.y, -pivot.z);
    }

    private Vec3 getPivot() {
        Vec3 pivot = new Vec3(0.5D, 0.5D, 0.5D);
        BlockState state = this.originalState;

        if (state.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
            Direction move = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
            return pivot.add(move.getStepX() * 0.5D, move.getStepY() * 0.5D, move.getStepZ() * 0.5D);
        }

        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction move = PlacementAnimationManager.getChestPartnerDirection(this.pos, state);
            if (move != null) {
                return pivot.add(
                        move.getStepX() * 0.5D,
                        0.0D,
                        move.getStepZ() * 0.5D
                );
            }
        }

        if (state.getBlock() instanceof BedBlock) {
            Direction dir = state.getValue(BedBlock.FACING);
            if (state.getValue(BedBlock.PART) == BedPart.FOOT) {
                return pivot.add(dir.getStepX() * 0.5D, 0.0D, dir.getStepZ() * 0.5D);
            }
            return pivot.add(-dir.getStepX() * 0.5D, 0.0D, -dir.getStepZ() * 0.5D);
        }

        return pivot;
    }


    private record AxisPair(Axis horizontalAxis, Axis verticalAxis) {
        private static AxisPair fromFace(Direction face) {
            return switch (face) {
                case SOUTH -> new AxisPair(Axis.ZP, Axis.YP);
                case NORTH -> new AxisPair(Axis.ZN, Axis.YP);
                case EAST -> new AxisPair(Axis.XP, Axis.YP);
                case WEST -> new AxisPair(Axis.XN, Axis.YP);
                case DOWN -> new AxisPair(Axis.XP, Axis.ZP);
                case UP -> new AxisPair(Axis.XN, Axis.ZP);
            };
        }
    }

    public boolean isAnimationFinished(long currentTick) {
        if (this.kind == AnimationKind.BREAK) {
            return false;
        }
        return currentTick - this.startTick >= this.durationTicks + END_HOLD_TICKS;
    }

    public boolean isFullyExpired(long currentTick) {
        return this.releaseTick != null && currentTick >= this.releaseTick;
    }

    public boolean isReleased() {
        return this.releaseTick != null;
    }

    public void beginRelease(long currentTick) {
        if (this.releaseTick == null) {
            this.releaseTick = currentTick + 2;
        }
    }

    public Direction face() {
        return this.face;
    }

    public int horizontalSign() {
        return this.horizontalSign;
    }

    public int verticalSign() {
        return this.verticalSign;
    }

    public long startTick() {
        return this.startTick;
    }
    public AnimationKind kind() {
        return this.kind;
    }

    public void refresh(long currentTick) {
        this.lastRefreshTick = currentTick;
        this.breakingReleaseStartTick = null;
        this.handoffTick = null;
    }

    public boolean isBreakingExpired(long currentTick) {
        if (this.kind != AnimationKind.BREAK) {
            return false;
        }
        return currentTick - this.lastRefreshTick > StupidBlockPlacementClient.CONFIG.breakingKeepAliveTicks;
    }
}
