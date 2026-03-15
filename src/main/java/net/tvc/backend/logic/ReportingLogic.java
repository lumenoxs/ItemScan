package net.tvc.backend.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.tvc.backend.utils.DiscordWebhook;
import net.tvc.backend.utils.EnvLoader;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class ReportingLogic {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path REPORT_FILE = Paths.get("illegal_item_reports.json");
    private static final String WEBHOOK_URL = EnvLoader.get("ILLEGAL_ITEMS_DISCORD_WEBHOOK_URL");

    public static Integer saveInventoryReport(ServerPlayer player, ItemStack stack) {
        player.setGameMode(GameType.SURVIVAL);
        double code = Math.floor(Math.random() * 899999) + 100000;
        JsonObject report = buildBaseReport(player, stack, code);
        report.addProperty("type", "inventory");

        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());

        report.add("playerPosition", position);

        saveReport(report);
        return (int) code;
    }

    public static Integer saveStorageReport(ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setGameMode(GameType.SURVIVAL);
        double code = Math.floor(Math.random() * 999999) + 100000;
        JsonObject report = buildBaseReport(player, stack, code);
        report.addProperty("type", "storage");

        JsonObject position = new JsonObject();
        position.addProperty("x", pos.getX());
        position.addProperty("y", pos.getY());
        position.addProperty("z", pos.getZ());

        report.add("storagePosition", position);

        saveReport(report);
        return (int) code;
    }

    /* ===================== INTERNAL ===================== */

    private static JsonObject buildBaseReport(ServerPlayer player, ItemStack stack, double code) {
        JsonObject json = new JsonObject();

        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("playerName", player.getName().getString());
        json.addProperty("playerUUID", player.getUUID().toString());
        json.addProperty("code", code);
        JsonObject item = new JsonObject();
        item.addProperty(
                "id",
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        item.addProperty("count", stack.getCount());
        item.addProperty("displayName", stack.getHoverName().getString());
        item.add("enchantments", GSON.toJsonTree(stack.getEnchantments()));

        json.add("item", item);

        return json;
    }

    @SuppressWarnings("null")
    private static synchronized void saveReport(JsonObject report) {
        try {
            JsonArray reports;

            if (Files.exists(REPORT_FILE)) {
                String existing = Files.readString(REPORT_FILE);
                reports = GSON.fromJson(existing, JsonArray.class);
            } else {
                reports = new JsonArray();
            }

            reports.add(report);

            Files.writeString(
                    REPORT_FILE,
                    GSON.toJson(reports),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // Send Discord webhook
            sendDiscordWebhook(report);

        } catch (IOException e) {
            System.err.println("[IllegalItemReport] Failed to save report");
            e.printStackTrace();
        }
    }

    private static void sendDiscordWebhook(JsonObject report) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(WEBHOOK_URL);
            webhook.setUsername("TVC Backend Reporter");

            DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                    .setTitle("Illegal Item Report")
                    .setColor(Color.RED)
                    .setDescription("An illegal item has been detected.");

            embed.addField("Player", report.get("playerName").getAsString(), true);
            embed.addField("UUID", report.get("playerUUID").getAsString(), true);
            embed.addField("Type", report.get("type").getAsString(), true);

            JsonObject item = report.getAsJsonObject("item");
            embed.addField("Item ID", item.get("id").getAsString(), false);
            embed.addField("Display Name", item.get("displayName").getAsString(), true);
            embed.addField("Count", String.valueOf(item.get("count").getAsInt()), true);
            embed.addField("Code", String.valueOf(report.get("code").getAsInt()), false);

            if (report.has("playerPosition")) {
                JsonObject pos = report.getAsJsonObject("playerPosition");
                embed.addField("Player Position", String.format("X: %d, Y: %d, Z: %d",
                        pos.get("x").getAsInt(), pos.get("y").getAsInt(), pos.get("z").getAsInt()), false);
            } else if (report.has("storagePosition")) {
                JsonObject pos = report.getAsJsonObject("storagePosition");
                embed.addField("Storage Position", String.format("X: %d, Y: %d, Z: %d",
                        pos.get("x").getAsInt(), pos.get("y").getAsInt(), pos.get("z").getAsInt()), false);
            }

            embed.addField("Timestamp", report.get("timestamp").getAsString(), false);

            webhook.addEmbed(embed);
            webhook.execute();

        } catch (Exception e) {
            System.err.println("[IllegalItemReport] Failed to send Discord webhook");
            e.printStackTrace();
        }
    }
}
