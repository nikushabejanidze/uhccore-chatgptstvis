package com.gmail.val59000mc.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.gmail.val59000mc.game.GameManager;

public class ReviveCommandExecutor implements CommandExecutor {

	private final ReviveService reviveService;

	public ReviveCommandExecutor(GameManager gameManager) {
		this.reviveService = new ReviveService(gameManager);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
		if (args.length != 1 && args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Correct usage: '/revive <player>' or use '/revive <player> clear' to respawn the player without giving their items back.");
			return true;
		}

		String name = args[0];
		boolean spawnWithItems = args.length != 2 || !args[1].equalsIgnoreCase("clear");

		return reviveService.reviveByName(sender, name, spawnWithItems);
	}
}
