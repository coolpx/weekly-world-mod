package net.coolpixels;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.io.*;
import java.util.*;

public class ServerChallengeData {
    private static final String OBJECTIVES_FILE = "weekly_world_objectives.json";
    private static final Gson GSON = new Gson();

    // Loads the challenge objectives from the config directory
    public static Map<String, Object> loadChallengeObjectives() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File objectivesFile = new File(configDir, OBJECTIVES_FILE);

        if (!objectivesFile.exists()) {
            WeeklyWorld.LOGGER.error("Could not find objectives file: {}", objectivesFile.getAbsolutePath());
            return Collections.emptyMap();
        }

        try (Reader reader = new FileReader(objectivesFile)) {
            Map<String, Object> result = GSON.fromJson(reader, new TypeToken<Map<String, Object>>() {
            }.getType());
            return result != null ? result : Collections.emptyMap();
        } catch (IOException e) {
            WeeklyWorld.LOGGER.error("Failed to load challenge objectives", e);
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getObjectives() {
        Map<String, Object> data = loadChallengeObjectives();
        if (data.containsKey("tasks")) {
            Object tasks = data.get("tasks");
            if (tasks instanceof List<?>) {
                return (List<Map<String, Object>>) tasks;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getRestrictions() {
        Map<String, Object> data = loadChallengeObjectives();
        if (data.containsKey("restrictions")) {
            Object restrictions = data.get("restrictions");
            if (restrictions instanceof List<?>) {
                return (List<Map<String, Object>>) restrictions;
            }
        }
        // Default restriction if none specified
        return List.of(Map.of("type", "hardcore", "content", "true"));
    }

    public static String formatObjective(String type, String content) {
        switch (type) {
            case "dimension":
                switch (content) {
                    case "minecraft:the_nether":
                        return "Enter the Nether";
                    case "minecraft:the_end":
                        return "Enter the End";
                    default:
                        return String.format("Enter dimension %s", content);
                }
            case "advancement":
                return String.format("Complete advancement: %s", content);
            case "item":
                return String.format("Obtain item: %s", content);
            case "kill":
                return String.format("Kill entity: %s", content);
            default:
                WeeklyWorld.LOGGER.warn("Unknown objective type: {}", type);
                return String.format("%s (%s)", type, content);
        }
    }

    public static String formatRestriction(String type, String content) {
        switch (type) {
            case "hardcore":
                return "Hardcore mode enabled";
            case "gamemode":
                return String.format("Game mode: %s", content);
            case "difficulty":
                return String.format("Difficulty: %s", content);
            default:
                WeeklyWorld.LOGGER.warn("Unknown restriction type: {}", type);
                return String.format("%s (%s)", type, content);
        }
    }

    public static boolean checkRestriction(ServerPlayerEntity player, String type, String content) {
        World world = player.getWorld();

        switch (type) {
            case "hardcore":
                return world.getLevelProperties().isHardcore() == Boolean.parseBoolean(content);
            case "gamemode":
                return player.interactionManager.getGameMode().name().toLowerCase().equals(content.toLowerCase());
            case "difficulty":
                return world.getDifficulty().getName().equals(content);
            default:
                WeeklyWorld.LOGGER.warn("Unknown restriction type: {}", type);
                return false;
        }
    }

    public static boolean areAllRestrictionsMet(ServerPlayerEntity player) {
        List<Map<String, Object>> restrictions = getRestrictions();
        for (Map<String, Object> restriction : restrictions) {
            String type = (String) restriction.get("type");
            String content = (String) restriction.get("content");
            if (!checkRestriction(player, type, content)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canCompleteObjectives(ServerPlayerEntity player) {
        // Players can complete objectives if:
        // 1. They are not an operator (permission level < 2)
        // 2. All restrictions are met
        return !player.hasPermissionLevel(2) && areAllRestrictionsMet(player);
    }

    public static int getWeek() {
        Map<String, Object> data = loadChallengeObjectives();
        if (data.containsKey("week")) {
            Object week = data.get("week");
            if (week instanceof Number) {
                return ((Number) week).intValue();
            }
        }
        return 1; // Default to week 1 if not specified
    }
}
