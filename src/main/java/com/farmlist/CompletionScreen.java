package com.farmlist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * A short "[SYSTEM] Collection complete" cutscene, about 2 seconds.
 * The screen dims, a cyan line sweeps across, and the text fades in line by line.
 * Click to skip. Then it returns to the lists menu.
 */
public class CompletionScreen extends Screen {

    private static final long TOTAL_MS = 2200;

    private final String listName;
    private final long startedAt;
    private boolean finished = false;

    public CompletionScreen(String listName) {
        super(Component.literal("Collection complete"));
        this.listName = listName;
        this.startedAt = System.currentTimeMillis();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.END_PORTAL_SPAWN, 1.0F));
    }

    /** 0 to 255, rising from 0 at "from" to 255 after "len" milliseconds. */
    private static int fade(long t, long from, long len) {
        if (t <= from) return 0;
        if (t >= from + len) return 255;
        return (int) (255 * (t - from) / len);
    }

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private void drawCentered(GuiGraphicsExtractor g, String text, int y, int alpha, int rgb) {
        if (alpha <= 0) return;
        Ui.textCenter(g, this.font, text, this.width / 2, y, argb(alpha, rgb), true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long t = System.currentTimeMillis() - startedAt;
        if (t >= TOTAL_MS) {
            finish();
            return;
        }

        int w = this.width;
        int h = this.height;
        int cy = h / 2;

        // 1. the screen dims to black
        int dim = fade(t, 0, 300) * 0xE0 / 255;
        g.fill(0, 0, w, h, argb(dim, 0x000000));

        // 2. a cyan line sweeps out from the middle
        int sweep = fade(t, 150, 300) * (w / 2) / 255;
        g.fill(w / 2 - sweep, cy - 1, w / 2 + sweep, cy + 1, argb(255, Ui.SYS_CYAN));

        // 3. the text, one line after another
        drawCentered(g, "[SYSTEM]", cy - 26, fade(t, 350, 250), Ui.SYS_CYAN);
        drawCentered(g, "Collection complete", cy - 8, fade(t, 650, 250), Ui.TEXT_DARK);
        drawCentered(g, listName, cy + 8, fade(t, 950, 250), Ui.WHITE);
        drawCentered(g, "click to skip", h - 20, fade(t, 1200, 300), Ui.TEXT_MUTED);
    }

    private void finish() {
        if (finished) return;
        finished = true;
        this.minecraft.gui.setScreen(new PickerScreen());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        finish();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
