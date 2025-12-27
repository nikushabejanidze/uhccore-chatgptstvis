package com.gmail.val59000mc.tab;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.configuration.TabConfig;
import com.gmail.val59000mc.utils.FileUtils;
import com.gmail.val59000mc.utils.TextUtil;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;

public class TablistManager {

	private final TabConfig config = new TabConfig();

	public void load() {
		try {
			File file = FileUtils.getResourceFile(UhcCore.getPlugin(), "tab.yml");
			config.setConfigurationFile(file);
			config.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void apply(Player player) {
		if (!config.isEnabled()) return;

		// ✅ Use YamlFile API directly (no getConfiguration())
		ConfigurationSection designs = config.getConfigurationSection("header-footer.designs");
		if (designs == null) return;

		for (String key : designs.getKeys(false)) {
			ConfigurationSection sec = designs.getConfigurationSection(key);
			if (sec == null) continue;

			String condition = sec.getString("display-condition", "");
			if (!condition.isEmpty() && !matches(player, condition)) continue;

			String header = String.join("\n", sec.getStringList("header"));
			String footer = String.join("\n", sec.getStringList("footer"));

			header = replace(player, header);
			footer = replace(player, footer);

			// ✅ hex + legacy color support
			header = TextUtil.toLegacy(header);
			footer = TextUtil.toLegacy(footer);

			// ✅ Works on Paper; Spigot also supports this method in modern versions
			player.setPlayerListHeaderFooter(header, footer);
			return;
		}
	}

	/**
	 * Supports:
	 * AND: ;
	 * OR : |
	 * =  and !=
	 */
	private boolean matches(Player p, String cond) {
		String[] andParts = cond.split(";");
		for (String andPart : andParts) {
			andPart = andPart.trim();
			if (andPart.isEmpty()) continue;

			boolean ok = false;
			String[] orParts = andPart.split("\\|");
			for (String orPart : orParts) {
				orPart = orPart.trim();
				if (orPart.isEmpty()) continue;

				if (eval(p, orPart)) {
					ok = true;
					break;
				}
			}
			if (!ok) return false;
		}
		return true;
	}

	private boolean eval(Player p, String expr) {
		if (expr.contains("!=")) {
			String[] s = expr.split("!=", 2);
			return !resolve(p, s[0]).equalsIgnoreCase(s[1]);
		}
		if (expr.contains("=")) {
			String[] s = expr.split("=", 2);
			return resolve(p, s[0]).equalsIgnoreCase(s[1]);
		}
		return false;
	}

	private String resolve(Player p, String token) {
		token = token.trim();
		if (token.equalsIgnoreCase("%world%")) return p.getWorld().getName();
		if (token.equalsIgnoreCase("%server%")) return Bukkit.getServer().getName();
		return token;
	}

	private String replace(Player p, String s) {
		if (s == null) return "";

		return s
			.replace("%world%", p.getWorld().getName())
			.replace("%server%", Bukkit.getServer().getName())
			.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
			.replace("%ping%", String.valueOf(getPingCompat(p)));
	}

	/**
	 * ✅ Compiles even if your API jar doesn't include Player#getPing()
	 */
	private int getPingCompat(Player player) {
		try {
			// Paper/Spigot modern: Player#getPing()
			return (int) player.getClass().getMethod("getPing").invoke(player);
		} catch (Throwable ignored) {
			return -1;
		}
	}
}
