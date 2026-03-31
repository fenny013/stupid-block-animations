package fnn.stupidblockplacement.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class PlacementAnimationRenderer {
    private PlacementAnimationRenderer() {
    }

    public static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) {
            return;
        }

        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector submitter = context.submitNodeCollector();
        float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 cameraPos = context.levelState().cameraRenderState.pos;

        for (PlacementAnimationState animation : PlacementAnimationManager.activeStates()) {
            if (!animation.usesCustomWorldRender()) {
                continue;
            }

            BlockPos pos = animation.pos();
            BlockState state = animation.originalState();

            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - cameraPos.x,
                    pos.getY() - cameraPos.y,
                    pos.getZ() - cameraPos.z
            );

            animation.applyLocal(poseStack, PlacementAnimationManager.getClientTicks(), tickDelta);

            submitter.submitMovingBlock(
                    poseStack,
                    new WorldAwareMovingBlockRenderState(world, pos, state)
            );
            poseStack.popPose();
        }
    }

    private static final class WorldAwareMovingBlockRenderState extends MovingBlockRenderState {
        private final ClientLevel world;

        private WorldAwareMovingBlockRenderState(ClientLevel world, BlockPos pos, BlockState state) {
            this.world = world;
            this.randomSeedPos = pos;
            this.blockPos = pos;
            this.blockState = state;
            this.biome = world.getBiome(pos);
            this.cardinalLighting = world.cardinalLighting();
            this.lightEngine = world.getLightEngine();
        }

        @Override
        public CardinalLighting cardinalLighting() {
            return this.world.cardinalLighting();
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return this.world.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            return this.world.getBlockTint(pos, color);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return this.world.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(this.blockPos) ? this.blockState : this.world.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return this.world.getHeight();
        }

        @Override
        public int getMinY() {
            return this.world.getMinY();
        }
    }
}
