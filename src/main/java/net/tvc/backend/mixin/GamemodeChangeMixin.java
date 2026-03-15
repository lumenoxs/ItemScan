package net.tvc.backend.mixin;

import net.tvc.backend.BackendInstance;
import net.tvc.backend.utils.DiscordWebhook;
import net.tvc.backend.utils.EnvLoader;

import java.awt.Color;
import java.time.Instant;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;

@Mixin(ServerPlayerGameMode.class)
public class GamemodeChangeMixin {
    private static final String WEBHOOK_URL = EnvLoader.get("GAMEMODE_CHANGE_DISCORD_WEBHOOK_URL");

    @Shadow
    protected ServerPlayer player;

    @Inject(method = "changeGameModeForPlayer", at = @At("HEAD"))
    private void onGamemodeChange(GameType gameMode, CallbackInfoReturnable<Boolean> cir) {
        BackendInstance.LOGGER.info("Gamemode change detected for player: " + player.getName().getString());

        String oldMode = player.gameMode.getGameModeForPlayer().name();
        String newMode = gameMode.name();

        if (oldMode.equals(newMode))
            return;

        try {

            DiscordWebhook webhook = new DiscordWebhook(WEBHOOK_URL);
            webhook.setUsername("TVC Backend Reporter");

            DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                    .setTitle("Gamemode Change")
                    .setColor(Color.ORANGE)
                    .setDescription("A player changed gamemode.");

            embed.addField("Player", player.getName().getString(), true);
            embed.addField("UUID", player.getUUID().toString(), true);
            embed.addField("Old Gamemode", oldMode, true);
            embed.addField("New Gamemode", newMode, true);

            embed.addField("Position",
                    "X: " + player.getBlockX()
                            + " Y: " + player.getBlockY()
                            + " Z: " + player.getBlockZ(),
                    false);

            embed.addField("Timestamp", Instant.now().toString(), false);

            webhook.addEmbed(embed);
            webhook.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}