package com.booksaw.betterTeams.menu;

import com.booksaw.betterTeams.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * One menu file, laid out like a DeluxeMenus menu: a title, a size and a list of items with a material,
 * slots, display name, lore and click commands.
 */
final class MenuDef {

	/**
	 * One entry under items.
	 */
	static final class Item {
		final String key;
		final ConfigurationSection section;
		final List<Integer> slots = new ArrayList<>();

		private Item(String key, ConfigurationSection section) {
			this.key = key;
			this.section = section;
			if (section.contains("slot")) {
				addSlots(String.valueOf(section.get("slot")));
			}
			for (Object slot : section.getList("slots", List.of())) {
				addSlots(String.valueOf(slot));
			}
		}

		/**
		 * Accepts a number or a range such as 45-53.
		 */
		private void addSlots(String text) {
			try {
				String trimmed = text.trim();
				int dash = trimmed.indexOf('-', 1);
				if (dash < 0) {
					slots.add(Integer.parseInt(trimmed));
					return;
				}
				int from = Integer.parseInt(trimmed.substring(0, dash).trim());
				int to = Integer.parseInt(trimmed.substring(dash + 1).trim());
				for (int slot = Math.min(from, to); slot <= Math.max(from, to); slot++) {
					slots.add(slot);
				}
			} catch (NumberFormatException e) {
				Main.plugin.getLogger().warning("Menu item " + key + " has a slot that is not a number: " + text);
			}
		}

		String material() {
			return section.getString("material", "STONE");
		}

		Material material(Material fallback) {
			Material material = Material.matchMaterial(material());
			return material != null && material.isItem() ? material : fallback;
		}

		String name() {
			return section.getString("display_name", " ");
		}

		List<String> lore() {
			return section.getStringList("lore");
		}

		List<String> commands() {
			return section.getStringList("click_commands");
		}

		String requirement() {
			return section.getString("view_requirement", "");
		}
	}

	final String title;
	final int size;
	final ConfigurationSection root;
	final List<Item> items = new ArrayList<>();

	private MenuDef(YamlConfiguration yaml) {
		this.root = yaml;
		this.title = yaml.getString("menu_title", "Teams");
		this.size = Math.max(1, Math.min(6, (yaml.getInt("size", 54) + 8) / 9)) * 9;
		ConfigurationSection section = yaml.getConfigurationSection("items");
		if (section != null) {
			for (String key : section.getKeys(false)) {
				ConfigurationSection item = section.getConfigurationSection(key);
				if (item != null) {
					items.add(new Item(key, item));
				}
			}
		}
	}

	Item item(String key) {
		return items.stream().filter(item -> item.key.equals(key)).findFirst().orElse(null);
	}

	static MenuDef load(String name) {
		String resource = "menus/" + name + ".yml";
		File file = new File(Main.plugin.getDataFolder(), resource);
		if (!file.exists()) {
			Main.plugin.saveResource(resource, false);
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		try (var stream = Main.plugin.getResource(resource)) {
			if (stream != null) {
				yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
			}
		} catch (IOException e) {
			Main.plugin.getLogger().warning("Could not read the default " + resource + ": " + e.getMessage());
		}
		return new MenuDef(yaml);
	}
}
