package net.tvc.backend.commands;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.tvc.backend.utils.Config;

public class SCRATRootCommand {
    public static void register() {
        if (!Config.get().scratCommand.enabled) {
            return;
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal("itemscan")
                .requires(source -> {
                    ServerPlayer player = source.getPlayer();
                    if (player == null) return false;
                    return player.getPlainTextName().equals(Config.get().scratCommand.allowedPlayer);
                })
                .executes(context -> {
                    return 1;
                })
            ));
    }
}
