package com.gmail.val59000mc.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

	private TextUtil() {}

	// Matches: &#RRGGBB
	private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&?#([0-9a-f]{6})");

	public static String toLegacy(String input) {
		if (input == null) return null;

		// First: convert &#RRGGBB
		String withHex = translateHexColors(input);

		// Then: convert normal & codes
		return ChatColor.translateAlternateColorCodes('&', withHex);
	}
	
	public static String translateHexColors(String input) {
		Matcher matcher = HEX_PATTERN.matcher(input);
		StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			String hex = matcher.group(1); // RRGGBB
			String replacement = toLegacyHex(hex);
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(buffer);

		return buffer.toString();
	}

	private static String toLegacyHex(String rrggbb) {
		// Builds: §x§R§R§G§G§B§B
		StringBuilder out = new StringBuilder(14);
		out.append(ChatColor.COLOR_CHAR).append('x');
		for (int i = 0; i < rrggbb.length(); i++) {
			out.append(ChatColor.COLOR_CHAR).append(rrggbb.charAt(i));
		}
		return out.toString();
	}

	/**
	 * Trims a legacy-colored string to a max raw length WITHOUT cutting in the middle
	 * of a color code (including hex sequences).
	 *
	 * This avoids breaking colors when lang strings are capped (inventory titles etc.).
	 */
	public static String safeTrimLegacy(String legacy, int maxLen) {
		if (legacy == null) return null;
		if (maxLen < 0) return legacy;
		if (legacy.length() <= maxLen) return legacy;

		String cut = legacy.substring(0, maxLen);

		// If we cut inside a color code, back up safely.
		while (endsWithIncompleteColorSequence(cut)) {
			if (cut.isEmpty()) break;
			cut = cut.substring(0, cut.length() - 1);
		}

		// Also handle "cut inside hex sequence": if last "§x" is incomplete at end, remove it.
		int lastHex = lastHexStartIndexIfIncomplete(cut);
		if (lastHex != -1) {
			cut = cut.substring(0, lastHex);
			while (endsWithIncompleteColorSequence(cut)) {
				if (cut.isEmpty()) break;
				cut = cut.substring(0, cut.length() - 1);
			}
		}

		return cut;
	}

	private static boolean endsWithIncompleteColorSequence(String s) {
		if (s.isEmpty()) return false;

		// Ends with '§' => incomplete
		if (s.charAt(s.length() - 1) == ChatColor.COLOR_CHAR) return true;

		// Ends with '§x' partial? (if last char is 'x' and previous is '§', it's still "complete" as 2 chars,
		// but the hex sequence is not complete; handled by lastHexStartIndexIfIncomplete)
		return false;
	}

	private static int lastHexStartIndexIfIncomplete(String s) {
		// Find last "§x"
		int idx = s.lastIndexOf(ChatColor.COLOR_CHAR + "x");
		if (idx == -1) return -1;

		// Complete hex is 14 chars total from idx: "§x§R§R§G§G§B§B"
		int remaining = s.length() - idx;
		if (remaining >= 14) return -1; // complete in this cut

		// Incomplete hex at the end → return start index to remove it
		return idx;
	}
}
