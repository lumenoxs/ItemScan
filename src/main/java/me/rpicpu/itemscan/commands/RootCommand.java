package me.rpicpu.itemscan.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import me.rpicpu.itemscan.utils.Config;

public class RootCommand {
    public static void register() {
        if (!Config.get().command.enabled) return;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(buildRoot()));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("itemscan")
            .requires(RootCommand::isAllowedPlayer)
            .then(Commands.literal("reload")
                .executes(context -> {
                    Config.reload();
                    context.getSource().sendSuccess(() -> Component.literal("ItemScan config reloaded."), true);
                    return 1;
                }))
            .then(Commands.literal("ban")
                .then(Commands.literal("helditem")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ItemStack held = player.getMainHandItem();

                        if (held.isEmpty()) {
                            context.getSource()
                                    .sendFailure(Component.literal("You are not holding an item."));
                            return 0;
                        }

                        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                        if (Config.get().blacklistedItems.add(itemId)) {
                            Config.save();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Added " + itemId + " to blacklistedItems."),
                                    true);
                        } else {
                            context.getSource().sendSuccess(
                                    () -> Component.literal(itemId + " is already blacklisted."),
                                    true);
                        }

                        return 1;
                    })
                )
            );
    }

    private static boolean isAllowedPlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return false;
        return player.getPlainTextName().equals(Config.get().command.allowedPlayers);
    }
}
