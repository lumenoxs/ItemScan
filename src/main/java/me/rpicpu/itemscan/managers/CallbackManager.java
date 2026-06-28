package me.rpicpu.itemscan.managers;

import me.rpicpu.itemscan.tasks.TickTask;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class CallbackManager {
    public static void registerCallbacks() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            TickTask.check(server);
        });
    }
}
