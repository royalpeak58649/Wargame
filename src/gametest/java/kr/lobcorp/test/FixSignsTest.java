package kr.lobcorp.test;

import kr.lobcorp.work.Facility;
import kr.lobcorp.work.FacilityBuilder;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import java.util.ArrayList;
import java.util.List;

public class FixSignsTest implements FabricClientGameTest {
    static void log(String s) { System.out.println("[LOBCORP-TEST] " + s); }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var sv = sp.getServer();
            sp.getConnection().waitForChunksRender();
            sv.runCommand("lobcorp facility");
            ctx.waitTicks(20);
            String r = sv.computeOnServer(s -> {
                var level = s.overworld();
                // 첫 실행: 새 시설은 이미 맞아야 한다
                int first = FacilityBuilder.fixSephirahSigns(level, Facility.get(s).slots);
                // 옛 버전처럼 망가뜨린다
                int broke = 0;
                StringBuilder seen = new StringBuilder();
                var slots = Facility.get(s).slots;
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
                for (var sl : slots) { minX = Math.min(minX, sl.origin.getX()); maxX = Math.max(maxX, sl.origin.getX()); minZ = Math.min(minZ, sl.origin.getZ()); maxZ = Math.max(maxZ, sl.origin.getZ()); }
                List<SignBlockEntity> signs = new ArrayList<>();
                for (int cx = (minX - 48) >> 4; cx <= (maxX + 48) >> 4; cx++)
                    for (int cz = (minZ - 48) >> 4; cz <= (maxZ + 48) >> 4; cz++)
                        for (var be : level.getChunk(cx, cz).getBlockEntities().values())
                            if (be instanceof SignBlockEntity sb) signs.add(sb);
                for (var sb : signs) {
                    List<Component> lines = new ArrayList<>(sb.getText(SignTextSlot.FRONT).getMessages(false));
                    String l0 = lines.get(0).getString();
                    String bad = l0.equals("안전팀") ? "호드" : l0.equals("교육팀") ? "네짜흐" : null;
                    if (bad == null) continue;
                    lines.set(1, Component.literal(bad));
                    sb.setText(new SignText(lines, lines, DyeColor.WHITE, true), SignTextSlot.FRONT);
                    broke++;
                }
                int fixed = FacilityBuilder.fixSephirahSigns(level, slots);
                for (var sb : signs) {
                    var m = sb.getText(SignTextSlot.FRONT).getMessages(false);
                    String l0 = m.get(0).getString();
                    if (l0.equals("안전팀") || l0.equals("교육팀")) seen.append(l0).append('=').append(m.get(1).getString()).append(' ');
                }
                return "first=" + first + " broke=" + broke + " fixed=" + fixed + " now: " + seen;
            });
            log("FIXSIGNS " + r);
        }
    }
}
