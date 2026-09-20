package com.booksaw.betterTeams.menu;

import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.Main;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * /teamlist: every team on the server, click a team to see its members.
 */
public class TeamListMenu implements Menu {

	static final int PAGE_SIZE = 45;
	private static final int SIZE = 54;
	private static final int PREVIOUS = 47;
	private static final int RETURN = 49;
	private static final int NEXT = 51;

	private enum View {TEAMS, MEMBERS}

	private final View view;
	private final int page;
	private final int pageCount;
	/**
	 * The team or player shown in each of the first slots
	 */
	private final List<UUID> entries;
	private final UUID teamId;
	private final int returnPage;
	private Inventory inventory;

	private TeamListMenu(View view, int page, int pageCount, List<UUID> entries, UUID teamId, int returnPage) {
		this.view = view;
		this.page = page;
		this.pageCount = pageCount;
		this.entries = entries;
		this.teamId = teamId;
		this.returnPage = returnPage;
	}

	// ---- opening ----

	/**
	 * Opens the list of teams.
	 */
	public static void openTeams(Player player, int requestedPage) {
		List<Team> teams = Team.getTeamManager().getLoadedTeamListClone().values().stream()
				.distinct()
				.sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
				.toList();

		int pageCount = pageCount(teams.size());
		int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
		List<Team> visible = teams.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).toList();

		ConfigurationSection config = MenuConfig.teamList();
		TeamListMenu menu = new TeamListMenu(View.TEAMS, page, pageCount, visible.stream().map(Team::getID).toList(), null, 0);
		menu.inventory = Bukkit.createInventory(menu, SIZE,
				MenuText.legacy(config.getString("title-teams", "Teams"), pageTags(page, pageCount)));

