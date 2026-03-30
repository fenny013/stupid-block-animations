package fnn.stupidblockplacement.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import fnn.stupidblockplacement.animation.Easing;
import fnn.stupidblockplacement.StupidBlockPlacementClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class StupidBlockPlacementConfigScreen {
    private StupidBlockPlacementConfigScreen() {
    }

    public static Screen create(Screen parent) {
        StupidBlockPlacementConfig defaults = new StupidBlockPlacementConfig();
        StupidBlockPlacementConfig preview = StupidBlockPlacementConfig.copyOf(StupidBlockPlacementClient.CONFIG);

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("stupid-block-animations.config.title"))
                .save(() -> {
                    StupidBlockPlacementClient.CONFIG.sanitize();
                    StupidBlockPlacementClient.saveConfigNow();
                })

                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("stupid-block-animations.config.category.placement"))
                        .tooltip(Component.translatable("stupid-block-animations.config.category.placement.tooltip"))

                        .option(previewed(
                                Option.<Boolean>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.enablePlacementAnimation"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.enablePlacementAnimation.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.enablePlacementAnimation,
                                                () -> StupidBlockPlacementClient.CONFIG.enablePlacementAnimation,
                                                value -> StupidBlockPlacementClient.CONFIG.enablePlacementAnimation = value
                                        ),
                                value -> preview.enablePlacementAnimation = value
                        ).controller(opt -> BooleanControllerBuilder.create(opt).coloured(true)).build())

                        .option(previewed(
                                Option.<Integer>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.durationTicks"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.durationTicks.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.durationTicks,
                                                () -> StupidBlockPlacementClient.CONFIG.durationTicks,
                                                value -> StupidBlockPlacementClient.CONFIG.durationTicks = value
                                        ),
                                value -> preview.durationTicks = value
                        ).controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 40)
                                .step(1)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.horizontalMaxAngle"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.horizontalMaxAngle.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.horizontalMaxAngle,
                                                () -> StupidBlockPlacementClient.CONFIG.horizontalMaxAngle,
                                                value -> StupidBlockPlacementClient.CONFIG.horizontalMaxAngle = value
                                        ),
                                value -> preview.horizontalMaxAngle = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 60.0F)
                                .step(0.5F)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.verticalMaxAngle"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.verticalMaxAngle.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.verticalMaxAngle,
                                                () -> StupidBlockPlacementClient.CONFIG.verticalMaxAngle,
                                                value -> StupidBlockPlacementClient.CONFIG.verticalMaxAngle = value
                                        ),
                                value -> preview.verticalMaxAngle = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 60.0F)
                                .step(0.5F)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.horizontalCycles"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.horizontalCycles.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.horizontalCycles,
                                                () -> StupidBlockPlacementClient.CONFIG.horizontalCycles,
                                                value -> StupidBlockPlacementClient.CONFIG.horizontalCycles = value
                                        ),
                                value -> preview.horizontalCycles = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 8.0F)
                                .step(0.1F)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.verticalCycles"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.verticalCycles.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.verticalCycles,
                                                () -> StupidBlockPlacementClient.CONFIG.verticalCycles,
                                                value -> StupidBlockPlacementClient.CONFIG.verticalCycles = value
                                        ),
                                value -> preview.verticalCycles = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 8.0F)
                                .step(0.1F)).build())

                        .option(previewed(
                                Option.<Easing>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.easing"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.easing.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.easing,
                                                () -> StupidBlockPlacementClient.CONFIG.easing,
                                                value -> StupidBlockPlacementClient.CONFIG.easing = value
                                        ),
                                value -> preview.easing = value
                        ).controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(Easing.class)
                                .formatValue(Easing::asText)).build())

                        .option(previewed(
                                Option.<Boolean>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.randomizeDirection"))
                                        .description(ConfigPreviewDescriptions.placement(
                                                "stupid-block-animations.config.randomizeDirection.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.randomizeDirection,
                                                () -> StupidBlockPlacementClient.CONFIG.randomizeDirection,
                                                value -> StupidBlockPlacementClient.CONFIG.randomizeDirection = value
                                        ),
                                value -> preview.randomizeDirection = value
                        ).controller(opt -> BooleanControllerBuilder.create(opt).coloured(true)).build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("stupid-block-animations.config.category.breaking"))
                        .tooltip(Component.translatable("stupid-block-animations.config.category.breaking.tooltip"))

                        .option(previewed(
                                Option.<Boolean>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.enableBreakingAnimation"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.enableBreakingAnimation.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.enableBreakingAnimation,
                                                () -> StupidBlockPlacementClient.CONFIG.enableBreakingAnimation,
                                                value -> StupidBlockPlacementClient.CONFIG.enableBreakingAnimation = value
                                        ),
                                value -> preview.enableBreakingAnimation = value
                        ).controller(opt -> BooleanControllerBuilder.create(opt).coloured(true)).build())

                        .option(previewed(
                                Option.<Integer>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingLoopTicks"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingLoopTicks.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingLoopTicks,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingLoopTicks,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingLoopTicks = value
                                        ),
                                value -> preview.breakingLoopTicks = value
                        ).controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 40)
                                .step(1)).build())

                        .option(previewed(
                                Option.<Integer>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingStartDelayTicks"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingStartDelayTicks.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingStartDelayTicks,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingStartDelayTicks,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingStartDelayTicks = value
                                        ),
                                value -> preview.breakingStartDelayTicks = value
                        ).controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 40)
                                .step(1)).build())

                        .option(previewed(
                                Option.<Integer>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingKeepAliveTicks"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingKeepAliveTicks.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingKeepAliveTicks,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingKeepAliveTicks,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingKeepAliveTicks = value
                                        ),
                                value -> preview.breakingKeepAliveTicks = value
                        ).controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 40)
                                .step(1)).build())

                        .option(previewed(
                                Option.<Boolean>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingAnimateConnectedBlocks"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingAnimateConnectedBlocks.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingAnimateConnectedBlocks,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingAnimateConnectedBlocks,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingAnimateConnectedBlocks = value
                                        ),
                                value -> preview.breakingAnimateConnectedBlocks = value
                        ).controller(opt -> BooleanControllerBuilder.create(opt).coloured(true)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingHorizontalMaxAngle"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingHorizontalMaxAngle.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingHorizontalMaxAngle,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingHorizontalMaxAngle,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingHorizontalMaxAngle = value
                                        ),
                                value -> preview.breakingHorizontalMaxAngle = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 60.0F)
                                .step(0.5F)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingVerticalMaxAngle"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingVerticalMaxAngle.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingVerticalMaxAngle,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingVerticalMaxAngle,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingVerticalMaxAngle = value
                                        ),
                                value -> preview.breakingVerticalMaxAngle = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 60.0F)
                                .step(0.5F)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingHorizontalCycles"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingHorizontalCycles.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingHorizontalCycles,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingHorizontalCycles,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingHorizontalCycles = value
                                        ),
                                value -> preview.breakingHorizontalCycles = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 8.0F)
                                .step(0.1F)).build())

                        .option(previewed(
                                Option.<Float>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingVerticalCycles"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingVerticalCycles.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingVerticalCycles,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingVerticalCycles,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingVerticalCycles = value
                                        ),
                                value -> preview.breakingVerticalCycles = value
                        ).controller(opt -> FloatSliderControllerBuilder.create(opt)
                                .range(0.0F, 8.0F)
                                .step(0.1F)).build())

                        .option(previewed(
                                Option.<Easing>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingEasing"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingEasing.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingEasing,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingEasing,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingEasing = value
                                        ),
                                value -> preview.breakingEasing = value
                        ).controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(Easing.class)
                                .formatValue(Easing::asText)).build())

                        .option(previewed(
                                Option.<Boolean>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingRandomizeDirection"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingRandomizeDirection.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingRandomizeDirection,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingRandomizeDirection,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingRandomizeDirection = value
                                        ),
                                value -> preview.breakingRandomizeDirection = value
                        ).controller(opt -> BooleanControllerBuilder.create(opt).coloured(true)).build())

                        .option(previewed(
                                Option.<Integer>createBuilder()
                                        .name(Component.translatable("stupid-block-animations.config.breakingReturnTicks"))
                                        .description(ConfigPreviewDescriptions.breaking(
                                                "stupid-block-animations.config.breakingReturnTicks.desc",
                                                () -> preview
                                        ))
                                        .binding(
                                                defaults.breakingReturnTicks,
                                                () -> StupidBlockPlacementClient.CONFIG.breakingReturnTicks,
                                                value -> StupidBlockPlacementClient.CONFIG.breakingReturnTicks = value
                                        ),
                                value -> preview.breakingReturnTicks = value
                        ).controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 40)
                                .step(1)).build())

                        .build())

                .build()
                .generateScreen(parent);
    }

    private static <T> Option.Builder<T> previewed(
            Option.Builder<T> builder,
            Consumer<T> previewSetter
    ) {
        return builder.addListener((opt, event) -> previewSetter.accept(opt.pendingValue()));
    }
}