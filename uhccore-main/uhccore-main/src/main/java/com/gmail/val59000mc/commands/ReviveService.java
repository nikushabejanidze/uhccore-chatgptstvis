package com.gmail.val59000mc.commands;

import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.game.GameState;
import com.gmail.val59000mc.players.PlayerManager;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReviveService {

	private final GameManager gameManager;
	private final PlayerManager playerManager;

	// ✅ In-memory revive tracker: once a player is revived, they can't be revived again.
	// Resets on server restart (usually fine for UHC).
	private static final Set<UUID> ALREADY_REVIVED = ConcurrentHashMap.newKeySet();

	// Identify revive item (must match your craft lore)
	private static final Material REVIVE_MATERIAL = Material.NETHER_STAR;
	private static final String REVIVE_LORE_PLAIN = "Craft this item to revive a dead team member";

	public ReviveService(GameManager gameManager) {
		this.gameManager = gameManager;
		this.playerManager = gameManager.getPlayerManager();
	}

	/**
	 * Admin revive (used by /revive). No team restrictions.
	 */
	public boolean reviveByName(CommandSender sender, String name, boolean spawnWithItems) {
		if (!validateGameState(sender)) return true;

		OfflinePlayer target = findOnlineOrOfflinePlayer(name);
		if (target == null) {
			sender.sendMessage(ChatColor.RED + "Player not found!");
			return true;
		}

		if (!isUhcPlayerDead(target.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "Player is not dead!");
			return true;
		}

		// ✅ block second revive
		if (ALREADY_REVIVED.contains(target.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "This player has already been revived once and cannot be revived again.");
			return true;
		}

		playerManager.revivePlayer(target.getUniqueId(), target.getName(), spawnWithItems);
		ALREADY_REVIVED.add(target.getUniqueId());

		if (target.isOnline()) {
			sender.sendMessage(ChatColor.GREEN + target.getName() + " has been revived!");
		} else {
			sender.sendMessage(ChatColor.GREEN + target.getName() + " can now join the game!");
		}
		return true;
	}

	/**
	 * Team revive (used by revive item GUI). Requires same team.
	 * ✅ Does NOT use commands and does NOT require permissions.
	 * ✅ Consumes revive item on success.
	 * ✅ Prevents reviving same teammate twice.
	 */
	public boolean reviveTeammate(Player reviver, UUID targetUuid, boolean spawnWithItems) {
		if (reviver == null) return true;
		if (!validateGameState(reviver)) return true;

		UhcPlayer reviverUhc = playerManager.getUhcPlayer(reviver);
		if (reviverUhc == null) {
			reviver.sendMessage(ChatColor.RED + "You are not in the UHC game!");
			return true;
		}

		UhcTeam reviverTeam = reviverUhc.getTeam();
		if (reviverTeam == null) {
			reviver.sendMessage(ChatColor.RED + "You are not in a team!");
			return true;
		}

		UhcPlayer targetUhc = getUhcPlayerByUuid(targetUuid);
		if (targetUhc == null) {
			reviver.sendMessage(ChatColor.RED + "Player not found!");
			return true;
		}

		if (!targetUhc.isDead()) {
			reviver.sendMessage(ChatColor.RED + "That player is not dead!");
			return true;
		}

		UhcTeam targetTeam = targetUhc.getTeam();
		if (targetTeam == null || targetTeam.getTeamNumber() != reviverTeam.getTeamNumber()) {
			reviver.sendMessage(ChatColor.RED + "You can only revive your teammates!");
			return true;
		}

		// ✅ block second revive
		if (ALREADY_REVIVED.contains(targetUuid)) {
			reviver.sendMessage(ChatColor.RED + "This teammate has already been revived once and cannot be revived again.");
			return true;
		}

		String targetName = targetUhc.getRealName();
		if (targetName == null || targetName.isEmpty()) {
			OfflinePlayer off = Bukkit.getOfflinePlayer(targetUuid);
			targetName = off.getName() != null ? off.getName() : "Player";
		}

		// ✅ revive
		playerManager.revivePlayer(targetUuid, targetName, spawnWithItems);
		ALREADY_REVIVED.add(targetUuid);

		// ✅ consume ONE revive item from reviver (main hand -> offhand -> inventory search)
		consumeOneReviveItem(reviver);

		reviver.sendMessage(ChatColor.GREEN + targetName + " has been revived!");
		return true;
	}

	private boolean validateGameState(CommandSender sender) {
		GameState st = gameManager.getGameState();
		if (st != GameState.PLAYING && st != GameState.DEATHMATCH) {
			sender.sendMessage(ChatColor.RED + "You can only use this while playing!");
			return false;
		}
		return true;
	}

	private boolean isUhcPlayerDead(UUID uuid) {
		return playerManager.getPlayersList().stream()
			.filter(UhcPlayer::isDead)
			.anyMatch(p -> p.getUuid().equals(uuid));
	}

	private UhcPlayer getUhcPlayerByUuid(UUID uuid) {
		for (UhcPlayer p : playerManager.getPlayersList()) {
			if (p.getUuid().equals(uuid)) return p;
		}
		return null;
	}

	private OfflinePlayer findOnlineOrOfflinePlayer(String name) {
		Player online = Bukkit.getPlayer(name);
		if (online != null) return online;

		for (UhcPlayer uhcPlayer : playerManager.getPlayersList()) {
			if (!uhcPlayer.getRealName().equals(name)) continue;

			OfflinePlayer off = Bukkit.getOfflinePlayer(uhcPlayer.getUuid());
			if (off.hasPlayedBefore()) return off;
		}
		return null;
	}

	private void consumeOneReviveItem(Player player) {
		// main hand
		ItemStack main = player.getInventory().getItemInMainHand();
		if (isReviveItem(main)) {
			decrementOne(player, main, true);
			return;
		}

		// off hand
		ItemStack off = player.getInventory().getItemInOffHand();
		if (isReviveItem(off)) {
			decrementOne(player, off, false);
			return;
		}

		// anywhere in inventory
		ItemStack[] contents = player.getInventory().getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack it = contents[i];
			if (!isReviveItem(it)) continue;

			if (it.getAmount() <= 1) {
				contents[i] = null;
			} else {
				it.setAmount(it.getAmount() - 1);
			}
			player.getInventory().setContents(contents);
			player.updateInventory();
			return;
		}
	}

	private void decrementOne(Player player, ItemStack stack, boolean mainHand) {
		if (stack.getAmount() <= 1) {
			if (mainHand) {
				player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
			} else {
				player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
			}
		} else {
			stack.setAmount(stack.getAmount() - 1);
		}
		player.updateInventory();
	}

	private boolean isReviveItem(ItemStack item) {
		if (item == null) return false;
		if (item.getType() != REVIVE_MATERIAL) return false;
		if (!item.hasItemMeta()) return false;

		ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.hasLore() || meta.getLore() == null) return false;

		List<String> lore = meta.getLore();
		for (String line : lore) {
			if (line == null) continue;
			String plain = stripColor(line).trim();
			if (plain.equalsIgnoreCase(REVIVE_LORE_PLAIN)) {
				return true;
			}
		}
		return false;
	}

	private String stripColor(String s) {
		return s == null ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "");
	}
}
