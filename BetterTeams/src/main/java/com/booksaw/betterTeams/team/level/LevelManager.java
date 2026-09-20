package com.booksaw.betterTeams.team.level;

import com.booksaw.betterTeams.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads the team levels from levels.yml. Servers that still have their levels in config.yml get a levels.yml
 * made from them the first time.
 */
public class LevelManager {

	private static final Map<Integer, TeamLevel> levels = new TreeMap<>();

	private LevelManager() {
	}

	/**
	 * Reloads all levels from levels.yml.
	 */
	public static void reload() {
		File file = new File(Main.plugin.getDataFolder(), "levels.yml");

		if (!file.exists()) {
			FileConfiguration config = Main.plugin.getConfig();
			if (config.isConfigurationSection("levels")) {
				try {
					convertOldConfig(config.getConfigurationSection("levels"), file);
					Main.plugin.getLogger().info("Your team levels were moved from config.yml to levels.yml. " +
							"You can remove the 'levels' section from config.yml.");
				} catch (IOException e) {
					Main.plugin.getLogger().log(Level.WARNING, "Could not create levels.yml from config.yml", e);
				}
			}
			if (!file.exists()) {
				Main.plugin.saveResource("levels.yml", false);
			}
		}

		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		load(yaml.getConfigurationSection("levels"), Main.plugin.getLogger());
	}

	/**
	 * Reads the levels from a config section. Levels have to be numbered 1, 2, 3 and so on, a level after a
	 * gap is ignored.
	 */
	static void load(ConfigurationSection section, Logger logger) {
		levels.clear();

		if (section != null) {
			Map<Integer, TeamLevel> found = new TreeMap<>();
			for (String key : section.getKeys(false)) {
				Integer number = parseNumber(key);
				ConfigurationSection level = section.getConfigurationSection(key);
				if (number == null || number < 1 || level == null) {
					logger.warning("levels.yml: '" + key + "' is not a valid level, levels are numbered 1, 2, 3...");
					continue;
				}
				found.put(number, parseLevel(number, level, logger));
			}

			int expected = 1;
			for (Map.Entry<Integer, TeamLevel> entry : found.entrySet()) {
				if (entry.getKey() != expected) {
					logger.warning("levels.yml: level " + expected + " is missing, so level " + entry.getKey()
							+ " and above are ignored.");
					break;
				}
				levels.put(entry.getKey(), entry.getValue());
				expected++;
			}
		}

		if (levels.isEmpty()) {
			logger.severe("levels.yml has no levels, using a single default level.");
			levels.put(1, new TeamLevel(1, "Level 1", "CHEST", 0, TeamLevel.PriceType.MONEY, 10, 2, 2, -1, 1,
					-1, List.of(), List.of(), List.of()));
		}
	}

	private static Integer parseNumber(String key) {
		try {
			// "l2" was used in config.yml before levels.yml
			return Integer.parseInt(key.toLowerCase(Locale.ROOT).replaceFirst("^l", ""));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static TeamLevel parseLevel(int number, ConfigurationSection level, Logger logger) {
		ConfigurationSection limits = level.getConfigurationSection("limits");
		if (limits == null) {
			limits = level.createSection("limits");
		}

		TeamLevel.PriceType priceType = "score".equalsIgnoreCase(level.getString("price-type", "money"))
				? TeamLevel.PriceType.SCORE : TeamLevel.PriceType.MONEY;

		return new TeamLevel(
				number,
				level.getString("name", "Level " + number),
				level.getString("icon", "CHEST"),
				level.getDouble("price", 0),
				priceType,
				limits.getInt("members", 10),
				limits.getInt("chests", 0),
				limits.getInt("warps", 0),
				limits.getInt("admins", -1),
				limits.getInt("owners", 1),
				limits.getDouble("balance", -1),
				level.getStringList("description"),
				level.getStringList("commands-on-reach"),
				level.getStringList("commands-on-leave")
		);
	}

	/**
	 * Turns the levels of an old config.yml (l1, l2, teamLimit, price: 100s...) into a levels.yml. The menu
	 * texts are copied from the default file.
	 */
	static void convertOldConfig(ConfigurationSection old, File target) throws IOException {
		YamlConfiguration out = convertOldLevels(old);

		try (var defaults = Main.plugin.getResource("levels.yml")) {
			if (defaults != null) {
				YamlConfiguration fresh = YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8));
				ConfigurationSection menu = fresh.getConfigurationSection("menu");
				if (menu != null) {
					out.set("menu", menu);
				}
			}
		}
		out.save(target);
	}

