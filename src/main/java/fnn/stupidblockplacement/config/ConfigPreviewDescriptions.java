package fnn.stupidblockplacement.config;

import dev.isxander.yacl3.api.OptionDescription;
import fnn.stupidblockplacement.animation.AnimationKind;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.function.Supplier;

public final class ConfigPreviewDescriptions {
    private ConfigPreviewDescriptions() {
    }

    public static OptionDescription placement(String descriptionKey, Supplier<StupidBlockPlacementConfig> previewConfig) {
        return build(descriptionKey, AnimationKind.PLACE, previewConfig);
    }

    public static OptionDescription breaking(String descriptionKey, Supplier<StupidBlockPlacementConfig> previewConfig) {
        return build(descriptionKey, AnimationKind.BREAK, previewConfig);
    }

    private static OptionDescription build(
            String descriptionKey,
            AnimationKind kind,
            Supplier<StupidBlockPlacementConfig> previewConfig
    ) {
        return OptionDescription.createBuilder()
                .text(Component.translatable(descriptionKey).withStyle(Style.EMPTY.withColor(0xc5bcd6)))
                .customImage(new ConfigPreviewImageRenderer(kind, previewConfig))
                .build();
    }
}