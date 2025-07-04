package net.coolpixels;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ServerApiClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Gson GSON = new Gson();
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private static HttpResponse<String> executeWithRetry(HttpRequest request, HttpResponse.BodyHandler<String> bodyHandler) 
            throws IOException, InterruptedException {
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                WeeklyWorld.LOGGER.debug("HTTP request attempt {} of {}: {}", attempt, MAX_RETRIES, request.uri());
                HttpResponse<String> response = HTTP_CLIENT.send(request, bodyHandler);
                WeeklyWorld.LOGGER.debug("HTTP response received: status {}", response.statusCode());
                return response;
            } catch (IOException e) {
                lastException = e;
                WeeklyWorld.LOGGER.debug("HTTP request attempt {} failed: {}", attempt, e.getMessage());
                
                // If this is a connection exception and we have more retries, wait and try again
                if (attempt < MAX_RETRIES && (e instanceof java.net.ConnectException || 
                    (e.getCause() != null && e.getCause() instanceof java.net.ConnectException))) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                } else {
                    throw e;
                }
            }
        }
        
        throw lastException;
    }

    public static CompletableFuture<Void> sendCompletionAsync(String playerUuid, int week) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendCompletion(playerUuid, week);
            } catch (Exception e) {
                handleApiError(playerUuid, week, e);
            }
        });
    }

    private static void handleApiError(String playerUuid, int week, Exception e) {
        if (e instanceof java.net.ConnectException ||
                (e.getCause() != null && e.getCause() instanceof java.net.ConnectException)) {
            WeeklyWorld.LOGGER.warn("Could not connect to API server for player {} week {} - server may be offline: {}",
                    playerUuid, week, ServerEnvironmentConfig.getApiBase());
        } else if (e.getClass().getSimpleName().contains("TimeoutException")) {
            WeeklyWorld.LOGGER.warn("API request timed out for player {} week {}", playerUuid, week);
        } else {
            WeeklyWorld.LOGGER.error("Failed to send completion to server for player {} week {}",
                    playerUuid, week, e);
        }
    }

    private static void sendCompletion(String playerUuid, int week) throws IOException, InterruptedException {
        if (!ServerEnvironmentConfig.isConfigured()) {
            WeeklyWorld.LOGGER.warn("Server environment not configured, skipping completion submission");
            return;
        }

        String apiBase = ServerEnvironmentConfig.getApiBase();
        String serverSecret = ServerEnvironmentConfig.getServerSecret();
        String apiUrl = apiBase + "/api/challenge/complete";

        WeeklyWorld.LOGGER.debug("Sending completion to API: {} for player {} week {}", apiUrl, playerUuid, week);

        // Create the JSON payload
        JsonObject payload = new JsonObject();
        payload.addProperty("week", week);
        payload.addProperty("playerUUID", playerUuid);
        payload.addProperty("timestamp", System.currentTimeMillis());

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("X-Server-Key", serverSecret)
                .header("User-Agent", "WeeklyWorldChallenge/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .timeout(Duration.ofSeconds(30))
                .build();

        // Send the request
        HttpResponse<String> response = executeWithRetryGeneric(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            WeeklyWorld.LOGGER.info("Successfully sent completion for player {} week {}", playerUuid, week);
        } else {
            WeeklyWorld.LOGGER.error("Failed to send completion for player {} week {}: HTTP {} - {}",
                    playerUuid, week, response.statusCode(), response.body());
        }
    }

    private static <T> HttpResponse<T> executeWithRetryGeneric(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                WeeklyWorld.LOGGER.debug("HTTP request attempt {} of {}: {}", attempt, MAX_RETRIES, request.uri());
                HttpResponse<T> response = HTTP_CLIENT.send(request, bodyHandler);
                WeeklyWorld.LOGGER.debug("HTTP response received: status {}", response.statusCode());
                return response;
            } catch (IOException e) {
                lastException = e;
                WeeklyWorld.LOGGER.debug("HTTP request attempt {} failed: {}", attempt, e.getMessage());

                // If this is a connection exception and we have more retries, wait and try again
                if (attempt < MAX_RETRIES && (e instanceof java.net.ConnectException ||
                        (e.getCause() != null && e.getCause() instanceof java.net.ConnectException))) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                } else {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    public static CompletableFuture<Void> checkPlayerProfileAsync(String playerUuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                checkPlayerProfile(playerUuid);
            } catch (Exception e) {
                handleProfileCheckError(playerUuid, e);
            }
        });
    }

    private static void handleProfileCheckError(String playerUuid, Exception e) {
        if (e instanceof java.net.ConnectException ||
                (e.getCause() != null && e.getCause() instanceof java.net.ConnectException)) {
            WeeklyWorld.LOGGER.warn("Could not connect to API server for profile check after {} attempts - server may be offline or firewall blocking connection: {}",
                    MAX_RETRIES, ServerEnvironmentConfig.getApiBase());
            WeeklyWorld.LOGGER.warn("Note: Some firewalls only allow connections from high port numbers (>45000). Consider checking firewall configuration.");
        } else if (e.getClass().getSimpleName().contains("TimeoutException")) {
            WeeklyWorld.LOGGER.warn("Profile check API request timed out for player {}", playerUuid);
        } else {
            WeeklyWorld.LOGGER.error("Failed to check player profile for player {}", playerUuid, e);
        }
    }

    private static void checkPlayerProfile(String playerUuid) throws IOException, InterruptedException {
        if (!ServerEnvironmentConfig.isConfigured()) {
            WeeklyWorld.LOGGER.debug("Server environment not configured, skipping profile check");
            return;
        }

        String apiBase = ServerEnvironmentConfig.getApiBase();
        String serverSecret = ServerEnvironmentConfig.getServerSecret();
        String apiUrl = apiBase + "/api/profile/minecraft/" + playerUuid;

        WeeklyWorld.LOGGER.debug("Checking player profile: {} for player {}", apiUrl, playerUuid);

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("X-Server-Key", serverSecret)
                .header("User-Agent", "WeeklyWorldChallenge/1.0")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        // Send the request with retry logic
        HttpResponse<String> response = executeWithRetry(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            WeeklyWorld.LOGGER.debug("Player {} has a valid profile", playerUuid);
        } else if (response.statusCode() == 404) {
            WeeklyWorld.LOGGER.info("Player {} does not have a profile - sending registration message", playerUuid);
            // Store the result so we can handle it in the event handler
            ProfileCheckResult.setResult(playerUuid, ProfileCheckResult.Status.NOT_FOUND);
        } else {
            WeeklyWorld.LOGGER.warn("Profile check failed for player {}: HTTP {} - {}",
                    playerUuid, response.statusCode(), response.body());
            ProfileCheckResult.setResult(playerUuid, ProfileCheckResult.Status.ERROR);
        }
    }
}
