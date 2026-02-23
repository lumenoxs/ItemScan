package net.tvc.backend.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.time.Instant;

public class AntiDupeDB {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DB_DIR = Paths.get(System.getProperty("user.home"), "AntiDupe-ItemsDB");

    public Map<?, ?> read(UUID dupeId) {
        try {
            Files.createDirectories(DB_DIR);
            Path file = DB_DIR.resolve(dupeId.toString() + ".json");
            if (!Files.exists(file)) return null;
            try (FileReader reader = new FileReader(file.toFile())) {
                return GSON.fromJson(reader, Map.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void write(UUID dupeId, Object data) {
        Map<?,?> dataMap = (Map<?,?>) data;
        try {
            Files.createDirectories(DB_DIR);
            Path file = DB_DIR.resolve(dupeId.toString() + ".json");
            try (FileWriter writer = new FileWriter(file.toFile())) {
                GSON.toJson(dataMap, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void addHistoryLog(UUID dupeId, String type, String info) {
        Map<?,?> db = read(dupeId);
        if (db == null) return;

        Object historyObj = db.get("history");
        List<Map<String,Object>> history;
        if (historyObj instanceof List) {
            history = (List<Map<String,Object>>) historyObj;
        } else {
            history = new ArrayList<>();
        }

        Map<String,Object> event = new HashMap<>();
        event.put("type", type);
        event.put("info", info);
        event.put("time", Instant.now().toString());

        history.add(event);

        Map<String,Object> writable = new HashMap<>();
        writable.put("type", db.get("type"));
        writable.put("history", history);

        write(dupeId, writable);
    }

    public UUID register(ItemStack item) {
        UUID uuid = UUID.randomUUID();
        try {
            Files.createDirectories(DB_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String type = item == null ? "unknown" : item.getItem().toString();

        Map<String,Object> db = new HashMap<>();
        db.put("type", type);

        List<Map<String,Object>> history = new ArrayList<>();
        Map<String,Object> creation = new HashMap<>();
        creation.put("type", "creation");
        creation.put("info", uuid.toString());
        creation.put("time", Instant.now().toString());
        history.add(creation);

        db.put("history", history);

        write(uuid, db);
        return uuid;
    }
}
