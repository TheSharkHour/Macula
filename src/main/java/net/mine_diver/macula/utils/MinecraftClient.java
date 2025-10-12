package net.mine_diver.macula.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Utility for accessing Minecraft client instance via Fabric Loader.
 */
public class MinecraftClient {

    /**
     * Gets current Minecraft client instance.
     *
     * @return Minecraft client instance
     */
    public static Minecraft get() {
        //noinspection deprecation
        return (Minecraft) FabricLoader.getInstance().getGameInstance();
    }
}
