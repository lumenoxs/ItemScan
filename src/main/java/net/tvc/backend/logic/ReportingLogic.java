package net.tvc.backend.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.tvc.backend.utils.DiscordWebhook;

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
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1412390362390990868/VSYilZaEpO_wyFfw2FW8sWJYZlA5l0e30kWpHM2w6RWaBbJu8c73QQEOBA3H6HdIEsBe"; // Replace with actual webhook URL

    public static void saveInventoryReport(ServerPlayer player, ItemStack stack) {
        JsonObject report = buildBaseReport(player, stack);
        report.addProperty("type", "inventory");

        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());

        report.add("playerPosition", position);

        saveReport(report);
    }

    public static void saveStorageReport(ServerPlayer player, ItemStack stack, BlockPos pos) {
        JsonObject report = buildBaseReport(player, stack);
        report.addProperty("type", "storage");

        JsonObject position = new JsonObject();
        position.addProperty("x", pos.getX());
        position.addProperty("y", pos.getY());
        position.addProperty("z", pos.getZ());

        report.add("storagePosition", position);

        saveReport(report);
    }

    /* ===================== INTERNAL ===================== */

    private static JsonObject buildBaseReport(ServerPlayer player, ItemStack stack) {
        JsonObject json = new JsonObject();

        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("playerName", player.getName().getString());
        json.addProperty("playerUUID", player.getUUID().toString());

        JsonObject item = new JsonObject();
        item.addProperty(
                "id",
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
        );
        item.addProperty("count", stack.getCount());
        item.addProperty("displayName", stack.getHoverName().getString());
        item.add("enchantments", GSON.toJsonTree(stack.getEnchantments()));

        json.add("item", item);

        return json;
    }

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
                    StandardOpenOption.TRUNCATE_EXISTING
            );

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
