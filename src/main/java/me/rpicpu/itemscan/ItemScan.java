package me.rpicpu.itemscan;
import me.rpicpu.itemscan.commands.RootCommand;
import me.rpicpu.itemscan.managers.CallbackManager;
import me.rpicpu.itemscan.utils.Config;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemScan implements ModInitializer {
	public static final String MOD_ID = "itemscan";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Integer configVersion = 1;
	
	@Override
	public void onInitialize() {
		Config.load();
		CallbackManager.registerCallbacks();
		if (Config.get().command.enabled) RootCommand.register();
		LOGGER.info("ItemScan initialized");
	}
}