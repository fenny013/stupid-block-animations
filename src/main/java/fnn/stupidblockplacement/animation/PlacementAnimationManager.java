package fnn.stupidblockplacement.animation;

import fnn.stupidblockplacement.DebugUtil;
import fnn.stupidblockplacement.StupidBlockPlacementClient;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.*;

public final class PlacementAnimationManager {
    private static final Map<BlockPos, PlacementAnimationState> ACTIVE = new HashMap<>();
    private static final Set<BlockPos> INVISIBLE = new HashSet<>();
    private static final Map<BlockPos, PendingPlacement> PENDING = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static long clientTicks = 0L;
    private static final int PENDING_LIFETIME_TICKS = 20;

    private PlacementAnimationManager() {
    }

    public static void tick() {
        clientTicks++;

        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) {
            ACTIVE.clear();
            INVISIBLE.clear();
            PENDING.clear();
            return;
        }

        for (PendingPlacement pending : new HashSet<>(PENDING.values())) {
            if (pending.expiresAt <= clientTicks) {
                PENDING.remove(pending.targetPos, pending);
            }
        }

        List<BlockPos> toRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, PlacementAnimationState> entry : ACTIVE.entrySet()) {
            BlockPos pos = entry.getKey();
            PlacementAnimationState state = entry.getValue();

            BlockState current = world.getBlockState(pos);
            if (!current.is(state.originalState().getBlock())) {
                toRemove.add(pos);
                continue;
            }

            if (state.kind() == AnimationKind.BREAK) {
                state.updateState(current);

                if (state.isBreakingExpired(clientTicks)) {
                    if (!state.isBreakingReleasing()) {
                        state.beginBreakingRelease(clientTicks);
                    } else if (state.isBreakingReleaseFinished(clientTicks)) {
                        if (state.usesCustomWorldRender()) {
                            if (!state.isInHandoff()) {
                                for (BlockPos renderPos : getStructurePositions(world, pos, state.originalState())) {
                                    if (INVISIBLE.remove(renderPos)) {
                                        rerender(world, renderPos);
                                    }
                                }
                                state.beginHandoff(clientTicks);
                            } else if (state.isHandoffFinished(clientTicks)) {
                                toRemove.add(pos);
                            }
                        } else {
                            toRemove.add(pos);
                        }
                    }
                }

                continue;
            }

            if (state.isAnimationFinished(clientTicks)) {
                if (state.usesCustomWorldRender()) {
                    if (!state.isInHandoff()) {
                        for (BlockPos renderPos : getStructurePositions(world, pos, state.originalState())) {
                            if (INVISIBLE.remove(renderPos)) {
                                rerender(world, renderPos);
                            }
                        }
                        state.beginHandoff(clientTicks);
                    } else if (state.isHandoffFinished(clientTicks)) {
                        toRemove.add(pos);
                    }
                } else {
                    toRemove.add(pos);
                }
            }
        }

        for (BlockPos pos : toRemove) {
            PlacementAnimationState removed = ACTIVE.remove(pos);
            if (removed != null) {
                if (removed.usesCustomWorldRender() && !removed.isReleased()) {
                    for (BlockPos renderPos : getStructurePositions(world, pos, removed.originalState())) {
                        if (INVISIBLE.remove(renderPos)) {
                            rerender(world, renderPos);
                        }
                    }
                }
            }
        }
    }
    public static void onBreakingProgress(ClientLevel world, BlockPos pos, BlockState state, Direction face) {
        if (!StupidBlockPlacementClient.CONFIG.enableBreakingAnimation || state.isAir()) {
            return;
        }

        int horizontalSign = RANDOM.nextBoolean() ? 1 : -1;
        int verticalSign = RANDOM.nextBoolean() ? 1 : -1;

        startOrRefreshBreaking(world, pos.immutable(), state, face, horizontalSign, verticalSign);

        if (state.getBlock() instanceof ChestBlock) {
            BlockPos otherPos = getChestOtherPos(pos, state);
            if (otherPos != null) {
                BlockState otherState = world.getBlockState(otherPos);

                if (isValidChestPair(pos, state, otherPos, otherState)) {
                    startOrRefreshBreaking(
                            world,
                            otherPos.immutable(),
                            otherState,
                            face,
                            horizontalSign,
                            verticalSign
                    );
                }
            }
            return;
        }

        if (StupidBlockPlacementClient.CONFIG.breakingAnimateConnectedBlocks) {
            BlockPos otherPos = getConnectedPos(pos, state);
            if (otherPos != null) {
                BlockState otherState = world.getBlockState(otherPos);
                if (!otherState.isAir() && otherState.is(state.getBlock())) {
                    startOrRefreshBreaking(
                            world,
                            otherPos.immutable(),
                            otherState,
                            face,
                            horizontalSign,
                            verticalSign
                    );
                }
            }
        }
    }

    private static void startOrRefreshBreaking(
            ClientLevel world,
            BlockPos pos,
            BlockState state,
            Direction face,
            int horizontalSign,
            int verticalSign
    ) {
        PlacementAnimationState existing = ACTIVE.get(pos);

        if (existing != null
                && existing.kind() == AnimationKind.BREAK
                && state.is(existing.originalState().getBlock())) {
            existing.updateState(state);
            existing.refresh(clientTicks);
            return;
        }

        ACTIVE.put(
                pos,
                new PlacementAnimationState(
                        pos,
                        state,
                        face,
                        clientTicks,
                        StupidBlockPlacementClient.CONFIG.breakingLoopTicks,
                        horizontalSign,
                        verticalSign,
                        AnimationKind.BREAK
                )
        );

        ACTIVE.get(pos).refresh(clientTicks);

        if (shouldUseCustomWorldRender(state)) {
            for (BlockPos renderPos : getStructurePositions(world, pos, state)) {
                if (INVISIBLE.add(renderPos.immutable())) {
                    rerender(world, renderPos);
                }
            }
        }
    }

    public static void stopBreaking(ClientLevel world, BlockPos pos) {
        PlacementAnimationState state = ACTIVE.get(pos);
        if (state != null && state.kind() == AnimationKind.BREAK) {
            state.beginBreakingRelease(clientTicks);
        }

        BlockState blockState = world.getBlockState(pos);

        if (blockState.getBlock() instanceof ChestBlock) {
            BlockPos otherPos = getChestOtherPos(pos, blockState);
            if (otherPos != null) {
                BlockState otherState = world.getBlockState(otherPos);

                if (isValidChestPair(pos, blockState, otherPos, otherState)) {
                    PlacementAnimationState other = ACTIVE.get(otherPos);
                    if (other != null && other.kind() == AnimationKind.BREAK) {
                        other.beginBreakingRelease(clientTicks);
                    }
                }
            }
            return;
        }

        BlockPos otherPos = getConnectedPos(pos, blockState);
        if (otherPos != null) {
            PlacementAnimationState other = ACTIVE.get(otherPos);
            if (other != null && other.kind() == AnimationKind.BREAK) {
                other.beginBreakingRelease(clientTicks);
            }
        }
    }

    public static void cancelBreakingNow(ClientLevel world, BlockPos pos) {
        removeBreakingStateNow(world, pos);

        BlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof ChestBlock) {
            BlockPos otherPos = getChestOtherPos(pos, blockState);
            if (otherPos != null) {
                removeBreakingStateNow(world, otherPos);
            }
            return;
        }

        BlockPos otherPos = getConnectedPos(pos, blockState);
        if (otherPos != null) {
            removeBreakingStateNow(world, otherPos);
        }
    }

    public static void clear() {
        ACTIVE.clear();
        INVISIBLE.clear();
        PENDING.clear();
    }

    public static long getClientTicks() {
        return clientTicks;
    }

    public static PlacementAnimationState get(BlockPos pos) {
        return ACTIVE.get(pos);
    }

    public static PlacementAnimationState getBreaking(BlockPos pos) {
        PlacementAnimationState state = ACTIVE.get(pos);
        return state != null && state.kind() == AnimationKind.BREAK ? state : null;
    }

    public static boolean isInvisible(BlockPos pos) {
        return INVISIBLE.contains(pos);
    }

    public static Collection<PlacementAnimationState> activeStates() {
        return List.copyOf(ACTIVE.values());
    }

    public static void markPlacementAttempt(BlockPos targetPos, Block block, Direction face) {
        int horizontalSign = RANDOM.nextBoolean() ? 1 : -1;
        int verticalSign = RANDOM.nextBoolean() ? 1 : -1;

        PendingPlacement pending = new PendingPlacement(
                targetPos.immutable(),
                block,
                face,
                horizontalSign,
                verticalSign,
                clientTicks + PENDING_LIFETIME_TICKS
        );

        PENDING.put(pending.targetPos, pending);
    }

    public static void cancelPlacementAttempt(BlockPos targetPos) {
        PENDING.remove(targetPos);
    }

    public static void onClientBlockUpdated(ClientLevel world, BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.immutable();

        DebugUtil.log("block update pos=" + immutablePos + " state=" + state);

        if (state.isAir()) {
            ACTIVE.remove(immutablePos);
            INVISIBLE.remove(immutablePos);
            return;
        }

        if (state.getBlock() instanceof ChestBlock) {
            refreshExisting(world, immutablePos, state);

            BlockPos otherPos = getChestOtherPos(immutablePos, state);
            BlockState otherState = otherPos != null ? world.getBlockState(otherPos) : null;

            if (isValidChestPair(immutablePos, state, otherPos, otherState)) {
                refreshExisting(world, otherPos, otherState);
            }

            PendingPlacement pendingHere = PENDING.get(immutablePos);
            if (pendingHere != null && state.is(pendingHere.block)) {
                startOrRefreshChest(
                        world,
                        immutablePos,
                        state,
                        pendingHere.face,
                        pendingHere.horizontalSign,
                        pendingHere.verticalSign
                );

                PENDING.remove(immutablePos);
                if (otherPos != null) {
                    PENDING.remove(otherPos);
                }
                return;
            }

            if (isValidChestPair(immutablePos, state, otherPos, otherState)) {
                PendingPlacement pendingOther = PENDING.get(otherPos);

                if (pendingOther != null && otherState.is(pendingOther.block)) {
                    startOrRefreshChest(
                            world,
                            immutablePos,
                            state,
                            pendingOther.face,
                            pendingOther.horizontalSign,
                            pendingOther.verticalSign
                    );

                    PENDING.remove(otherPos);
                    PENDING.remove(immutablePos);
                    return;
                }

                PlacementAnimationState otherAnimation = ACTIVE.get(otherPos);
                if (otherAnimation != null && otherAnimation.originalState().is(state.getBlock())) {
                    if (ACTIVE.get(immutablePos) == null) {
                        startSyncedFrom(world, immutablePos, state, otherAnimation);
                    } else {
                        refreshExisting(world, immutablePos, state);
                    }
                }
            }

            return;
        }

        refreshExisting(world, immutablePos, state);

        BlockPos otherPos = getConnectedPos(immutablePos, state);
        if (otherPos != null) {
            BlockState otherState = world.getBlockState(otherPos);
            refreshExisting(world, otherPos, otherState);
        }

        PendingPlacement pending = PENDING.get(immutablePos);

        if (pending != null && state.is(pending.block)) {
            if (ACTIVE.get(immutablePos) == null) {
                start(
                        world,
                        immutablePos,
                        state,
                        pending.face,
                        pending.horizontalSign,
                        pending.verticalSign
                );
            } else {
                refreshExisting(world, immutablePos, state);
            }

            startConnectedIfPresent(
                    world,
                    immutablePos,
                    state,
                    pending.face,
                    pending.horizontalSign,
                    pending.verticalSign
            );

            PENDING.remove(immutablePos);
            return;
        }

        PlacementAnimationState connectedAnimation = findConnectedAnimation(world, immutablePos, state);
        if (connectedAnimation != null) {
            if (ACTIVE.get(immutablePos) == null) {
                startSyncedFrom(world, immutablePos, state, connectedAnimation);
            } else {
                refreshExisting(world, immutablePos, state);
            }
        }
    }
    private static boolean tryStartChestAnimation(
            ClientLevel world,
            BlockPos pos,
            BlockState state,
            PendingPlacement pending
    ) {
        if (!(state.getBlock() instanceof ChestBlock)) {
            return false;
        }

        ChestType type = state.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.is(state.getBlock())) {
                    return false;
                }
            }

            if (ACTIVE.get(pos) == null) {
                start(world, pos, state, pending.face, pending.horizontalSign, pending.verticalSign);
            } else {
                refreshExisting(world, pos, state);
            }
            return true;
        }

        BlockPos otherPos = getChestOtherPos(pos, state);
        if (otherPos == null) {
            return false;
        }

        BlockState otherState = world.getBlockState(otherPos);
        if (!otherState.is(state.getBlock())) {
            return false;
        }

        if (otherState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return false;
        }

        if (otherState.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)) {
            return false;
        }

        if (ACTIVE.get(pos) == null) {
            start(world, pos, state, pending.face, pending.horizontalSign, pending.verticalSign);
        } else {
            refreshExisting(world, pos, state);
        }

        if (ACTIVE.get(otherPos) == null) {
            start(world, otherPos, otherState, pending.face, pending.horizontalSign, pending.verticalSign);
        } else {
            refreshExisting(world, otherPos, otherState);
        }

        return true;
    }
    public static PlacementAnimationState getChestAnimation(ClientLevel world, BlockPos pos) {
        PlacementAnimationState direct = ACTIVE.get(pos);
        if (direct != null) {
            return direct;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !(state.getBlock() instanceof ChestBlock)) {
            return null;
        }

        BlockPos otherPos = getChestOtherPos(pos, state);
        if (otherPos == null) {
            return null;
        }

        return ACTIVE.get(otherPos);
    }

    public static void start(ClientLevel world, BlockPos pos, BlockState state, Direction face) {
        int horizontalSign = RANDOM.nextBoolean() ? 1 : -1;
        int verticalSign = RANDOM.nextBoolean() ? 1 : -1;
        start(world, pos, state, face, horizontalSign, verticalSign);
    }

    private static BlockPos getAnyConnectedPos(ClientLevel world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof ChestBlock) {
            BlockPos otherPos = getChestOtherPos(pos, state);
            if (otherPos == null) {
                return null;
            }

            BlockState otherState = world.getBlockState(otherPos);
            return isValidChestPair(pos, state, otherPos, otherState) ? otherPos : null;
        }

        return getConnectedPos(pos, state);
    }

    private static List<BlockPos> getStructurePositions(ClientLevel world, BlockPos pos, BlockState state) {
        List<BlockPos> result = new ArrayList<>();
        result.add(pos);

        if (state.getBlock() instanceof ChestBlock) {
            BlockPos otherPos = getChestOtherPos(pos, state);
            if (otherPos != null) {
                BlockState otherState = world.getBlockState(otherPos);

                if (isValidChestPair(pos, state, otherPos, otherState)) {
                    result.add(otherPos);
                }
            }

            return result;
        }

        BlockPos other = getConnectedPos(pos, state);
        if (other != null) {
            result.add(other);
        }

        return result;
    }
    public static void start(
            ClientLevel world,
            BlockPos pos,
            BlockState state,
            Direction face,
            int horizontalSign,
            int verticalSign
    ) {
        if (state.isAir()) {
            return;
        }

        DebugUtil.log("START animation pos=" + pos
                + " block=" + state.getBlock()
                + " renderType=" + state.getRenderShape()
                + " state=" + state);

        BlockPos immutablePos = pos.immutable();

        ACTIVE.put(
                immutablePos,
                new PlacementAnimationState(
                        immutablePos,
                        state,
                        face,
                        clientTicks,
                        StupidBlockPlacementClient.CONFIG.durationTicks,
                        horizontalSign,
                        verticalSign,
                        AnimationKind.PLACE
                )
        );

        if (shouldUseCustomWorldRender(state)) {
            for (BlockPos renderPos : getStructurePositions(world, immutablePos, state)) {
                if (INVISIBLE.add(renderPos.immutable())) {
                    rerender(world, renderPos);
                }
            }
        }
    }
    private static void startConnectedIfPresent(
            ClientLevel world,
            BlockPos pos,
            BlockState state,
            Direction face,
            int horizontalSign,
            int verticalSign
    ) {
        if (state.getBlock() instanceof ChestBlock) {
            return;
        }

        BlockPos otherPos = getConnectedPos(pos, state);
        if (otherPos == null) {
            return;
        }

        BlockState otherState = world.getBlockState(otherPos);
        if (otherState.isAir() || !otherState.is(state.getBlock())) {
            return;
        }

        PlacementAnimationState existing = ACTIVE.get(otherPos);
        if (existing != null) {
            existing.updateState(otherState);
            return;
        }

        start(world, otherPos, otherState, face, horizontalSign, verticalSign);
    }

    private static PlacementAnimationState findConnectedAnimation(ClientLevel world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return null;
        }

        if (state.getBlock() instanceof ChestBlock) {
            return null;
        }

        BlockPos otherPos = getConnectedPos(pos, state);
        if (otherPos == null) {
            return null;
        }

        PlacementAnimationState other = ACTIVE.get(otherPos);
        if (other == null) {
            return null;
        }

        if (!other.originalState().is(state.getBlock())) {
            return null;
        }

        return other;
    }


    private static BlockPos getConnectedPos(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            Direction otherHalf = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
            return pos.relative(otherHalf);
        }

        if (state.getBlock() instanceof BedBlock) {
            Direction facing = state.getValue(BedBlock.FACING);
            Direction offset = state.getValue(BedBlock.PART) == BedPart.FOOT ? facing : facing.getOpposite();
            return pos.relative(offset);
        }

        return null;
    }

    public static Direction getConnectedOffsetDirection(BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        }

        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction right = state.getValue(ChestBlock.FACING).getClockWise();
            return state.getValue(ChestBlock.TYPE) == ChestType.LEFT ? right : right.getOpposite();
        }

        if (state.getBlock() instanceof BedBlock) {
            Direction facing = state.getValue(BedBlock.FACING);
            return state.getValue(BedBlock.PART) == BedPart.FOOT ? facing : facing.getOpposite();
        }

        return null;
    }
    private static void rerender(ClientLevel world, BlockPos pos) {
        BlockState current = world.getBlockState(pos);
        world.sendBlockUpdated(pos, current, current, Block.UPDATE_CLIENTS);
        world.setBlocksDirty(pos, current, current);
    }

    private static void refreshExisting(ClientLevel world, BlockPos pos, BlockState state) {
        PlacementAnimationState existing = ACTIVE.get(pos);
        if (existing == null) {
            return;
        }

        if (!state.isAir() && state.is(existing.originalState().getBlock())) {
            existing.updateState(state);
        }
    }

    private static void removeBreakingStateNow(ClientLevel world, BlockPos pos) {
        PlacementAnimationState state = ACTIVE.get(pos);
        if (state == null || state.kind() != AnimationKind.BREAK) {
            return;
        }

        ACTIVE.remove(pos);
        if (state.usesCustomWorldRender()) {
            for (BlockPos renderPos : getStructurePositions(world, pos, state.originalState())) {
                if (INVISIBLE.remove(renderPos)) {
                    rerender(world, renderPos);
                }
            }
        }
    }

    public static void startSyncedFrom(
            ClientLevel world,
            BlockPos pos,
            BlockState state,
            PlacementAnimationState source
    ) {
        if (state.isAir()) {
            return;
        }

        BlockPos immutablePos = pos.immutable();

        ACTIVE.put(
                immutablePos,
                new PlacementAnimationState(
                        immutablePos,
                        state,
                        source.face(),
                        source.startTick(),
                        StupidBlockPlacementClient.CONFIG.durationTicks,
                        source.horizontalSign(),
                        source.verticalSign(),
                        source.kind()
                )
        );
    }


    public static PlacementAnimationState getForRender(ClientLevel world, BlockPos pos) {
        PlacementAnimationState direct = ACTIVE.get(pos);
        if (direct != null) {
            return direct;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        BlockPos otherPos = getConnectedPos(pos, state);
        if (otherPos == null) {
            return null;
        }

        PlacementAnimationState other = ACTIVE.get(otherPos);
        if (other == null) {
            return null;
        }

        if (!other.originalState().is(state.getBlock())) {
            return null;
        }

        startSyncedFrom(world, pos, state, other);
        return ACTIVE.get(pos);
    }


    private static boolean isChest(BlockState state) {
        return state.getBlock() instanceof ChestBlock;
    }

    private static BlockPos getChestOtherPos(BlockPos pos, BlockState state) {
        if (!isChest(state)) {
            return null;
        }

        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE) {
            return null;
        }

        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return null;
        }

        Direction facing = state.getValue(ChestBlock.FACING);
        ChestType selfType = state.getValue(ChestBlock.TYPE);

        BlockPos found = null;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir.getAxis() == facing.getAxis()) {
                continue;
            }

            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = world.getBlockState(neighborPos);

            if (!isChest(neighborState)) {
                continue;
            }

            if (!neighborState.is(state.getBlock())) {
                continue;
            }

            if (neighborState.getValue(ChestBlock.FACING) != facing) {
                continue;
            }

            ChestType neighborType = neighborState.getValue(ChestBlock.TYPE);
            if (neighborType == ChestType.SINGLE) {
                continue;
            }

            if (neighborType == selfType) {
                continue;
            }

            if (found != null) {
                return null;
            }

            found = neighborPos.immutable();
        }

        return found;
    }


    public static Direction getChestPartnerDirection(BlockPos pos, BlockState state) {
        BlockPos otherPos = getChestOtherPos(pos, state);
        if (otherPos == null) {
            return null;
        }

        int dx = otherPos.getX() - pos.getX();
        int dy = otherPos.getY() - pos.getY();
        int dz = otherPos.getZ() - pos.getZ();

        if (dx == 1 && dy == 0 && dz == 0) return Direction.EAST;
        if (dx == -1 && dy == 0 && dz == 0) return Direction.WEST;
        if (dx == 0 && dy == 0 && dz == 1) return Direction.SOUTH;
        if (dx == 0 && dy == 0 && dz == -1) return Direction.NORTH;

        return null;
    }

    private static boolean shouldUseCustomWorldRender(BlockState state) {
        return state.getRenderShape() == RenderShape.MODEL
                && !(state.getBlock() instanceof ChestBlock);
    }

    public static PlacementAnimationState getVisualAnimation(ClientLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        if (state.getBlock() instanceof ChestBlock) {
            return getForChestRender(world, pos);
        }

        return getForRender(world, pos);
    }

    public static PlacementAnimationState getForChestRender(ClientLevel world, BlockPos pos) {
        PlacementAnimationState direct = ACTIVE.get(pos);
        if (direct != null) {
            return direct;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !(state.getBlock() instanceof ChestBlock)) {
            return null;
        }

        BlockPos otherPos = getChestOtherPos(pos, state);
        if (otherPos == null) {
            return null;
        }

        BlockState otherState = world.getBlockState(otherPos);
        if (!isValidChestPair(pos, state, otherPos, otherState)) {
            return null;
        }

        PlacementAnimationState other = ACTIVE.get(otherPos);
        if (other == null) {
            return null;
        }

        if (!other.originalState().is(state.getBlock())) {
            return null;
        }

        return new PlacementAnimationState(
                pos.immutable(),
                state,
                other.face(),
                other.startTick(),
                other.kind() == AnimationKind.BREAK
                        ? StupidBlockPlacementClient.CONFIG.breakingLoopTicks
                        : StupidBlockPlacementClient.CONFIG.durationTicks,
                other.horizontalSign(),
                other.verticalSign(),
                other.kind()
        );
    }

    private static boolean isValidChestPair(
            BlockPos pos,
            BlockState state,
            BlockPos otherPos,
            BlockState otherState
    ) {
        if (otherPos == null || !isChest(state) || !isChest(otherState)) {
            return false;
        }

        ChestType type = state.getValue(ChestBlock.TYPE);
        ChestType otherType = otherState.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE || otherType == ChestType.SINGLE) {
            return false;
        }

        if (!state.is(otherState.getBlock())) {
            return false;
        }

        if (state.getValue(ChestBlock.FACING) != otherState.getValue(ChestBlock.FACING)) {
            return false;
        }

        if (type == otherType) {
            return false;
        }

        BlockPos expectedOther = getChestOtherPos(pos, state);
        BlockPos expectedBack = getChestOtherPos(otherPos, otherState);

        return otherPos.equals(expectedOther) && pos.equals(expectedBack);
    }

    private static void startOrRefreshChest(
            ClientLevel world,
            BlockPos pos,
            BlockState state,
            Direction face,
            int horizontalSign,
            int verticalSign
    ) {
        BlockPos otherPos = getChestOtherPos(pos, state);
        BlockState otherState = otherPos != null ? world.getBlockState(otherPos) : null;

        if (isValidChestPair(pos, state, otherPos, otherState)) {
            startOrRefreshChestHalf(world, pos, state, face, horizontalSign, verticalSign);
            startOrRefreshChestHalf(world, otherPos, otherState, face, horizontalSign, verticalSign);
            return;
        }

        startOrRefreshChestHalf(world, pos, state, face, horizontalSign, verticalSign);
    }

    private static void startOrRefreshChestHalf(
            ClientLevel world,
            BlockPos pos,
            BlockState state,
            Direction face,
            int horizontalSign,
            int verticalSign
    ) {
        PlacementAnimationState existing = ACTIVE.get(pos);
        if (existing == null) {
            start(world, pos, state, face, horizontalSign, verticalSign);
        } else {
            existing.updateState(state);
        }
    }

    private record PendingPlacement(
            BlockPos targetPos,
            Block block,
            Direction face,
            int horizontalSign,
            int verticalSign,
            long expiresAt
    ) {
    }
}
