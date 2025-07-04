package net.coolpixels;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;

public class ServerEventHandler {

    public static void handlePlayerJoin(ServerPlayerEntity player) {
        String playerUuid = player.getUuidAsString();
        String worldIdentifier = WorldUUIDSync.getOrCreateWorldIdentifier(player.getWorld());

        // Check player profile first - this runs during join process
        checkPlayerProfile(player, playerUuid, worldIdentifier);
    }

    private static void checkPlayerProfile(ServerPlayerEntity player, String playerUuid, String worldIdentifier) {
        // Start the profile check asynchronously
        ServerApiClient.checkPlayerProfileAsync(playerUuid)
                .thenRun(() -> {
                    // Check the result and send appropriate message
                    ProfileCheckResult.Status status = ProfileCheckResult.getAndRemoveResult(playerUuid);
                    if (status == ProfileCheckResult.Status.NOT_FOUND) {
                        player.networkHandler.disconnect(
                                Text.literal("Please make an account at https://weeklyworld.net")
                                        .formatted(Formatting.YELLOW));
                    } else if (status == ProfileCheckResult.Status.ERROR) {
                        player.sendMessage(
                                Text.literal("A server error has occurred.")
                                        .formatted(Formatting.RED),
                                false);
                    }
                    
                    // Continue with join logic only if profile check didn't result in disconnect
                    if (status != ProfileCheckResult.Status.NOT_FOUND) {
                        completePlayerJoin(player, playerUuid, worldIdentifier);
                    }
                });
    }

    private static void completePlayerJoin(ServerPlayerEntity player, String playerUuid, String worldIdentifier) {
        // Send greeting
        player.sendMessage(
                Text.literal("Welcome to Weekly World!")
                        .formatted(Formatting.GOLD, Formatting.BOLD),
                false);

        // Display restrictions and check them
        displayRestrictions(player, playerUuid, worldIdentifier);

        // Display objectives
        displayObjectives(player, playerUuid, worldIdentifier);

        // Check if player can complete objectives
        if (!ServerChallengeData.canCompleteObjectives(player)) {
            if (player.hasPermissionLevel(2)) {
                warnCommandsEnabled(player);
            } else {
                warnRestrictionsNotMet(player);
            }
        }
    }

    public static void reportEvent(ServerPlayerEntity player, String type, String value) {
        String playerUuid = player.getUuidAsString();
        String worldIdentifier = WorldUUIDSync.getOrCreateWorldIdentifier(player.getWorld());

        WeeklyWorld.LOGGER.info("Player {} reported event {} with value {} in world {} (can complete: {})",
                player.getName().getString(), type, value, worldIdentifier,
                ServerChallengeData.canCompleteObjectives(player));

        // Check if event matches an objective
        List<Map<String, Object>> objectives = ServerChallengeData.getObjectives();
        for (Map<String, Object> objective : objectives) {
            if (objective.get("type").equals(type) && objective.get("content").equals(value)) {
                // Check if already completed
                if (ServerPlayerData.isObjectiveCompleted(playerUuid, worldIdentifier, type, value)) {
                    return; // Already completed
                }

                // Check if player can complete objectives
                if (ServerChallengeData.canCompleteObjectives(player)) {
                    // Mark objective as completed
                    ServerPlayerData.markObjectiveCompleted(playerUuid, worldIdentifier, type, value);
                    player.sendMessage(
                            Text.literal(String.format("Objective completed: %s",
                                    ServerChallengeData.formatObjective(type, value)))
                                    .formatted(Formatting.GREEN),
                            false);

                    // Check if all objectives are completed
                    if (areAllObjectivesCompleted(playerUuid, worldIdentifier)) {
                        // Send player a congratulatory message
                        player.sendMessage(
                                Text.literal("🎉 Congratulations! You have completed all objectives!")
                                        .formatted(Formatting.GOLD, Formatting.BOLD),
                                false);

                        // Log completion on server
                        WeeklyWorld.LOGGER.info("Player {} completed all objectives for week {} in world {}",
                                player.getName().getString(), ServerChallengeData.getWeek(), worldIdentifier);

                        // Send completion to server API
                        ServerApiClient.sendCompletionAsync(playerUuid, ServerChallengeData.getWeek());

                        // Inform player of success
                        player.sendMessage(
                                Text.literal("✔ Completion recorded successfully!")
                                        .formatted(Formatting.GREEN),
                                false);
                    }
                } else {
                    if (player.hasPermissionLevel(2)) {
                        warnCommandsEnabled(player);
                    } else {
                        warnRestrictionsNotMet(player);
                    }
                }
                return;
            }
        }
    }

    private static void displayRestrictions(ServerPlayerEntity player, String playerUuid, String worldIdentifier) {
        List<Map<String, Object>> restrictions = ServerChallengeData.getRestrictions();

        if (!restrictions.isEmpty()) {
            player.sendMessage(Text.literal("Restrictions:").formatted(Formatting.BOLD), false);

            boolean allRestrictionsMet = true;
            for (Map<String, Object> restriction : restrictions) {
                String type = (String) restriction.get("type");
                String content = (String) restriction.get("content");
                boolean met = ServerChallengeData.checkRestriction(player, type, content);

                if (!met) {
                    allRestrictionsMet = false;
                }

                player.sendMessage(
                        Text.literal(String.format("%s %s",
                                met ? "☑" : "☐",
                                ServerChallengeData.formatRestriction(type, content))),
                        false);
            }

            if (!allRestrictionsMet) {
                warnRestrictionsNotMet(player);
            }
        }
    }

    private static void displayObjectives(ServerPlayerEntity player, String playerUuid, String worldIdentifier) {
        List<Map<String, Object>> objectives = ServerChallengeData.getObjectives();

        if (!objectives.isEmpty()) {
            player.sendMessage(
                    Text.literal(String.format("Objective%s:", objectives.size() == 1 ? "" : "s"))
                            .formatted(Formatting.BOLD),
                    false);

            for (Map<String, Object> objective : objectives) {
                String type = (String) objective.get("type");
                String content = (String) objective.get("content");
                boolean completed = ServerPlayerData.isObjectiveCompleted(playerUuid, worldIdentifier, type, content);

                player.sendMessage(
                        Text.literal(String.format("%s %s",
                                completed ? "☑" : "☐",
                                ServerChallengeData.formatObjective(type, content))),
                        false);
            }
        }
    }

    private static boolean areAllObjectivesCompleted(String playerUuid, String worldIdentifier) {
        List<Map<String, Object>> objectives = ServerChallengeData.getObjectives();

        for (Map<String, Object> objective : objectives) {
            String type = (String) objective.get("type");
            String content = (String) objective.get("content");

            if (!ServerPlayerData.isObjectiveCompleted(playerUuid, worldIdentifier, type, content)) {
                return false;
            }
        }

        return true;
    }

    private static void warnCommandsEnabled(ServerPlayerEntity player) {
        player.sendMessage(
                Text.literal("Objectives cannot be completed while commands are enabled.")
                        .formatted(Formatting.RED),
                false);
    }

    private static void warnRestrictionsNotMet(ServerPlayerEntity player) {
        player.sendMessage(
                Text.literal("Some restrictions are not met. Objectives cannot be completed until they are.")
                        .formatted(Formatting.RED),
                false);
    }
}
