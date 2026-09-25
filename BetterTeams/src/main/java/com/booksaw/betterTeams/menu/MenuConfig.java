package com.booksaw.betterTeams.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The menus, loaded from the files in the menus folder. A file that is missing is written from the
 * default when the menu is first needed.
 */
public final class MenuConfig {

	private static final Map<String, MenuDef> MENUS = new HashMap<>();

	private MenuConfig() {
	}

	public static void reload() {
		MENUS.clear();
		for (String name : List.of("teamlist", "members", "levels")) {
			MENUS.put(name, MenuDef.load(name));
		}
	}

	static MenuDef get(String name) {
		return MENUS.computeIfAbsent(name, MenuDef::load);
	}
}
