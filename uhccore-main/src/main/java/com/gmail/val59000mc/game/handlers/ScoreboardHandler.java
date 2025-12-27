package com.gmail.val59000mc.game.handlers;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.configuration.MainConfig;
import com.gmail.val59000mc.exceptions.UhcPlayerNotOnlineException;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.players.PlayerManager;
import com.gmail.val59000mc.players.PlayerState;
import com.gmail.val59000mc.players.TeamManager;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;
import com.gmail.val59000mc.scoreboard.ScoreboardLayout;
import com.gmail.val59000mc.scoreboard.ScoreboardManager;
import com.gmail.val59000mc.scoreboard.ScoreboardType;
import com.gmail.val59000mc.tasks.UpdateScoreboardTask;
import com.gmail.val59000mc.utils.TextUtil;
import com.gmail.val59000mc.utils.VersionUtils;
import com.gmail.val59000mc.versionadapters.adapters.SetTeamColorAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.logging.Logger;

public class ScoreboardHandler {

	private static final Logger LOGGER = Logger.getLogger(ScoreboardHandler.class.getCanonicalName());

	public static final String[] SCOREBOARD_LINES = new String[]{
		ChatColor.UNDERLINE + "" + ChatColor.RESET,
		ChatColor.ITALIC + "" + ChatColor.RESET,
		ChatColor.BOLD + "" + ChatColor.RESET,
		ChatColor.RESET + "" + ChatColor.RESET,
		ChatColor.GREEN + "" + ChatColor.RESET,
		ChatColor.DARK_GRAY + "" + ChatColor.RESET,
		ChatColor.GOLD + "" + ChatColor.RESET,
		ChatColor.RED + "" + ChatColor.RESET,
		ChatColor.YELLOW + "" + ChatColor.RESET,
		ChatColor.WHITE + "" + ChatColor.RESET,
		ChatColor.DARK_GREEN + "" + ChatColor.RESET,
		ChatColor.BLUE + "" + ChatColor.RESET,
		ChatColor.STRIKETHROUGH + "" + ChatColor.RESET,
		ChatColor.MAGIC + "" + ChatColor.RESET,
		ChatColor.DARK_RED + "" + ChatColor.RESET
	};

	private final GameManager gameManager;
	private final MainConfig config;
	private final ScoreboardLayout scoreboardLayout;

	public ScoreboardHandler(GameManager gameManager, MainConfig config, ScoreboardLayout scoreboardLayout) {
		this.gameManager = gameManager;
		this.config = config;
		this.scoreboardLayout = scoreboardLayout;
	}

	public void setUpPlayerScoreboard(UhcPlayer uhcPlayer, Player bukkitPlayer) {

		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		uhcPlayer.setScoreboard(scoreboard);
		bukkitPlayer.setScoreboard(scoreboard);

		Objective healthTab = null;
		Objective healthBelowName = null;

		if (config.get(MainConfig.HEARTS_ON_TAB)) {
			healthTab = VersionUtils.getVersionUtils().registerNewObjective(
				scoreboard,
				"health_tab",
				"health",
				config.get(MainConfig.HEARTS_ON_TAB_RENDER_TYPE)
			);
			healthTab.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}

		if (config.get(MainConfig.HEARTS_BELOW_NAME)) {
			healthBelowName = VersionUtils.getVersionUtils().registerNewObjective(
				scoreboard,
				ChatColor.RED + "\u2764",
				"health"
			);
			healthBelowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
		}

		// add teams for no flicker scoreboard
		for (int i = 0; i < 15; i++) {
			Team team = scoreboard.registerNewTeam(SCOREBOARD_LINES[i]);
			team.addEntry(SCOREBOARD_LINES[i]);
		}

		boolean disableEnemyNameTags = config.get(MainConfig.DISABLE_ENEMY_NAMETAGS);
		PlayerManager playerManager = gameManager.getPlayerManager();

		// setup teams
		if (config.get(MainConfig.TEAM_COLORS)) {
			setColoredTeams(gameManager.getTeamManager(), uhcPlayer, scoreboard, disableEnemyNameTags, healthTab, healthBelowName);
		} else {
			setFriendEnemyTeams(uhcPlayer, playerManager, scoreboard, disableEnemyNameTags, healthTab, healthBelowName);
		}

		updatePlayerOnTab(uhcPlayer);

		Bukkit.getScheduler().scheduleSyncDelayedTask(
			UhcCore.getPlugin(),
			new UpdateScoreboardTask(this, uhcPlayer),
			1L
		);
	}

