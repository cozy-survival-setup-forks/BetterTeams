package com.booksaw.betterTeams.menu;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Builds the items of the menus.
 */
final class MenuItems {

	private MenuItems() {
	}

	static ItemStack item(Material material, String name, List<String> lore, boolean glow) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		decorate(meta, name, lore, glow);
		item.setItemMeta(meta);
		return item;
	}

	static void decorate(ItemMeta meta, String name, List<String> lore, boolean glow) {
		meta.setDisplayName(name);
		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
				ItemFlag.HIDE_DYE, ItemFlag.HIDE_UNBREAKABLE);
		if (glow) {
			meta.setEnchantmentGlintOverride(true);
		}
	}

	static ItemStack filler(Material material) {
		return item(material, " ", List.of(), false);
	}
}
