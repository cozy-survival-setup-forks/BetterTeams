package com.booksaw.betterTeams.menu;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.MessageManager;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /teamlist: every team on the server, click a team to see its members. The layout is in
 * menus/teamlist.yml and menus/members.yml.
 */
public class TeamListMenu implements Menu {

	private enum View {TEAMS, MEMBERS}

	private record Click(List<String> commands, UUID entry) {
	}

	private final View view;
	private final MenuDef def;
	private final int page;
	private final int pageCount;
	private final UUID teamId;
	private final int returnPage;
	private final Map<Integer, Click> clicks = new HashMap<>();
	private Inventory inventory;

	private TeamListMenu(View view, MenuDef def, int page, int pageCount, UUID teamId, int returnPage) {
		this.view = view;
		this.def = def;
		this.page = page;
		this.pageCount = pageCount;
		this.teamId = teamId;
		this.returnPage = returnPage;
	}

	// ---- opening ----

	/**
	 * Opens the list of teams.
	 */
	public static void openTeams(Player player, int requestedPage) {
		MenuDef def = MenuConfig.get("teamlist");
		MenuDef.Item entry = def.item("team");
		int pageSize = pageSize(entry);

		List<Team> teams = Team.getTeamManager().getLoadedTeamListClone().values().stream()
				.distinct()
				.sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
				.toList();

		int pageCount = pageCount(teams.size(), pageSize);
		int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
		List<Team> visible = teams.stream().skip((long) page * pageSize).limit(pageSize).toList();

		TeamListMenu menu = new TeamListMenu(View.TEAMS, def, page, pageCount, null, 0);
		TagResolver[] tags = pageTags(page, pageCount);
		menu.inventory = Bukkit.createInventory(menu, def.size, MenuText.legacy(def.title, tags));

		List<ItemStack> stacks = new ArrayList<>();
		List<UUID> ids = new ArrayList<>();
		for (Team team : visible) {
			stacks.add(teamItem(entry, team));
			ids.add(team.getID());
		}
		menu.draw("team", stacks, ids, teams.isEmpty(), tags);
		player.openInventory(menu.inventory);
	}

	/**
	 * Opens the members of a team.
	 *
	 * @param returnPage the page of the team list to go back to
	 */
	public static void openMembers(Player player, UUID teamId, int requestedPage, int returnPage) {
		Team team = Team.getTeam(teamId);
		MenuDef def = MenuConfig.get("members");
		if (team == null) {
			player.sendMessage(MenuText.legacy(MenuConfig.get("teamlist").root.getString("gone",
					"That team no longer exists.")));
			openTeams(player, returnPage);
			return;
		}

		MenuDef.Item entry = def.item("member");
		int pageSize = pageSize(entry);
		List<TeamPlayer> members = members(team);
		int pageCount = pageCount(members.size(), pageSize);
		int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
		List<TeamPlayer> visible = members.stream().skip((long) page * pageSize).limit(pageSize).toList();

		TeamListMenu menu = new TeamListMenu(View.MEMBERS, def, page, pageCount, team.getID(), returnPage);
		TagResolver[] tags = {Placeholder.unparsed("team", MenuText.strip(team.getName())),
				Placeholder.unparsed("page", String.valueOf(page + 1)), Placeholder.unparsed("pages", String.valueOf(pageCount))};
		menu.inventory = Bukkit.createInventory(menu, def.size, MenuText.legacy(def.title, tags));

		List<ItemStack> stacks = new ArrayList<>();
		for (TeamPlayer member : visible) {
			stacks.add(memberItem(entry, member));
		}
		menu.draw("member", stacks, new ArrayList<>(), false, tags);
		player.openInventory(menu.inventory);
	}

	private static int pageSize(MenuDef.Item entry) {
		return entry == null ? 1 : Math.max(1, entry.slots.size());
	}

	private static int pageCount(int entries, int pageSize) {
		return Math.max(1, (entries + pageSize - 1) / pageSize);
	}

	private static TagResolver[] pageTags(int page, int pageCount) {
		return new TagResolver[]{Placeholder.unparsed("page", String.valueOf(page + 1)),
				Placeholder.unparsed("pages", String.valueOf(pageCount))};
	}

	// ---- drawing ----