	private void setFriendEnemyTeams(UhcPlayer scoreboardPlayer, PlayerManager pm, Scoreboard scoreboard,
									 boolean disableEnemyNameTags, Objective healthTab, Objective healthBelowName) {

		Team friends = scoreboard.registerNewTeam("friends");
		Team enemies = scoreboard.registerNewTeam("enemies");

		friends.setPrefix(ChatColor.GREEN + "");
		enemies.setPrefix(ChatColor.RED + "");
		friends.setSuffix(ChatColor.RESET + "");
		enemies.setSuffix(ChatColor.RESET + "");

		if (disableEnemyNameTags) {
			VersionUtils.getVersionUtils().setTeamNameTagVisibility(enemies, false);
		}

		Team spectators = scoreboard.registerNewTeam("spectators");
		spectators.setPrefix(ChatColor.GRAY + "");
		spectators.setSuffix(ChatColor.RESET + "");

		for (UhcPlayer uhcPlayer : pm.getPlayersList()) {
			updatePlayerHealth(uhcPlayer, healthTab, healthBelowName);

			if (uhcPlayer.getState().equals(PlayerState.DEAD) || uhcPlayer.getState().equals(PlayerState.WAITING)) {
				spectators.addEntry(uhcPlayer.getName());
			} else if (uhcPlayer.isInTeamWith(scoreboardPlayer)) {
				friends.addEntry(uhcPlayer.getName());
			} else {
				enemies.addEntry(uhcPlayer.getName());
			}
		}
	}

	private void setColoredTeams(TeamManager teamManager, UhcPlayer scoreboardPlayer, Scoreboard scoreboard,
								 boolean disableEnemyNameTags, Objective healthTab, Objective healthBelowName) {

		Team spectators = scoreboard.registerNewTeam("spectators");
		spectators.setPrefix(ChatColor.GRAY + "");
		spectators.setSuffix(ChatColor.RESET + "");

		for (UhcTeam uhcTeam : teamManager.getUhcTeams()) {

			Team team;
			if (uhcTeam.contains(scoreboardPlayer)) {
				team = scoreboard.registerNewTeam("0" + uhcTeam.getTeamNumber());
			} else {
				team = scoreboard.registerNewTeam("" + uhcTeam.getTeamNumber());
				if (disableEnemyNameTags) {
					VersionUtils.getVersionUtils().setTeamNameTagVisibility(team, false);
				}
			}

			team.setPrefix(uhcTeam.getPrefix());
			team.setSuffix(ChatColor.RESET + "");

			if (config.get(MainConfig.SET_SCOREBOARD_TEAM_COLORS)) {
				UhcCore.getVersionAdapterLoader()
					.getVersionAdapter(SetTeamColorAdapter.class)
					.setTeamColor(team, uhcTeam.getColor());
			}

			for (UhcPlayer member : uhcTeam.getMembers()) {
				updatePlayerHealth(member, healthTab, healthBelowName);

				if (member.getState().equals(PlayerState.DEAD)) {
					spectators.addEntry(member.getName());
				} else {
					team.addEntry(member.getName());
				}
			}
		}
	}

	private void updatePlayerHealth(UhcPlayer uhcPlayer, Objective healthTab, Objective healthBelowName) {

		Player player;
		try {
			player = uhcPlayer.getPlayer();
		} catch (UhcPlayerNotOnlineException ex) {
			return;
		}

		if (healthTab != null) {
			setReadOnlyPlayerScore(healthTab.getScore(player.getName()), (int) player.getHealth());
		}
		if (healthBelowName != null) {
			setReadOnlyPlayerScore(healthBelowName.getScore(player.getName()), (int) player.getHealth());
		}
	}

	// Workaround for https://bugs.mojang.com/browse/MC-111729
	private void setReadOnlyPlayerScore(Score score, int value) {
		if (UhcCore.getNmsAdapter().isPresent()) {
			UhcCore.getNmsAdapter().get().setReadOnlyPlayerScore(score, value);
		} else {
			score.setScore(value);
		}
	}

	public void updatePlayerOnTab(UhcPlayer uhcPlayer) {

		for (UhcPlayer scoreboardOwner : gameManager.getPlayerManager().getPlayersList()) {
			if (config.get(MainConfig.TEAM_COLORS)) {
				updatePlayerOnColoredTab(uhcPlayer, scoreboardOwner);
			} else {
				updatePlayerOnFriendEnemyTab(uhcPlayer, scoreboardOwner);
			}
		}

		if (config.get(MainConfig.CHANGE_DISPLAY_NAMES)) {
			try {
				uhcPlayer.getPlayer().setDisplayName(uhcPlayer.getDisplayName());
			} catch (UhcPlayerNotOnlineException ignored) {
			}
		}
	}

