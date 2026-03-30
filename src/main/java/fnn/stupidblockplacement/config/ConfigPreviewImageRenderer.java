package fnn.stupidblockplacement.config;

import com.mojang.math.Transformation;
import dev.isxander.yacl3.gui.image.ImageRenderer;
import fnn.stupidblockplacement.animation.AnimationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Supplier;

public final class ConfigPreviewImageRenderer implements ImageRenderer {
    private static final int HEIGHT = 96;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 6;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float BASE_YAW_RAD = -20.0F * DEG_TO_RAD;
    private static final float BASE_PITCH_RAD = -8.0F * DEG_TO_RAD;
    private static final float PLACE_PAUSE_TICKS = 8.0F;
    private static final float BREAK_ACTIVE_LOOPS = 2.5F;
    private static final float BREAK_PAUSE_TICKS = 8.0F;
    private static final BlockState PLACE_PREVIEW_BLOCK = Blocks.TUFF_BRICKS.defaultBlockState();
    private static final BlockState BREAK_PREVIEW_BLOCK = Blocks.CALCITE.defaultBlockState();
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final AnimationKind kind;
    private final Supplier<StupidBlockPlacementConfig> configSupplier;
    private final long startedAtNanos = System.nanoTime();
    private final int horizontalSign;
    private final int verticalSign;

    public ConfigPreviewImageRenderer(AnimationKind kind, Supplier<StupidBlockPlacementConfig> configSupplier) {
        this.kind = kind;
        this.configSupplier = configSupplier;
        this.horizontalSign = kind == AnimationKind.PLACE ? 1 : -1;
        this.verticalSign = -1;
    }

    @Override
    public int render(GuiGraphicsExtractor context, int x, int y, int renderWidth, float tickDelta) {
        int previewLeft = x + PADDING_X;
        int previewTop = y + PADDING_Y;
        int previewRight = x + renderWidth - PADDING_X;
        int previewBottom = y + HEIGHT - PADDING_Y;

        StupidBlockPlacementConfig config = getPreviewConfig();
        float previewTicks = (System.nanoTime() - this.startedAtNanos) / 50_000_000.0F;
        float[] angles = this.kind == AnimationKind.BREAK
                ? computeBreakingPreviewAngles(config, previewTicks)
                : computePlacementPreviewAngles(config, previewTicks);

        context.fillGradient(previewLeft, previewTop, previewRight, previewBottom, 0x22303C4D, 0x22223344);
        context.outline(previewLeft, previewTop, previewRight - previewLeft, previewBottom - previewTop, 0x6688A3C2);

        Transformation blockTransformation = buildCenteredBlockTransformation(angles[0], angles[1], getPreviewFace());
        BlockDisplayEntityRenderState renderState = createRenderState(getPreviewBlockState(), previewTicks, blockTransformation);
        if (!renderState.hasSubState()) {
            return HEIGHT;
        }

        int viewportWidth = previewRight - previewLeft;
        int viewportHeight = previewBottom - previewTop;
        float scale = Math.min(viewportWidth, viewportHeight) * 0.58F;
        Vector3f translation = new Vector3f(0.25F, 0.5F, 0.0F);
        Quaternionf entityRotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY(BASE_YAW_RAD)
                .rotateX(-BASE_PITCH_RAD);

        context.enableScissor(previewLeft, previewTop, previewRight, previewBottom);
        try {
            context.entity(
                    renderState,
                    scale,
                    translation,
                    entityRotation,
                    null,
                    previewLeft,
                    previewTop,
                    previewRight,
                    previewBottom
            );
        } finally {
            context.disableScissor();
        }

        return HEIGHT;
    }

    @Override
    public void close() {
    }

    private BlockDisplayEntityRenderState createRenderState(BlockState blockState, float previewTicks, Transformation transformation) {
        Minecraft client = Minecraft.getInstance();
        BlockDisplayEntityRenderState renderState = new BlockDisplayEntityRenderState();

        renderState.entityType = EntityType.BLOCK_DISPLAY;
        renderState.ageInTicks = previewTicks;
        renderState.x = 0.0D;
        renderState.y = 0.0D;
        renderState.z = 0.0D;
        renderState.boundingBoxWidth = 1.0F;
        renderState.boundingBoxHeight = 1.0F;
        renderState.eyeHeight = 0.5F;
        renderState.lightCoords = 15728880;
        renderState.outlineColor = EntityRenderState.NO_OUTLINE;
        renderState.shadowRadius = 0.0F;
        renderState.passengerOffset = Vec3.ZERO;
        renderState.nameTagAttachment = Vec3.ZERO;
        renderState.interpolationProgress = 1.0F;
        renderState.entityYRot = 0.0F;
        renderState.entityXRot = 0.0F;
        renderState.cameraYRot = 0.0F;
        renderState.cameraXRot = 0.0F;
        renderState.renderState = new Display.RenderState(
                Display.GenericInterpolator.constant(transformation),
                Display.BillboardConstraints.FIXED,
                15728880,
                Display.FloatInterpolator.constant(0.0F),
                Display.FloatInterpolator.constant(0.0F),
                -1
        );

        BlockModelResolver resolver = new BlockModelResolver(client.getModelManager());
        resolver.update(renderState.blockModel, blockState, BLOCK_DISPLAY_CONTEXT);

        return renderState;
    }

