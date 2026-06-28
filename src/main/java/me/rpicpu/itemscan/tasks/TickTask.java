package me.rpicpu.itemscan.tasks;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import me.rpicpu.itemscan.services.IllegalItemService;
import me.rpicpu.itemscan.utils.Config;

public class TickTask {
    public static int ticks = 0;
    
    public static void check(MinecraftServer server) {
        if (!Config.get().enabled) {
            return;
        }

        ticks++;
        if (ticks >= 200) ticks = 0;

        var inventoryScan = Config.get().inventoryScan;
        if (inventoryScan.enabled && inventoryScan.interval > 0 && ticks % inventoryScan.interval == 0) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                IllegalItemService.checkPlayerInventory(player);
            }
        }

        var positionScan = Config.get().positionScan;
        if (positionScan.enabled) {
            if (positionScan.small.enabled && positionScan.small.interval > 0 && ticks % positionScan.small.interval == 0) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    IllegalItemService.checkPlayerPosition(player, positionScan.small.radius, positionScan.small.innerRadius);
                }
            }

            if (positionScan.medium.enabled && positionScan.medium.interval > 0 && ticks % positionScan.medium.interval == 0) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    IllegalItemService.checkPlayerPosition(player, positionScan.medium.radius, positionScan.medium.innerRadius);
                }
            }

            if (positionScan.large.enabled && positionScan.large.interval > 0 && ticks % positionScan.large.interval == 0) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    IllegalItemService.checkPlayerPosition(player, positionScan.large.radius, positionScan.large.innerRadius);
                }
            }
        }
    }
}
