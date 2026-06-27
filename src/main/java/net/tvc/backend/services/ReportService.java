package net.tvc.backend.services;

import net.tvc.backend.BackendInstance;
import net.tvc.backend.data.ViolationCategory;
import net.tvc.backend.utils.Config;
import net.tvc.backend.utils.DiscordWebhook;

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
import net.minecraft.network.chat.TextColor;
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
    private static final String WIKI_URL = "https://truevanilla.net/wiki/SCRAT/Inventory";

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
        String message = buildPlayerMessage(category, pos, isEnderChest);
        String plainText = buildPlainText(message, code);

        JsonObject report = buildReport(category, player, stack, code, pos, isEnderChest, reason);
        persistReport(category, report);

        sendMessage(player, message, code);
        BackendInstance.LOGGER.warn(
            player.getPlainTextName() + " triggered " + category.name() + " violation (code " + code + ")"
        );

        return code;
    }

    public static ItemStack createNotificationPaper(String plainText, int code) {
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_NAME, Component.literal("SCRAT Violation Notice #" + code));

        List<Component> lore = new ArrayList<>();
        for (String line : plainText.split("\n")) {
            lore.add(Component.literal(line));
        }
        paper.set(DataComponents.LORE, new ItemLore(lore));

        return paper;
    }

    @SuppressWarnings("null")
    public static void sendMessage(ServerPlayer player, String cmessage, int code) {
        TextColor darkRed = TextColor.fromRgb(0xAA0000);
        TextColor red = TextColor.fromRgb(0xFF5555);
        TextColor gold = TextColor.fromRgb(0xFFAA00);

        MutableComponent line1 = Component.literal(cmessage);
        MutableComponent line2 = Component.literal("\nFor more info, go here: ");
        MutableComponent link = Component.literal(WIKI_URL);
        MutableComponent line3 = Component.literal("\nCode: ");
        MutableComponent codeText = Component.literal(Integer.toString(code));

        line1.setStyle(line1.getStyle().withColor(darkRed));
        line2.setStyle(line2.getStyle().withColor(red));
        link.setStyle(link.getStyle().withColor(red).withUnderlined(true)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(WIKI_URL))));
        line3.setStyle(line3.getStyle().withColor(gold));
        codeText.setStyle(codeText.getStyle().withColor(gold)
            .withClickEvent(new ClickEvent.CopyToClipboard(Integer.toString(code)))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy!"))));

        MutableComponent message = Component.literal("")
            .append(line1)
            .append(line2)
            .append(link)
            .append(line3)
            .append(codeText);
        player.sendSystemMessage(message);
    }

    public static String buildPlayerMessage(ViolationCategory category, BlockPos pos, boolean isEnderChest) {
        return switch (category) {
            case REPAIR_COST -> pos == null
                ? "An automated scan found an item with an invalid repair cost in your inventory"
                : "An automated scan found an item with an invalid repair cost in a storage block near you";
            case DUPE_UUID -> pos == null
                ? "An automated scan found a duplicated item UUID in your inventory"
                : "An automated scan found a duplicated item UUID in a storage block near you";
            case SEVERE -> {
                if (pos != null) {
                    yield "An automated scan found illegal item(s) in a storage block near you and wiped its contents";
                }
                if (isEnderChest) {
                    yield "An automated scan found illegal item(s) in your ender chest and wiped your inventory and ender chest";
                }
                yield "An automated scan found illegal item(s) in your inventory and wiped your inventory and ender chest";
            }
        };
    }

    public static String buildPlainText(String message, int code) {
        return message + "\nFor more info, go here: " + WIKI_URL + "\nCode: " + code;
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
                    reports = GSON.fromJson(existing, JsonArray.class);
                    if (reports == null) {
                        reports = new JsonArray();
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
                    BackendInstance.LOGGER.warn(
                        "Discord reporting is enabled for " + category.name() + " but webhookUrl is not configured."
                    );
                }
            }
        } catch (IOException e) {
            BackendInstance.LOGGER.error("[IllegalItemReport] Failed to save report", e);
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
            BackendInstance.LOGGER.error("[IllegalItemReport] Failed to send Discord webhook", e);
        }
    }
}
