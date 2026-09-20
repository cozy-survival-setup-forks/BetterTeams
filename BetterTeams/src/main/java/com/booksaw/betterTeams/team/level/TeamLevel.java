package com.booksaw.betterTeams.team.level;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;

/**
 * One level a team can be, as set up in levels.yml. It holds the limits a team has at this level and
 * what it costs to reach it.
 */
@Getter
public final class TeamLevel {

	/**
	 * What a level costs to reach.
	 */
	public enum PriceType {
		/**
		 * Taken from the team bank (needs Vault)
		 */
		MONEY,
		/**
		 * Taken from the team score
		 */
		SCORE
	}

	/**
	 * Limits use -1 for "no limit"
	 */
	public static final int UNLIMITED = -1;

	private final int level;
	private final String name;
	private final String iconName;

	private final double price;
	private final PriceType priceType;

	private final int teamLimit;
	private final int maxChests;
	private final int maxWarps;
	private final int maxAdmins;
	private final int maxOwners;
	private final double maxBalance;

	private final List<String> description;
	private final List<String> startCommands;
	private final List<String> endCommands;

	public TeamLevel(int level, String name, String iconName, double price, PriceType priceType, int teamLimit,
	                 int maxChests, int maxWarps, int maxAdmins, int maxOwners, double maxBalance,
	                 List<String> description, List<String> startCommands, List<String> endCommands) {
		this.level = level;
		this.name = name;
		this.iconName = iconName;
		this.price = Math.max(0, price);
		this.priceType = priceType;
		this.teamLimit = teamLimit;
		this.maxChests = maxChests;
		this.maxWarps = maxWarps;
		this.maxAdmins = maxAdmins;
		this.maxOwners = maxOwners;
		this.maxBalance = maxBalance;
		this.description = List.copyOf(description);
		this.startCommands = List.copyOf(startCommands);
		this.endCommands = List.copyOf(endCommands);
	}

	/**
	 * @return the icon of the level in menus, a chest if the name in levels.yml is not an item
	 */
	public Material getIcon() {
		Material material = Material.matchMaterial(iconName);
		return material != null && material.isItem() ? material : Material.CHEST;
	}

	/**
	 * @return what it costs to reach this level from the one before
	 */
	public double getCostValue() {
		return price;
	}

	public boolean isScoreCost() {
		return priceType == PriceType.SCORE;
	}

	public boolean isMoneyCost() {
		return priceType == PriceType.MONEY;
	}

	/**
	 * @return the description lines with colour codes translated
	 */
	public List<String> getColoredLore() {
		return description.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList();
	}
}
