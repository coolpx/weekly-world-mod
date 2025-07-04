package net.coolpixels;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerEnvironmentConfig {
    private static final String CONFIG_FILE = "weekly_world_server_env.json";
    private static String apiBase = null;
    private static String serverSecret = null;
    private static boolean loaded = false;

    public static void loadConfig() {
        if (loaded)
            return;

        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
            if (!Files.exists(configPath)) {
                WeeklyWorld.LOGGER.warn("Server environment config file not found: {}", configPath);
                return;
            }

            String content = Files.readString(configPath);
            JsonObject json = new Gson().fromJson(content, JsonObject.class);

            if (json.has("api_base")) {
                apiBase = json.get("api_base").getAsString();
            }
            if (json.has("server_secret")) {
                serverSecret = json.get("server_secret").getAsString();
            }

            loaded = true;
            WeeklyWorld.LOGGER.info("Server environment config loaded successfully");
        } catch (IOException e) {
            WeeklyWorld.LOGGER.error("Failed to load server environment config", e);
        }
    }

    public static String getApiBase() {
        if (!loaded)
            loadConfig();
        return apiBase;
    }

    public static String getServerSecret() {
        if (!loaded)
            loadConfig();
        return serverSecret;
    }

    public static boolean isConfigured() {
        if (!loaded)
            loadConfig();
        return apiBase != null && serverSecret != null;
    }
}
