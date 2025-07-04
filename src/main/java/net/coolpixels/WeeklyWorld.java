package net.coolpixels;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeeklyWorld implements ModInitializer {
    public static final String MOD_ID = "weekly-world";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Weekly World (Server-Only)");

        // Load server environment configuration
        ServerEnvironmentConfig.loadConfig();

        // Register world UUID sync
        WorldUUIDSync.register();

        // Register server events
        registerServerEvents();

        // Save data when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, saving data");
            WorldUUIDSync.saveAllUUIDs();
            ServerPlayerData.saveData();
        });

        // Clean up deleted worlds when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, cleaning up deleted worlds");
            WorldUUIDSync.cleanupDeletedWorlds();
            ServerPlayerData.cleanupDeletedWorlds(server);
        });
    }

    private void registerServerEvents() {
        // Handle player joining
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerEventHandler.handlePlayerJoin(handler.player);
        });

        LOGGER.info("Server events registered");
    }
}