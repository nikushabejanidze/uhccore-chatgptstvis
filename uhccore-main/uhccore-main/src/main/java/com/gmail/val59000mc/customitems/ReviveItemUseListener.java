package com.gmail.val59000mc.customitems;

import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.players.PlayerManager;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReviveItemUseListener implements Listener {

	private static final Material REVIVE_MATERIAL = Material.NETHER_STAR;

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent e) {
		Action a = e.getAction();
		if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

		ItemStack item = e.getItem();
		if (!isReviveItem(item)) return;

		e.setCancelled(true); // important: works in air and blocks too

		openMenu(e.getPlayer());
	}

	private boolean isReviveItem(ItemStack item) {
		if (item == null || item.getType() != REVIVE_MATERIAL) return false;
		if (!item.hasItemMeta()) return false;

		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.hasLore() || meta.getLore() == null) return false;

		// You can make this stricter if you want.
		// Right now we just require the lore line you showed in crafts.yml
		for (String line : meta.getLore()) {
			if (line == null) continue;
			String plain = stripColor(line);
			if (plain.toLowerCase().contains("revive a dead team member")) {
				return true;
			}
		}
		return false;
	}

	private void openMenu(Player player) {
		GameManager gm = GameManager.getGameManager();
		if (gm == null) return;

		PlayerManager pm = gm.getPlayerManager();
		UhcPlayer uhc = pm.getUhcPlayer(player);
		if (uhc == null) return;

		UhcTeam team = uhc.getTeam();

		Inventory inv = Bukkit.createInventory(new ReviveMenuHolder(), 27, "Revive Teammate");

		List<UhcPlayer> deadTeammates = new ArrayList<UhcPlayer>();
		if (team != null) {
			for (UhcPlayer p : pm.getPlayersList()) {
				if (!p.isDead()) continue;
				if (p.getTeam() == null) continue;
				if (p.getTeam().getTeamNumber() != team.getTeamNumber()) continue;
				deadTeammates.add(p);
			}
		}

		if (deadTeammates.isEmpty()) {
			ItemStack barrier = new ItemStack(Material.BARRIER);
			ItemMeta m = barrier.getItemMeta();
			if (m != null) {
				m.setDisplayName("§cNo dead teammates");
				List<String> lore = new ArrayList<String>();
				lore.add("§7You have no dead teammates to revive.");
				m.setLore(lore);
				barrier.setItemMeta(m);
			}
			inv.setItem(13, barrier);
			player.openInventory(inv);
			return;
		}

		int slot = 10;
		for (UhcPlayer dead : deadTeammates) {
			if (slot >= inv.getSize()) break;

			ItemStack head = HeadUtils.createPlayerHead(dead.getUuid(), dead.getRealName());
			ItemMeta meta = head.getItemMeta();
			if (meta != null) {
				meta.setDisplayName("§e" + dead.getRealName());
				List<String> lore = new ArrayList<String>();
				lore.add("§7Click to revive this teammate.");
				lore.add("§8UUID:" + dead.getUuid().toString()); // store UUID for click
				meta.setLore(lore);
				head.setItemMeta(meta);
			}

			inv.setItem(slot, head);
			slot++;
		}

		player.openInventory(inv);
	}

	private String stripColor(String s) {
		return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "");
	}
}
