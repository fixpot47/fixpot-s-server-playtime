package dev.fixpot47.fixpotsserverplaytime;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class FixpotsServerPlaytimeClient implements ClientModInitializer {
    private static final long SAVE_INTERVAL_MS = 60_000L;
    private static final int JOIN_MESSAGE_DELAY_TICKS = 40;

    private final ServerPlaytimeStore store = new ServerPlaytimeStore();

    private String currentServerKey;
    private String currentServerName;
    private long lastAccountedAtMs;
    private int joinMessageTicks;
    private boolean joinMessageShown;

    @Override
    public void onInitializeClient() {
        store.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        ServerData server = client.getCurrentServer();

        if (client.getConnection() == null || server == null) {
            finishCurrentSession();
            return;
        }

        String key = normalizeServerKey(server.ip);
        String name = server.name == null || server.name.isBlank() ? server.ip : server.name;

        if (currentServerKey == null || !currentServerKey.equals(key)) {
            finishCurrentSession();
            startSession(key, name);
        }

        long now = System.currentTimeMillis();

        if (!joinMessageShown && client.player != null) {
            joinMessageTicks++;
            if (joinMessageTicks >= JOIN_MESSAGE_DELAY_TICKS) {
                long totalMillis = store.getMillis(currentServerKey) + Math.max(0L, now - lastAccountedAtMs);
                client.gui.hud.getChat().addClientSystemMessage(
                        Component.literal("Server playtime: " + formatDuration(totalMillis))
                );
                joinMessageShown = true;
            }
        }

        if (now - lastAccountedAtMs >= SAVE_INTERVAL_MS) {
            checkpoint(now);
        }
    }

    private void startSession(String key, String name) {
        currentServerKey = key;
        currentServerName = name;
        lastAccountedAtMs = System.currentTimeMillis();
        joinMessageTicks = 0;
        joinMessageShown = false;
    }

    private void checkpoint(long now) {
        if (currentServerKey == null) {
            return;
        }

        long delta = Math.max(0L, now - lastAccountedAtMs);
        store.addMillis(currentServerKey, currentServerName, delta);
        lastAccountedAtMs = now;
        store.save();
    }

    private void finishCurrentSession() {
        if (currentServerKey == null) {
            return;
        }

        checkpoint(System.currentTimeMillis());
        currentServerKey = null;
        currentServerName = null;
        lastAccountedAtMs = 0L;
        joinMessageTicks = 0;
        joinMessageShown = false;
    }

    private static String normalizeServerKey(String address) {
        return address == null ? "unknown" : address.strip().toLowerCase(Locale.ROOT);
    }

    private static String formatDuration(long millis) {
        long totalMinutes = Math.max(0L, millis) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;

        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }
}
