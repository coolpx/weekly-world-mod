package net.coolpixels;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeeklyWorld implements ModInitializer {
    public static final String MOD_ID = "weekly-world";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Weekly World");
        WorldUUIDSync.register();

        // Save world UUIDs when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, saving world UUIDs");
            WorldUUIDSync.saveAllUUIDs();
        });

        // Clean up deleted worlds when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, cleaning up deleted world UUIDs");
            WorldUUIDSync.cleanupDeletedWorlds();
        });
    }
}