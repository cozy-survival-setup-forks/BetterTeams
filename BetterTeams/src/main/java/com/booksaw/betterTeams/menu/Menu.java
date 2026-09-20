package com.booksaw.betterTeams.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * A menu of BetterTeams. Everything in it is view only, {@link MenuListener} cancels every click and
 * passes it here.
 */
public interface Menu extends InventoryHolder {

	/**
	 * @param player the player who clicked
	 * @param slot   the slot in the menu (not in the player's own inventory)
	 */
	void click(Player player, int slot);

	default void onClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == event.getView().getTopInventory()) {
			click(player, event.getRawSlot());
		}
	}
}
