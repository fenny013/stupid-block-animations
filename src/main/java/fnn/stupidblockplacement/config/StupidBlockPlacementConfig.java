package fnn.stupidblockplacement.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import fnn.stupidblockplacement.animation.Easing;
import fnn.stupidblockplacement.StupidBlockPlacementClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public final class StupidBlockPlacementConfig {
    public static final ConfigClassHandler<StupidBlockPlacementConfig> HANDLER =
            ConfigClassHandler.createBuilder(StupidBlockPlacementConfig.class)
                    .id(Identifier.fromNamespaceAndPath(StupidBlockPlacementClient.MOD_ID, "config"))
                    .serializer(config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(FabricLoader.getInstance().getConfigDir().resolve("stupid-block-animations.json5"))
                            .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                            .setJson5(true)
                            .build())
                    .build();

    @SerialEntry public boolean enablePlacementAnimation = true;
    @SerialEntry public int durationTicks = 6;
    @SerialEntry public float horizontalMaxAngle = 15.0F;
    @SerialEntry public float verticalMaxAngle = 15.0F;
    @SerialEntry public float horizontalCycles = 1.5F;
    @SerialEntry public float verticalCycles = 1.0F;
    @SerialEntry public Easing easing = Easing.EASE_IN_OUT;
    @SerialEntry public boolean randomizeDirection = true;

    @SerialEntry public boolean enableBreakingAnimation = true;
    @SerialEntry public int breakingLoopTicks = 6;
    @SerialEntry public int breakingStartDelayTicks = 3;
    @SerialEntry public int breakingKeepAliveTicks = 2;
    @SerialEntry public boolean breakingAnimateConnectedBlocks = true;
    @SerialEntry public float breakingHorizontalMaxAngle = 8.0F;
    @SerialEntry public float breakingVerticalMaxAngle = 4.0F;
    @SerialEntry public float breakingHorizontalCycles = 1.5F;
    @SerialEntry public float breakingVerticalCycles = 1.0F;
    @SerialEntry public Easing breakingEasing = Easing.CONSTANT;
    @SerialEntry public boolean breakingRandomizeDirection = true;
    @SerialEntry public int breakingReturnTicks = 4;

    public static StupidBlockPlacementConfig load() {
        HANDLER.load();
        StupidBlockPlacementConfig config = HANDLER.instance();
        config.sanitize();
        HANDLER.save();
        return config;
    }

    public static StupidBlockPlacementConfig copyOf(StupidBlockPlacementConfig source) {
        StupidBlockPlacementConfig copy = new StupidBlockPlacementConfig();

        copy.enablePlacementAnimation = source.enablePlacementAnimation;
        copy.durationTicks = source.durationTicks;
        copy.horizontalMaxAngle = source.horizontalMaxAngle;
        copy.verticalMaxAngle = source.verticalMaxAngle;
        copy.horizontalCycles = source.horizontalCycles;
        copy.verticalCycles = source.verticalCycles;
        copy.easing = source.easing;
        copy.randomizeDirection = source.randomizeDirection;

        copy.enableBreakingAnimation = source.enableBreakingAnimation;
        copy.breakingLoopTicks = source.breakingLoopTicks;
        copy.breakingStartDelayTicks = source.breakingStartDelayTicks;
        copy.breakingKeepAliveTicks = source.breakingKeepAliveTicks;
        copy.breakingAnimateConnectedBlocks = source.breakingAnimateConnectedBlocks;
        copy.breakingHorizontalMaxAngle = source.breakingHorizontalMaxAngle;
        copy.breakingVerticalMaxAngle = source.breakingVerticalMaxAngle;
        copy.breakingHorizontalCycles = source.breakingHorizontalCycles;
        copy.breakingVerticalCycles = source.breakingVerticalCycles;
        copy.breakingEasing = source.breakingEasing;
        copy.breakingRandomizeDirection = source.breakingRandomizeDirection;
        copy.breakingReturnTicks = source.breakingReturnTicks;

        copy.sanitize();
        return copy;
    }

    public void save() {
        sanitize();
        HANDLER.save();
    }

    public void sanitize() {
        this.durationTicks = clamp(this.durationTicks, 1, 40);
        this.horizontalMaxAngle = clamp(this.horizontalMaxAngle, 0.0F, 60.0F);
        this.verticalMaxAngle = clamp(this.verticalMaxAngle, 0.0F, 60.0F);
        this.horizontalCycles = clamp(this.horizontalCycles, 0.0F, 8.0F);
        this.verticalCycles = clamp(this.verticalCycles, 0.0F, 8.0F);
        this.easing = this.easing == null ? Easing.EASE_IN_OUT : this.easing;

        this.breakingLoopTicks = clamp(this.breakingLoopTicks, 1, 40);
        this.breakingStartDelayTicks = clamp(this.breakingStartDelayTicks, 0, 40);
        this.breakingKeepAliveTicks = clamp(this.breakingKeepAliveTicks, 0, 40);
        this.breakingHorizontalMaxAngle = clamp(this.breakingHorizontalMaxAngle, 0.0F, 60.0F);
        this.breakingVerticalMaxAngle = clamp(this.breakingVerticalMaxAngle, 0.0F, 60.0F);
        this.breakingHorizontalCycles = clamp(this.breakingHorizontalCycles, 0.0F, 8.0F);
        this.breakingVerticalCycles = clamp(this.breakingVerticalCycles, 0.0F, 8.0F);
        this.breakingEasing = this.breakingEasing == null ? Easing.CONSTANT : this.breakingEasing;
        this.breakingReturnTicks = clamp(this.breakingReturnTicks, 1, 40);
    }

    public void applyPlacementDefaults() {
        StupidBlockPlacementConfig defaults = new StupidBlockPlacementConfig();

        this.enablePlacementAnimation = defaults.enablePlacementAnimation;
        this.durationTicks = defaults.durationTicks;
        this.horizontalMaxAngle = defaults.horizontalMaxAngle;
        this.verticalMaxAngle = defaults.verticalMaxAngle;
        this.horizontalCycles = defaults.horizontalCycles;
        this.verticalCycles = defaults.verticalCycles;
        this.easing = defaults.easing;
        this.randomizeDirection = defaults.randomizeDirection;

        sanitize();
    }

    public void applyBreakingDefaults() {
        StupidBlockPlacementConfig defaults = new StupidBlockPlacementConfig();

        this.enableBreakingAnimation = defaults.enableBreakingAnimation;
        this.breakingLoopTicks = defaults.breakingLoopTicks;
        this.breakingStartDelayTicks = defaults.breakingStartDelayTicks;
        this.breakingKeepAliveTicks = defaults.breakingKeepAliveTicks;
        this.breakingAnimateConnectedBlocks = defaults.breakingAnimateConnectedBlocks;
        this.breakingHorizontalMaxAngle = defaults.breakingHorizontalMaxAngle;
        this.breakingVerticalMaxAngle = defaults.breakingVerticalMaxAngle;
        this.breakingHorizontalCycles = defaults.breakingHorizontalCycles;
        this.breakingVerticalCycles = defaults.breakingVerticalCycles;
        this.breakingEasing = defaults.breakingEasing;
        this.breakingRandomizeDirection = defaults.breakingRandomizeDirection;
        this.breakingReturnTicks = defaults.breakingReturnTicks;

        sanitize();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
