package net.coolpixels;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class WorldUUIDSyncClient {
    private static String currentWorldIdentifier = null;

    public static void register() {
        // Register client-side packet handler to receive world identifier from server
        ClientPlayNetworking.registerGlobalReceiver(WorldUUIDSync.WorldUUIDPayload.ID, (payload, context) -> {
            currentWorldIdentifier = payload.worldIdentifier();

            WeeklyWorld.LOGGER.info("Received world identifier from server: {}", currentWorldIdentifier);

            // Note: Objective display timing is now handled by the delayed mechanism in
            // WeeklyWorldClient
        });
    }

    public static String getCurrentWorldIdentifier() {
        return currentWorldIdentifier;
    }

    public static void clearWorldIdentifier() {
        currentWorldIdentifier = null;
    }
}
