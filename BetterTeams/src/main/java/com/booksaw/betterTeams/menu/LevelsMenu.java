package com.booksaw.betterTeams.menu;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.team.level.LevelManager;
import com.booksaw.betterTeams.team.level.TeamLevel;
import com.booksaw.betterTeams.util.MoneyUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /team levels: every level with its limits and what it costs to reach. The layout is in menus/levels.yml.
 */
public class LevelsMenu implements Menu {

	private final Map<Integer, List<String>> commands = new HashMap<>();
	private Inventory inventory;

	/**
	 * Opens the menu.
	 *
	 * @param player the player who sees it
	 * @param team   the team whose level is marked, or null to show the levels without marking one
	 */
	public static void open(Player player, @Nullable Team team) {
		LevelsMenu menu = new LevelsMenu();
		menu.build(team);
		player.openInventory(menu.inventory);
	}

	private void build(@Nullable Team team) {
		MenuDef def = MenuConfig.get("levels");
		List<TeamLevel> levels = LevelManager.getLevelList();
		inventory = Bukkit.createInventory(this, def.size, MenuText.legacy(def.title));

		int currentLevel = team == null ? -1 : team.getLevel();
		for (MenuDef.Item item : def.items) {
			if (item.key.equals("level")) {
				for (int i = 0; i < levels.size() && i < item.slots.size(); i++) {
					put(item.slots.get(i), levelItem(def, item, levels.get(i), team, currentLevel), item.commands());
				}
			} else {
				ItemStack stack = MenuItems.item(item.material(Material.STONE), MenuText.legacy(item.name()),
						MenuText.legacy(item.lore()), false);
				for (int slot : item.slots) {
					put(slot, stack, item.commands());
				}
			}
		}
	}

	private void put(int slot, ItemStack stack, List<String> itemCommands) {
		if (slot >= 0 && slot < inventory.getSize()) {
			inventory.setItem(slot, stack);
			commands.put(slot, itemCommands);
		}
	}

	private ItemStack levelItem(MenuDef def, MenuDef.Item item, TeamLevel level, @Nullable Team team, int currentLevel) {
		ConfigurationSection section = item.section;
		TeamLevel next = LevelManager.getNextLevel(level.getLevel());
		String unlimited = def.root.getString("unlimited", "Unlimited");
		String symbol = def.root.getString("money-symbol", "$");

		List<TagResolver> resolvers = new ArrayList<>(List.of(
				Placeholder.unparsed("level", String.valueOf(level.getLevel())),
				Placeholder.unparsed("name", level.getName()),
				Placeholder.unparsed("previous", String.valueOf(level.getLevel() - 1)),
				Placeholder.unparsed("next", String.valueOf(level.getLevel() + 1)),
				Placeholder.unparsed("members", limit(level.getTeamLimit(), unlimited)),
				Placeholder.unparsed("chests", limit(level.getMaxChests(), unlimited)),
				Placeholder.unparsed("admins", limit(level.getMaxAdmins(), unlimited)),
				Placeholder.unparsed("owners", limit(level.getMaxOwners(), unlimited)),
				Placeholder.unparsed("balance", level.getMaxBalance() < 0 ? unlimited : symbol + money(level.getMaxBalance())),
				Placeholder.unparsed("price", price(level, symbol)),
				Placeholder.unparsed("team", team == null ? "" : MenuText.strip(team.getName()))
		));
		if (next != null) {
			resolvers.add(Placeholder.unparsed("next-name", next.getName()));
			resolvers.add(Placeholder.unparsed("next-price", price(next, symbol)));
		}
		TagResolver[] tags = resolvers.toArray(new TagResolver[0]);

		boolean current = level.getLevel() == currentLevel;
		String requirement = level.getLevel() > 1 ? section.getString("requirement", "") : "";
		String rankup = next == null ? section.getString("max", "")
				: section.getString(next.isScoreCost() ? "rankup-score" : "rankup-money", "");

		List<String> lore = new ArrayList<>();
		for (String line : item.lore()) {
			switch (line.trim()) {
				case "<description>" -> lore.addAll(MenuText.legacy(level.getDescription(), tags));
				case "<requirement>" -> addIfSet(lore, requirement, tags);
				case "<rankup>" -> addIfSet(lore, rankup, tags);
				case "<current>" -> addIfSet(lore, current ? section.getString("current", "") : "", tags);
				default -> lore.add(MenuText.legacy(line, tags));
			}
		}

		Material material = item.material().equalsIgnoreCase("icon") ? level.getIcon() : item.material(level.getIcon());
		return MenuItems.item(material, MenuText.legacy(item.name(), tags), lore,
				current && section.getBoolean("glow-current", true));
	}

	private static void addIfSet(List<String> lore, String text, TagResolver[] tags) {
		if (!text.isEmpty()) {
			lore.add(MenuText.legacy(text, tags));
		}
	}

	/**
	 * Whole amounts are written without decimals ($25,000), amounts with cents keep the number of decimals
	 * from config.yml.
	 */
	private static String money(double amount) {
		if (amount == Math.rint(amount)) {
			return MoneyUtils.getFormattedDouble(amount, "0", MoneyUtils.useShortFormatting());
		}
		return MoneyUtils.getFormattedDouble(amount);
	}

	private static String limit(int value, String unlimited) {
		return value < 0 ? unlimited : String.valueOf(value);
	}

	private static String price(TeamLevel level, String symbol) {
		if (level.isScoreCost()) {
			return String.valueOf((long) level.getCostValue());
		}
		return symbol + money(level.getCostValue());
	}

	@Override
	public void click(Player player, int slot) {
		List<String> list = commands.get(slot);
		if (list != null) {
			MenuActions.run(player, list, Map.of("<player>", player.getName()), (tag, argument) -> false);
		}
	}

	@Override
	public @NotNull Inventory getInventory() {
		return inventory;
	}
}
