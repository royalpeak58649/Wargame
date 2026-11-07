package kr.lobcorp.test;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.CrumblingArmorEntity;
import kr.lobcorp.entity.LadyFacingTheWallEntity;
import kr.lobcorp.entity.TodaysShyLookEntity;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.WorkManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;

public class TethTest implements FabricClientGameTest {
    static void log(String s) { System.out.println("[LOBCORP-TEST] " + s); }
    static ServerPlayer pl(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }
    static <T extends AbnormalityEntity> T find(MinecraftServer s, Class<T> c) {
        for (Entity e : s.overworld().getAllEntities()) if (c.isInstance(e)) return c.cast(e);
        return null;
    }
    static int deaths(MinecraftServer s) { return pl(s).getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)); }
    static void fresh(MinecraftServer s) {
        var d = Employees.get(pl(s)); d.panicTicks = 0; Employees.restoreSp(pl(s)); WorkManager.clear(pl(s));
    }
    static String st(TestServerContext sv) {
        return sv.computeOnServer(s -> { var d = Employees.get(pl(s));
            return "hp=" + pl(s).getHealth() + " sp=" + d.sp + "/" + d.maxSp() + " deaths=" + deaths(s) + " gifts=" + d.gifts
                    + " fort=" + d.fortitude + " temp=" + d.temperance; });
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var sv = sp.getServer();
            sp.getConnection().waitForChunksRender();
            for (String c : new String[]{"difficulty normal", "time set noon", "gamerule advance_time false", "weather clear",
                    "gamerule immediate_respawn true", "gamerule keep_inventory true", "gamemode survival @p"}) sv.runCommand(c);
            int[] b = sv.computeOnServer(s -> new int[]{pl(s).getBlockX(), pl(s).getBlockY(), pl(s).getBlockZ()});
            int bx = b[0], by = b[1], bz = b[2];
            sv.runCommand("setworldspawn " + (bx - 6) + " " + by + " " + bz);
            String[] rooms = {"todays_shy_look", "lady_facing_the_wall", "crumbling_armor"};
            for (int i = 0; i < 3; i++) sv.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14 * i) + " run lobcorp build " + rooms[i]);

            // ── 오늘의 수줍은 표정 ──
            sv.runCommand("tp @p " + (bx + 3.5) + " " + by + " " + (bz + 0.5) + " -90 5");
            ctx.waitTicks(40);
            sp.getConnection().waitForChunksRender();
            ctx.takeScreenshot("t01_shy_look");
            StringBuilder faces = new StringBuilder();
            for (int f = 0; f < 5; f++) {
                int face = f;
                int chance = sv.computeOnServer(s -> {
                    fresh(s);
                    var a = find(s, TodaysShyLookEntity.class);
                    a.setFace(face);
                    WorkManager.handleAction(pl(s), a.getId(), 0);
                    var ses = WorkManager.session(pl(s));
                    int c = ses == null ? -1 : ses.chance;
                    WorkManager.clear(pl(s));
                    return c;
                });
                faces.append("face").append(f).append("=").append(chance).append("% ");
            }
            log("shy look chances (base 50 + temp/5=3): " + faces);
            ctx.waitTicks(5);
            sv.runOnServer(s -> find(s, TodaysShyLookEntity.class).setFace(4));
            ctx.waitTicks(10);
            ctx.takeScreenshot("t02_shy_look_angry_face");
            // 실제 작업 한 번 (face 0, 회복 확인)
            sv.runOnServer(s -> { fresh(s); pl(s).setHealth(10f); Employees.get(pl(s)).sp = 5;
                var a = find(s, TodaysShyLookEntity.class); a.setFace(0); WorkManager.handleAction(pl(s), a.getId(), 1); });
            sv.waitFor(s -> WorkManager.session(pl(s)) == null, 1500);
            log("after face0 work (started hp10 sp5): " + st(sv));

            // ── 벽을 보는 여자 ──
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14.5) + " -90 5");
            ctx.waitTicks(30);
            ctx.takeScreenshot("t03_wall_lady");
            String lady = sv.computeOnServer(s -> {
                StringBuilder r = new StringBuilder();
                var a = find(s, LadyFacingTheWallEntity.class);
                int panics = 0;
                for (int i = 0; i < 3; i++) {
                    fresh(s);
                    int before = a.getQliphoth(), spBefore = Employees.get(pl(s)).sp;
                    WorkManager.handleAction(pl(s), a.getId(), 2); // 애착
                    if (Employees.get(pl(s)).panicTicks > 0) panics++;
                    r.append("q ").append(before).append("->").append(a.getQliphoth()).append(" sp ").append(spBefore).append("->")
                            .append(Employees.get(pl(s)).sp).append("; ");
                    WorkManager.clear(pl(s));
                }
                return r + " panics=" + panics;
            });
            log("lady attachment starts: " + lady);

            // ── 부서지는 갑옷 ──
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 28.5) + " -90 5");
            ctx.waitTicks(30);
            ctx.takeScreenshot("t04_armor");
            int d0 = sv.computeOnServer(TethTest::deaths);
            sv.runOnServer(s -> { fresh(s); WorkManager.handleAction(pl(s), find(s, CrumblingArmorEntity.class).getId(), 0); });
            ctx.waitTicks(10);
            log("armor with fortitude 15: deaths " + d0 + " -> " + sv.computeOnServer(TethTest::deaths));
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 28.5) + " -90 5");
            sv.runOnServer(s -> { fresh(s); Employees.get(pl(s)).fortitude = 30; Employees.apply(pl(s));
                WorkManager.handleAction(pl(s), find(s, CrumblingArmorEntity.class).getId(), 3); }); // 억압
            sv.waitFor(s -> WorkManager.session(pl(s)) == null, 1500);
            log("after repression with fortitude 30: " + st(sv));
            int d1 = sv.computeOnServer(TethTest::deaths);
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 0.5) + " -90 5");
            sv.runOnServer(s -> { fresh(s); WorkManager.handleAction(pl(s), find(s, TodaysShyLookEntity.class).getId(), 2); }); // 애착
            ctx.waitTicks(10);
            log("courage holder attachment on shy look: deaths " + d1 + " -> " + sv.computeOnServer(TethTest::deaths));
            ctx.takeScreenshot("t05_courage_death");
            log("DONE");
        }
    }
}
