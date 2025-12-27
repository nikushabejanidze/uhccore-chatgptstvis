package com.gmail.val59000mc.configuration;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.utils.FileUtils;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TabConfig extends YamlFile {

	private static final Logger LOGGER = Logger.getLogger(TabConfig.class.getCanonicalName());

	private boolean enabled = true;

	public TabConfig() {
		loadConfig();
	}

	private void loadConfig() {
		File file = new File(UhcCore.getPlugin().getDataFolder(), "tab.yml");

		try {
			FileUtils.saveResourceIfNotAvailable(UhcCore.getPlugin(), "tab.yml");
			setConfigurationFile(file);
			load();
		} catch (IOException | InvalidConfigurationException ex) {
			LOGGER.log(Level.WARNING, "Unable to load tab.yml", ex);
			return;
		}

		enabled = getBoolean("header-footer.enabled", true);
	}

	public boolean isEnabled() {
		return enabled;
	}
}
