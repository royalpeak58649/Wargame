package kr.lobcorp.test;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BeautyBeastEntity;
import kr.lobcorp.entity.FragmentEntity;
import kr.lobcorp.entity.MeatLanternEntity;
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

public class Teth3Test implements FabricClientGameTest {
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
            String[] rooms = {"beauty_and_the_beast", "meat_lantern", "fragment_of_universe"};
            for (int i = 0; i < 3; i++) sv.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14 * i) + " run lobcorp build " + rooms[i]);
            String[] tp = new String[3];
            for (int i = 0; i < 3; i++) tp[i] = "tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14 * i + 0.5) + " -90 10";

            // ── 미녀와 야수 ──
            sv.runCommand(tp[0]);
            ctx.waitTicks(40);
            sp.getConnection().waitForChunksRender();
            sv.runOnServer(s -> sturdy(s, 40));
            ctx.takeScreenshot("v01_beast");
            work(sv, BeautyBeastEntity.class, 3);
            log("beast after repression: wounded=" + sv.computeOnServer(s -> find(s, BeautyBeastEntity.class).isWounded()));
            ctx.waitTicks(20);
            ctx.takeScreenshot("v02_beast_wounded");
            work(sv, BeautyBeastEntity.class, 1);
            log("beast after insight: wounded=" + sv.computeOnServer(s -> find(s, BeautyBeastEntity.class).isWounded()));
            work(sv, BeautyBeastEntity.class, 3);
            int d0 = sv.computeOnServer(Teth3Test::deaths);
            work(sv, BeautyBeastEntity.class, 3);
            ctx.waitTicks(10);
            log("beast after 2x repression: deaths " + d0 + "->" + sv.computeOnServer(Teth3Test::deaths)
                    + " name=" + sv.computeOnServer(s -> { var a = find(s, BeautyBeastEntity.class);
                        return a.getCustomName() == null ? "none" : a.getCustomName().getString(); })
                    + " wounded=" + sv.computeOnServer(s -> find(s, BeautyBeastEntity.class).isWounded()));
            sv.runCommand(tp[0]);
            ctx.waitTicks(30);
            ctx.takeScreenshot("v03_beast_new");

            // ── 고기 등불 ──
            sv.runCommand(tp[1]);
            ctx.waitTicks(30);
            ctx.takeScreenshot("v04_lantern");
            sv.runOnServer(s -> sturdy(s, 15));
            work(sv, MeatLanternEntity.class, 1);
            log("lantern after slow insight (temperance 15): q=" + sv.computeOnServer(s -> find(s, MeatLanternEntity.class).getQliphoth()));
            sv.runOnServer(s -> sturdy(s, 80));
            BlockPos home = sv.computeOnServer(s -> find(s, MeatLanternEntity.class).blockPosition());
            work(sv, MeatLanternEntity.class, 1);
            log("lantern after fast insight (temperance 80): q=" + sv.computeOnServer(s -> find(s, MeatLanternEntity.class).getQliphoth()));
            ctx.waitTicks(100);
            BlockPos hide = sv.computeOnServer(s -> find(s, MeatLanternEntity.class).blockPosition());
            log("lantern breached=" + sv.computeOnServer(s -> find(s, MeatLanternEntity.class).isBreached())
                    + " home=" + home + " hide=" + hide + " dist=" + String.format("%.1f", Math.sqrt(home.distSqr(hide))));
            sv.runCommand("tp @p " + (hide.getX() + 3.5) + " " + (hide.getY()) + " " + (hide.getZ() + 0.5) + " 90 25");
            ctx.waitTicks(20);
            sp.getConnection().waitForChunksRender();
            ctx.takeScreenshot("v05_lantern_hidden");
            ctx.waitTicks(20);
            int d1 = sv.computeOnServer(Teth3Test::deaths);
            log("standing 3 blocks away: deaths unchanged=" + (d1 == sv.computeOnServer(Teth3Test::deaths)));
            sv.runCommand("tp @p " + (hide.getX() + 0.9) + " " + (hide.getY()) + " " + (hide.getZ() + 0.5) + " 90 25");
            ctx.waitTicks(4);
            ctx.takeScreenshot("v06_lantern_bite");
            ctx.waitTicks(10);
            log("stepped on hairs: deaths " + d1 + "->" + sv.computeOnServer(Teth3Test::deaths));
            sv.runOnServer(s -> { var m = find(s, MeatLanternEntity.class);
                m.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f); });
            ctx.waitTicks(20);
            log("lantern after beating: breached=" + sv.computeOnServer(s -> find(s, MeatLanternEntity.class).isBreached())
                    + " pos=" + sv.computeOnServer(s -> find(s, MeatLanternEntity.class).blockPosition()));

            // ── 우주의 파편 ──
            sv.runCommand(tp[2]);
            ctx.waitTicks(30);
            ctx.takeScreenshot("v07_fragment");
            sv.runOnServer(s -> { sturdy(s, 40); fresh(s);
                WorkManager.handleAction(pl(s), find(s, FragmentEntity.class).getId(), 2); });
            ctx.waitTicks(30);
            sv.runOnServer(s -> Employees.panic(pl(s)));
            log("fragment after panic mid-work: q=" + sv.computeOnServer(s -> find(s, FragmentEntity.class).getQliphoth()));
            sv.runOnServer(s -> { fresh(s); find(s, FragmentEntity.class).breach(); });
            ctx.waitTicks(80);
            ctx.takeScreenshot("v08_fragment_breach");
            int sang = -1; float hp0 = sv.computeOnServer(s -> pl(s).getHealth());
            for (int i = 0; i < 60; i++) {
                ctx.waitTicks(20);
                sv.runOnServer(s -> { var d = Employees.get(pl(s)); if (pl(s).getHealth() < 20) pl(s).setHealth(pl(s).getMaxHealth()); d.panicTicks = 0; });
                if (sv.computeOnServer(s -> find(s, FragmentEntity.class).getSingTicks() > 0)) { sang = (i + 1) * 20; break; }
            }
            int sp0 = sv.computeOnServer(s -> Employees.get(pl(s)).sp);
            ctx.waitTicks(15);
            ctx.takeScreenshot("v09_fragment_song");
            ctx.waitTicks(50);
            log("fragment sang after " + sang + " ticks; SP " + sp0 + "->" + sv.computeOnServer(s -> Employees.get(pl(s)).sp)
                    + "; hp start " + hp0 + " dist=" + sv.computeOnServer(s -> String.format("%.1f", find(s, FragmentEntity.class).distanceTo(pl(s)))));
            sv.runOnServer(s -> { var m = find(s, FragmentEntity.class);
                m.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f); });
            ctx.waitTicks(10);
            log("fragment suppressed: breached=" + sv.computeOnServer(s -> find(s, FragmentEntity.class).isBreached()));
            log("DONE");
        }
    }
}
