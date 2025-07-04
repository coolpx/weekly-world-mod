package net.coolpixels;

import java.util.*;
import java.io.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.loader.api.FabricLoader;

public class ChallengeData {
    private static final String DATA_NAME = "weekly_world_objectives.json";
    private static final String PLAYERDATA_NAME = "weekly_world_player_data.json";
    private static ChallengeData instance;
    private final Map<String, Set<String>> worldObjectiveCompletions = new HashMap<>(); // worldId -> set of completed
                                                                                        // objective keys
    private static final Gson GSON = new Gson();

    // Loads the challenge objectives from the config directory
    public static Map<String, Object> loadChallengeObjectives() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File objectivesFile = new File(configDir, DATA_NAME);

        if (!objectivesFile.exists()) {
            System.err.println("Could not find objectives file: " + objectivesFile.getAbsolutePath());
            return Collections.emptyMap();
        }

        try (Reader reader = new FileReader(objectivesFile)) {
            return GSON.fromJson(reader, new TypeToken<Map<String, Object>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    // Example objectives (should be fetched from server in real use)
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getObjectives() {
        Map<String, Object> data = loadChallengeObjectives();
        if (data.containsKey("tasks")) {
            Object tasks = data.get("tasks");
            if (tasks instanceof List<?>) {
                return (List<Map<String, Object>>) tasks;
            }
        }
        return List.of();
    }

    public static List<Map<String, Object>> getRestrictions() {
        return List.of(Map.of("type", "hardcore", "content", "true"));
    }

    public static String formatObjective(String type, String content) {
        if (type.equals("dimension")) {
            switch (content) {
                case "minecraft:the_nether":
                    return "Enter the Nether";
                case "minecraft:the_end":
                    return "Enter the End";
                default:
                    return String.format("Enter dimension %s", content);
            }
        }
        WeeklyWorld.LOGGER.warn("Unknown objective type: {}", type);
        return String.format("%s (%s)", type, content);
    }

    public static String formatRestriction(String type, String content) {
        if (type.equals("hardcore")) {
            return "Hardcore mode enabled";
        }
        WeeklyWorld.LOGGER.warn("Unknown restriction type: {}", type);
        return String.format("%s (%s)", type, content);
    }

    public static boolean restrictionMet(MinecraftClient client, String type, String content) {
        if (type.equals("hardcore")) {
            return client.world.getLevelProperties().isHardcore() == Boolean.parseBoolean(content);
        }
        WeeklyWorld.LOGGER.warn("Unknown restriction type: {}", type);
        return false;
    }

    // --- Persistence logic ---
    public static ChallengeData get(MinecraftClient client) {
        if (instance != null)
            return instance;
        instance = new ChallengeData();
        instance.load(client);
        return instance;
    }

    // Get the world identifier synced from the server
    private static String getWorldIdentifier(MinecraftClient client) {
        String worldIdentifier = WorldUUIDSyncClient.getCurrentWorldIdentifier();
        if (worldIdentifier != null) {
            WeeklyWorld.LOGGER.debug("Using synced world identifier: {}", worldIdentifier);
            return worldIdentifier;
        }

        // Fallback if identifier not yet synced from server
        if (client.world == null) {
            WeeklyWorld.LOGGER.debug("Using fallback identifier: unknown (no world)");
            return "unknown";
        }
        // Use dimension name as fallback (not unique, but avoids crash)
        String fallback = client.world.getRegistryKey().getValue().toString();
        WeeklyWorld.LOGGER.debug("Using fallback identifier: {} (dimension key)", fallback);
        return fallback;
    }

    // Store completions for all worlds in a single file, indexed by world
    // identifier
    private void load(MinecraftClient client) {
        File file = getSaveFile();
        worldObjectiveCompletions.clear();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Map<String, Set<String>> data = GSON.fromJson(reader, new TypeToken<Map<String, Set<String>>>() {
                }.getType());
                if (data != null)
                    worldObjectiveCompletions.putAll(data);
            } catch (IOException e) {
                WeeklyWorld.LOGGER.error("Failed to load objective completion data", e);
            }
        }
    }

    private void save(MinecraftClient client) {
        File file = getSaveFile();
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(worldObjectiveCompletions, writer);
        } catch (IOException e) {
            WeeklyWorld.LOGGER.error("Failed to save objective completion data", e);
        }
    }

    private static File getSaveFile() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        return new File(configDir, PLAYERDATA_NAME);
    }

    // --- Objective completion logic ---
    private static String getObjectiveKey(String type, String content) {
        return type + "|" + content;
    }

    public static boolean isObjectiveCompleted(MinecraftClient client, String type, String content) {
        ChallengeData data = get(client);
        if (data == null)
            return false;
        String worldIdentifier = getWorldIdentifier(client);
        Set<String> completed = data.worldObjectiveCompletions.getOrDefault(worldIdentifier, Collections.emptySet());
        WeeklyWorld.LOGGER.debug("Checking completion for {} in world {}: {}", getObjectiveKey(type, content),
                worldIdentifier, completed);
        WeeklyWorld.LOGGER.debug("Completed objectives: {}", completed);
        return completed.contains(getObjectiveKey(type, content));
    }

    public static void markObjectiveCompleted(MinecraftClient client, String type, String content) {
        ChallengeData data = get(client);
        if (data == null)
            return;
        String worldIdentifier = getWorldIdentifier(client);
        data.worldObjectiveCompletions.computeIfAbsent(worldIdentifier, k -> new HashSet<>())
                .add(getObjectiveKey(type, content));
        data.save(client);
    }

    // Clean up challenge data for worlds that no longer exist
    public static void cleanupDeletedWorlds(MinecraftClient client) {
        ChallengeData data = get(client);
        if (data == null)
            return;

        // Get the list of existing world folders
        Set<String> existingWorlds = getExistingWorldFolders(client);

        // Remove completion data for worlds that no longer exist
        data.worldObjectiveCompletions.entrySet().removeIf(entry -> {
            String worldId = entry.getKey();
            // Check if the world identifier exists in our list of existing worlds
            boolean exists = existingWorlds.contains(worldId)
                    || existingWorlds.stream().anyMatch(world -> worldId.startsWith(world));
            if (!exists) {
                WeeklyWorld.LOGGER.info("Removing challenge data for deleted world: {}", worldId);
            }
            return !exists;
        });

        data.save(client);
    }

    private static Set<String> getExistingWorldFolders(MinecraftClient client) {
        Set<String> existingWorlds = new HashSet<>();

        // Get the saves directory path
        File savesDir = new File(client.runDirectory, "saves");

        if (savesDir.exists() && savesDir.isDirectory()) {
            File[] worldFolders = savesDir.listFiles(File::isDirectory);

            if (worldFolders != null) {
                for (File worldFolder : worldFolders) {
                    String worldName = worldFolder.getName();
                    existingWorlds.add(worldName);
                    // Also add dimension-specific identifiers for backwards compatibility
                    existingWorlds.add(worldName + "_minecraft:overworld");
                    existingWorlds.add(worldName + "_minecraft:the_nether");
                    existingWorlds.add(worldName + "_minecraft:the_end");
                }
            }
        }

        return existingWorlds;
    }
}
