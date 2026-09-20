package com.booksaw.betterTeams.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

/**
 * Makes menus view only: nothing can be taken out or put in.
 */
public class MenuListener implements Listener {

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
			event.setCancelled(true);
			menu.onClick(event);
		}
	}

	@EventHandler
	public void onDrag(InventoryDragEvent event) {
		if (event.getView().getTopInventory().getHolder() instanceof Menu) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onMove(InventoryMoveItemEvent event) {
		if (event.getDestination().getHolder() instanceof Menu || event.getSource().getHolder() instanceof Menu) {
			event.setCancelled(true);
		}
	}
}
