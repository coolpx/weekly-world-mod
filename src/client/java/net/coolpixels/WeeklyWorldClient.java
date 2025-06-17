package net.coolpixels;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WeeklyWorldClient implements ClientModInitializer {
	private static MinecraftClient client = MinecraftClient.getInstance();

	@Override
	public void onInitializeClient() {
		WeeklyWorld.LOGGER.info("Initializing Weekly World client");
		WorldUUIDSyncClient.register();
		registerEvents();
	}

	// validation
	public static boolean isOperator(ClientPlayerEntity player) {
		// check if player is operator
		if (player == null) {
			WeeklyWorld.LOGGER.warn("Player is null, cannot check if operator");
			return false;
		}
		return player.hasPermissionLevel(2);
	}

	public static boolean areRestrictionsMet(ClientPlayerEntity player) {
		// check if all restrictions are met
		if (player == null) {
			WeeklyWorld.LOGGER.warn("Player is null, cannot check restrictions");
			return false;
		}
		List<Map<String, Object>> restrictions = ChallengeData.getRestrictions();
		for (Map<String, Object> restriction : restrictions) {
			String type = (String) restriction.get("type");
			String content = (String) restriction.get("content");
			if (!ChallengeData.restrictionMet(client, type, content)) {
				return false;
			}
		}
		return true;
	}

	public static boolean canCompleteObjectives(ClientPlayerEntity player) {
		// check if player is operator or restrictions are not met
		if (player == null) {
			WeeklyWorld.LOGGER.warn("Player is null, cannot check if objectives can be completed");
			return false;
		}
		return !isOperator(player) && areRestrictionsMet(player);
	}

	// warnings
	public static void warnCommandsEnabled(ClientPlayerEntity player) {
		player.sendMessage(
				Text.literal("Objectives cannot be completed while commands are enabled.")
						.formatted(Formatting.RED),
				false);
	}

	public static void warnRestrictionsNotMet(ClientPlayerEntity player) {
		player.sendMessage(
				Text.literal("Some restrictions are not met. Objectives cannot be completed until they are.")
						.formatted(Formatting.RED),
				false);
	}

	// main
	public static void registerEvents() {
		WeeklyWorld.LOGGER.info("Registering client events");

		// tick (for permission checks)
		final boolean[] lastIsOperator = { false };
		ClientTickEvents.END_WORLD_TICK.register(listener -> {
			// check if player is operator
			assert client.player != null;
			boolean isOperator = isOperator(client.player);

			// if operator status changed, warn player
			if (isOperator != lastIsOperator[0]) {
				lastIsOperator[0] = isOperator;
				if (isOperator) {
					// player became operator
					warnCommandsEnabled(client.player);
				} else {
					// player is no longer operator
					client.player.sendMessage(
							Text.literal("You are no longer an operator. Objectives can now be completed.")
									.formatted(Formatting.GREEN),
							false);
				}
			}
		});

		// world join
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			// Clean up data for deleted worlds
			ChallengeData.cleanupDeletedWorlds(client);

			// send greeting
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			player.sendMessage(
					Text.literal("Welcome to Weekly World!").formatted(Formatting.GOLD,
							Formatting.BOLD),
					false);

			// Schedule delayed objective display to wait for world identifier
			scheduleDelayedObjectiveDisplay(player, 3000); // Wait up to 3 seconds

			// send restrictions
			List<Map<String, Object>> restrictions = ChallengeData.getRestrictions();
			boolean allRestrictionsMet = true;
			if (!restrictions.isEmpty()) {
				player.sendMessage(Text.literal("Restrictions:").formatted(Formatting.BOLD), false);
				for (Map<String, Object> restriction : restrictions) {
					String type = (String) restriction.get("type");
					String content = (String) restriction.get("content");
					boolean met = ChallengeData.restrictionMet(client, type, content);
					if (!met)
						allRestrictionsMet = false;
					player.sendMessage(
							Text.literal(String.format("%s %s", met ? "☑" : "☐",
									ChallengeData.formatRestriction(type, content))),
							false);
				}
			}

			if (!allRestrictionsMet) {
				warnRestrictionsNotMet(player);
			}

			// check if player is operator
			assert client.player != null;
			boolean isOperator = isOperator(client.player);
			lastIsOperator[0] = isOperator;
			if (isOperator) {
				warnCommandsEnabled(player);
			}
		});

		// world disconnect - clear world identifier
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			WorldUUIDSyncClient.clearWorldIdentifier();
		});
	}

	public static void reportEvent(String type, String value) {
		// log to console
		WeeklyWorld.LOGGER.info("Reporting event with type {} and value {} (valid: {})", type, value,
				canCompleteObjectives(client.player));

		// check if event matches an objective
		for (Map<String, Object> objective : ChallengeData.getObjectives()) {
			if (objective.get("type").equals(type) && objective.get("content").equals(value)) {
				// check if player can complete objectives
				if (canCompleteObjectives(client.player)) {
					// mark objective as completed
					ChallengeData.markObjectiveCompleted(client, type, value);
					client.player.sendMessage(
							Text.literal(String.format("Objective completed: %s",
									ChallengeData.formatObjective(type, value)))
									.formatted(Formatting.GREEN),
							false);
				} else {
					warnRestrictionsNotMet(client.player);
				}
				return;
			}
		}
	}

	public static void displayObjectivesWithCorrectStatus() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null)
			return;

		ClientPlayerEntity player = client.player;

		// Display objectives with correct completion status
		List<Map<String, Object>> objectives = ChallengeData.getObjectives();
		player.sendMessage(Text.literal("Objectives:").formatted(Formatting.BOLD), false);
		for (Map<String, Object> objective : objectives) {
			String type = (String) objective.get("type");
			String content = (String) objective.get("content");
			boolean completed = ChallengeData.isObjectiveCompleted(client, type, content);
			player.sendMessage(
					Text.literal(String.format("%s %s", completed ? "☑" : "☐",
							ChallengeData.formatObjective(type, content))),
					false);
		}
	}

	private static void scheduleDelayedObjectiveDisplay(ClientPlayerEntity player, long delayMs) {
		AtomicBoolean objectivesDisplayed = new AtomicBoolean(false);

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// Check if we have received the world identifier
				String worldIdentifier = WorldUUIDSyncClient.getCurrentWorldIdentifier();

				if (worldIdentifier != null) {
					// We have the world identifier, display objectives with correct status
					MinecraftClient.getInstance().execute(() -> {
						if (!objectivesDisplayed.getAndSet(true)) {
							displayObjectivesWithCorrectStatus();
						}
					});
				} else {
					// Still no world identifier, display with "will be updated shortly" message
					MinecraftClient.getInstance().execute(() -> {
						if (!objectivesDisplayed.getAndSet(true)) {
							displayObjectivesWithPendingStatus(player);
						}
					});
				}
				timer.cancel();
			}
		}, delayMs);

		// Also check periodically in case the identifier arrives before the delay
		Timer checkTimer = new Timer();
		checkTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				String worldIdentifier = WorldUUIDSyncClient.getCurrentWorldIdentifier();
				if (worldIdentifier != null) {
					MinecraftClient.getInstance().execute(() -> {
						if (!objectivesDisplayed.getAndSet(true)) {
							displayObjectivesWithCorrectStatus();
						}
					});
					checkTimer.cancel();
					timer.cancel();
				}
			}
		}, 100, 100); // Check every 100ms
	}

	private static void displayObjectivesWithPendingStatus(ClientPlayerEntity player) {
		List<Map<String, Object>> objectives = ChallengeData.getObjectives();
		player.sendMessage(Text.literal(String.format("Objective%s:", objectives.size() == 1 ? "" : "s"))
				.formatted(Formatting.BOLD), false);
		for (Map<String, Object> objective : objectives) {
			String type = (String) objective.get("type");
			String content = (String) objective.get("content");
			// Always show unchecked initially, will be updated when identifier arrives
			player.sendMessage(
					Text.literal(String.format("☐ %s", ChallengeData.formatObjective(type, content))),
					false);
		}
		player.sendMessage(Text.literal("(Completion status will be updated shortly...)").formatted(Formatting.GRAY,
				Formatting.ITALIC), false);
	}
}