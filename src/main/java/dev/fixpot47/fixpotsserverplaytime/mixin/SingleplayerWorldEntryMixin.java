package dev.fixpot47.fixpotsserverplaytime.mixin;

import dev.fixpot47.fixpotsserverplaytime.FixpotsServerPlaytimeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class SingleplayerWorldEntryMixin {
    @Inject(method = "extractContent", at = @At("TAIL"))
    private void fixpot$showPlaytime(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        WorldSelectionList.WorldListEntry entry = (WorldSelectionList.WorldListEntry) (Object) this;
        LevelSummary summary = entry.getLevelSummary();
        if (summary == null) {
            return;
        }

        String text = "Playtime: " + FixpotsServerPlaytimeClient.singleplayerPlaytime(summary.getLevelId());
        Minecraft client = Minecraft.getInstance();

        // Keep playtime on the same top line as the world name,
        // aligned to the right side of the world entry so it never sits
        // on top of the mode/version text below.
        int x = entry.getContentX() + entry.getContentWidth() - client.font.width(text) - 5;
        int y = entry.getContentY() + 2;

        graphics.text(client.font, text, x, y, 0xFFAAAAAA, false);
    }
}
