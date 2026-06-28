package me.rpicpu.itemscan.services;

import me.rpicpu.itemscan.ItemScan;
import me.rpicpu.itemscan.data.ViolationCategory;
import me.rpicpu.itemscan.utils.Config;
import me.rpicpu.itemscan.utils.DiscordWebhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static int reportViolation(
        ViolationCategory category,
        ServerPlayer player,
        ItemStack stack,
        BlockPos pos,
        boolean isEnderChest,
        String reason
    ) {
        player.setGameMode(GameType.SURVIVAL);
        
        int code = (int) (Math.floor(Math.random() * 999999) + 100000);
        
        JsonObject report = buildReport(category, player, stack, code, pos, isEnderChest, reason);
        persistReport(category, report);
        
        sendMessage(player, code);
        ItemScan.LOGGER.warn(player.getPlainTextName() + " triggered " + category.name() + " violation (code " + code + ")");
        
        return code;
    }
    
    @SuppressWarnings("null")
    public static ItemStack createNotificationPaper(MutableComponent plainText) {
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_NAME, Component.literal("ItemScan Violation Notice"));
        
        List<Component> lore = new ArrayList<>();
        for (String line : plainText.getString().split("\n")) lore.add(Component.literal(line));
        paper.set(DataComponents.LORE, new ItemLore(lore));
        
        return paper;
    }

    @SuppressWarnings("null")
    public static MutableComponent buildMessage(int code, boolean styles) {
        MutableComponent line1 = Component.literal("A scan found an non-vanilla item, and it has been removed.\nFor more info, go here: ");
        MutableComponent link = Component.literal("https://www.truevanilla.net/wiki/ItemScan/Appeal");
        MutableComponent line3 = Component.literal("\nCode: "+Integer.toString(code));
        
        if (styles) {
            link.setStyle(link.getStyle()
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://www.truevanilla.net/wiki/ItemScan/Appeal")))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open the appeal page!")))
            );
            line3.setStyle(line3.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(Integer.toString(code)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy!")))
            );
        }
        
        return Component.literal("").append(line1).append(link).append(line3);
    }
    
    @SuppressWarnings("null")
    public static void sendMessage(ServerPlayer player, int code) {
        player.sendSystemMessage(buildMessage(code, true));
    }
    
    private static JsonObject buildReport(
        ViolationCategory category,
        ServerPlayer player,
        ItemStack stack,
        int code,
        BlockPos pos,
        boolean isEnderChest,
        String reason
    ) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("category", category.name());
        json.addProperty("reason", reason);
        json.addProperty("playerName", player.getName().getString());
        json.addProperty("playerUUID", player.getUUID().toString());
        json.addProperty("code", code);
        
        if (pos == null) {
            json.addProperty("type", isEnderChest ? "ender_chest" : "inventory");
            JsonObject position = new JsonObject();
            position.addProperty("x", player.getX());
            position.addProperty("y", player.getY());
            position.addProperty("z", player.getZ());
            json.add("playerPosition", position);
        } else {
            json.addProperty("type", "storage");
            JsonObject position = new JsonObject();
            position.addProperty("x", pos.getX());
            position.addProperty("y", pos.getY());
            position.addProperty("z", pos.getZ());
            json.add("storagePosition", position);
        }
        
        JsonObject item = new JsonObject();
        item.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        item.addProperty("count", stack.getCount());
        item.addProperty("displayName", stack.getHoverName().getString());
        item.add("enchantments", GSON.toJsonTree(stack.getEnchantments()));
        json.add("item", item);
        
        return json;
    }
    
    @SuppressWarnings("null")
    private static synchronized void persistReport(ViolationCategory category, JsonObject report) {
        try {
            var fileReport = Config.get().reports.file;
            if (fileReport.enabled) {
                Path reportFile = Paths.get(fileReport.path);
                JsonArray reports;
                
                if (Files.exists(reportFile)) {
                    String existing = Files.readString(reportFile);
                    if (existing == null || existing.isBlank()) {
                        reports = new JsonArray();
                    } else {
                        reports = GSON.fromJson(existing, JsonArray.class);
                    }
                } else {
                    reports = new JsonArray();
                }
                
                reports.add(report);
                
                Files.writeString(
                    reportFile,
                    GSON.toJson(reports),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );
            }
            
            var discordReport = getCategoryReport(category);
            if (discordReport.enabled) {
                if (!discordReport.webhookUrl.isBlank()) {
                    sendDiscordWebhook(report, discordReport.webhookUrl, category);
                } else {
                    ItemScan.LOGGER.warn("Discord reporting is enabled for " + category.name() + " but webhookUrl is not configured.");
                }
            }
        } catch (IOException e) {
            ItemScan.LOGGER.error("Failed to save report", e);
        }
    }
    
    private static Config.DiscordReport getCategoryReport(ViolationCategory category) {
        var categories = Config.get().reports.categories;
        return switch (category) {
            case REPAIR_COST -> categories.repairCost;
            case DUPE_UUID -> categories.dupeUuid;
            case SEVERE -> categories.severe;
        };
    }
    
    private static void sendDiscordWebhook(JsonObject report, String webhookUrl, ViolationCategory category) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
            webhook.setUsername("TVC Backend Reporter");
            
            DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
            .setTitle(category.displayName() + " Report")
            .setColor(Color.RED)
            .setDescription("An item scan violation was detected.");
            
            embed.addField("Category", category.displayName(), true);
            embed.addField("Player", report.get("playerName").getAsString(), true);
            embed.addField("UUID", report.get("playerUUID").getAsString(), true);
            embed.addField("Type", report.get("type").getAsString(), true);
            embed.addField("Reason", report.get("reason").getAsString(), false);
            
            JsonObject item = report.getAsJsonObject("item");
            embed.addField("Item ID", item.get("id").getAsString(), false);
            embed.addField("Display Name", item.get("displayName").getAsString(), true);
            embed.addField("Count", String.valueOf(item.get("count").getAsInt()), true);
            embed.addField("Code", String.valueOf(report.get("code").getAsInt()), false);
            
            if (report.has("playerPosition")) {
                JsonObject pos = report.getAsJsonObject("playerPosition");
                embed.addField(
                    "Player Position",
                    String.format("X: %d, Y: %d, Z: %d", pos.get("x").getAsInt(), pos.get("y").getAsInt(), pos.get("z").getAsInt()),
                    false
                );
            } else if (report.has("storagePosition")) {
                JsonObject pos = report.getAsJsonObject("storagePosition");
                embed.addField(
                    "Storage Position",
                    String.format("X: %d, Y: %d, Z: %d", pos.get("x").getAsInt(), pos.get("y").getAsInt(), pos.get("z").getAsInt()),
                    false
                );
            }
            
            embed.addField("Timestamp", report.get("timestamp").getAsString(), false);
            
            webhook.addEmbed(embed);
            webhook.execute();
        } catch (Exception e) {
            ItemScan.LOGGER.error("Failed to send Discord webhook", e);
        }
    }
}
