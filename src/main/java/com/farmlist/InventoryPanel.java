package com.farmlist;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * The pinned checklist, shown in a cyan panel beside your inventory.
 * It starts open; the button above it hides or shows it. Unpin is at the bottom.
 * It's drawn inside the inventory screen itself, so Spatial GUI can show it too.
 */
final class InventoryPanel {

    private static final int W = 150;
    private static final int H = 166;          // same height as the inventory
    private static final int ROW_H = 18;
    private static final int MAX_ROWS = 7;
    private static final int INV_W = 176;       // the inventory's own width

    private static boolean open = true;
    private static Button toggle;
    private static Button unpin;

    private InventoryPanel() {
    }

    private static int leftPos(int sw) {
        return (sw - INV_W) / 2;
    }

    private static int topPos(int sh) {
        return (sh - H) / 2;
    }

    /** To the right of the inventory if there's room, otherwise to the left. */
    private static int panelX(int sw) {
        int right = leftPos(sw) + INV_W + 6;
        return right + W <= sw - 4 ? right : leftPos(sw) - 6 - W;
    }

    static void init() {
        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (!(screen instanceof InventoryScreen)) return;
            Minecraft mc = Minecraft.getInstance();
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            int x = panelX(sw);
            int y = topPos(sh);

            toggle = Button.builder(Component.literal(open ? "Hide" : "Checklist"), b -> {
                open = !open;
                b.setMessage(Component.literal(open ? "Hide" : "Checklist"));
                unpin.setVisible(open);
            }).bounds(x, y - 16, W, 14).build();
            access.addRenderableWidget(toggle);

            unpin = Button.builder(Component.literal("Unpin"), b -> {
                FarmListClient.pin(null);
                FarmListClient.message("[SYSTEM] Unpinned.");
            }).bounds(x + 8, y + H - 22, W - 16, 16).build();
            unpin.setVisible(open);
            access.addRenderableWidget(unpin);
        });

        ClientGuiEvent.RENDER_POST.register((screen, g, mouseX, mouseY, delta) -> {
            if (screen instanceof InventoryScreen && open) {
                render(g);
            }
        });
    }

    private static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = panelX(sw);
        int y = topPos(sh);

        Ui.panel(g, x, y, W, H);
        Ui.textCenter(g, mc.font, "[SYSTEM] Pinned", x + W / 2, y + 7, Ui.TEXT_DARK, false);

        String file = FarmListClient.pinnedFile();
        if (file == null) {
            Ui.textCenter(g, mc.font, "Nothing pinned.", x + W / 2, y + 40, Ui.TEXT_DARK, false);
            Ui.textCenter(g, mc.font, "Pin a list from the", x + W / 2, y + 56, Ui.TEXT_MUTED, false);
            Ui.textCenter(g, mc.font, "Checklists menu.", x + W / 2, y + 66, Ui.TEXT_MUTED, false);
            return;
        }

        String name = FarmListClient.baseName(file);
        Progress progress = new Progress(name);
        Map<String, Integer> required = FarmListClient.parseRequired(FarmListClient.TRANSCRIPTION_DIR.resolve(file));

        int checked = 0;
        for (String id : required.keySet()) {
            if (progress.isChecked(id)) checked++;
        }

        Ui.text(g, mc.font, Ui.fit(mc.font, name, W - 16), x + 8, y + 22, Ui.WHITE, true);

        int barY = y + 34;
        int barW = W - 16;
        Ui.inset(g, x + 8, barY, barW, 10, 0xFF10283B);
        int total = Math.max(1, required.size());
        int fill = (barW - 2) * checked / total;
        g.fill(x + 9, barY + 1, x + 9 + fill, barY + 9, Ui.SYS_CYAN);
        Ui.text(g, mc.font, checked + "/" + required.size(), x + 8, barY + 14, Ui.TEXT_DARK, false);

        int rowY = barY + 28;
        int shown = 0;
        for (Map.Entry<String, Integer> row : required.entrySet()) {
            if (shown >= MAX_ROWS) break;
            String id = row.getKey();
            int need = row.getValue();
            Integer have = progress.getHave(id);
            String count = (have == null ? 0 : have) + "/" + need;

            FarmListClient.CatalogEntry entry = FarmListClient.entryFor(FarmListClient.countedId(id, progress));
            if (entry != null) {
                Ui.icon(g, entry.stack(), x + 8, rowY);
            }
            String label = entry != null ? entry.stack().getHoverName().getString() : id;
            int maxLabel = W - 36 - mc.font.width(count);
            Ui.text(g, mc.font, Ui.fit(mc.font, label, maxLabel), x + 28, rowY + 4,
                    progress.isChecked(id) ? Ui.SYS_CYAN : Ui.TEXT_DARK, false);
            Ui.text(g, mc.font, count, x + W - 8 - mc.font.width(count), rowY + 4, Ui.WHITE, false);

            rowY += ROW_H;
            shown++;
        }
    }
}
