package net.tvc.backend.managers;

import net.tvc.backend.logic.TickLogic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class CallbackManager {
    public static void registerCallbacks() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            TickLogic.check(server);
        });
    }
}
