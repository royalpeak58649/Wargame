package kr.lobcorp.test;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.work.FacilityBuilder;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.Extraction;
import kr.lobcorp.AbnoProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class FacilityTest implements FabricClientGameTest {
    static void log(String s) { System.out.println("[LOBCORP-TEST] " + s); }
    static ServerPlayer pl(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    static boolean passable(ServerLevel l, BlockPos p) {
        return l.getBlockState(p).getCollisionShape(l, p).isEmpty();
    }
    static boolean standable(ServerLevel l, BlockPos p) {
        return passable(l, p) && passable(l, p.above()) && !l.getBlockState(p.below()).getCollisionShape(l, p.below()).isEmpty();
    }

    /** 걷기 BFS: 4방향, 높이차 ±1. 반환: 도달한 칸 집합. */
    static Set<BlockPos> walk(ServerLevel l, BlockPos start, int limit) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        if (!standable(l, start)) return seen;
        q.add(start); seen.add(start);
        int[][] d = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!q.isEmpty() && seen.size() < limit) {
            BlockPos c = q.poll();
            for (int[] v : d) for (int dy = -1; dy <= 1; dy++) {
                BlockPos n = c.offset(v[0], dy, v[1]);
                if (seen.contains(n)) continue;
                if (dy == 1 && !passable(l, c.above(2))) continue;
                if (dy == -1 && !passable(l, n.above(2))) continue;
                if (standable(l, n)) { seen.add(n); q.add(n); }
            }
        }
        return seen;
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var sv = sp.getServer();
            sp.getConnection().waitForChunksRender();
            for (String c : new String[]{"time set noon", "gamerule advance_time false", "gamemode creative @p"}) sv.runCommand(c);
            int[] b = sv.computeOnServer(s -> new int[]{pl(s).getBlockX(), pl(s).getBlockY(), pl(s).getBlockZ()});
            sv.runCommand("lobcorp facility");
            ctx.waitTicks(40);
            int topY = sv.computeOnServer(s -> { int y = b[1]; int lowest = y - 2 * FacilityBuilder.FLOOR_GAP - 2; int min = s.overworld().getMinY() + 1; return lowest < min ? y + (min - lowest) : y; });
            int ox = b[0], oz = b[2];
            sv.runCommand("tp @p " + (b[0] + 40) + " " + (b[1] + 0) + " " + b[2]);
            ctx.waitTicks(1);
            int abnos0 = 0; int abnos = sv.computeOnServer(s -> { int n = 0; for (Entity e : s.overworld().getAllEntities()) if (e instanceof AbnormalityEntity) n++; return n; });
            sv.runCommand("forceload add " + (b[0] - 40) + " " + (b[2] - 60) + " " + (b[0] + 200) + " " + (b[2] + 60));
            ctx.waitTicks(60);
            abnos = sv.computeOnServer(s -> { int n = 0; for (Entity e : s.overworld().getAllEntities()) if (e instanceof AbnormalityEntity) n++; return n; });
            log("facility built: topY=" + topY + " abnormalities=" + abnos + " slots=" + sv.computeOnServer(s -> Facility.get(s).slots.size())
                    + " first=" + sv.computeOnServer(s -> Facility.get(s).slots.get(0).abno));
            sv.runCommand("lobcorp facility");
            log("second facility refused: slots=" + sv.computeOnServer(s -> Facility.get(s).slots.size()));

            // 하루가 끝나면 후보 3종
            sv.runCommand("lobcorp day next");
            ctx.waitTicks(5);
            String offers = sv.computeOnServer(s -> { StringBuilder sb = new StringBuilder();
                for (String id : Facility.get(s).offers) sb.append(id).append('(').append(AbnoProfile.BY_ID.get(id).risk()).append(") ");
                return sb.toString(); });
            log("day 2 offers: " + offers);
            String pick = sv.computeOnServer(s -> Facility.get(s).offers.get(0));
            sv.runCommand("lobcorp extract " + pick);
            ctx.waitTicks(5);
            log("picked " + pick + ": placed=" + sv.computeOnServer(s -> Extraction.placed(s).size()) + " slot1=" + sv.computeOnServer(s -> Facility.get(s).slots.get(1).abno)
                    + " offers cleared=" + sv.computeOnServer(s -> Facility.get(s).offers.isEmpty()));
            sv.runCommand("lobcorp extract " + pick);
            log("duplicate refused: placed=" + sv.computeOnServer(s -> Extraction.placed(s).size()));
            // 진행도: 8종 넘기면 HE, 20종 넘기면 WAW 후보가 섞인다
            StringBuilder prog = new StringBuilder();
            for (int day = 0; day < 22; day++) {
                sv.runCommand("lobcorp day next");
                String o = sv.computeOnServer(s -> { int n = Extraction.placed(s).size(); StringBuilder sb = new StringBuilder(n + ":");
                    for (String id : Facility.get(s).offers) sb.append(AbnoProfile.BY_ID.get(id).risk().charAt(0));
                    return sb.toString(); });
                prog.append(o).append(' ');
                sv.runOnServer(s -> Extraction.place(s, Facility.get(s).offers.get(0)));
            }
            log("progression (placed:offer risks) " + prog);

            // 각 부서 홀 도달 여부
            String r = sv.computeOnServer(s -> {
                ServerLevel l = s.overworld();
                Set<BlockPos> reach = walk(l, new BlockPos(ox + 3, topY, oz + 3), 400000);
                StringBuilder sb = new StringBuilder("reachable cells=" + reach.size() + " :");
                int[] counts = {4, 3, 3};
                String[] names = {"U", "M", "L"};
                for (int f = 0; f < 3; f++) for (int i = 0; i < counts[f]; i++) {
                    BlockPos hall = new BlockPos(ox + i * FacilityBuilder.DEPT_SPACING + 3, topY - f * FacilityBuilder.FLOOR_GAP, oz + 3);
                    sb.append(' ').append(names[f]).append(i).append(reach.contains(hall) ? "=ok" : "=NO");
                }
                // 격리실 문 앞 (첫 부서 북쪽 첫 칸)
                sb.append(" | unit-door U0N0=").append(reach.contains(new BlockPos(ox + 1, topY, oz - 13)) ? "ok" : "NO");
                return sb.toString();
            });
            log(r);

            // 스크린샷: 홀, 복도, 통로, 계단, 하층 홀
            String[][] shots = {
                {"f1_control_hall", (ox - 5.5) + " " + topY + " " + (oz + 5.5) + " -135 15"},
                {"f2_hallway_north", (ox + 0.5) + " " + topY + " " + (oz - 9.5) + " 180 5"},
                {"f3_unit_window", (ox + 0.5) + " " + topY + " " + (oz - 12.5) + " -90 10"},
                {"f4_corridor", (ox + 9.5) + " " + topY + " " + (oz + 0.5) + " -90 5"},
                {"f5_stairs_top", (ox - 15.5) + " " + topY + " " + (oz + 18.5) + " 0 35"},
                {"f6_central_hall", (ox - 5.5) + " " + (topY - 14) + " " + (oz + 5.5) + " -135 15"},
                {"f7_lower_hall", (ox + 96 + 5.5) + " " + (topY - 28) + " " + (oz + 5.5) + " 135 15"},
            };
            for (String[] sh : shots) {
                sv.runCommand("tp @p " + sh[1]);
                ctx.waitTicks(30);
                sp.getConnection().waitForChunksRender();
                ctx.takeScreenshot(sh[0]);
            }
            log("DONE");
        }
    }
}
