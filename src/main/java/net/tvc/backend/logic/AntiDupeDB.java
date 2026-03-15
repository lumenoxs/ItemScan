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

    // read dupe id json file
    public static Map<?, ?> read(UUID dupeId) {
        try {
            Files.createDirectories(DB_DIR);
            Path file = DB_DIR.resolve(dupeId.toString() + ".json");
            if (!Files.exists(file))
                return null;
            try (FileReader reader = new FileReader(file.toFile())) {
                return GSON.fromJson(reader, Map.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // write dupe id json file
    public static void write(UUID dupeId, Object data) {
        Map<?, ?> dataMap = (Map<?, ?>) data;
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
    public static void addHistoryLog(UUID dupeId, String type, String info) {
        // read db
        Map<?, ?> db = read(dupeId);
        if (db == null)
            return;

        // get the history list, and create if it doesn't exist
        Object historyObj = db.get("history");
        List<Map<String, Object>> history;
        if (historyObj instanceof List) {
            history = (List<Map<String, Object>>) historyObj;
        } else {
            history = new ArrayList<>();
        }

        // create event map
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("info", info);
        event.put("time", Instant.now().toString());

        // add to history
        history.add(event);

        // write back to db
        Map<String, Object> writable = new HashMap<>(castMapToStringObjectMap(db));
        writable.put("history", history);
        write(dupeId, writable);
    }

    public static void updateLocation(UUID dupeId, String location) {
        // read db
        Map<?, ?> dbData = read(dupeId);
        if (dbData == null)
            return;

        // cast to writable map
        Map<String, Object> db = castMapToStringObjectMap(dbData);

        // see if location changed, and if it did add history transfer log
        String previousLocation = (String) db.get("currentLocation");
        if (previousLocation != null && !previousLocation.equals(location)) {
            addHistoryLog(dupeId, "transfer", previousLocation + " -> " + location);
        }

        // set new location and write to db
        db.put("currentLocation", location);
        write(dupeId, db);
    }

    public static void updateNBT(UUID dupeId, String nbt) {
        // read db
        Map<?, ?> dbData = read(dupeId);
        if (dbData == null)
            return;

        // cast to writable map
        Map<String, Object> db = castMapToStringObjectMap(dbData);

        // see if nbt changed, and if it did add history modify log
        String previousNBT = (String) db.get("currentNBT");
        if (previousNBT != null && !previousNBT.equals(nbt)) {
            addHistoryLog(dupeId, "modify", previousNBT + ";" + nbt);
        }

        // set new nbt and write to db
        db.put("currentNBT", nbt);
        write(dupeId, db);
    }

    private static Map<String, Object> castMapToStringObjectMap(Map<?, ?> map) {
        // this is a workaround for the fact that gson returns Map<String,Object> as
        // Map<?,?>
        // func made by ai
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put((String) entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static UUID register(ItemStack item) {
        UUID uuid = UUID.randomUUID();
        try {
            Files.createDirectories(DB_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String type = item == null ? "unknown" : item.getItem().toString();

        Map<String, Object> db = new HashMap<>();
        db.put("type", type);
        db.put("currentLocation", "unknown");
        db.put("currentNBT", "{}");

        List<Map<String, Object>> history = new ArrayList<>();
        Map<String, Object> creation = new HashMap<>();
        creation.put("type", "creation");
        creation.put("info", uuid.toString());
        creation.put("time", Instant.now().toString());
        history.add(creation);

        db.put("history", history);

        write(uuid, db);
        return uuid;
    }
}
