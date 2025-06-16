package net.coolpixels;

import java.util.List;
import java.util.Map;

import net.minecraft.client.MinecraftClient;

public class ChallengeData {
    public static List<Map<String, Object>> getObjectives() {
        // TODO: fetch actual objectives from server
        return List.of(Map.of("type", "dimension", "content", "minecraft:the_nether", "complete", false));
    }

    public static String formatObjective(String type, String content) {
        if (type == "dimension") {
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

    public static List<Map<String, Object>> getRestrictions() {
        return List.of(
                Map.of("type", "hardcore", "content", "true"));
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
}
