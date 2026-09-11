package dev.fixpot47.fixpotsserverplaytime;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.integrated.IntegratedServer;

import java.nio.file.Path;
import java.util.Locale;

public final class FixpotsServerPlaytimeClient implements ClientModInitializer {
    private static final long SAVE_INTERVAL_MS = 30_000L;
    private static final ServerPlaytimeStore STORE = new ServerPlaytimeStore();

    private static String pendingRealmKey;
    private static String pendingRealmName;

    private String currentKey;
    private String currentName;
    private long lastAccountedAtMs;

    @Override
    public void onInitializeClient() {
        STORE.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        SessionTarget target = resolveTarget(client);

        if (target == null) {
            finishCurrentSession();
            return;
        }

        if (currentKey == null || !currentKey.equals(target.key())) {
            finishCurrentSession();
            startSession(target.key(), target.name());
        }

        long now = System.currentTimeMillis();
        if (now - lastAccountedAtMs >= SAVE_INTERVAL_MS) {
            checkpoint(now);
        }
    }

    private SessionTarget resolveTarget(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }

        if (client.hasSingleplayerServer()) {
            IntegratedServer server = client.getSingleplayerServer();
            if (server == null) {
                return null;
            }

            Path directory = server.getServerDirectory();
            Path fileName = directory == null ? null : directory.getFileName();
            String levelId = fileName == null ? "unknown" : fileName.toString();
            String levelName = server.getWorldData().getLevelName();
            return new SessionTarget(worldKey(levelId), levelName == null || levelName.isBlank() ? levelId : levelName);
        }

        ServerData server = client.getCurrentServer();
        if (client.getConnection() == null || server == null) {
            return null;
        }

        if (server.isRealm() && pendingRealmKey != null) {
            return new SessionTarget(pendingRealmKey, pendingRealmName == null ? server.name : pendingRealmName);
        }

        String name = server.name == null || server.name.isBlank() ? server.ip : server.name;
        return new SessionTarget(serverKey(server.ip), name);
    }

    private void startSession(String key, String name) {
        currentKey = key;
        currentName = name == null ? "" : name;
        lastAccountedAtMs = System.currentTimeMillis();
    }

    private void checkpoint(long now) {
        if (currentKey == null) {
            return;
        }

        long delta = Math.max(0L, now - lastAccountedAtMs);
        STORE.addMillis(currentKey, currentName, delta);
        lastAccountedAtMs = now;
        STORE.save();
    }

    private void finishCurrentSession() {
        if (currentKey == null) {
            return;
        }

        checkpoint(System.currentTimeMillis());
        currentKey = null;
        currentName = null;
        lastAccountedAtMs = 0L;
    }

    public static void prepareRealm(long realmId, String realmName) {
        pendingRealmKey = realmKey(realmId);
        pendingRealmName = realmName;
    }

    public static String multiplayerPlaytime(String address) {
        return formatDuration(STORE.getMillis(serverKey(address)));
    }

    public static String singleplayerPlaytime(String levelId) {
        return formatDuration(STORE.getMillis(worldKey(levelId)));
    }

    public static String realmPlaytime(long realmId) {
        return formatDuration(STORE.getMillis(realmKey(realmId)));
    }

    public static String serverKey(String address) {
        String normalized = address == null ? "unknown" : address.strip().toLowerCase(Locale.ROOT);
        return "server:" + normalized;
    }

    public static String worldKey(String levelId) {
        String normalized = levelId == null ? "unknown" : levelId.strip();
        return "world:" + normalized;
    }

    public static String realmKey(long realmId) {
        return "realm:" + realmId;
    }

    public static String formatDuration(long millis) {
        long totalMinutes = Math.max(0L, millis) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;

        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }

    private record SessionTarget(String key, String name) {
    }
}
