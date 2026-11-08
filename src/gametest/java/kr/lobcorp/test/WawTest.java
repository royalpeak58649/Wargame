package kr.lobcorp.test;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.*;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.ToolState;
import kr.lobcorp.work.Facility;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

public class WawTest implements FabricClientGameTest {
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



    static int energy(MinecraftServer s) { return Facility.get(s).energy; }
    static BreachingAbnormality byId(MinecraftServer s, String id) {
        for (Entity e : s.overworld().getAllEntities()) if (e instanceof BreachingAbnormality b && b.profile().id().equals(id)) return b;
        return null;
    }
    static void kill(MinecraftServer s, Entity e) { e.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 100000f); }
    static void heal(MinecraftServer s) { pl(s).setHealth(pl(s).getMaxHealth()); var d = Employees.get(pl(s)); d.panicTicks = 0; Employees.restoreSp(pl(s)); }

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
            String[] rooms = {"big_bird", "judgement_bird", "queen_of_hatred", "knight_of_despair", "king_of_greed", "little_red",
                    "big_bad_wolf", "alriune", "queen_bee", "black_swan", "yin", "snow_white_apple", "little_prince", "parasite_tree", "naked_nest", "dreaming_current", "burrowing_heaven"};
            String[] tp = new String[rooms.length];
            for (int i = 0; i < rooms.length; i++) {
                sv.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14 * i) + " run lobcorp build " + rooms[i]);
                tp[i] = "tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14 * i + 0.5) + " -90 5";
            }
            for (int i = 0; i < rooms.length; i++) { sv.runCommand(tp[i]); ctx.waitTicks(25); if (i == 0) sp.getConnection().waitForChunksRender(); ctx.takeScreenshot(String.format("y%02d_%s", i, rooms[i])); }

            // ── 큰 새 ──
            sv.runCommand(tp[0]); ctx.waitTicks(20);
            log("big bird: " + sv.computeOnServer(s -> { sturdy(s, 40); var a = byId(s, "big_bird");
                a.onWorkFinished(pl(s), WorkManager.Result.GOOD, ses(a, 16)); int q1 = a.getQliphoth();
                a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); return "good q=" + q1 + " bad q=" + a.getQliphoth(); }));
            int d0 = sv.computeOnServer(WawTest::deaths);
            sv.runOnServer(s -> { heal(s); byId(s, "big_bird").breach(); });
            ctx.waitTicks(70);
            sv.runCommand("tp @p " + (bx - 6.5) + " " + by + " " + (bz + 0.5) + " -90 0");
            String glow = "";
            for (int t = 0; t < 45; t++) {
                ctx.waitTicks(20);
                sv.runOnServer(s -> { if (pl(s).getHealth() < 40) pl(s).setHealth(pl(s).getMaxHealth()); });
                if (glow.isEmpty() && sv.computeOnServer(s -> pl(s).hasEffect(net.minecraft.world.effect.MobEffects.GLOWING))) { glow = "marked at " + (t * 20); ctx.takeScreenshot("y20_big_bird_lamp"); }
                if (sv.computeOnServer(WawTest::deaths) > d0) { glow += ", eaten at " + (t * 20); break; }
            }
            log("big bird breach: " + glow + " deaths " + d0 + "->" + sv.computeOnServer(WawTest::deaths));
            sv.runOnServer(s -> kill(s, byId(s, "big_bird")));

            // ── 심판 새 ──
            sv.runCommand(tp[1]); ctx.waitTicks(20);
            sv.runOnServer(s -> { sturdy(s, 40); heal(s); byId(s, "judgement_bird").breach(); });
            ctx.waitTicks(70);
            sv.runOnServer(s -> heal(s));
            float jh = sv.computeOnServer(s -> pl(s).getHealth());
            String judged = "none";
            for (int t = 0; t < 20; t++) { ctx.waitTicks(20); if (sv.computeOnServer(s -> pl(s).getHealth()) < jh - 20) { judged = "at " + (t * 20); break; }
                if (t == 2) ctx.takeScreenshot("y21_judgement"); }
            log("judgement bird: big hit " + judged + " deaths=" + sv.computeOnServer(WawTest::deaths));
            sv.runOnServer(s -> kill(s, byId(s, "judgement_bird")));

            // ── 증오의 여왕 ──
            sv.runCommand(tp[2]); ctx.waitTicks(20);
            log("queen: " + sv.computeOnServer(s -> { sturdy(s, 40); var a = (QueenHatredEntity) byId(s, "queen_of_hatred");
                a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2));
                String r = "after bad q=" + a.getQliphoth() + " hysteric=" + a.isHysteric() + " bonus=" + a.workChanceBonus(pl(s), WorkType.ATTACHMENT);
                a.onWorkFinished(pl(s), WorkManager.Result.NORMAL, ses(a, 12)); return r + " | hysteric normal(12) q=" + a.getQliphoth(); }));
            ctx.waitTicks(80);
            sv.runOnServer(s -> heal(s));
            float qh = sv.computeOnServer(s -> pl(s).getHealth());
            ctx.waitTicks(200);
            ctx.takeScreenshot("y22_queen_snake");
            log("queen breached=" + sv.computeOnServer(s -> byId(s, "queen_of_hatred").isBreached()) + " HP " + qh + "->" + sv.computeOnServer(s -> pl(s).getHealth())
                    + " deaths=" + sv.computeOnServer(WawTest::deaths));

            // ── 빨간 두건 고용 (여왕을 목표로) ──
            sv.runCommand(tp[5]); ctx.waitTicks(20);
            sv.runOnServer(s -> { var lr = byId(s, "little_red"); if (lr.isBreached()) kill(s, lr); lr.resetQliphoth(); });
            ctx.waitTicks(5);
            sv.runOnServer(s -> { heal(s); Facility.get(s).energy = 300; WorkManager.handleAction(pl(s), byId(s, "little_red").getId(), 2); });
            float qhp0 = sv.computeOnServer(s -> byId(s, "queen_of_hatred").getHealth());
            log("little red hire: hired=" + sv.computeOnServer(s -> ((LittleRedEntity) byId(s, "little_red")).isHired()) + " energy=" + sv.computeOnServer(WawTest::energy)
                    + " q=" + sv.computeOnServer(s -> byId(s, "little_red").getQliphoth()));
            ctx.waitTicks(300);
            log("little red vs queen: queen HP " + qhp0 + "->" + sv.computeOnServer(s -> byId(s, "queen_of_hatred").getHealth()));
            sv.runOnServer(s -> kill(s, byId(s, "queen_of_hatred")));
            ctx.waitTicks(400);
            log("little red after target down: hired=" + sv.computeOnServer(s -> ((LittleRedEntity) byId(s, "little_red")).isHired())
                    + " dist home=" + sv.computeOnServer(s -> { var r = byId(s, "little_red"); return String.format("%.1f", Math.sqrt(r.blockPosition().distSqr(new BlockPos(bx + 7, by, bz + 70)))); }));

            // ── 절망의 기사 ──
            sv.runCommand(tp[3]); ctx.waitTicks(20);
            sv.runOnServer(s -> { sturdy(s, 40); var a = byId(s, "knight_of_despair"); a.onWorkFinished(pl(s), WorkManager.Result.GOOD, ses(a, 16)); });
            log("knight: blessed=" + sv.computeOnServer(s -> Employees.get(pl(s)).blessed));
            sv.runOnServer(s -> WorkManager.handleAction(pl(s), byId(s, "knight_of_despair").getId(), 1));
            log("knight: work while blessed -> session=" + sv.computeOnServer(s -> WorkManager.session(pl(s)) != null));
            sv.runOnServer(s -> Employees.panic(pl(s)));
            ctx.waitTicks(80);
            ctx.takeScreenshot("y23_knight");
            log("knight after blessed panic: breached=" + sv.computeOnServer(s -> byId(s, "knight_of_despair").isBreached()) + " blessed=" + sv.computeOnServer(s -> Employees.get(pl(s)).blessed));
            sv.runOnServer(s -> { kill(s, byId(s, "knight_of_despair")); heal(s); });

            // ── 탐욕의 왕 ──
            sv.runCommand(tp[4]); ctx.waitTicks(20);
            sv.runOnServer(s -> byId(s, "king_of_greed").breach());
            ctx.waitTicks(80);
            BlockPos k0 = sv.computeOnServer(s -> byId(s, "king_of_greed").blockPosition());
            ctx.waitTicks(100);
            BlockPos k1 = sv.computeOnServer(s -> byId(s, "king_of_greed").blockPosition());
            log("king of greed moved " + String.format("%.1f", Math.sqrt(k0.distSqr(k1))) + " in 5s; red resist dmg: " + sv.computeOnServer(s -> {
                var k = byId(s, "king_of_greed"); float h = k.getHealth();
                ItemStack old = pl(s).getMainHandItem(); pl(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(kr.lobcorp.ModItems.CRIMSON_SCAR));
                k.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 20f); pl(s).setItemInHand(InteractionHand.MAIN_HAND, old);
                return h + "->" + k.getHealth(); }));
            sv.runOnServer(s -> kill(s, byId(s, "king_of_greed")));

            // ── 늑대 ──
            sv.runCommand(tp[6]); ctx.waitTicks(20);
            sv.runOnServer(s -> { sturdy(s, 40); Employees.get(pl(s)).lastWorked = "little_red"; WorkManager.handleAction(pl(s), byId(s, "big_bad_wolf").getId(), 0); });
            ctx.waitTicks(5);
            log("wolf after little_red worker: devoured=" + sv.computeOnServer(s -> Employees.get(pl(s)).devoured) + " q=" + sv.computeOnServer(s -> byId(s, "big_bad_wolf").getQliphoth()));
            double wz = sv.computeOnServer(s -> pl(s).getZ());
            sv.runCommand("tp @p ~ ~ ~4"); ctx.waitTicks(10);
            log("devoured lock dz=" + String.format("%.2f", sv.computeOnServer(s -> pl(s).getZ()) - wz));
            sv.runOnServer(s -> byId(s, "big_bad_wolf").breach());
            ctx.waitTicks(80);
            ctx.takeScreenshot("y24_wolf");
            sv.runOnServer(s -> byId(s, "big_bad_wolf").die(s.overworld().damageSources().playerAttack(pl(s))));
            ctx.waitTicks(10);
            log("wolf suppressed: devoured=" + sv.computeOnServer(s -> Employees.get(pl(s)).devoured));

            // ── 알리우네 ──
            sv.runCommand(tp[7]); ctx.waitTicks(20);
            sv.runOnServer(s -> { sturdy(s, 70); heal(s); byId(s, "alriune").breach(); });
            ctx.waitTicks(80);
            BlockPos ap = sv.computeOnServer(s -> byId(s, "alriune").blockPosition());
            sv.runCommand("tp @p " + (ap.getX() + 3.5) + " " + ap.getY() + " " + (ap.getZ() + 0.5) + " 90 10");
            sv.runOnServer(s -> heal(s));
            int asp = sv.computeOnServer(s -> Employees.get(pl(s)).sp);
            ctx.waitTicks(250);
            ctx.takeScreenshot("y25_alriune_petals");
            ctx.waitTicks(80);
            log("alriune: petals now=" + sv.computeOnServer(s -> ((AlriuneEntity) byId(s, "alriune")).getPetals()) + " SP " + asp + "->" + sv.computeOnServer(s -> Employees.get(pl(s)).sp)
                    + " moved=" + !ap.equals(sv.computeOnServer(s -> byId(s, "alriune").blockPosition())));
            sv.runOnServer(s -> { kill(s, byId(s, "alriune")); heal(s); });

            // ── 여왕벌 ──
            sv.runCommand(tp[8]); ctx.waitTicks(20);
            log("queen bee: " + sv.computeOnServer(s -> { sturdy(s, 40); QueenBeeEntity a = find(s, QueenBeeEntity.class); int n = 0;
                while (n < 30 && Employees.get(pl(s)).sporeTicks == 0) { a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); n++; if (a.getQliphoth() == 0) break; }
                return "bad works=" + n + " q=" + a.getQliphoth(); }));
            ctx.waitTicks(80);
            log("queen bee spores: sporeTicks=" + sv.computeOnServer(s -> Employees.get(pl(s)).sporeTicks));
            sv.runOnServer(s -> pl(s).setHealth(5));
            ctx.waitTicks(60);
            int bees = sv.computeOnServer(s -> { int c = 0; for (Entity e : s.overworld().getAllEntities()) if (e instanceof WorkerBeeEntity) c++; return c; });
            log("queen bee: deaths=" + sv.computeOnServer(WawTest::deaths) + " worker bees=" + bees);
            sv.runCommand(tp[8]); ctx.waitTicks(20);
            ctx.takeScreenshot("y26_worker_bee");
            sv.runCommand("lobcorp day next"); ctx.waitTicks(5);
            log("after day next bees=" + sv.computeOnServer(s -> { int c = 0; for (Entity e : s.overworld().getAllEntities()) if (e instanceof WorkerBeeEntity) c++; return c; }));

            // ── 흑조의 꿈 ──
            sv.runCommand(tp[9]); ctx.waitTicks(20);
            log("black swan: " + sv.computeOnServer(s -> { sturdy(s, 40); var a = (BlackSwanEntity) byId(s, "black_swan");
                a.onWorkFinished(pl(s), WorkManager.Result.NORMAL, ses(a, 12)); return "after normal q=" + a.getQliphoth() + " changed=" + a.getChanged(); }));
            sv.runOnServer(s -> { heal(s); byId(s, "black_swan").breach(); });
            ctx.waitTicks(80);
            log("swan front reflect: " + sv.computeOnServer(s -> { var a = byId(s, "black_swan");
                pl(s).teleportTo(a.getX() + a.getLookAngle().x * 2, a.getY(), a.getZ() + a.getLookAngle().z * 2);
                float h = a.getHealth(); float ph = pl(s).getHealth();
                a.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 10f);
                return "swan " + h + "->" + a.getHealth() + " player " + ph + "->" + pl(s).getHealth(); }));
            ctx.takeScreenshot("y27_black_swan");
            sv.runOnServer(s -> kill(s, byId(s, "black_swan")));
            ctx.waitTicks(10);
            log("DONE");
        }
    }
}
