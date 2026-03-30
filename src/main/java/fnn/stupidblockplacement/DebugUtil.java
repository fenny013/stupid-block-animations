package fnn.stupidblockplacement;

import net.fabricmc.loader.api.FabricLoader;

public final class DebugUtil {

    public static final boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment();

    public static void log(String msg) {
        if (DEBUG) {
            System.out.println("[StupidBlockPlacement] " + msg);
        }
    }
}