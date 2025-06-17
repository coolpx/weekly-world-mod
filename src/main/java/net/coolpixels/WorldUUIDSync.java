package net.coolpixels;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class WorldUUIDSync {
    public static final Identifier WORLD_UUID_PACKET = Identifier.of(WeeklyWorld.MOD_ID, "world_uuid");
    private static final WeakHashMap<World, String> worldIdentifiers = new WeakHashMap<>();
    private static final Map<String, String> persistentWorldIdentifiers = new HashMap<>();
    private static final String DATA_FILE = "world_identifiers.json";
    private static final Gson GSON = new Gson();

    static {
        loadWorldIdentifiers();
    }

    // Custom payload record for the world identifier packet
    public record WorldUUIDPayload(String worldIdentifier) implements CustomPayload {
        public static final CustomPayload.Id<WorldUUIDPayload> ID = new CustomPayload.Id<>(WORLD_UUID_PACKET);
        public static final PacketCodec<PacketByteBuf, WorldUUIDPayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeString(value.worldIdentifier),
                buf -> new WorldUUIDPayload(buf.readString()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        // Register the payload type for server-to-client communication
        PayloadTypeRegistry.playS2C().register(WorldUUIDPayload.ID, WorldUUIDPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendWorldUUID(handler.player);
        });
    }

    public static void saveAllUUIDs() {
        saveWorldIdentifiers();
    }

    // Clean up identifiers for worlds that no longer exist
    public static void cleanupDeletedWorlds() {
        // This method should be called periodically or when worlds are known to be
        // deleted
        // Check the server's saves directory for existing world folders
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File savesDir = new File(configDir.getParentFile(), "saves");

        if (savesDir.exists()) {
            Set<String> existingWorlds = new HashSet<>();
            File[] worldFolders = savesDir.listFiles(File::isDirectory);

            if (worldFolders != null) {
                for (File worldFolder : worldFolders) {
                    String worldName = worldFolder.getName();
                    // Add the world folder name as a valid identifier
                    existingWorlds.add(worldName);
                    // Also add dimension-specific identifiers for backwards compatibility
                    existingWorlds.add(worldName + "_minecraft:overworld");
                    existingWorlds.add(worldName + "_minecraft:the_nether");
                    existingWorlds.add(worldName + "_minecraft:the_end");
                }
            }

            // Remove identifiers for worlds that no longer exist
            persistentWorldIdentifiers.entrySet().removeIf(entry -> !existingWorlds.contains(entry.getKey()));
            saveWorldIdentifiers();
        }
    }

    public static String getOrCreateWorldIdentifier(World world) {
        return worldIdentifiers.computeIfAbsent(world, w -> {
            String worldKey = getWorldKey(w);

            return persistentWorldIdentifiers.computeIfAbsent(worldKey, key -> {
                String newIdentifier = getWorldFolderName(w);
                WeeklyWorld.LOGGER.debug("Creating new world identifier: {} for key: {}", newIdentifier, key);
                saveWorldIdentifiers();
                return newIdentifier;
            });
        });
    }

    private static String getWorldKey(World world) {
        // Use the world folder name and dimension to create a unique key
        // This should be consistent across server restarts
        String folderName = getWorldFolderName(world);
        String dimensionKey = world.getRegistryKey().getValue().toString();
        return folderName + "_" + dimensionKey;
    }

    private static String getWorldFolderName(World world) {
        // Get the actual world folder name (the unique directory name used to save this
        // world)
        if (world.getServer() != null) {
            // Try to get the save directory path
            try {
                // Get the server's root save directory - this gives us the actual folder name
                File rootSaveDir = world.getServer().getSavePath(WorldSavePath.ROOT).toFile();

                if (rootSaveDir != null) {
                    // If the path ends with "." get the parent directory name instead
                    File actualWorldDir = rootSaveDir;
                    if (".".equals(rootSaveDir.getName())) {
                        actualWorldDir = rootSaveDir.getParentFile();
                    }

                    if (actualWorldDir != null) {
                        String folderName = actualWorldDir.getName();

                        if (folderName != null && !folderName.trim().isEmpty()) {
                            WeeklyWorld.LOGGER.debug("Got world folder name: {} for world {}", folderName,
                                    world.getRegistryKey().getValue());
                            return folderName;
                        }
                    }
                }
            } catch (Exception e) {
                WeeklyWorld.LOGGER.warn("Failed to get world folder name from save path for world {}",
                        world.getRegistryKey().getValue(), e);
            }

            // Fallback to save properties level name (may not be unique!)
            if (world.getServer().getSaveProperties() != null) {
                String levelName = world.getServer().getSaveProperties().getLevelName();

                if (levelName != null && !levelName.trim().isEmpty()) {
                    WeeklyWorld.LOGGER.debug("Using level name as fallback: {} for world {}", levelName,
                            world.getRegistryKey().getValue());
                    return levelName;
                }
            }
        }
        // Final fallback to "unknown" if we can't determine the folder name
        WeeklyWorld.LOGGER.warn("Could not determine world folder name for world {}, using 'unknown'",
                world.getRegistryKey().getValue());
        return "unknown";
    }

    private static void loadWorldIdentifiers() {
        File file = getDataFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Map<String, String> data = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {
                }.getType());
                if (data != null) {
                    for (Map.Entry<String, String> entry : data.entrySet()) {
                        persistentWorldIdentifiers.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (IOException e) {
                WeeklyWorld.LOGGER.error("Failed to load world identifiers", e);
            }
        }
    }

    private static void saveWorldIdentifiers() {
        File file = getDataFile();
        file.getParentFile().mkdirs();

        // Save identifiers directly as strings
        Map<String, String> data = new HashMap<>();
        for (Map.Entry<String, String> entry : persistentWorldIdentifiers.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }

        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            WeeklyWorld.LOGGER.error("Failed to save world identifiers", e);
        }
    }

    private static File getDataFile() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        return new File(configDir, DATA_FILE);
    }

    public static void sendWorldUUID(ServerPlayerEntity player) {
        World world = player.getWorld();
        String identifier = getOrCreateWorldIdentifier(world);
        WorldUUIDPayload payload = new WorldUUIDPayload(identifier);
        ServerPlayNetworking.send(player, payload);
    }
}