	public void updateTeamOnTab(UhcTeam uhcTeam) {
		for (UhcPlayer teamMember : uhcTeam.getMembers()) {
			updatePlayerOnTab(teamMember);
		}
	}

	private void updatePlayerOnFriendEnemyTab(UhcPlayer uhcPlayer, UhcPlayer scoreboardOwner) {

		Scoreboard scoreboard = scoreboardOwner.getScoreboard();
		if (scoreboard == null) return;

		if (uhcPlayer.getState().equals(PlayerState.PLAYING)) {
			if (scoreboardOwner.isInTeamWith(uhcPlayer)) {
				scoreboard.getTeam("friends").addEntry(uhcPlayer.getName());
			} else {
				scoreboard.getTeam("enemies").addEntry(uhcPlayer.getName());
			}
		} else {
			scoreboard.getTeam("spectators").addEntry(uhcPlayer.getName());
		}
	}

	private void updatePlayerOnColoredTab(UhcPlayer uhcPlayer, UhcPlayer scoreboardOwner) {

		Scoreboard scoreboard = scoreboardOwner.getScoreboard();
		if (scoreboard == null) return;

		String playerName = uhcPlayer.getName();

		// 🔥 IMPORTANT: remove player from ALL teams first
		for (Team t : scoreboard.getTeams()) {
			if (t.hasEntry(playerName)) {
				t.removeEntry(playerName);
			}
		}

		// spectators always last
		if (uhcPlayer.getState() == PlayerState.DEAD) {
			Team spec = scoreboard.getTeam("spectators");
			if (spec == null) {
				spec = scoreboard.registerNewTeam("spectators");
				spec.setPrefix(ChatColor.GRAY + "");
				spec.setSuffix(ChatColor.RESET + "");
			}
			spec.addEntry(playerName);
			return;
		}

		// ✅ numeric team sorting: 001, 002, 003...
		int num = uhcPlayer.getTeam().getTeamNumber();
		String teamKey = String.format("%03d", num);

		Team team = scoreboard.getTeam(teamKey);
		if (team == null) {
			team = scoreboard.registerNewTeam(teamKey);

			if (config.get(MainConfig.DISABLE_ENEMY_NAMETAGS)
				&& !scoreboardOwner.isInTeamWith(uhcPlayer)) {
				VersionUtils.getVersionUtils().setTeamNameTagVisibility(team, false);
			}
		}

		team.setPrefix(uhcPlayer.getTeam().getPrefix());
		team.setSuffix(ChatColor.RESET + "");
		team.addEntry(playerName);
	}



