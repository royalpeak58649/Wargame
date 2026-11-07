package kr.lobcorp.test;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.OldLadyEntity;
import kr.lobcorp.entity.SpiderBudEntity;
import kr.lobcorp.entity.VoidDreamEntity;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;

public class Teth2Test implements FabricClientGameTest {
    static void log(String s) { System.out.println("[LOBCORP-TEST] " + s); }
    static ServerPlayer pl(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }
    static <T extends AbnormalityEntity> T find(MinecraftServer s, Class<T> c) {
        for (Entity e : s.overworld().getAllEntities()) if (c.isInstance(e)) return c.cast(e);
        return null;
    }
    static int deaths(MinecraftServer s) { return pl(s).getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)); }
    static void fresh(MinecraftServer s) {
        var d = Employees.get(pl(s)); d.panicTicks = 0; d.sleepTicks = 0; d.fairyTicks = 0; Employees.restoreSp(pl(s)); WorkManager.clear(pl(s));
    }
    static int work(TestServerContext sv, Class<? extends AbnormalityEntity> c, int type) {
        sv.runOnServer(s -> { fresh(s); WorkManager.handleAction(pl(s), find(s, c).getId(), type); });
        return sv.waitFor(s -> WorkManager.session(pl(s)) == null, 1500);
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
            String[] rooms = {"old_lady", "spider_bud", "void_dream", "fairy_festival"};
            for (int i = 0; i < 4; i++) sv.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14 * i) + " run lobcorp build " + rooms[i]);

            // ── 늙은 여인 ──
            sv.runCommand("tp @p " + (bx + 3.5) + " " + by + " " + (bz + 0.5) + " -90 8");
            ctx.waitTicks(40);
            sp.getConnection().waitForChunksRender();
            ctx.takeScreenshot("u01_old_lady");
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 42.5) + " -90 5");
            work(sv, FairyFestivalEntity.class, 2);
            log("old lady q after 1 real fairy work: " + sv.computeOnServer(s -> find(s, OldLadyEntity.class).getQliphoth()));
            String ol = sv.computeOnServer(s -> {
                var a = find(s, OldLadyEntity.class);
                var f = find(s, FairyFestivalEntity.class);
                for (int i = 0; i < 3; i++) a.onOtherWorkFinished(pl(s), f, WorkManager.Result.GOOD);
                return "q=" + a.getQliphoth() + " solitude=" + a.isSolitude() + " bonus=" + a.workChanceBonus(pl(s), WorkType.ATTACHMENT)
                        + " mult=" + a.workDamageMultiplier(pl(s));
            });
            log("old lady after 3 more other works: " + ol);
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 0.5) + " -90 5");
            ctx.waitTicks(60);
            ctx.takeScreenshot("u02_old_lady_solitude");
            // 고독 상태에서 좋은 결과가 나올 때까지 작업 (최대 3회) — 고독은 위험하므로 SP·HP 넉넉한 직원으로
            sv.runOnServer(s -> { var d = Employees.get(pl(s)); d.prudence = 80; d.fortitude = 60; Employees.apply(pl(s)); pl(s).setHealth(65f); });
            for (int i = 0; i < 3; i++) {
                work(sv, OldLadyEntity.class, 2);
                String r = sv.computeOnServer(s -> { var a = find(s, OldLadyEntity.class); return "q=" + a.getQliphoth() + " solitude=" + a.isSolitude(); });
                log("old lady after own work #" + (i + 1) + ": " + r);
                if (r.contains("solitude=false")) break;
            }

            // ── 거미 봉오리 ──
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14.5) + " -90 -25");
            ctx.waitTicks(30);
            ctx.takeScreenshot("u03_spider");
            sv.runOnServer(s -> { Employees.get(pl(s)).prudence = 30; Employees.apply(pl(s)); });
            int d0 = sv.computeOnServer(Teth2Test::deaths);
            work(sv, SpiderBudEntity.class, 0); // 본능, 신중 30
            log("spider instinct with prudence 30: deaths " + d0 + "->" + sv.computeOnServer(Teth2Test::deaths)
                    + " cocoons=" + sv.computeOnServer(s -> find(s, SpiderBudEntity.class).getCocoons()));
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14.5) + " -90 -25");
            sv.runOnServer(s -> pl(s).setHealth(pl(s).getMaxHealth()));
            work(sv, SpiderBudEntity.class, 1); // 통찰 → 고치
            ctx.waitTicks(5);
            log("spider insight: deaths -> " + sv.computeOnServer(Teth2Test::deaths)
                    + " cocoons=" + sv.computeOnServer(s -> find(s, SpiderBudEntity.class).getCocoons()));
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14.5) + " -90 -25");
            sv.runOnServer(s -> { Employees.get(pl(s)).prudence = 30; Employees.apply(pl(s)); });
            int e0 = sv.computeOnServer(s -> Facility.get(s).energy);
            work(sv, SpiderBudEntity.class, 0);
            log("spider instinct with 1 cocoon: energy " + e0 + "->" + sv.computeOnServer(s -> Facility.get(s).energy)
                    + " (boxes + 1 bonus expected)");
            ctx.waitTicks(10);
            ctx.takeScreenshot("u04_spider_cocoon");

            // ── 공허한 꿈 ──
            sv.runCommand("tp @p " + (bx + 3.5) + " " + by + " " + (bz + 28.5) + " -90 0");
            ctx.waitTicks(30);
            ctx.takeScreenshot("u05_void_dream");
            sv.runOnServer(s -> { fresh(s); find(s, VoidDreamEntity.class).breach(); });
            int slept = -1;
            for (int i = 0; i < 20; i++) {
                ctx.waitTicks(20);
                int st = sv.computeOnServer(s -> Employees.get(pl(s)).sleepTicks);
                if (st > 0) { slept = (i + 1) * 20; break; }
            }
            log("void dream breached; player fell asleep after " + slept + " ticks; dream pos="
                    + sv.computeOnServer(s -> find(s, VoidDreamEntity.class).blockPosition()));
            ctx.waitTicks(10);
            ctx.takeScreenshot("u06_asleep");
            sv.runOnServer(s -> { var v = find(s, VoidDreamEntity.class);
                v.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1f); });
            ctx.waitTicks(5);
            log("after hit: rooster=" + sv.computeOnServer(s -> find(s, VoidDreamEntity.class).isRooster())
                    + " sleep=" + sv.computeOnServer(s -> Employees.get(pl(s)).sleepTicks));
            sv.runOnServer(s -> Employees.get(pl(s)).sleepTicks = 200);
            int crowed = -1;
            for (int i = 0; i < 10; i++) {
                ctx.waitTicks(20);
                boolean panic = sv.computeOnServer(s -> Employees.get(pl(s)).panicTicks > 0);
                if (panic) { crowed = (i + 1) * 20; break; }
            }
            log("rooster crow woke sleeper into panic after " + crowed + " ticks");
            sv.runCommand("tp @p " + (bx - 2.5) + " " + by + " " + (bz + 28.5) + " -90 0");
            ctx.waitTicks(10);
            ctx.takeScreenshot("u07_rooster");
            log("DONE");
        }
    }
}
