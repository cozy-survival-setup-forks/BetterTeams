package com.booksaw.betterTeams.team.level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelManagerTest {

	private static final Logger LOG = Logger.getLogger("test");

	private static YamlConfiguration yaml(String text) throws Exception {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.loadFromString(text);
		return yaml;
	}

	@Test
	void theDefaultFileHasFiveLevelsLikeTheMenuExamples() {
		var stream = getClass().getResourceAsStream("/levels.yml");
		assertNotNull(stream);
		YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));

		LevelManager.load(defaults.getConfigurationSection("levels"), LOG);

		assertEquals(5, LevelManager.getMaxLevel());
		TeamLevel first = LevelManager.getLevel(1);
		assertEquals(5, first.getTeamLimit());
		assertEquals(2, first.getMaxChests());
		assertEquals(1, first.getMaxAdmins());
		assertEquals(50000, first.getMaxBalance());

		TeamLevel last = LevelManager.getFinalLevel();
		assertEquals(20, last.getTeamLimit());
		assertEquals(500000, last.getMaxBalance());
		assertNull(LevelManager.getNextLevel(5));
		assertEquals(25000, LevelManager.getLevel(2).getCostValue());
		assertTrue(LevelManager.getLevel(2).isMoneyCost());
	}

	@Test
	void levelsCanBeAddedWithAnyLimits() throws Exception {
		LevelManager.load(yaml("""
				levels:
				  1:
				    limits: {members: 3, chests: 1, warps: 0, admins: 0, owners: 1, balance: 0}
				  2:
				    price: 40
				    price-type: score
				    limits: {members: -1, chests: -1, warps: -1, admins: -1, owners: 2, balance: -1}
				"""
		).getConfigurationSection("levels"), LOG);

		assertEquals(2, LevelManager.getMaxLevel());
		TeamLevel two = LevelManager.getLevel(2);
		assertTrue(two.isScoreCost());
		assertEquals(40, two.getCostValue());
		assertEquals(-1, two.getTeamLimit());
		assertEquals(2, two.getMaxOwners());
		assertEquals("Level 2", two.getName());
		assertEquals("CHEST", two.getIconName());
	}

	@Test
	void aMissingLevelEndsTheList() throws Exception {
		LevelManager.load(yaml("""
				levels:
				  1: {limits: {members: 5}}
				  2: {limits: {members: 6}}
				  4: {limits: {members: 8}}
				"""
		).getConfigurationSection("levels"), LOG);

		assertEquals(2, LevelManager.getMaxLevel());
		assertFalse(LevelManager.exists(4));
	}

	@Test
	void nothingToLoadStillGivesALevel() {
		LevelManager.load(null, LOG);

		assertEquals(1, LevelManager.getMaxLevel());
		assertNotNull(LevelManager.getLevel(1));
	}

	@Test
	void aTeamWhoseLevelWasRemovedGetsTheClosestLevel() throws Exception {
		LevelManager.load(yaml("levels:\n  1: {}\n  2: {}\n  3: {}\n").getConfigurationSection("levels"), LOG);

		assertEquals(3, LevelManager.getLevelOrClosest(9).getLevel());
		assertEquals(1, LevelManager.getLevelOrClosest(0).getLevel());
		assertEquals(2, LevelManager.getLevelOrClosest(2).getLevel());
	}

	@Test
	void oldConfigLevelsAreConverted() throws Exception {
		YamlConfiguration old = yaml("""
				levels:
				  l1:
				    teamLimit: 10
				    maxChests: 2
				    maxWarps: 3
				    maxBal: -1
				    maxAdmins: 5
				    maxOwners: 2
				    rankLore:
				      - '&7The first level'
				  l2:
				    price: 100s
				    teamLimit: 20
				    startCommands:
				      - say hi
				""");

		YamlConfiguration converted = LevelManager.convertOldLevels(old.getConfigurationSection("levels"));
		LevelManager.load(converted.getConfigurationSection("levels"), LOG);

		TeamLevel one = LevelManager.getLevel(1);
		assertEquals(10, one.getTeamLimit());
		assertEquals(2, one.getMaxOwners());
		assertEquals("&7The first level", one.getDescription().get(0));

		TeamLevel two = LevelManager.getLevel(2);
		assertTrue(two.isScoreCost());
		assertEquals(100, two.getCostValue());
		assertEquals("say hi", two.getStartCommands().get(0));
	}
}
