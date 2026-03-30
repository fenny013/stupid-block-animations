package fnn.stupidblockplacement;

import fnn.stupidblockplacement.animation.PlacementAnimationManager;
import fnn.stupidblockplacement.animation.PlacementAnimationRenderer;
import fnn.stupidblockplacement.config.StupidBlockPlacementConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public final class StupidBlockPlacementClient implements ClientModInitializer {
    public static final String MOD_ID = "stupid-block-animations";
    public static StupidBlockPlacementConfig CONFIG;

    private static long configSaveAtTick = -1L;

    @Override
    public void onInitializeClient() {
        CONFIG = StupidBlockPlacementConfig.load();

        LevelRenderEvents.COLLECT_SUBMITS.register(PlacementAnimationRenderer::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlacementAnimationManager.tick();
            if (client.level == null) { PlacementAnimationManager.clear(); }
            if (configSaveAtTick >= 0L && PlacementAnimationManager.getClientTicks() >= configSaveAtTick) {
                saveConfigNow();
            }
        });
    }

    public static void queueConfigSave() {
        configSaveAtTick = PlacementAnimationManager.getClientTicks() + 8L;
    }

    public static void saveConfigNow() {
        if (CONFIG != null) {
            CONFIG.save();
        }
        configSaveAtTick = -1L;
    }
}