		for (int slot = 0; slot < visible.size(); slot++) {
			menu.inventory.setItem(slot, teamItem(config, visible.get(slot)));
		}
		if (visible.isEmpty()) {
			ConfigurationSection empty = config.getConfigurationSection("empty");
			if (empty != null) {
				menu.inventory.setItem(22, MenuItems.item(Material.BARRIER, MenuText.legacy(empty.getString("name", "")),
						MenuText.legacy(empty.getStringList("lines")), false));
			}
		}
		menu.addBottomRow(config, "lines-list");
		player.openInventory(menu.inventory);
	}

	/**
	 * Opens the members of a team.
	 *
	 * @param returnPage the page of the team list to go back to
	 */
	public static void openMembers(Player player, UUID teamId, int requestedPage, int returnPage) {
		Team team = Team.getTeam(teamId);
		ConfigurationSection config = MenuConfig.teamList();
		if (team == null) {
			player.sendMessage(MenuText.legacy(config.getString("gone", "That team no longer exists.")));
			openTeams(player, returnPage);
			return;
		}

		List<TeamPlayer> members = members(team);
		int pageCount = pageCount(members.size());
		int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
		List<TeamPlayer> visible = members.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).toList();

		TeamListMenu menu = new TeamListMenu(View.MEMBERS, page, pageCount,
				visible.stream().map(TeamPlayer::getPlayerUUID).toList(), team.getID(), returnPage);
		TagResolver[] titleTags = {Placeholder.unparsed("team", MenuText.strip(team.getName())),
				Placeholder.unparsed("page", String.valueOf(page + 1)), Placeholder.unparsed("pages", String.valueOf(pageCount))};
		menu.inventory = Bukkit.createInventory(menu, SIZE, MenuText.legacy(config.getString("title-members", "Teams"), titleTags));

		for (int slot = 0; slot < visible.size(); slot++) {
			menu.inventory.setItem(slot, memberItem(config, visible.get(slot)));
		}
		menu.addBottomRow(config, "lines-members");
		player.openInventory(menu.inventory);
	}

	private static int pageCount(int entries) {
		return Math.max(1, (entries + PAGE_SIZE - 1) / PAGE_SIZE);
	}

	private static TagResolver[] pageTags(int page, int pageCount) {
		return new TagResolver[]{Placeholder.unparsed("page", String.valueOf(page + 1)),
				Placeholder.unparsed("pages", String.valueOf(pageCount))};
	}

	// ---- items ----

	private void addBottomRow(ConfigurationSection config, String returnLines) {
		Material filler = MenuConfig.material(config, "filler", Material.BLACK_STAINED_GLASS_PANE);
		for (int slot = PAGE_SIZE; slot < SIZE; slot++) {
			inventory.setItem(slot, MenuItems.filler(filler));
		}

		if (page > 0) {
			inventory.setItem(PREVIOUS, button(config.getConfigurationSection("previous"), Material.RED_STAINED_GLASS_PANE, "lines"));
		}
		inventory.setItem(RETURN, button(config.getConfigurationSection("return"), Material.GLOBE_BANNER_PATTERN, returnLines));
		if (page + 1 < pageCount) {
			inventory.setItem(NEXT, button(config.getConfigurationSection("next"), Material.LIME_STAINED_GLASS_PANE, "lines"));
		}
	}

	private static ItemStack button(ConfigurationSection section, Material material, String linesKey) {
		if (section == null) {
			return MenuItems.item(material, " ", List.of(), false);
		}
		return MenuItems.item(material, MenuText.legacy(section.getString("name", " ")),
				MenuText.legacy(section.getStringList(linesKey)), false);
	}

	private static ItemStack teamItem(ConfigurationSection config, Team team) {
		ConfigurationSection section = config.getConfigurationSection("team");
		if (section == null) {
			return new ItemStack(Material.WHITE_BANNER);
		}

		String accent = accent(team.getColor());
		List<String> description = MenuText.wrap(team.getDescription(), 38, 3);
		if (description.isEmpty()) {
			description.add(section.getString("no-description", "No description set."));
		}

		TagResolver[] tags = {
				Placeholder.styling("accent", TextColor.fromHexString(accent)),
				Placeholder.unparsed("team", MenuText.strip(team.getName())),
				Placeholder.unparsed("leader", MenuText.strip(leaderName(team))),
				Placeholder.unparsed("members", String.valueOf(members(team).size()))
		};

		List<String> lore = new ArrayList<>();
		for (String line : section.getStringList("lines")) {
			if (line.contains("<description>")) {
				for (String text : description) {
					lore.add(MenuText.legacy(line.replace("<description>", "<white>" + MenuText.plain(text)), tags));
				}
			} else {
				lore.add(MenuText.legacy(line, tags));
			}
		}
		return MenuItems.item(banner(team.getColor()), MenuText.legacy(section.getString("name", "<team>"), tags), lore, false);
	}

	private static ItemStack memberItem(ConfigurationSection config, TeamPlayer member) {
		ConfigurationSection section = config.getConfigurationSection("member");
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		if (section == null || !(item.getItemMeta() instanceof SkullMeta meta)) {
			return item;
		}

		String name = member.getPlayer().getName();
		String rank;
		String accent;
		switch (member.getRank()) {
			case OWNER -> {
				rank = section.getString("rank-owner", "Owner");
				accent = "#FF7490";
			}
			case ADMIN -> {
				rank = section.getString("rank-admin", "Admin");
				accent = "#FFE05A";
			}
			default -> {
				rank = section.getString("rank-member", "Member");
				accent = "#74C7FF";
			}
		}
		boolean online = member.isOnline();
		TagResolver[] tags = {
				Placeholder.styling("accent", TextColor.fromHexString(accent)),
				Placeholder.styling("status-color", TextColor.fromHexString(online ? "#B0FF83" : "#B6C1C8")),
				Placeholder.unparsed("player", MenuText.strip(name == null ? "Unknown Player" : name)),
				Placeholder.unparsed("rank", rank),
				Placeholder.unparsed("status", section.getString(online ? "online" : "offline", online ? "Online" : "Offline"))
		};

		meta.setOwningPlayer(member.getPlayer());
		MenuItems.decorate(meta, MenuText.legacy(section.getString("name", "<player>"), tags),
				MenuText.legacy(section.getStringList("lines"), tags), false);
		item.setItemMeta(meta);
		return item;
	}

	// ---- team information ----

	/**
	 * The members of a team, owners first, then admins, then everyone else, each group by name.
	 */
	private static List<TeamPlayer> members(Team team) {
		List<TeamPlayer> members = new ArrayList<>();
		for (PlayerRank rank : new PlayerRank[]{PlayerRank.OWNER, PlayerRank.ADMIN, PlayerRank.DEFAULT}) {
			members.addAll(team.getRank(rank));
		}
		return members.stream()
				.filter(distinctByUuid())
				.sorted(Comparator.<TeamPlayer>comparingInt(member -> member.getRank().ordinal())
						.thenComparing(member -> {
							String name = member.getPlayer().getName();
							return name == null ? member.getPlayerUUID().toString() : name;
						}, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	private static java.util.function.Predicate<TeamPlayer> distinctByUuid() {
		java.util.Set<UUID> seen = new java.util.HashSet<>();
		return member -> seen.add(member.getPlayerUUID());
	}

	private static String leaderName(Team team) {
		return team.getRank(PlayerRank.OWNER).stream()
				.map(owner -> owner.getPlayer().getName())
				.filter(java.util.Objects::nonNull)
				.findFirst()
				.orElse("Unknown");
	}

	static Material banner(ChatColor color) {
		if (color == null) {
			return Material.WHITE_BANNER;
		}
		return switch (color) {
			case BLACK -> Material.BLACK_BANNER;
			case DARK_BLUE -> Material.BLUE_BANNER;
			case DARK_GREEN -> Material.GREEN_BANNER;
			case DARK_AQUA -> Material.CYAN_BANNER;
			case DARK_RED, RED -> Material.RED_BANNER;
			case DARK_PURPLE -> Material.PURPLE_BANNER;
			case GOLD -> Material.ORANGE_BANNER;
			case GRAY -> Material.LIGHT_GRAY_BANNER;
			case DARK_GRAY -> Material.GRAY_BANNER;
			case BLUE, AQUA -> Material.LIGHT_BLUE_BANNER;
			case GREEN -> Material.LIME_BANNER;
			case LIGHT_PURPLE -> Material.MAGENTA_BANNER;
			case YELLOW -> Material.YELLOW_BANNER;
			default -> Material.WHITE_BANNER;
		};
	}

	static String accent(ChatColor color) {
		if (color == null) {
			return "#AAA5FF";
		}
		return switch (color) {
			case BLACK -> "#777777";
			case DARK_BLUE -> "#6E82FF";
			case DARK_GREEN -> "#63D471";
			case DARK_AQUA -> "#56D8D2";
			case DARK_RED -> "#FF6B6B";
			case DARK_PURPLE -> "#C58CFF";
			case GOLD -> "#FFB45E";
			case GRAY -> "#C5CCD2";
			case DARK_GRAY -> "#9099A1";
			case BLUE -> "#74C7FF";
			case GREEN -> "#B0FF83";
			case AQUA -> "#7ED5FF";
			case RED -> "#FF7490";
			case LIGHT_PURPLE -> "#F2A3FF";
			case YELLOW -> "#FFE05A";
			default -> "#AAA5FF";
		};
	}

	// ---- clicking ----

	@Override
	public void click(Player player, int slot) {
		switch (slot) {
			case PREVIOUS -> {
				if (page > 0) {
					sound(player, true);
					show(player, page - 1);
				}
			}
			case RETURN -> {
				sound(player, true);
				if (view == View.MEMBERS) {
					openTeams(player, returnPage);
				} else {
					closeOrCommand(player);
				}
			}
			case NEXT -> {
				if (page + 1 < pageCount) {
					sound(player, false);
					show(player, page + 1);
				}
			}
			default -> {
				if (view == View.TEAMS && slot >= 0 && slot < entries.size()) {
					sound(player, false);
					openMembers(player, entries.get(slot), 0, page);
				}
			}
		}
	}

	private void show(Player player, int newPage) {
		if (view == View.TEAMS) {
			openTeams(player, newPage);
		} else {
			openMembers(player, teamId, newPage, returnPage);
		}
	}

	private void closeOrCommand(Player player) {
		String command = MenuConfig.teamList().getConfigurationSection("return") == null ? ""
				: MenuConfig.teamList().getString("return.command", "");
		Bukkit.getScheduler().runTask(Main.plugin, () -> {
			if (!player.isOnline()) {
				return;
			}
			if (command == null || command.isBlank()) {
				player.closeInventory();
			} else {
				player.performCommand(command.startsWith("/") ? command.substring(1) : command);
			}
		});
	}

	private static void sound(Player player, boolean back) {
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
		player.playSound(player.getLocation(), back ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
				SoundCategory.MASTER, back ? 0.3f : 1.0f, 1.0f);
	}

	@Override
	public @NotNull Inventory getInventory() {
		return inventory;
	}

	/**
	 * Used by the command, so it can say the team list could not be loaded.
	 */
	public static void open(Player player) {
		try {
			openTeams(player, 0);
		} catch (RuntimeException e) {
			MessageManager.sendMessage(player, "internalError");
			Main.plugin.getLogger().warning("Could not open the team list: " + e.getMessage());
		}
	}
}
