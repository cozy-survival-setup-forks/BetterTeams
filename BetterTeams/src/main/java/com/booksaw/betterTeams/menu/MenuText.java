package com.booksaw.betterTeams.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the texts in levels.yml and menus.yml into text for items and inventory titles. The texts are
 * MiniMessage, and old style codes such as &amp;7 and &amp;#RRGGBB work as well.
 */
public final class MenuText {

	private static final MiniMessage MINI = MiniMessage.miniMessage();
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
			.character(LegacyComponentSerializer.SECTION_CHAR).hexColors().useUnusualXRepeatedCharacterHexFormat().build();

	private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
	private static final Pattern CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
	private static final Map<Character, String> TAGS = Map.ofEntries(
			Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"), Map.entry('3', "dark_aqua"),
			Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"), Map.entry('6', "gold"), Map.entry('7', "gray"),
			Map.entry('8', "dark_gray"), Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
			Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"), Map.entry('f', "white"),
			Map.entry('k', "obfuscated"), Map.entry('l', "bold"), Map.entry('m', "strikethrough"),
			Map.entry('n', "underlined"), Map.entry('o', "italic"), Map.entry('r', "reset"));

	private MenuText() {
	}

	/**
	 * Turns old style codes into MiniMessage tags.
	 */
	static String convertLegacy(String text) {
		String result = HEX.matcher(text).replaceAll("<#$1>");
		return CODE.matcher(result).replaceAll(match -> Matcher.quoteReplacement(
				"<" + TAGS.get(Character.toLowerCase(match.group(1).charAt(0))) + ">"));
	}

	public static Component component(String template, TagResolver... resolvers) {
		return MINI.deserialize(convertLegacy(template), resolvers);
	}

	/**
	 * Text for an item name, lore line or title. It is reset first so items do not show in italics.
	 */
	public static String legacy(String template, TagResolver... resolvers) {
		return ChatColor.RESET + LEGACY.serialize(component(template, resolvers));
	}

	public static List<String> legacy(List<String> templates, TagResolver... resolvers) {
		List<String> lines = new ArrayList<>(templates.size());
		for (String template : templates) {
			lines.add(legacy(template, resolvers));
		}
		return lines;
	}

	/**
	 * A team name or player name without colour codes, for use as a placeholder value.
	 */
	public static String strip(String input) {
		String stripped = ChatColor.stripColor(input == null ? "" : input);
		return stripped == null ? "" : stripped;
	}

	/**
	 * Plain text from a team name or description, safe to write inside a template.
	 */
	public static String plain(String input) {
		String stripped = ChatColor.stripColor(input == null ? "" : input);
		return MINI.escapeTags(stripped == null ? "" : stripped);
	}

	/**
	 * Splits text into at most {@code maxLines} lines of about {@code width} characters.
	 */
	public static List<String> wrap(String input, int width, int maxLines) {
		String clean = ChatColor.stripColor(input == null ? "" : input);
		clean = clean == null ? "" : clean.replaceAll("\\s+", " ").trim();
		List<String> lines = new ArrayList<>();
		if (clean.isEmpty()) {
			return lines;
		}

		StringBuilder current = new StringBuilder();
		for (String word : clean.split(" ")) {
			if (current.length() > 0 && current.length() + word.length() + 1 > width) {
				lines.add(current.toString());
				current = new StringBuilder();
			}
			if (current.length() > 0) {
				current.append(' ');
			}
			current.append(word);
		}
		if (current.length() > 0) {
			lines.add(current.toString());
		}
		return lines.size() > maxLines ? new ArrayList<>(lines.subList(0, maxLines)) : lines;
	}
}
