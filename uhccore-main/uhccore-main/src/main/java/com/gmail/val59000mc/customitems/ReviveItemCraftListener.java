package com.gmail.val59000mc.customitems;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ReviveItemCraftListener implements Listener {

	private static final Material REVIVE_MATERIAL = Material.NETHER_STAR;
	private static final String REVIVE_LORE_PLAIN = "Craft this item to revive a dead team member";

	public ReviveItemCraftListener() {
	}

	/**
	 * ✅ Allows taking item with LEFT or RIGHT click,
	 * ✅ Blocks crafting if player already has one,
	 * ✅ Blocks SHIFT-click crafting (because it can craft multiple at once).
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCraft(CraftItemEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) return;
		Player player = (Player) e.getWhoClicked();

		ItemStack result = (e.getRecipe() != null) ? e.getRecipe().getResult() : null;
		if (!isReviveItem(result)) return;

		// prevent mass-crafting into inventory
		if (e.isShiftClick()) {
			e.setCancelled(true);
			player.sendMessage("§cYou can only craft one revive item.");
			return;
		}

		// if player already has one, block
		if (hasReviveItem(player)) {
			e.setCancelled(true);
			player.sendMessage("§cYou already crafted a revive item.");
		}
	}

	private boolean hasReviveItem(Player player) {
		ItemStack cursor = player.getItemOnCursor();
		if (isReviveItem(cursor)) return true;

		for (ItemStack it : player.getInventory().getContents()) {
			if (isReviveItem(it)) return true;
		}
		return false;
	}

	private boolean isReviveItem(ItemStack item) {
		if (item == null) return false;
		if (item.getType() != REVIVE_MATERIAL) return false;
		if (!item.hasItemMeta()) return false;

		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.hasLore() || meta.getLore() == null) return false;

		for (String line : meta.getLore()) {
			if (line == null) continue;
			String plain = stripColor(line).trim();
			if (plain.equalsIgnoreCase(REVIVE_LORE_PLAIN)) return true;
		}
		return false;
	}

	private String stripColor(String s) {
		return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "");
	}
}
