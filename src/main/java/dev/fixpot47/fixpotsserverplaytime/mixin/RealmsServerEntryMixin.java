package dev.fixpot47.fixpotsserverplaytime.mixin;

import com.mojang.realmsclient.dto.RealmsServer;
import dev.fixpot47.fixpotsserverplaytime.FixpotsServerPlaytimeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.realmsclient.RealmsMainScreen$ServerEntry")
public abstract class RealmsServerEntryMixin {
    @Shadow
    public abstract RealmsServer getServer();

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void fixpot$showPlaytime(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        RealmsServer server = getServer();
        if (server == null) {
            return;
        }

        ObjectSelectionList.Entry<?> entry = (ObjectSelectionList.Entry<?>) (Object) this;
        String text = "Playtime: " + FixpotsServerPlaytimeClient.realmPlaytime(server.name);
        Minecraft client = Minecraft.getInstance();
        int x = entry.getContentX() + entry.getContentWidth() - client.font.width(text) - 5 - 10;
        int y = entry.getContentY() + entry.getContentHeight() - 10;
        graphics.text(client.font, text, x, y, 0xFFAAAAAA, false);
    }
}
