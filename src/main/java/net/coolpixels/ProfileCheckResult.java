package net.coolpixels;

import java.util.concurrent.ConcurrentHashMap;

public class ProfileCheckResult {
    private static final ConcurrentHashMap<String, Status> results = new ConcurrentHashMap<>();

    public enum Status {
        NOT_FOUND,
        ERROR
    }

    public static void setResult(String playerUuid, Status status) {
        results.put(playerUuid, status);
    }

    public static Status getAndRemoveResult(String playerUuid) {
        return results.remove(playerUuid);
    }

    public static void clearResult(String playerUuid) {
        results.remove(playerUuid);
    }
}
