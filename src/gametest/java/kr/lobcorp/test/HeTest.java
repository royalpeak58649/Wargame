package kr.lobcorp.test;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.*;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.WorkManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;

public class HeTest implements FabricClientGameTest {
    static void log(String s) { System.out.println("[LOBCORP-TEST] " + s); }
    static ServerPlayer pl(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }
    static <T extends AbnormalityEntity> T find(MinecraftServer s, Class<T> c) {
        for (Entity e : s.overworld().getAllEntities()) if (c.isInstance(e)) return c.cast(e);
        return null;
    }
    static int deaths(MinecraftServer s) { return pl(s).getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)); }
    static void fresh(MinecraftServer s) {
        var d = Employees.get(pl(s)); d.panicTicks = 0; d.sleepTicks = 0; d.fairyTicks = 0; Employees.restoreSp(pl(s)); WorkManager.clear(pl(s));
        pl(s).setHealth(pl(s).getMaxHealth());
    }
    static void sturdy(MinecraftServer s, int temperance) {
        var d = Employees.get(pl(s)); d.prudence = 80; d.fortitude = 80; d.temperance = temperance; Employees.apply(pl(s));
        pl(s).setHealth(pl(s).getMaxHealth()); Employees.restoreSp(pl(s));
    }
    static int work(TestServerContext sv, Class<? extends AbnormalityEntity> c, int type) {
        sv.runOnServer(s -> { fresh(s); WorkManager.handleAction(pl(s), find(s, c).getId(), type); });
        return sv.waitFor(s -> WorkManager.session(pl(s)) == null, 1500);
    }


    static WorkSession ses(AbnormalityEntity a, int good) { WorkSession s = new WorkSession(a, WorkType.INSTINCT, a.profile().maxBox(), 50, 20); s.good = good; s.box = a.profile().maxBox(); return s; }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var sv = sp.getServer();
            sp.getConnection().waitForChunksRender();
            for (String c : new String[]{"difficulty normal", "time set noon", "gamerule advance_time false", "weather clear",
                    "gamerule immediate_respawn true", "gamerule keep_inventory true", "gamemode survival @p"}) sv.runCommand(c);
            int[] b = sv.computeOnServer(s -> new int[]{pl(s).getBlockX(), pl(s).getBlockY(), pl(s).getBlockZ()});
            int bx = b[0], by = b[1], bz = b[2];
            sv.runCommand("setworldspawn " + (bx - 40) + " " + by + " " + bz);
            String[] rooms = {"mhz", "red_shoes", "singing_machine", "porccubus", "laetitia", "all_around_helper"};
            for (int i = 0; i < rooms.length; i++) sv.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14 * i) + " run lobcorp build " + rooms[i]);
            String[] tp = new String[rooms.length];
            for (int i = 0; i < rooms.length; i++) tp[i] = "tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14 * i + 0.5) + " -90 10";
            String[] shots = {"w01_mhz", "w03_red_shoes", "w05_singing", "w07_porccubus", "w09_laetitia", "w11_helper"};
            for (int i = 0; i < rooms.length; i++) { sv.runCommand(tp[i]); ctx.waitTicks(25); if (i == 0) sp.getConnection().waitForChunksRender(); ctx.takeScreenshot(shots[i]); }

            // ── 1.76 MHz ──
            sv.runCommand(tp[0]);
            sv.runOnServer(s -> sturdy(s, 40));
            String m = sv.computeOnServer(s -> { var a = find(s, MhzEntity.class); int n = 0;
                while (a.getRadius() == 0 && n < 40) { a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); n++; }
                return "bad works=" + n + " q=" + a.getQliphoth() + " radius=" + a.getRadius(); });
            log("mhz " + m);
            int sp0 = sv.computeOnServer(s -> Employees.get(pl(s)).sp);
            ctx.waitTicks(130);
            ctx.takeScreenshot("w02_mhz_noise");
            log("mhz noise: SP " + sp0 + "->" + sv.computeOnServer(s -> Employees.get(pl(s)).sp) + " radius=" + sv.computeOnServer(s -> find(s, MhzEntity.class).getRadius()));
            log("mhz restore: " + sv.computeOnServer(s -> { var a = find(s, MhzEntity.class); int n = 0;
                while (a.getQliphoth() < 4 && n < 10) { a.onWorkFinished(pl(s), WorkManager.Result.GOOD, ses(a, 12)); n++; }
                return "full works=" + n + " q=" + a.getQliphoth() + " radius=" + a.getRadius(); }));

            // ── 붉은 구두 ──
            sv.runCommand(tp[1]);
            sv.runOnServer(s -> sturdy(s, 15));
            log("red shoes attachment bonus (temp 15)=" + sv.computeOnServer(s -> find(s, RedShoesEntity.class).workChanceBonus(pl(s), WorkType.ATTACHMENT))
                    + " (temp 50)=" + sv.computeOnServer(s -> { Employees.get(pl(s)).temperance = 50; int v = find(s, RedShoesEntity.class).workChanceBonus(pl(s), WorkType.ATTACHMENT); Employees.get(pl(s)).temperance = 15; return v; }));
            float mh0 = sv.computeOnServer(s -> pl(s).getMaxHealth());
            log("red shoes: " + sv.computeOnServer(s -> { var a = find(s, RedShoesEntity.class); int n = 0;
                while (Employees.get(pl(s)).possessTicks == 0 && n < 40) { a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); n++; }
                return "bad works=" + n + " possess=" + Employees.get(pl(s)).possessTicks + " label=" + Employees.get(pl(s)).possessLabel; })
                    + " maxHP " + mh0 + "->" + sv.computeOnServer(s -> pl(s).getMaxHealth()));
            ctx.waitTicks(20);
            ctx.takeScreenshot("w04_possessed");
            sv.runOnServer(s -> Employees.get(pl(s)).possessTicks = 3);
            ctx.waitTicks(10);
            log("red shoes timeout release: possess=" + sv.computeOnServer(s -> Employees.get(pl(s)).possessTicks) + " maxHP=" + sv.computeOnServer(s -> pl(s).getMaxHealth()));

            // ── 노래하는 기계 ──
            sv.runCommand(tp[2]);
            sv.runOnServer(s -> sturdy(s, 15));
            int d0 = sv.computeOnServer(HeTest::deaths);
            sv.runOnServer(s -> { var a = find(s, SingingMachineEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.GOOD, ses(a, 16)); });
            ctx.waitTicks(10);
            log("singing machine temp 15 finish: deaths " + d0 + "->" + sv.computeOnServer(HeTest::deaths)
                    + " music=" + sv.computeOnServer(s -> find(s, SingingMachineEntity.class).getMusicTicks()) + " q=" + sv.computeOnServer(s -> find(s, SingingMachineEntity.class).getQliphoth()));
            sv.runCommand(tp[2]);
            ctx.waitTicks(20);
            ctx.takeScreenshot("w06_singing_open");
            sv.runOnServer(s -> { sturdy(s, 50); Employees.get(pl(s)).fortitude = 50; Employees.apply(pl(s)); });
            int d1 = sv.computeOnServer(HeTest::deaths);
            sv.runOnServer(s -> { var a = find(s, SingingMachineEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.GOOD, ses(a, 16)); });
            ctx.waitTicks(5);
            log("singing machine temp 50 fort 50 good finish: deaths " + d1 + "->" + sv.computeOnServer(HeTest::deaths));

            // ── 포큐버스 ──
            sv.runCommand(tp[3]);
            sv.runOnServer(s -> sturdy(s, 30));
            int d2 = sv.computeOnServer(HeTest::deaths);
            sv.runOnServer(s -> { var a = find(s, PorccubusEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.GOOD, ses(a, 16)); });
            ctx.waitTicks(5);
            log("porccubus good with temp 30: deaths " + d2 + "->" + sv.computeOnServer(HeTest::deaths));
            BlockPos ph = sv.computeOnServer(s -> find(s, PorccubusEntity.class).blockPosition());
            sv.runOnServer(s -> { sturdy(s, 70); find(s, PorccubusEntity.class).breach(); });
            ctx.waitTicks(100);
            BlockPos pp = sv.computeOnServer(s -> find(s, PorccubusEntity.class).blockPosition());
            log("porccubus breached=" + sv.computeOnServer(s -> find(s, PorccubusEntity.class).isBreached()) + " moved " + String.format("%.1f", Math.sqrt(ph.distSqr(pp))));
            sv.runCommand("tp @p " + (pp.getX() + 4.5) + " " + pp.getY() + " " + (pp.getZ() + 0.5) + " 90 0");
            sv.runOnServer(s -> sturdy(s, 70));
            int psp = sv.computeOnServer(s -> Employees.get(pl(s)).sp);
            ctx.waitTicks(80);
            ctx.takeScreenshot("w08_porccubus_breach");
            log("porccubus attack on temp 70: SP " + psp + "->" + sv.computeOnServer(s -> Employees.get(pl(s)).sp)
                    + " dist=" + sv.computeOnServer(s -> String.format("%.1f", find(s, PorccubusEntity.class).distanceTo(pl(s)))));
            sv.runOnServer(s -> find(s, PorccubusEntity.class).hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f));
            ctx.waitTicks(10);
            log("porccubus suppressed: breached=" + sv.computeOnServer(s -> find(s, PorccubusEntity.class).isBreached()));

            // ── 라티샤 ──
            sv.runCommand(tp[4]);
            sv.runOnServer(s -> { sturdy(s, 40); var a = find(s, LaetitiaEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.NORMAL, ses(a, 9)); });
            ctx.waitTicks(10);
            ctx.takeScreenshot("w10_laetitia_heart");
            log("laetitia normal: heart=" + sv.computeOnServer(s -> Employees.get(pl(s)).laetitiaHeart));
            // 선물을 가진 채 작업 시작 → 절반 전에 멀리 떠나 포기하면 산다
            sv.runCommand(tp[0]);
            sv.runOnServer(s -> { sturdy(s, 40); WorkManager.handleAction(pl(s), find(s, MhzEntity.class).getId(), 3); });
            ctx.waitTicks(30);
            sv.runCommand("tp @p " + (bx - 20.5) + " " + by + " " + (bz + 0.5));
            ctx.waitTicks(5);
            log("laetitia abandon: session=" + sv.computeOnServer(s -> WorkManager.session(pl(s)) != null) + " heart=" + sv.computeOnServer(s -> Employees.get(pl(s)).laetitiaHeart)
                    + " deaths=" + sv.computeOnServer(HeTest::deaths));
            sv.runCommand(tp[0]);
            int d3 = sv.computeOnServer(HeTest::deaths);
            sv.runOnServer(s -> { sturdy(s, 40); WorkManager.handleAction(pl(s), find(s, MhzEntity.class).getId(), 3); });
            sv.waitFor(s -> WorkManager.session(pl(s)) == null, 1500);
            ctx.waitTicks(10);
            int friends = sv.computeOnServer(s -> { int c = 0; for (Entity e : s.overworld().getAllEntities()) if (e instanceof LaetitiaFriendEntity) c++; return c; });
            log("laetitia heart carried into mhz work: deaths " + d3 + "->" + sv.computeOnServer(HeTest::deaths) + " friends=" + friends
                    + " heart=" + sv.computeOnServer(s -> Employees.get(pl(s)).laetitiaHeart));
            sv.runCommand(tp[0]);
            ctx.waitTicks(30);
            ctx.takeScreenshot("w12_laetitia_friend");
            sv.runOnServer(s -> { for (Entity e : s.overworld().getAllEntities()) if (e instanceof LaetitiaFriendEntity f) f.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f); });
            ctx.waitTicks(5);
            log("friend killed: remaining=" + sv.computeOnServer(s -> { int c = 0; for (Entity e : s.overworld().getAllEntities()) if (e instanceof LaetitiaFriendEntity f && f.isAlive()) c++; return c; }));

            // ── 만능 도우미 ──
            sv.runCommand("tp @p " + (bx + 2.5) + " " + by + " " + (bz + 70.5) + " -90 10");
            sv.runOnServer(s -> { sturdy(s, 40); find(s, AllAroundHelperEntity.class).breach(); });
            float hp = sv.computeOnServer(s -> pl(s).getHealth());
            StringBuilder phases = new StringBuilder();
            int last = -1;
            for (int i = 0; i < 40; i++) {
                ctx.waitTicks(10);
                int ph2 = sv.computeOnServer(s -> find(s, AllAroundHelperEntity.class).getPhase());
                if (ph2 != last) { phases.append(i * 10).append(":").append(ph2).append(" "); last = ph2; }
                if (i == 12) ctx.takeScreenshot("w13_helper_breach");
            }
            log("helper phases " + phases + " HP " + hp + "->" + sv.computeOnServer(s -> pl(s).getHealth()) + " deaths=" + sv.computeOnServer(HeTest::deaths));
            sv.runOnServer(s -> find(s, AllAroundHelperEntity.class).hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f));
            ctx.waitTicks(10);
            log("helper suppressed: breached=" + sv.computeOnServer(s -> find(s, AllAroundHelperEntity.class).isBreached()));
            log("DONE");
        }
    }
}
