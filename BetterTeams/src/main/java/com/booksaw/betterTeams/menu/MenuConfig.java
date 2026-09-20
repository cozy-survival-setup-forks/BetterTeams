package com.booksaw.betterTeams.menu;

import com.booksaw.betterTeams.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The texts of the menus, from menus.yml and the menu section of levels.yml. Anything missing from a
 * file that was made by an older version falls back to the default.
 */
public final class MenuConfig {

	private static YamlConfiguration menus = new YamlConfiguration();
	private static YamlConfiguration levels = new YamlConfiguration();

	private MenuConfig() {
	}

	public static void reload() {
		menus = load("menus.yml");
		levels = load("levels.yml");
	}

	private static YamlConfiguration load(String name) {
		File file = new File(Main.plugin.getDataFolder(), name);
		if (!file.exists()) {
			Main.plugin.saveResource(name, false);
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		try (var stream = Main.plugin.getResource(name)) {
			if (stream != null) {
				yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
			}
		} catch (java.io.IOException e) {
			Main.plugin.getLogger().warning("Could not read the default " + name + ": " + e.getMessage());
		}
		return yaml;
	}

	/**
	 * @return the teamlist section of menus.yml
	 */
	public static ConfigurationSection teamList() {
		return section(menus, "teamlist");
	}

	/**
	 * @return the menu section of levels.yml
	 */
	public static ConfigurationSection levelsMenu() {
		return section(levels, "menu");
	}

	private static ConfigurationSection section(YamlConfiguration yaml, String path) {
		ConfigurationSection section = yaml.getConfigurationSection(path);
		return section != null ? section : yaml.createSection(path);
	}

	public static Material material(ConfigurationSection section, String path, Material fallback) {
		Material material = Material.matchMaterial(section.getString(path, fallback.name()));
		return material != null && material.isItem() ? material : fallback;
	}
}
