package dev.fixpot47.fixpotsserverplaytime.mixin;

import dev.fixpot47.fixpotsserverplaytime.FixpotsServerPlaytimeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class MultiplayerServerEntryMixin {
    @Inject(method = "extractContent", at = @At("TAIL"))
    private void fixpot$showPlaytime(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        ServerSelectionList.OnlineServerEntry entry = (ServerSelectionList.OnlineServerEntry) (Object) this;
        ServerData server = entry.getServerData();
        if (server == null || server.isRealm()) {
            return;
        }

        String text = "Playtime: " + FixpotsServerPlaytimeClient.multiplayerPlaytime(server.ip);
        Minecraft client = Minecraft.getInstance();
        int x = entry.getContentX() + entry.getContentWidth() - client.font.width(text) - 5;
        int y = entry.getContentY() + entry.getContentHeight() - 10;
        graphics.text(client.font, text, x, y, 0xFFAAAAAA, false);
    }
}
