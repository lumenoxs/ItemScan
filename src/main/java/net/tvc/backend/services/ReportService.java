package net.tvc.backend.services;

import net.tvc.backend.BackendInstance;
import net.tvc.backend.utils.Config;
import net.tvc.backend.utils.DiscordWebhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;

import java.awt.Color;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.time.Instant;

public class ReportService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path REPORT_FILE = Paths.get(Config.REPORT_FILE_PATH);
    private static final String WEBHOOK_URL = Config.DISCORD_WEBHOOK_URL;

    @SuppressWarnings("null")
    public static void sendMessage(ServerPlayer player, String cmessage, Integer code) {
        TextColor DARK_RED = TextColor.fromRgb(0xAA0000);
        TextColor RED = TextColor.fromRgb(0xFF5555);
        TextColor GOLD = TextColor.fromRgb(0xFFAA00);

        MutableComponent line1 = Component.literal(cmessage);
        MutableComponent line2 = Component.literal("\nFor more info, go here: ");
        MutableComponent link = Component.literal("https://truevanilla.net/wiki/SCRAT/Inventory");
        MutableComponent line3 = Component.literal("\nCode: ");
        MutableComponent codeText = Component.literal(code.toString());

        line1.setStyle(line1.getStyle().withColor(DARK_RED));
        line2.setStyle(line2.getStyle().withColor(RED));
        link.setStyle(link.getStyle().withColor(RED).withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://truevanilla.net/wiki/SCRAT/Inventory"))));
        line3.setStyle(line3.getStyle().withColor(GOLD));
        codeText.setStyle(codeText.getStyle().withColor(GOLD)
                .withClickEvent(new ClickEvent.CopyToClipboard(code.toString()))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy!"))));

        MutableComponent message = Component.literal("").append(line1).append(line2).append(link)
                .append(line3).append(codeText);
        player.sendSystemMessage(message);
        BackendInstance.LOGGER.warn(player.getPlainTextName() + " had an illegal item");
    }
    
    public static Integer saveInventoryReport(ServerPlayer player, ItemStack stack) {
        player.setGameMode(GameType.SURVIVAL);
        Integer code = (int) (Math.floor(Math.random() * 999999) + 100000);
        JsonObject report = buildBaseReport(player, stack, code);
        report.addProperty("type", "inventory");
        
        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        
        report.add("playerPosition", position);
        
        saveReport(report);
        sendMessage(player, "An automated scan has successfully found and removed illegal item(s) in your inventory", code);
        return (int) code;
    }
    
    public static Integer saveStorageReport(ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setGameMode(GameType.SURVIVAL);
        Integer code = (int) (Math.floor(Math.random() * 999999) + 100000);
        JsonObject report = buildBaseReport(player, stack, code);
        report.addProperty("type", "storage");
        
        JsonObject position = new JsonObject();
        position.addProperty("x", pos.getX());
        position.addProperty("y", pos.getY());
        position.addProperty("z", pos.getZ());
        
        report.add("storagePosition", position);
        
        saveReport(report);
        sendMessage(player, "An automated scan has successfully found and removed illegal item(s) in a storage block near you", code);
        return (int) code;
    }
    
    /* ===================== INTERNAL ===================== */
    
    private static JsonObject buildBaseReport(ServerPlayer player, ItemStack stack, Integer code) {
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
                if (Config.ENABLE_REPORT_FILE) {
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
                }

                if (Config.ENABLE_DISCORD_REPORTS) {
                    if (!WEBHOOK_URL.isBlank()) {
                        sendDiscordWebhook(report);
                    } else {
                        BackendInstance.LOGGER.warn("Discord reporting is enabled but DISCORD_WEBHOOK_URL is not configured.");
                    }
                }
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
        