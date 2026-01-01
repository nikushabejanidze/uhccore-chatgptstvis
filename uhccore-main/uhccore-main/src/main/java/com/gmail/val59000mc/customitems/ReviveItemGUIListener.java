package com.gmail.val59000mc.customitems;

import com.gmail.val59000mc.commands.ReviveService;
import com.gmail.val59000mc.game.GameManager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class ReviveItemGUIListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onClick(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) return;
		Player player = (Player) e.getWhoClicked();

		Inventory inv = e.getInventory();
		if (inv == null) return;

		InventoryHolder holder = inv.getHolder();
		if (!(holder instanceof ReviveMenuHolder)) return; // ONLY our menu

		e.setCancelled(true); // prevent taking items

		InventoryAction action = e.getAction();
		if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY
			|| action == InventoryAction.HOTBAR_SWAP
			|| action == InventoryAction.HOTBAR_MOVE_AND_READD
			|| action == InventoryAction.COLLECT_TO_CURSOR) {
			return;
		}

		ItemStack clicked = e.getCurrentItem();
		if (clicked == null || clicked.getType() == Material.AIR) return;

		if (clicked.getType() == Material.BARRIER) {
			player.closeInventory();
			return;
		}

		if (clicked.getType() != Material.PLAYER_HEAD) return;

		UUID target = readUuid(clicked);
		if (target == null) return;

		player.closeInventory();

		GameManager gm = GameManager.getGameManager();
		if (gm == null) return;

		boolean reviveWithInventory = true; // your crafts.yml revive-with-inventory: true
		new ReviveService(gm).reviveTeammate(player, target, reviveWithInventory);
	}

	private UUID readUuid(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return null;

		List<String> lore = meta.getLore();
		if (lore == null) return null;

		for (String line : lore) {
			if (line == null) continue;
			String raw = stripColor(line).trim();

			// accepts "UUID: <uuid>" or "UUID:<uuid>"
			if (raw.startsWith("UUID:")) {
				String u = raw.substring("UUID:".length()).trim();
				try {
					return UUID.fromString(u);
				} catch (IllegalArgumentException ignored) {
					return null;
				}
			}
		}
		return null;
	}

	private String stripColor(String s) {
		return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "");
	}
}
