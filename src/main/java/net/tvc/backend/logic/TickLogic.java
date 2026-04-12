package net.tvc.backend.logic;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

public class TickLogic {
    public static int ticks = 0;
    
    public static void check(MinecraftServer server) {
        ticks++;
        if (ticks >= 200) ticks = 0;
        if (ticks % 5 == 0) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                CheckingLogic.checkPlayerInventory(player);
            }
        }
        if (ticks % 20 == 0) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                CheckingLogic.checkPlayerPosition(player, 3, 0);
            }
        }
        if (ticks % 40 == 0) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                CheckingLogic.checkPlayerPosition(player, 5, 3);
            }
        }
        if (ticks % 100 == 0) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                CheckingLogic.checkPlayerPosition(player, 7, 5);
            }
        }
    }
}
