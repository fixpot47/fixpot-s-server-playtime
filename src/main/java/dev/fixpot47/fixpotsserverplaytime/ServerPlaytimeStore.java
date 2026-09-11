package dev.fixpot47.fixpotsserverplaytime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class ServerPlaytimeStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<String, Entry>>() { }.getType();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fixpots-server-playtime.json");

    private final Map<String, Entry> entries = new HashMap<>();

    void load() {
        entries.clear();
        if (!Files.exists(FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Map<String, Entry> loaded = GSON.fromJson(reader, DATA_TYPE);
            if (loaded != null) {
                entries.putAll(loaded);
            }
        } catch (Exception ignored) {
        }
    }

    long getMillis(String key) {
        Entry entry = entries.get(key);
        return entry == null ? 0L : Math.max(0L, entry.millis);
    }

    void addMillis(String key, String serverName, long millis) {
        if (millis <= 0L) {
            return;
        }

        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        entry.serverName = serverName;
        entry.millis = Math.max(0L, entry.millis) + millis;
    }

    void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(entries, DATA_TYPE, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class Entry {
        String serverName = "";
        long millis = 0L;
    }
}
