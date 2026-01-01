package com.gmail.val59000mc.customitems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class HeadUtils {

	@SuppressWarnings("deprecation")
	public static ItemStack createPlayerHead(UUID uuid, String name) {
		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) head.getItemMeta();
		if (meta != null) {
			OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
			try {
				meta.setOwningPlayer(off);
			} catch (Throwable ignored) {
				if (name != null) meta.setOwner(name);
			}
			head.setItemMeta(meta);
		}
		return head;
	}
}
