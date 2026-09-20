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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * /team levels: every level with its limits and what it costs to reach.
 */
public class LevelsMenu implements Menu {

	private static final int PER_ROW = 7;

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
		ConfigurationSection config = MenuConfig.levelsMenu();
		List<TeamLevel> levels = LevelManager.getLevelList();

		int itemRows = Math.max(1, (levels.size() + PER_ROW - 1) / PER_ROW);
		int rows = Math.min(6, itemRows + 2);
		inventory = Bukkit.createInventory(this, rows * 9, MenuText.legacy(config.getString("title", "Team Levels")));

		Material filler = MenuConfig.material(config, "filler", Material.BLACK_STAINED_GLASS_PANE);
		for (int slot = 0; slot < inventory.getSize(); slot++) {
			inventory.setItem(slot, MenuItems.filler(filler));
		}

		int currentLevel = team == null ? -1 : team.getLevel();
		for (int i = 0; i < levels.size() && i < (rows - 2) * PER_ROW; i++) {
			int row = 1 + i / PER_ROW;
			int inRow = Math.min(PER_ROW, levels.size() - (i / PER_ROW) * PER_ROW);
			int column = 1 + (PER_ROW - inRow) / 2 + i % PER_ROW;
			inventory.setItem(row * 9 + column, item(config, levels.get(i), team, currentLevel));
		}
	}

	private org.bukkit.inventory.ItemStack item(ConfigurationSection config, TeamLevel level, @Nullable Team team, int currentLevel) {
		TeamLevel next = LevelManager.getNextLevel(level.getLevel());
		String unlimited = config.getString("unlimited", "Unlimited");
		String symbol = config.getString("money-symbol", "$");

		List<TagResolver> resolvers = new ArrayList<>(List.of(
				Placeholder.unparsed("level", String.valueOf(level.getLevel())),
				Placeholder.unparsed("name", level.getName()),
				Placeholder.unparsed("previous", String.valueOf(level.getLevel() - 1)),
				Placeholder.unparsed("next", String.valueOf(level.getLevel() + 1)),
				Placeholder.unparsed("members", limit(level.getTeamLimit(), unlimited)),
				Placeholder.unparsed("chests", limit(level.getMaxChests(), unlimited)),
				Placeholder.unparsed("warps", limit(level.getMaxWarps(), unlimited)),
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

		List<String> lore = new ArrayList<>(MenuText.legacy(config.getStringList("lines"), tags));
		lore.add(MenuText.legacy(""));
		if (!level.getDescription().isEmpty()) {
			lore.addAll(MenuText.legacy(level.getDescription(), tags));
		}
		if (level.getLevel() > 1) {
			lore.add(MenuText.legacy(config.getString("requirement", ""), tags));
		}
		lore.add(MenuText.legacy(""));
		if (next == null) {
			lore.add(MenuText.legacy(config.getString("max", ""), tags));
		} else {
			lore.add(MenuText.legacy(config.getString(next.isScoreCost() ? "next-score" : "next-money", ""), tags));
		}

		boolean current = level.getLevel() == currentLevel;
		if (current) {
			lore.add(MenuText.legacy(""));
			lore.add(MenuText.legacy(config.getString("current", ""), tags));
		}

		return MenuItems.item(level.getIcon(), MenuText.legacy(config.getString("name", "<name>"), tags), lore, current);
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
		// view only
	}

	@Override
	public @NotNull Inventory getInventory() {
		return inventory;
	}
}
