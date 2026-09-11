package dev.fixpot47.fixpotsserverplaytime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

final class ServerPlaytimeStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fixpots-server-playtime.json");
    private static final Path TEMP_FILE = FabricLoader.getInstance().getConfigDir().resolve("fixpots-server-playtime.json.tmp");

    private final Map<String, Long> millisByKey = new HashMap<>();

    synchronized void load() {
        millisByKey.clear();
        if (!Files.exists(FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> item : root.entrySet()) {
                JsonElement value = item.getValue();
                long millis = 0L;

                // Current format: "server:example.com": 123456
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    millis = value.getAsLong();
                }
                // Migration from older versions:
                // "server:example.com": { "serverName": "Example", "millis": 123456 }
                else if (value != null && value.isJsonObject()) {
                    JsonElement oldMillis = value.getAsJsonObject().get("millis");
                    if (oldMillis != null && oldMillis.isJsonPrimitive() && oldMillis.getAsJsonPrimitive().isNumber()) {
                        millis = oldMillis.getAsLong();
                    }
                }

                if (millis > 0L) {
                    millisByKey.put(item.getKey(), millis);
                }
            }
        } catch (Exception exception) {
            System.err.println("[fixpot's Server Playtime] Could not load playtime data: " + exception.getMessage());
        }
    }

    synchronized long getMillis(String key) {
        return Math.max(0L, millisByKey.getOrDefault(key, 0L));
    }

    synchronized void addMillis(String key, String serverName, long millis) {
        if (key == null || millis <= 0L) {
            return;
        }

        long current = Math.max(0L, millisByKey.getOrDefault(key, 0L));
        millisByKey.put(key, current + millis);
    }

    synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());

            try (Writer writer = Files.newBufferedWriter(TEMP_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(millisByKey, writer);
            }

            try {
                Files.move(
                        TEMP_FILE,
                        FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(TEMP_FILE, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[fixpot's Server Playtime] Could not save playtime data: " + exception.getMessage());
        }
    }
}