	/**
	 * The levels of an old config.yml in the layout of levels.yml.
	 */
	static YamlConfiguration convertOldLevels(ConfigurationSection old) {
		YamlConfiguration out = new YamlConfiguration();
		int next = 1;
		for (String key : old.getKeys(false)) {
			ConfigurationSection level = old.getConfigurationSection(key);
			if (level == null) {
				continue;
			}
			Integer parsed = parseNumber(key);
			int n = parsed == null ? next : parsed;
			next = n + 1;
			String base = "levels." + n + ".";

			out.set(base + "name", "Level " + n);
			out.set(base + "icon", "CHEST");
			String price = level.getString("price", "0m").trim().toLowerCase(Locale.ROOT);
			double amount = 0;
			try {
				amount = Double.parseDouble(price.substring(0, Math.max(0, price.length() - 1)));
			} catch (RuntimeException ignored) {
				// no price, this is level 1
			}
			out.set(base + "price", amount);
			out.set(base + "price-type", price.endsWith("s") ? "score" : "money");
			out.set(base + "limits.members", level.getInt("teamLimit", 10));
			out.set(base + "limits.chests", level.getInt("maxChests", 0));
			out.set(base + "limits.warps", level.getInt("maxWarps", 0));
			out.set(base + "limits.admins", level.getInt("maxAdmins", -1));
			out.set(base + "limits.owners", level.getInt("maxOwners", 1));
			out.set(base + "limits.balance", level.getDouble("maxBal", -1));
			out.set(base + "description", level.getStringList("rankLore"));
			out.set(base + "commands-on-reach", level.getStringList("startCommands"));
			out.set(base + "commands-on-leave", level.getStringList("endCommands"));
		}
		return out;
	}

	/**
	 * Retrieves a specific level object.
	 *
	 * @param level The level number.
	 * @return The TeamLevel object, or null if there is no such level
	 */
	public static TeamLevel getLevel(int level) {
		return levels.get(level);
	}

	/**
	 * A team keeps working if its level was removed from levels.yml: it gets the closest level below it,
	 * or the highest level if it was above all of them.
	 */
	public static TeamLevel getLevelOrClosest(int level) {
		TeamLevel exact = levels.get(level);
		if (exact != null) {
			return exact;
		}
		return levels.get(Math.max(1, Math.min(level, getMaxLevel())));
	}

	public static TeamLevel getNextLevel(int currentLevel) {
		return levels.get(currentLevel + 1);
	}

	public static boolean hasNextLevel(int currentLevel) {
		return levels.containsKey(currentLevel + 1);
	}

	public static TeamLevel getFinalLevel() {
		return levels.get(getMaxLevel());
	}

	public static int getMaxLevel() {
		return levels.keySet().stream().mapToInt(v -> v).max().orElse(1);
	}

	public static boolean exists(int level) {
		return levels.containsKey(level);
	}

	/**
	 * Returns an unmodifiable view of all loaded levels, in order.
	 *
	 * @return Map of Level ID -> TeamLevel
	 */
	public static Map<Integer, TeamLevel> getLevels() {
		return Collections.unmodifiableMap(levels);
	}

	/**
	 * @return all levels in order
	 */
	public static List<TeamLevel> getLevelList() {
		return new ArrayList<>(levels.values());
	}
}