	public void resetObjective(Scoreboard scoreboard, ScoreboardType scoreboardType) {

		Objective objective = scoreboard.getObjective("informations");
		if (objective != null) {
			objective.unregister();
		}

		objective = VersionUtils.getVersionUtils().registerNewObjective(scoreboard, "informations", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		// ✅ Hex + & support on title
		objective.setDisplayName(TextUtil.toLegacy(scoreboardLayout.getTitle()));

		int lines = scoreboardLayout.getLines(scoreboardType).size();
		for (int i = 0; i < lines; i++) {
			Score score = objective.getScore(SCOREBOARD_LINES[i]);
			score.setScore(i);
		}
	}

	public void updatePlayerSidebar(UhcPlayer uhcPlayer, ScoreboardType scoreboardType) {

		ScoreboardManager scoreboardManager = gameManager.getScoreboardManager();

		Player player;
		try {
			player = uhcPlayer.getPlayer();
		} catch (UhcPlayerNotOnlineException ignored) {
			return;
		}

		int i = 0;
		for (String line : scoreboardLayout.getLines(scoreboardType)) {

			String first = "";
			String second = "";

			if (!line.isEmpty()) {
				String translatedLine = scoreboardManager.translatePlaceholders(line, uhcPlayer, player);
				translatedLine = TextUtil.toLegacy(translatedLine);

				// ✅ HEX-SAFE SPLIT (by visible chars, not raw string length)
				String[] parts = splitForScoreboard(translatedLine, 16);
				first = parts[0];
				second = parts[1];
			}

			Team lineTeam = uhcPlayer.getScoreboard().getTeam(SCOREBOARD_LINES[i]);

			if (!lineTeam.getPrefix().equals(first)) {
				lineTeam.setPrefix(first);
			}
			if (!lineTeam.getSuffix().equals(second)) {
				lineTeam.setSuffix(second);
			}

			i++;
		}
	}

	// ---------------------------
	// ✅ HEX-safe scoreboard splitting helpers
	// ---------------------------

	/**
	 * Splits a legacy-colored string into prefix/suffix where the prefix contains maxVisiblePrefix visible characters.
	 * Color codes (including hex §x§R§R§G§G§B§B) do not count toward visible length.
	 * The suffix is prefixed with the last full color state from the prefix (including hex), so hex continues properly.
	 */
	private static String[] splitForScoreboard(String text, int maxVisiblePrefix) {
		if (text == null) return new String[]{"", ""};
		if (maxVisiblePrefix <= 0) return new String[]{"", text};

		int splitIndex = indexAfterVisibleChars(text, maxVisiblePrefix);

		// If we didn't reach the limit, everything fits in prefix.
		if (splitIndex >= text.length()) {
			return new String[]{text, ""};
		}

		String prefix = text.substring(0, splitIndex);
		String suffix = text.substring(splitIndex);

		// Carry full last color (including hex) into suffix
		String carry = getLastColorsIncludingHex(prefix);
		suffix = carry + suffix;

		return new String[]{prefix, suffix};
	}

	/**
	 * Returns the raw string index positioned AFTER consuming 'visibleCount' visible characters.
	 * Skips over legacy color codes and full hex sequences.
	 */
	private static int indexAfterVisibleChars(String text, int visibleCount) {
		int visible = 0;

		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);

			if (ch == ChatColor.COLOR_CHAR && i + 1 < text.length()) {
				char code = Character.toLowerCase(text.charAt(i + 1));

				// Hex format: §x§R§R§G§G§B§B (14 chars total)
				if (code == 'x') {
					// Ensure we can skip the whole sequence safely
					if (i + 13 < text.length()) {
						i += 13;
						continue;
					} else {
						// malformed hex, stop skipping safely
						i++;
						continue;
					}
				}

				// Normal legacy code: skip § + code
				i++;
				continue;
			}

			// Visible character
			visible++;
			if (visible >= visibleCount) {
				return i + 1; // split AFTER this char
			}
		}

		return text.length();
	}

	/**
	 * Like ChatColor.getLastColors, but also preserves full hex sequences.
	 * We scan left->right and remember the last "color" (0-9a-f or §x...) and active formats.
	 */
	private static String getLastColorsIncludingHex(String input) {
		if (input == null || input.isEmpty()) return "";

		String lastColor = "";
		StringBuilder formats = new StringBuilder();

		for (int i = 0; i < input.length() - 1; i++) {
			if (input.charAt(i) != ChatColor.COLOR_CHAR) continue;

			char code = Character.toLowerCase(input.charAt(i + 1));

			if (code == 'x') {
				// Full hex sequence length: 14 chars total starting at i (§x§R§R§G§G§B§B)
				if (i + 13 < input.length()) {
					lastColor = input.substring(i, i + 14);
					formats.setLength(0); // new color resets formats
					i += 13;
				}
				continue;
			}

			// Reset
			if (code == 'r') {
				lastColor = "";
				formats.setLength(0);
				continue;
			}

			// Color (0-9 a-f)
			if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
				lastColor = "" + ChatColor.COLOR_CHAR + code;
				formats.setLength(0); // new color resets formats
				continue;
			}

			// Formatting codes (k-o)
			if (code >= 'k' && code <= 'o') {
				String fmt = "" + ChatColor.COLOR_CHAR + code;
				// Avoid duplicates
				if (formats.indexOf(fmt) == -1) {
					formats.append(fmt);
				}
			}
		}

		return lastColor + formats;
	}

	public ScoreboardType getPlayerScoreboardType(UhcPlayer uhcPlayer) {

		if (uhcPlayer.getState().equals(PlayerState.DEAD)) {
			return ScoreboardType.SPECTATING;
		}

		switch (gameManager.getGameState()) {
			case LOADING:
			case WAITING:
			case STARTING:
				return ScoreboardType.WAITING;

			case PLAYING:
			case ENDED:
				return ScoreboardType.PLAYING;

			case DEATHMATCH:
				return ScoreboardType.DEATHMATCH;
		}

		return ScoreboardType.WAITING;
	}
}
