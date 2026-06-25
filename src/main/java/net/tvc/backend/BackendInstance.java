package net.tvc.backend;
import net.tvc.backend.commands.SCRATRootCommand;
import net.tvc.backend.managers.CallbackManager;
import net.tvc.backend.utils.Config;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendInstance implements ModInitializer {
	public static final String MOD_ID = "itemscan";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	@Override
	public void onInitialize() {
		Config.load();
		CallbackManager.registerCallbacks();
		if (Config.get().scratCommand.enabled) SCRATRootCommand.register();
		LOGGER.info("ItemScan initialized");
	}
}