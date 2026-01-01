package com.gmail.val59000mc;

import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.players.PlayerManager;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class UhcCorePlaceholderExpansion extends PlaceholderExpansion {

	private final UhcCore plugin;

	public UhcCorePlaceholderExpansion(UhcCore plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getIdentifier() {
		return "uhccore";
	}

	@Override
	public String getAuthor() {
		return "UhcCore";
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onPlaceholderRequest(Player player, String params) {
		if (player == null || params == null) return "";

		GameManager gm = GameManager.getGameManager();
		if (gm == null) return "";

		PlayerManager pm = gm.getPlayerManager();
		if (pm == null) return "";

		UhcPlayer uhcPlayer = pm.getUhcPlayer(player);
		if (uhcPlayer == null) return "";

		UhcTeam team = uhcPlayer.getTeam();
		int teamNumber = (team == null) ? -1 : team.getTeamNumber();

		// %uhccore_teamNumber%
		if (params.equalsIgnoreCase("teamNumber")) {
			return (teamNumber > 0) ? String.valueOf(teamNumber) : "-";
		}

		// %uhccore_teamSort%  (best for TAB numeric sorting)
		if (params.equalsIgnoreCase("teamSort")) {
			if (teamNumber > 0) return String.valueOf(teamNumber);
			return "9999"; // no team / spectators go bottom
		}

		// %uhccore_teamSortPadded%  (safe if you use A_TO_Z sorting)
		if (params.equalsIgnoreCase("teamSortPadded")) {
			if (teamNumber <= 0) return "9999";
			if (teamNumber < 10) return "000" + teamNumber;
			if (teamNumber < 100) return "00" + teamNumber;
			if (teamNumber < 1000) return "0" + teamNumber;
			return String.valueOf(teamNumber);
		}

		// %uhccore_teamColor%  (simple deterministic color from team number)
		if (params.equalsIgnoreCase("teamColor")) {
			if (teamNumber <= 0) return ChatColor.GRAY.toString();
			// 1..16 -> chat colors rotation
			ChatColor[] colors = new ChatColor[]{
				ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW,
				ChatColor.AQUA, ChatColor.LIGHT_PURPLE, ChatColor.GOLD, ChatColor.DARK_AQUA,
				ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE, ChatColor.DARK_RED, ChatColor.DARK_BLUE,
				ChatColor.WHITE, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.BLACK
			};
			return colors[(teamNumber - 1) % colors.length].toString();
		}

		return "";
	}
}
