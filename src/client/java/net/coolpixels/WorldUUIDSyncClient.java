package net.coolpixels;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class WorldUUIDSyncClient {
    private static String currentWorldIdentifier = null;

    public static void register() {
        // Register client-side packet handler to receive world identifier from server
        ClientPlayNetworking.registerGlobalReceiver(WorldUUIDSync.WorldUUIDPayload.ID, (payload, context) -> {
            String oldIdentifier = currentWorldIdentifier;
            currentWorldIdentifier = payload.worldIdentifier();

            WeeklyWorld.LOGGER.info("Received world identifier from server: {}", currentWorldIdentifier);

            // If this is the first time we're receiving the identifier, display objectives
            // with
            // proper completion status
            if (oldIdentifier == null && MinecraftClient.getInstance().player != null) {
                WeeklyWorld.LOGGER.info("Displaying objectives with correct completion status");
                WeeklyWorldClient.displayObjectivesWithCorrectStatus();
            }
        });
    }

    public static String getCurrentWorldIdentifier() {
        return currentWorldIdentifier;
    }

    public static void clearWorldIdentifier() {
        currentWorldIdentifier = null;
    }
}