    private static Transformation buildCenteredBlockTransformation(float horizontalAngleDeg, float verticalAngleDeg, Direction face) {
        Quaternionf animRotation = buildAnimationRotation(horizontalAngleDeg, verticalAngleDeg, face);
        Matrix4f matrix = new Matrix4f()
                .translation(0.5F, 0.5F, 0.5F)
                .rotate(animRotation)
                .translate(-0.5F, -0.5F, -0.5F);
        return new Transformation(matrix);
    }

    private float[] computePlacementPreviewAngles(StupidBlockPlacementConfig config, float previewTicks) {
        if (!config.enablePlacementAnimation) {
            return zeroAngles();
        }

        float duration = Math.max(1, config.durationTicks);
        float cycle = duration + PLACE_PAUSE_TICKS;
        float phase = previewTicks % cycle;
        if (phase >= duration) {
            return zeroAngles();
        }

        float t = phase / duration;
        if (t <= 0.0F) {
            return zeroAngles();
        }

        float horizontalOscillation = (float) Math.sin(config.horizontalCycles * Math.PI * t);
        float verticalOscillation = (float) Math.sin(config.verticalCycles * Math.PI * t);
        float decay = config.easing.apply(t);
        float horizontalAngle = config.horizontalMaxAngle * horizontalOscillation * decay;
        float verticalAngle = config.verticalMaxAngle * verticalOscillation * decay;

        if (config.randomizeDirection) {
            horizontalAngle *= this.horizontalSign;
            verticalAngle *= this.verticalSign;
        }

        return new float[]{horizontalAngle, verticalAngle};
    }

    private float[] computeBreakingPreviewAngles(StupidBlockPlacementConfig config, float previewTicks) {
        if (!config.enableBreakingAnimation) {
            return zeroAngles();
        }

        float loopTicks = Math.max(1, config.breakingLoopTicks);
        float delay = Math.max(0, config.breakingStartDelayTicks);
        float activeTicks = loopTicks * BREAK_ACTIVE_LOOPS;
        float keepAlive = Math.max(0, config.breakingKeepAliveTicks);
        float returnTicks = Math.max(1, config.breakingReturnTicks);
        float cycle = delay + activeTicks + keepAlive + returnTicks + BREAK_PAUSE_TICKS;
        float phase = previewTicks % cycle;
        if (phase < delay) {
            return zeroAngles();
        }

        float motionAge = phase - delay;
        float oscillatingDuration = activeTicks + keepAlive;
        if (motionAge < oscillatingDuration) {
            return computeBreakingLoopAngles(config, motionAge);
        }

        float releaseAge = motionAge - oscillatingDuration;
        if (releaseAge < returnTicks) {
            float[] startAngles = computeBreakingLoopAngles(config, oscillatingDuration);
            float k = 1.0F - Mth.clamp(releaseAge / returnTicks, 0.0F, 1.0F);
            return new float[]{startAngles[0] * k, startAngles[1] * k};
        }

        return zeroAngles();
    }

    private float[] computeBreakingLoopAngles(StupidBlockPlacementConfig config, float ageTicks) {
        float loopTicks = Math.max(1, config.breakingLoopTicks);
        float loopT = (ageTicks % loopTicks) / loopTicks;
        float horizontalOscillation = (float) Math.sin(config.breakingHorizontalCycles * (float) (Math.PI * 2.0) * loopT);
        float verticalOscillation = (float) Math.sin(config.breakingVerticalCycles * (float) (Math.PI * 2.0) * loopT);
        float amplitude = config.breakingEasing.apply(loopT);
        float horizontalAngle = config.breakingHorizontalMaxAngle * horizontalOscillation * amplitude;
        float verticalAngle = config.breakingVerticalMaxAngle * verticalOscillation * amplitude;

        if (config.breakingRandomizeDirection) {
            horizontalAngle *= this.horizontalSign;
            verticalAngle *= this.verticalSign;
        }

        return new float[]{horizontalAngle, verticalAngle};
    }

    private Direction getPreviewFace() {
        return this.kind == AnimationKind.PLACE ? Direction.UP : Direction.NORTH;
    }

    private static Quaternionf buildAnimationRotation(float horizontalAngleDeg, float verticalAngleDeg, Direction face) {
        float horizontal = horizontalAngleDeg * DEG_TO_RAD;
        float vertical = verticalAngleDeg * DEG_TO_RAD;
        Quaternionf rotation = new Quaternionf();

        switch (face) {
            case SOUTH -> {
                rotation.rotateZ(vertical);
                rotation.rotateY(horizontal);
            }
            case NORTH -> {
                rotation.rotateZ(-vertical);
                rotation.rotateY(horizontal);
            }
            case EAST -> {
                rotation.rotateX(vertical);
                rotation.rotateY(horizontal);
            }
            case WEST -> {
                rotation.rotateX(-vertical);
                rotation.rotateY(horizontal);
            }
            case DOWN -> {
                rotation.rotateX(vertical);
                rotation.rotateZ(horizontal);
            }
            case UP -> {
                rotation.rotateX(-vertical);
                rotation.rotateZ(horizontal);
            }
        }

        return rotation;
    }

    private static float[] zeroAngles() {
        return new float[]{0.0F, 0.0F};
    }

    private BlockState getPreviewBlockState() {
        return this.kind == AnimationKind.BREAK ? BREAK_PREVIEW_BLOCK : PLACE_PREVIEW_BLOCK;
    }

    private StupidBlockPlacementConfig getPreviewConfig() {
        StupidBlockPlacementConfig supplied = this.configSupplier.get();
        if (supplied == null) {
            return new StupidBlockPlacementConfig();
        }
        return StupidBlockPlacementConfig.copyOf(supplied);
    }
}
