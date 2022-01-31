package me.hasenzahn1.villagertradefindermod;

import me.hasenzahn1.villagertradefindermod.config.Config;
import me.hasenzahn1.villagertradefindermod.interaction.WorldManager;
import me.hasenzahn1.villagertradefindermod.listeners.KeyPressListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerTradeFinderMod implements ModInitializer, ClientModInitializer {

    public static final String MOD_ID = "villagertradefindermod";
    public static final Logger LOGGER = LoggerFactory.getLogger("VillagerTradeFinder");
    private KeyPressListener keyPressListener;
    private static VillagerTradeFinderMod instance;
    private Config config;
    private WorldManager worldManager;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("This is a client side mod and should be remove from the server");
    }

    @Override
    public void onInitializeClient() {
        if(instance == null){
            instance = this;
        }

        config = new Config();
        worldManager = new WorldManager(config);
        worldManager.reset();

        if(keyPressListener == null) keyPressListener = new KeyPressListener(MinecraftClient.getInstance());
    }

    public static VillagerTradeFinderMod getInstance() {
        return instance;
    }

    public Config getConfig() {
        return config;
    }

    public KeyPressListener getKeyPressListener() {
        return keyPressListener;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

}
