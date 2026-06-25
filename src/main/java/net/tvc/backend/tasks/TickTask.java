package net.tvc.backend.tasks;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import net.tvc.backend.services.IllegalItemService;
import net.tvc.backend.utils.Config;

public class TickTask {
    public static int ticks = 0;
    
    public static void check(MinecraftServer server) {
        if (!Config.ENABLE_BACKEND || !Config.ENABLE_ITEM_SCAN) {
            return;
        }

        ticks++;
        if (ticks >= 200) ticks = 0;

        if (Config.ENABLE_INVENTORY_SCAN && Config.INVENTORY_CHECK_TICK_INTERVAL > 0 && ticks % Config.INVENTORY_CHECK_TICK_INTERVAL == 0) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                IllegalItemService.checkPlayerInventory(player);
            }
        }

        if (Config.ENABLE_PLAYER_POSITION_SCAN) {
            if (Config.ENABLE_PLAYER_POSITION_SMALL_SCAN && Config.POSITION_SMALL_CHECK_TICK_INTERVAL > 0 && ticks % Config.POSITION_SMALL_CHECK_TICK_INTERVAL == 0) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    IllegalItemService.checkPlayerPosition(player, Config.POSITION_SMALL_RADIUS, Config.POSITION_SMALL_INNER);
                }
            }

            if (Config.ENABLE_PLAYER_POSITION_MEDIUM_SCAN && Config.POSITION_MEDIUM_CHECK_TICK_INTERVAL > 0 && ticks % Config.POSITION_MEDIUM_CHECK_TICK_INTERVAL == 0) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    IllegalItemService.checkPlayerPosition(player, Config.POSITION_MEDIUM_RADIUS, Config.POSITION_MEDIUM_INNER);
                }
            }

            if (Config.ENABLE_PLAYER_POSITION_LARGE_SCAN && Config.POSITION_LARGE_CHECK_TICK_INTERVAL > 0 && ticks % Config.POSITION_LARGE_CHECK_TICK_INTERVAL == 0) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    IllegalItemService.checkPlayerPosition(player, Config.POSITION_LARGE_RADIUS, Config.POSITION_LARGE_INNER);
                }
            }
        }
    }
}
