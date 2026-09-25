package com.booksaw.betterTeams.menu;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runs the click_commands of a menu item.
 */
final class MenuActions {

	/**
	 * Lets a menu handle commands that only make sense in it, such as [next]. Returns true when it did.
	 */
	interface Handler {
		boolean handle(String tag, String argument);
	}

	private MenuActions() {
	}

	/**
	 * @param replace text to swap in the commands, for example "&lt;team&gt;" for a team name
	 */
	static void run(Player player, List<String> commands, Map<String, String> replace, Handler handler) {
		if (commands.isEmpty()) {
			return;
		}
		// changing or closing the inventory is done after the click has finished
		Bukkit.getScheduler().runTask(Main.plugin, () -> {
			for (String raw : commands) {
				if (!player.isOnline()) {
					return;
				}
				String line = raw.trim();
				for (Map.Entry<String, String> entry : replace.entrySet()) {
					line = line.replace(entry.getKey(), entry.getValue());
				}
				int end = line.indexOf(']');
				if (!line.startsWith("[") || end < 0) {
					continue;
				}
				String tag = line.substring(1, end).toLowerCase(Locale.ROOT);
				String argument = line.substring(end + 1).trim();
				if (!handler.handle(tag, argument)) {
					builtIn(player, tag, argument);
				}
			}
		});
	}

	private static void builtIn(Player player, String tag, String argument) {
		switch (tag) {
			case "player" -> player.performCommand(argument.startsWith("/") ? argument.substring(1) : argument);
			case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					argument.startsWith("/") ? argument.substring(1) : argument);
			case "close" -> player.closeInventory();
			case "sound" -> sound(player, argument);
			case "openmenu" -> {
				switch (argument.toLowerCase(Locale.ROOT)) {
					case "teamlist" -> TeamListMenu.openTeams(player, 0);
					case "levels" -> LevelsMenu.open(player, Team.getTeam(player));
					default -> Main.plugin.getLogger().warning("Unknown menu in click_commands: " + argument);
				}
			}
			default -> Main.plugin.getLogger().warning("Unknown click command: [" + tag + "]");
		}
	}

	private static void sound(Player player, String argument) {
		String[] parts = argument.split("\\s+");
		Sound sound = Registry.SOUNDS.stream()
				.filter(candidate -> candidate.getKey().getKey().replace('.', '_').equalsIgnoreCase(parts[0]))
				.findFirst().orElse(null);
		if (sound == null) {
			Main.plugin.getLogger().warning("Unknown sound in click_commands: " + parts[0]);
			return;
		}
		player.playSound(player.getLocation(), sound, SoundCategory.MASTER, number(parts, 1), number(parts, 2));
	}

	private static float number(String[] parts, int index) {
		try {
			return index < parts.length ? Float.parseFloat(parts[index]) : 1.0f;
		} catch (NumberFormatException e) {
			return 1.0f;
		}
	}
}
