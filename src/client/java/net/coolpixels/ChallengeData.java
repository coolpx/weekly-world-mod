package net.coolpixels;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.loader.api.FabricLoader;

public class ChallengeData {
    private static final String DATA_NAME = "weekly_world_objectives.json";
    private static ChallengeData instance;
    private final Map<String, Set<String>> worldObjectiveCompletions = new HashMap<>(); // worldId -> set of completed
                                                                                        // objective keys
    private static final Gson GSON = new Gson();

    // Example objectives (should be fetched from server in real use)
    public static List<Map<String, Object>> getObjectives() {
        return List.of(Map.of("type", "dimension", "content", "minecraft:the_nether", "complete", false));
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

    private void load(MinecraftClient client) {
        File file = getSaveFile(client);
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
        File file = getSaveFile(client);
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(worldObjectiveCompletions, writer);
        } catch (IOException e) {
            WeeklyWorld.LOGGER.error("Failed to save objective completion data", e);
        }
    }

    private static File getSaveFile(MinecraftClient client) {
        // Always use the config directory for client-side persistence
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        return new File(configDir, DATA_NAME);
    }

    // --- Objective completion logic ---
    private static String getWorldId(MinecraftClient client) {
        if (client.world == null)
            return "unknown";
        return client.world.getRegistryKey().getValue().toString();
    }

    private static String getObjectiveKey(String type, String content) {
        return type + ":" + content;
    }

    public static boolean isObjectiveCompleted(MinecraftClient client, String type, String content) {
        ChallengeData data = get(client);
        if (data == null)
            return false;
        String worldId = getWorldId(client);
        Set<String> completed = data.worldObjectiveCompletions.getOrDefault(worldId, Collections.emptySet());
        return completed.contains(getObjectiveKey(type, content));
    }

    public static void markObjectiveCompleted(MinecraftClient client, String type, String content) {
        ChallengeData data = get(client);
        if (data == null)
            return;
        String worldId = getWorldId(client);
        data.worldObjectiveCompletions.computeIfAbsent(worldId, k -> new HashSet<>())
                .add(getObjectiveKey(type, content));
        data.save(client);
    }
}
