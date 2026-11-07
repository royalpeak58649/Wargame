package kr.lobcorp.client.screen;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.net.Payloads;
import kr.lobcorp.work.WorkType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 환상체 우클릭 시 뜨는 관리 화면 */
public class WorkScreen extends Screen {
    private static final String[] STAT_UP = {"용기 상승", "신중 상승", "자제 상승", "정의 상승"};

    private final Payloads.OpenWork data;
    private final AbnoProfile prof;
    private int panelX, panelY;
    private static final int PW = 360, PH = 196;

    public WorkScreen(Payloads.OpenWork data) {
        super(Component.literal("관리 업무"));
        this.data = data;
        this.prof = AbnoProfile.BY_ID.getOrDefault(data.abnoId(), AbnoProfile.ONE_SIN);
    }

    @Override
    protected void init() {
        panelX = (width - PW) / 2;
        panelY = (height - PH) / 2;
        if (prof.isTool()) {
            int tw = 160;
            Button use = Button.builder(Component.literal("사용"), btn -> send(0))
                    .bounds(panelX + (PW - tw) / 2, panelY + 112, tw, 20)
                    .tooltip(Tooltip.create(Component.literal(prof.name())))
                    .build();
            use.active = !data.busy();
            addRenderableWidget(use);
            return;
        }
        int bw = 68, gap = 6;
        int rowX = panelX + (PW - (bw * 4 + gap * 3)) / 2;
        int rowY = panelY + 112;
        for (int i = 0; i < 4; i++) {
            WorkType t = WorkType.byIndex(i);
            int idx = i;
            String label = (i == 2 && prof.id().equals("freischutz")) ? "의뢰 (에너지 10%)" : t.ko;
            Button b = Button.builder(Component.literal(label).withStyle(s -> s.withColor(t.rgb)), btn -> send(idx))
                    .bounds(rowX + i * (bw + gap), rowY, bw, 20)
                    .tooltip(Tooltip.create(Component.literal(STAT_UP[i] + " · 기본 성공률 " + prof.chances()[i] + "%")))
                    .build();
            b.active = !data.busy();
            addRenderableWidget(b);
        }
        int ew = 160;
        int ex = panelX + (PW - (ew * 2 + gap)) / 2;
        int ey = rowY + 28;
        String wName = new ItemStack(prof.weapon().get()).getHoverName().getString();
        String sName = new ItemStack(prof.suit().get()).getHoverName().getString();
        Button w = Button.builder(Component.literal("E.G.O 무기 추출 (" + prof.weaponCost() + " PE)"), btn -> send(11))
                .bounds(ex, ey, ew, 20).tooltip(Tooltip.create(Component.literal(wName))).build();
        Button s = Button.builder(Component.literal("E.G.O 방어구 추출 (" + prof.suitCost() + " PE)"), btn -> send(12))
                .bounds(ex + ew + gap, ey, ew, 20).tooltip(Tooltip.create(Component.literal(sName))).build();
        w.active = data.pe() >= prof.weaponCost();
        s.active = data.pe() >= prof.suitCost();
        addRenderableWidget(w);
        addRenderableWidget(s);
    }

    private void send(int action) {
        ClientPlayNetworking.send(new Payloads.WorkAction(data.entityId(), action));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = panelX, y = panelY;
        g.fill(x, y, x + PW, y + 104, 0xD0101010);
        g.fill(x, y, x + PW, y + 2, 0xFF000000 | prof.riskColor());

        g.text(font, prof.code(), x + 8, y + 8, 0xFFAAAAAA, false);
        int codeW = font.width(prof.code() + "  ");
        g.text(font, prof.risk(), x + 8 + codeW, y + 8, 0xFF000000 | prof.riskColor(), false);
        g.text(font, prof.name(), x + 8, y + 20, 0xFFF0E6C8, true);

        if (prof.isTool()) {
            g.text(font, "도구형 환상체", x + 8, y + 34, 0xFF55FFFF, false);
            g.text(font, "작업도 PE-BOX도 없습니다. 직접 사용해 보십시오.", x + 8, y + 46, 0xFF777777, false);
        } else {
            String peLine = "추출 가능 PE " + data.pe() + "    최대 PE-BOX " + prof.maxBox()
                    + "    피해 " + prof.workDamage().label + " " + prof.dmgMin() + "~" + prof.dmgMax();
            g.text(font, peLine, x + 8, y + 34, 0xFF55FFFF, false);
            if (prof.canBreach()) {
                int col = data.qliphoth() <= 0 ? 0xFFFF5555 : 0xFFFFAA00;
                g.text(font, "클리포트 카운터 " + data.qliphoth() + " / " + prof.qliphothMax(), x + 8, y + 46, col, false);
            } else {
                g.text(font, "클리포트 카운터 없음", x + 8, y + 46, 0xFF777777, false);
            }
            g.text(font, "결과  좋음 " + prof.goodMin() + "~" + prof.maxBox() + "  /  보통 " + prof.normalMin() + "~" + (prof.goodMin() - 1)
                    + "  /  나쁨 0~" + (prof.normalMin() - 1), x + 8, y + 58, 0xFFAAAAAA, false);
        }
        int ny = y + 72;
        for (String note : prof.notes()) {
            g.text(font, "· " + note, x + 8, ny, 0xFFCCCCCC, false);
            ny += 10;
        }
        if (data.busy()) {
            String msg = "다른 직원이 작업 중입니다.";
            g.text(font, msg, x + (PW - font.width(msg)) / 2, y + 168, 0xFFFF5555, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
