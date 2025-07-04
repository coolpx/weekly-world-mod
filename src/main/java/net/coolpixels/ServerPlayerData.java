package net.coolpixels;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.util.*;

public class ServerPlayerData {
    private static final String PLAYER_DATA_FILE = "weekly_world_player_data.json";
    private static final Gson GSON = new Gson();

    // Map: PlayerUUID -> WorldIdentifier -> Set of completed objective keys
    private static final Map<String, Map<String, Set<String>>> playerObjectiveCompletions = new HashMap<>();

    // Map: PlayerUUID -> WorldIdentifier -> Set of checked restrictions
    private static final Map<String, Map<String, Set<String>>> playerRestrictionChecks = new HashMap<>();

    private static boolean dataLoaded = false;

    public static void loadData() {
        if (dataLoaded)
            return;

        File file = getDataFile();
        playerObjectiveCompletions.clear();
        playerRestrictionChecks.clear();

        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Map<String, Object> data = GSON.fromJson(reader, new TypeToken<Map<String, Object>>() {
                }.getType());
                if (data != null) {
                    // Load objective completions
                    if (data.containsKey("objectives")) {
                        Map<String, Map<String, Set<String>>> objectives = GSON.fromJson(
                                GSON.toJson(data.get("objectives")),
                                new TypeToken<Map<String, Map<String, Set<String>>>>() {
                                }.getType());
                        if (objectives != null) {
                            playerObjectiveCompletions.putAll(objectives);
                        }
                    }

                    // Load restriction checks
                    if (data.containsKey("restrictions")) {
                        Map<String, Map<String, Set<String>>> restrictions = GSON.fromJson(
                                GSON.toJson(data.get("restrictions")),
                                new TypeToken<Map<String, Map<String, Set<String>>>>() {
                                }.getType());
                        if (restrictions != null) {
                            playerRestrictionChecks.putAll(restrictions);
                        }
                    }
                }
            } catch (IOException e) {
                WeeklyWorld.LOGGER.error("Failed to load player data", e);
            }
        }

        dataLoaded = true;
    }

    public static void saveData() {
        File file = getDataFile();
        file.getParentFile().mkdirs();

        Map<String, Object> data = new HashMap<>();
        data.put("objectives", playerObjectiveCompletions);
        data.put("restrictions", playerRestrictionChecks);

        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            WeeklyWorld.LOGGER.error("Failed to save player data", e);
        }
    }

    private static File getDataFile() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        return new File(configDir, PLAYER_DATA_FILE);
    }

    private static String getObjectiveKey(String type, String content) {
        return type + "|" + content;
    }

    private static String getRestrictionKey(String type, String content) {
        return type + "|" + content;
    }

    public static boolean isObjectiveCompleted(String playerUuid, String worldIdentifier, String type, String content) {
        loadData();
        Map<String, Set<String>> playerWorlds = playerObjectiveCompletions.get(playerUuid);
        if (playerWorlds == null)
            return false;

        Set<String> completedObjectives = playerWorlds.get(worldIdentifier);
        if (completedObjectives == null)
            return false;

        return completedObjectives.contains(getObjectiveKey(type, content));
    }

    public static void markObjectiveCompleted(String playerUuid, String worldIdentifier, String type, String content) {
        loadData();
        playerObjectiveCompletions
                .computeIfAbsent(playerUuid, k -> new HashMap<>())
                .computeIfAbsent(worldIdentifier, k -> new HashSet<>())
                .add(getObjectiveKey(type, content));
        saveData();
    }

    public static boolean isRestrictionChecked(String playerUuid, String worldIdentifier, String type, String content) {
        loadData();
        Map<String, Set<String>> playerWorlds = playerRestrictionChecks.get(playerUuid);
        if (playerWorlds == null)
            return false;

        Set<String> checkedRestrictions = playerWorlds.get(worldIdentifier);
        if (checkedRestrictions == null)
            return false;

        return checkedRestrictions.contains(getRestrictionKey(type, content));
    }

    public static void markRestrictionChecked(String playerUuid, String worldIdentifier, String type, String content) {
        loadData();
        playerRestrictionChecks
                .computeIfAbsent(playerUuid, k -> new HashMap<>())
                .computeIfAbsent(worldIdentifier, k -> new HashSet<>())
                .add(getRestrictionKey(type, content));
        saveData();
    }

    public static void cleanupDeletedWorlds(MinecraftServer server) {
        loadData();

        // Get existing world identifiers
        Set<String> existingWorlds = WorldUUIDSync.getExistingWorldIdentifiers(server);

        // Clean up objective completions
        for (Map<String, Set<String>> playerWorlds : playerObjectiveCompletions.values()) {
            playerWorlds.entrySet().removeIf(entry -> {
                String worldId = entry.getKey();
                boolean exists = existingWorlds.contains(worldId);
                if (!exists) {
                    WeeklyWorld.LOGGER.info("Removing objective data for deleted world: {}", worldId);
                }
                return !exists;
            });
        }

        // Clean up restriction checks
        for (Map<String, Set<String>> playerWorlds : playerRestrictionChecks.values()) {
            playerWorlds.entrySet().removeIf(entry -> {
                String worldId = entry.getKey();
                boolean exists = existingWorlds.contains(worldId);
                if (!exists) {
                    WeeklyWorld.LOGGER.info("Removing restriction data for deleted world: {}", worldId);
                }
                return !exists;
            });
        }

        saveData();
    }

    public static Set<String> getAllTrackedPlayers() {
        loadData();
        return new HashSet<>(playerObjectiveCompletions.keySet());
    }

    public static Set<String> getAllTrackedWorlds() {
        loadData();
        Set<String> worlds = new HashSet<>();
        for (Map<String, Set<String>> playerWorlds : playerObjectiveCompletions.values()) {
            worlds.addAll(playerWorlds.keySet());
        }
        return worlds;
    }
}
