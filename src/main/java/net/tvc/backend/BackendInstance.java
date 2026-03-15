package net.tvc.backend;

import net.tvc.backend.managers.CallbackManager;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendInstance implements ModInitializer {
	public static final String MOD_ID = "tvc-backend";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CallbackManager.registerCallbacks();
		LOGGER.info("TVC-Backend initialized!");
	}
}