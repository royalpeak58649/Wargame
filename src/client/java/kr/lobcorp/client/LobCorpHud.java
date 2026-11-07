package kr.lobcorp.client;

import kr.lobcorp.work.WorkType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 좌상단 SP 바 + 작업 중 PE-BOX 진행 표시 */
public final class LobCorpHud {
    private LobCorpHud() {}

    public static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientState.received || mc.player == null) return;
        Font font = mc.font;

        // ── SP 바 ──
        int x = 6, y = 6, w = 82;
        int sp = Math.max(0, ClientState.sp()), max = ClientState.spMax();
        g.fill(x - 1, y - 1, x + w + 1, y + 7, 0xC0000000);
        g.fill(x, y, x + (int) (w * Math.min(1f, sp / (float) max)), y + 6, ClientState.panic() > 0 ? 0xFF777777 : 0xFF3FD8E8);
        g.text(font, "SP " + sp + "/" + max, x + w + 5, y - 1, 0xFFFFFFFF, true);
        int line = y + 10;
        if (ClientState.panic() > 0) {
            g.text(font, "패닉 " + (ClientState.panic() / 20 + 1) + "초", x, line, 0xFFFF5555, true);
            line += 10;
        }
        if (ClientState.fairy() > 0) {
            g.text(font, "요정의 축복 " + (ClientState.fairy() / 20 + 1) + "초 · 다른 환상체 작업 금지", x, line, 0xFF7CFFE0, true);
            line += 10;
        }
        if (ClientState.sleep() > 0) {
            g.text(font, "잠듦 " + (ClientState.sleep() / 20 + 1) + "초 · 피해를 받으면 깹니다", x, line, 0xFFC9C3E6, true);
        }

        // ── 작업 진행 ──
        if (ClientState.working()) {
            int n = Math.max(1, ClientState.maxBox());
            int size = 8, gap = 2;
            int total = n * size + (n - 1) * gap;
            int sx = (g.guiWidth() - total) / 2;
            int sy = g.guiHeight() - 62;
            WorkType t = WorkType.byIndex(ClientState.workType());
            String label = t.ko + " 작업  " + ClientState.good() + " / " + n;
            g.text(font, label, (g.guiWidth() - font.width(label)) / 2, sy - 11, 0xFF000000 | t.rgb, true);
            int fails = ClientState.box() - ClientState.good();
            for (int i = 0; i < n; i++) {
                int col;
                if (i < ClientState.good()) col = 0xFF55FF55;
                else if (i < ClientState.good() + fails) col = 0xFFFF5555;
                else col = 0xFF333333;
                int bx = sx + i * (size + gap);
                g.fill(bx - 1, sy - 1, bx + size + 1, sy + size + 1, 0xC0000000);
                g.fill(bx, sy, bx + size, sy + size, col);
            }
        }
    }
}