	/**
	 * Draws the items of the menu file in order, so later items cover earlier ones.
	 *
	 * @param entryKey the item that is drawn once for every team or member
	 * @param stacks   those items
	 * @param ids      the team behind each of them, empty for members
	 * @param empty    whether there is nothing to list
	 */
	private void draw(String entryKey, List<ItemStack> stacks, List<UUID> ids, boolean empty, TagResolver[] tags) {
		for (MenuDef.Item item : def.items) {
			if (item.key.equals(entryKey)) {
				for (int i = 0; i < stacks.size() && i < item.slots.size(); i++) {
					put(item.slots.get(i), stacks.get(i), item.commands(), i < ids.size() ? ids.get(i) : null);
				}
			} else if (visible(item.requirement(), empty)) {
				ItemStack stack = MenuItems.item(item.material(Material.STONE), MenuText.legacy(item.name(), tags),
						MenuText.legacy(item.lore(), tags), false);
				for (int slot : item.slots) {
					put(slot, stack, item.commands(), null);
				}
			}
		}
	}

	private void put(int slot, ItemStack stack, List<String> commands, UUID entry) {
		if (slot >= 0 && slot < inventory.getSize()) {
			inventory.setItem(slot, stack);
			clicks.put(slot, new Click(commands, entry));
		}
	}

	private boolean visible(String requirement, boolean empty) {
		return switch (requirement.toLowerCase(java.util.Locale.ROOT)) {
			case "has_previous" -> page > 0;
			case "has_next" -> page + 1 < pageCount;
			case "empty" -> empty;
			case "not_empty" -> !empty;
			default -> true;
		};
	}

	private static ItemStack teamItem(MenuDef.Item item, Team team) {
		if (item == null) {
			return new ItemStack(Material.WHITE_BANNER);
		}

		String accent = accent(team.getColor());
		List<String> description = MenuText.wrap(team.getDescription(), 38, 3);
		if (description.isEmpty()) {
			description.add(item.section.getString("no-description", "No description set."));
		}

		TagResolver[] tags = {
				Placeholder.styling("accent", TextColor.fromHexString(accent)),
				Placeholder.unparsed("team", MenuText.strip(team.getName())),
				Placeholder.unparsed("leader", MenuText.strip(leaderName(team))),
				Placeholder.unparsed("members", String.valueOf(members(team).size()))
		};

		List<String> lore = new ArrayList<>();
		for (String line : item.lore()) {
			if (line.contains("<description>")) {
				for (String text : description) {
					lore.add(MenuText.legacy(line.replace("<description>", "<white>" + MenuText.plain(text)), tags));
				}
			} else {
				lore.add(MenuText.legacy(line, tags));
			}
		}
		Material material = item.material().equalsIgnoreCase("banner") ? banner(team.getColor())
				: item.material(Material.WHITE_BANNER);
		return MenuItems.item(material, MenuText.legacy(item.name(), tags), lore, false);
	}

	private static ItemStack memberItem(MenuDef.Item item, TeamPlayer member) {
		ItemStack stack = new ItemStack(item == null ? Material.PLAYER_HEAD : item.material(Material.PLAYER_HEAD));
		if (item == null) {
			return stack;
		}
		ConfigurationSection section = item.section;

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

		if (stack.getItemMeta() instanceof SkullMeta meta) {
			meta.setOwningPlayer(member.getPlayer());
			MenuItems.decorate(meta, MenuText.legacy(item.name(), tags), MenuText.legacy(item.lore(), tags), false);
			stack.setItemMeta(meta);
			return stack;
		}
		return MenuItems.item(stack.getType(), MenuText.legacy(item.name(), tags), MenuText.legacy(item.lore(), tags), false);
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
		Click click = clicks.get(slot);
		if (click == null) {
			return;
		}
		Map<String, String> replace = new HashMap<>();
		Team team = Team.getTeam(click.entry() != null ? click.entry() : teamId);
		if (team != null) {
			replace.put("<team>", MenuText.strip(team.getName()));
		}
		replace.put("<player>", player.getName());
		MenuActions.run(player, click.commands(), replace, (tag, argument) -> handle(player, click, tag, argument));
	}

	private boolean handle(Player player, Click click, String tag, String argument) {
		switch (tag) {
			case "previous" -> {
				if (page > 0) {
					show(player, page - 1);
				}
			}
			case "next" -> {
				if (page + 1 < pageCount) {
					show(player, page + 1);
				}
			}
			case "back" -> {
				if (view == View.MEMBERS) {
					openTeams(player, returnPage);
				} else {
					player.closeInventory();
				}
			}
			case "openmenu" -> {
				if (!argument.equalsIgnoreCase("members")) {
					return false;
				}
				if (view == View.TEAMS && click.entry() != null) {
					openMembers(player, click.entry(), 0, page);
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	private void show(Player player, int newPage) {
		if (view == View.TEAMS) {
			openTeams(player, newPage);
		} else {
			openMembers(player, teamId, newPage, returnPage);
		}
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
