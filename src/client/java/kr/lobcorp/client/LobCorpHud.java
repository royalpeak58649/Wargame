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
            line += 10;
        }
        if (ClientState.possess() > 0) {
            g.text(font, "홀림 " + (ClientState.possess() / 20 + 1) + "초 · 다른 직원이 세 번 때리면 풀립니다", x, line, 0xFFFF5555, true);
            line += 10;
        }
        if (ClientState.heart()) {
            g.text(font, "♥ 라티샤의 선물 · 다른 환상체 작업 시 위험", x, line, 0xFFFF9AD0, true);
            line += 10;
        }
        if (ClientState.sheltered()) {
            g.text(font, "대피소 안 · 피해 무효", x, line, 0xFF7CFFE0, true);
            line += 10;
        }
        if (ClientState.notes() > 0) {
            if (ClientState.notes() < 30) {
                g.text(font, "노트 보유 " + ClientState.notes() + "초 · 30초 전 반납 금지", x, line, 0xFFFFAA00, true);
            } else {
                g.text(font, "노트 보유 " + ClientState.notes() + "초 · 반납 가능", x, line, 0xFF55FF55, true);
            }
            line += 10;
        }
        if (ClientState.pebble()) {
            g.text(font, "★ 우정의 조약돌 · 다른 환상체 작업 시 위험", x, line, 0xFF9AD0FF, true);
            line += 10;
        }
        if (ClientState.frost() == 1) {
            g.text(font, "❄ 심장의 얼음 조각", x, line, 0xFF7CFFE0, true);
            line += 10;
        } else if (ClientState.frost() == 2) {
            g.text(font, "❄ 얼어붙음 · 누군가 눈의 여왕과 결투해야 합니다", x, line, 0xFF7CFFE0, true);
            line += 10;
        }
        if (ClientState.blessed()) {
            g.text(font, "✦ 절망의 기사의 가호 · 작업 불가", x, line, 0xFF9AD0FF, true);
            line += 10;
        }
        if (ClientState.portrait() == 1) {
            g.text(font, "초상 · 당신의 상처는 다른 사람이 받습니다", x, line, 0xFFB46CFF, true);
        } else if (ClientState.portrait() == 2) {
            g.text(font, "초상 · 누군가의 상처가 당신에게 옵니다", x, line, 0xFF8A1A1A, true);
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
