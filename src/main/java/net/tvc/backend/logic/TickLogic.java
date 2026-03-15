package net.tvc.backend.logic;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

public class TickLogic {
    public static int tickCounter = 0;

    public static void check(MinecraftServer server) {
        tickCounter++;
        if (tickCounter >= 30) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                CheckingLogic.checkPlayer(player);
                CheckingLogic.checkPosition(player);
            }
            tickCounter = 0;
        }
    }
}
