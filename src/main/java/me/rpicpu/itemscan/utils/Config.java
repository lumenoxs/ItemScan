package me.rpicpu.itemscan.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import me.rpicpu.itemscan.ItemScan;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("itemscan.json");
    private static Data data;

    private Config() {}

    public static void load() {
        try {
            if (Files.notExists(CONFIG_FILE)) {
                data = new Data();
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                data = GSON.fromJson(reader, Data.class);
            }

            if (data == null) {
                data = new Data();
                save();
            }

            if (data.configVersion < ItemScan.configVersion) {
                data = new Data();
                save();
            }

            normalize(data);
        } catch (Exception e) {
            data = new Data();
            normalize(data);
            e.printStackTrace();
        }
    }

    public static void reload() { load(); }

    private static void normalize(Data config) {
        if (config.command == null) {
            config.command = new Command();
        }
        if (config.inventoryScan == null) {
            config.inventoryScan = new InventoryScan();
        }
        if (config.positionScan == null) {
            config.positionScan = new PositionScan();
        }
        if (config.tracking == null) {
            config.tracking = new Tracking();
        }
        if (config.reports == null) {
            config.reports = new Reports();
        }
        if (config.reports.file == null) {
            config.reports.file = new FileReport();
        }
        if (config.reports.categories == null) {
            config.reports.categories = new CategoryReports();
        }
        if (config.reports.categories.repairCost == null) {
            config.reports.categories.repairCost = new DiscordReport();
        }
        if (config.reports.categories.dupeUuid == null) {
            config.reports.categories.dupeUuid = new DiscordReport();
        }
        if (config.reports.categories.severe == null) {
            config.reports.categories.severe = new DiscordReport();
        }
        if (config.blacklistedItems == null) {
            config.blacklistedItems = new HashSet<>();
        }
        if (config.blacklistedItemPatterns == null) {
            config.blacklistedItemPatterns = new HashSet<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Data get() { return data; }

    public static final class Data {
        public boolean enabled = true;
        public Command command = new Command();
        public InventoryScan inventoryScan = new InventoryScan();
        public PositionScan positionScan = new PositionScan();
        public Tracking tracking = new Tracking();
        public Reports reports = new Reports();
        public Set<String> blacklistedItems = new HashSet<>(Set.of(
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:command_block_minecart",
            "minecraft:structure_block",
            "minecraft:jigsaw",
            "minecraft:structure_void",
            "minecraft:barrier",
            "minecraft:light",
            "minecraft:debug_stick",
            "minecraft:knowledge_book",
            "minecraft:spawner",
            "minecraft:end_portal_frame",
            "minecraft:bedrock"
        ));
        public Set<String> blacklistedItemPatterns = new HashSet<>(Set.of(
            "spawn_egg"
        ));
        public boolean debug = false;
        public int configVersion = ItemScan.configVersion;
    }

    public static final class Command {
        public boolean enabled = true;
        public Set<String> allowedPlayers = new HashSet<>(Set.of(
            "RPiCPU"
        ));;
        public Set<String> allowedPlayersUuids = new HashSet<>(Set.of(
            "a45eb56f-022a-4d6d-a978-96143df3cde6"
        ));;
    }

    public static final class InventoryScan {
        public boolean enabled = true;
        public int interval = 5;
    }

    public static final class PositionScan {
        public boolean enabled = true;
        public ScanRange small = new ScanRange(20, 4, 0);
        public ScanRange medium = new ScanRange(40, 7, 4);
        public ScanRange large = new ScanRange(100, 8, 7);
    }

    public static final class ScanRange {
        public boolean enabled = true;
        public int interval;
        public int radius;
        public int innerRadius;

        public ScanRange() {}

        public ScanRange(int interval, int radius, int innerRadius) {
            this.interval = interval;
            this.radius = radius;
            this.innerRadius = innerRadius;
        }
    }

    public static final class Tracking {
        public boolean dupe = true;
    }

    public static final class Reports {

        public FileReport file = new FileReport();

        public CategoryReports categories = new CategoryReports();
    }

    public static final class CategoryReports {
        public DiscordReport repairCost = new DiscordReport();
        public DiscordReport dupeUuid = new DiscordReport();
        public DiscordReport severe = new DiscordReport();
    }

    public static final class FileReport {
        public boolean enabled = true;
        public String path = "illegal_item_reports.json";
    }

    public static final class DiscordReport {
        public boolean enabled = true;
        public String webhookUrl = "";
    }
}