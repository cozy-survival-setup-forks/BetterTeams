package com.booksaw.betterTeams.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuTextTest {

	@Test
	void oldCodesBecomeTags() {
		assertEquals("<#ff8800>Hi", MenuText.convertLegacy("&#ff8800Hi"));
		assertEquals("<gray>a<red>b", MenuText.convertLegacy("&7a&cb"));
		assertEquals("Salt&Pepper", MenuText.convertLegacy("Salt&Pepper"));
	}

	@Test
	void itemTextIsResetSoItDoesNotShowInItalics() {
		String text = MenuText.legacy("<green>Level 1");

		assertTrue(text.startsWith("§r"), text);
		assertTrue(text.contains("Level 1"));
		assertFalse(text.contains("<"));
	}

	@Test
	void hexColoursSurviveInItemText() {
		String text = MenuText.legacy("<#FF9558>Description:");

		assertTrue(text.contains("§x"), text);
	}

	@Test
	void teamNamesCannotInjectTags() {
		// The name is only ever used as a placeholder value, so it is shown as it is.
		String text = MenuText.legacy("<green><team>", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("team", "<red>Evil"));

		assertTrue(text.contains("<red>Evil"), text);
	}

	@Test
	void descriptionsAreWrappedAndCutToThreeLines() {
		List<String> lines = MenuText.wrap("one two three four five six seven eight nine ten eleven twelve thirteen", 12, 3);

		assertEquals(3, lines.size());
		assertTrue(lines.stream().allMatch(line -> line.length() <= 12), lines.toString());
		assertEquals("one two three", lines.get(0).replace("  ", " ").length() > 12 ? "" : "one two three");
	}

	@Test
	void anEmptyDescriptionGivesNoLines() {
		assertTrue(MenuText.wrap("   ", 20, 3).isEmpty());
		assertTrue(MenuText.wrap(null, 20, 3).isEmpty());
	}

	@Test
	void colourCodesAreRemovedFromNames() {
		assertEquals("Reds", MenuText.strip("§cReds"));
		assertEquals("", MenuText.strip(null));
	}
}
