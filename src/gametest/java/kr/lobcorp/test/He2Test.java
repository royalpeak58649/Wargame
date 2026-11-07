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

public class He2Test implements FabricClientGameTest {
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
    static String shoot(ClientGameTestContext ctx, TestServerContext sv, String[] tp, int i, String name) { return name; }

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
            String[] rooms = {"tree_sap", "shelter", "portrait", "notes", "child_galaxy", "freischutz", "nameless_fetus",
                    "snow_queen", "woodsman", "scarecrow", "rudolta", "funeral", "schadenfreude"};
            String[] tp = new String[rooms.length];
            for (int i = 0; i < rooms.length; i++) {
                sv.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14 * i) + " run lobcorp build " + rooms[i]);
                tp[i] = "tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14 * i + 0.5) + " -90 10";
            }
            for (int i = 0; i < rooms.length; i++) { sv.runCommand(tp[i]); ctx.waitTicks(25); if (i == 0) sp.getConnection().waitForChunksRender(); ctx.takeScreenshot(String.format("x%02d_%s", i, rooms[i])); }

            // ── 수액 ──
            sv.runCommand(tp[0]); ctx.waitTicks(20);
            sv.runOnServer(s -> { sturdy(s, 40); pl(s).setHealth(20); });
            StringBuilder sap = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                sv.runOnServer(s -> { Employees.get(pl(s)).sapTicks = 0; WorkManager.handleAction(pl(s), find(s, TreeSapEntity.class).getId(), 0); });
                String one = sv.computeOnServer(s -> (int) pl(s).getHealth() + "hp/" + ToolState.sapChance + "% "); sap.append(one).append(' ');
            }
            log("sap uses: " + sap);
            sv.runOnServer(s -> { Employees.get(pl(s)).sapTicks = 0; ToolState.sapChance = 100; WorkManager.handleAction(pl(s), find(s, TreeSapEntity.class).getId(), 0); });
            int d0 = sv.computeOnServer(He2Test::deaths);
            log("sap forced: sapTicks=" + sv.computeOnServer(s -> Employees.get(pl(s)).sapTicks));
            ctx.waitTicks(420);
            log("sap explosion: deaths " + d0 + "->" + sv.computeOnServer(He2Test::deaths) + " chance now " + ToolState.sapChance);

            // ── 대피소 ──
            sv.runCommand(tp[1]); ctx.waitTicks(20);
            ctx.waitTicks(5);
            sv.runOnServer(s -> { sturdy(s, 40); WorkManager.handleAction(pl(s), find(s, ShelterEntity.class).getId(), 0); });
            ctx.waitTicks(5);
            log("shelter: sheltered=" + sv.computeOnServer(s -> ToolState.isSheltered(pl(s))) + " " + sv.computeOnServer(s -> {
                float h = pl(s).getHealth(); int spv = Employees.get(pl(s)).sp;
                pl(s).hurtServer(s.overworld(), s.overworld().damageSources().magic(), 50f); Employees.damageSp(pl(s), 30);
                return "hp " + h + "->" + pl(s).getHealth() + " sp " + spv + "->" + Employees.get(pl(s)).sp; }));
            ctx.takeScreenshot("x20_shelter_inside");
            sv.runCommand("tp @p " + (bx + 2.5) + " " + by + " " + (bz + 14 + 0.5));
            ctx.waitTicks(5);
            log("shelter after leaving 3+ blocks: sheltered=" + sv.computeOnServer(s -> ToolState.isSheltered(pl(s))));

            // ── 초상 ──
            sv.runCommand(tp[2]); ctx.waitTicks(20);
            sv.runOnServer(s -> WorkManager.handleAction(pl(s), find(s, PortraitEntity.class).getId(), 0));
            log("portrait alone: etched=" + (ToolState.etched != null));

            // ── 노트 ──
            sv.runCommand(tp[3]); ctx.waitTicks(20);
            int d1 = sv.computeOnServer(He2Test::deaths);
            sv.runOnServer(s -> { sturdy(s, 40); int id = find(s, NotesEntity.class).getId(); WorkManager.handleAction(pl(s), id, 0); });
            log("notes taken: holder=" + sv.computeOnServer(s -> ToolState.isNotesHolder(pl(s))));
            sv.runOnServer(s -> WorkManager.handleAction(pl(s), find(s, NotesEntity.class).getId(), 0));
            ctx.waitTicks(5);
            log("notes returned early: deaths " + d1 + "->" + sv.computeOnServer(He2Test::deaths));
            sv.runCommand(tp[3]); ctx.waitTicks(5);
            sv.runOnServer(s -> { sturdy(s, 40); WorkManager.handleAction(pl(s), find(s, NotesEntity.class).getId(), 0); ToolState.notesTicks = 700; ToolState.notesWorked = true;
                WorkManager.handleAction(pl(s), find(s, NotesEntity.class).getId(), 0); });
            ctx.waitTicks(5);
            log("notes returned properly: deaths=" + sv.computeOnServer(He2Test::deaths) + " holder=" + (ToolState.notesHolder != null));

            // ── 은하수의 아이 ──
            sv.runCommand(tp[4]); ctx.waitTicks(20);
            sv.runOnServer(s -> { sturdy(s, 40); var a = find(s, ChildGalaxyEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.NORMAL, ses(a, 10)); });
            log("child: pebble=" + sv.computeOnServer(s -> Employees.get(pl(s)).pebble) + " q=" + sv.computeOnServer(s -> find(s, ChildGalaxyEntity.class).getQliphoth()));
            sv.runCommand(tp[5]); ctx.waitTicks(20);
            sv.runOnServer(s -> { fresh(s); WorkManager.handleAction(pl(s), find(s, FreischutzEntity.class).getId(), 1); });
            ctx.waitTicks(3);
            sv.runOnServer(s -> WorkManager.clear(pl(s)));
            log("child after pebble holder works elsewhere: q=" + sv.computeOnServer(s -> find(s, ChildGalaxyEntity.class).getQliphoth()));
            int d2 = sv.computeOnServer(He2Test::deaths);
            sv.runOnServer(s -> { fresh(s); WorkManager.handleAction(pl(s), find(s, FreischutzEntity.class).getId(), 1); });
            ctx.waitTicks(5);
            sv.runOnServer(s -> WorkManager.clear(pl(s)));
            log("child collapse at 0: deaths " + d2 + "->" + sv.computeOnServer(He2Test::deaths) + " q=" + sv.computeOnServer(s -> find(s, ChildGalaxyEntity.class).getQliphoth()));

            // ── 마탄의 사수 ──
            sv.runCommand(tp[5]); ctx.waitTicks(20);
            ctx.waitTicks(5);
            sv.runOnServer(s -> { sturdy(s, 40); Facility.get(s).energy = 5; WorkManager.handleAction(pl(s), find(s, FreischutzEntity.class).getId(), 2); });
            log("freischutz request with 5 energy: energy=" + sv.computeOnServer(He2Test::energy) + " cd=" + sv.computeOnServer(s -> find(s, FreischutzEntity.class).getCooldown()));
            sv.runOnServer(s -> { Facility.get(s).energy = 100; WorkManager.handleAction(pl(s), find(s, FreischutzEntity.class).getId(), 2); });
            ctx.waitTicks(3);
            ctx.takeScreenshot("x21_magic_bullet");
            log("freischutz request with 100: energy=" + sv.computeOnServer(He2Test::energy) + " cd=" + sv.computeOnServer(s -> find(s, FreischutzEntity.class).getCooldown()));
            log("freischutz normal result: " + sv.computeOnServer(s -> { var a = find(s, FreischutzEntity.class); Employees.get(pl(s)).justice = 60;
                a.onWorkFinished(pl(s), WorkManager.Result.NORMAL, ses(a, 9)); return "counter=" + a.getCounter(); }));

            // ── 이름 없는 태아 ──
            sv.runCommand(tp[6]); ctx.waitTicks(20);
            log("fetus: " + sv.computeOnServer(s -> { sturdy(s, 40); var a = find(s, FetusEntity.class); int n = 0;
                while (a.getCry() == 0 && n < 30) { a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); n++; } return "bad works=" + n + " cry=" + a.getCry(); }));
            int sp6 = sv.computeOnServer(s -> Employees.get(pl(s)).sp);
            ctx.waitTicks(210);
            ctx.takeScreenshot("x22_fetus_cry");
            log("fetus crying: SP " + sp6 + "->" + sv.computeOnServer(s -> Employees.get(pl(s)).sp));
            int d3 = sv.computeOnServer(He2Test::deaths);
            sv.runOnServer(s -> WorkManager.handleAction(pl(s), find(s, FetusEntity.class).getId(), 0));
            ctx.waitTicks(5);
            log("fetus volunteer: deaths " + d3 + "->" + sv.computeOnServer(He2Test::deaths) + " cry=" + sv.computeOnServer(s -> find(s, FetusEntity.class).getCry()));

            // ── 눈의 여왕 ──
            sv.runCommand(tp[7]); ctx.waitTicks(20);
            ctx.waitTicks(5);
            sv.runOnServer(s -> { sturdy(s, 40); var a = find(s, SnowQueenEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); });
            log("snow queen bad #1: frost=" + sv.computeOnServer(s -> Employees.get(pl(s)).frost));
            sv.runOnServer(s -> { var a = find(s, SnowQueenEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); });
            log("snow queen bad #2: frost=" + sv.computeOnServer(s -> Employees.get(pl(s)).frost));
            double z0 = sv.computeOnServer(s -> pl(s).getZ());
            sv.runCommand("tp @p ~ ~ ~3");
            ctx.waitTicks(10);
            ctx.takeScreenshot("x23_frozen");
            log("frozen moved? dz=" + String.format("%.2f", sv.computeOnServer(s -> pl(s).getZ()) - z0));
            int d4 = sv.computeOnServer(He2Test::deaths);
            sv.runCommand("lobcorp day next");
            ctx.waitTicks(5);
            log("snow queen day end while frozen: deaths " + d4 + "->" + sv.computeOnServer(He2Test::deaths) + " frost=" + sv.computeOnServer(s -> Employees.get(pl(s)).frost));

            // ── 나무꾼 ──
            sv.runCommand(tp[8]); ctx.waitTicks(20);
            sv.runOnServer(s -> { sturdy(s, 40); var a = find(s, WoodsmanEntity.class); a.onWorkFinished(pl(s), WorkManager.Result.BAD, ses(a, 2)); });
            ctx.waitTicks(80);
            log("woodsman after bad: armed=" + sv.computeOnServer(s -> find(s, WoodsmanEntity.class).isArmed()) + " breached=" + sv.computeOnServer(s -> find(s, WoodsmanEntity.class).isBreached()));
            int d5 = sv.computeOnServer(He2Test::deaths);
            sv.runOnServer(s -> WorkManager.handleAction(pl(s), find(s, WoodsmanEntity.class).getId(), 0));
            ctx.waitTicks(80);
            log("woodsman next visitor: deaths " + d5 + "->" + sv.computeOnServer(He2Test::deaths) + " breached=" + sv.computeOnServer(s -> find(s, WoodsmanEntity.class).isBreached()));
            sv.runOnServer(s -> find(s, WoodsmanEntity.class).hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f));

            // ── 허수아비 / 루돌타 / 장례식 : 탈출 후 피해 확인 ──
            String[] br = {"scarecrow", "rudolta", "funeral"};
            for (int k = 0; k < 3; k++) {
                int idx = 9 + k; String nm = br[k];
                sv.runCommand(tp[idx]); ctx.waitTicks(20);
                ctx.waitTicks(5);
                sv.runOnServer(s -> { sturdy(s, 70); for (Entity e : s.overworld().getAllEntities())
                    if (e instanceof BreachingAbnormality ba && ba.profile().id().equals(nm)) ba.breach(); });
                ctx.waitTicks(80);
                sv.runOnServer(s -> sturdy(s, 70));
                float h0 = sv.computeOnServer(s -> pl(s).getHealth()); int s0 = sv.computeOnServer(s -> Employees.get(pl(s)).sp);
                for (int t = 0; t < 12; t++) { ctx.waitTicks(20); sv.runOnServer(s -> { if (pl(s).getHealth() < 30) pl(s).setHealth(pl(s).getMaxHealth()); var d = Employees.get(pl(s)); d.panicTicks = 0; if (d.sp < 20) Employees.restoreSp(pl(s)); }); if (t == 5) ctx.takeScreenshot("x3" + k + "_" + nm + "_breach"); }
                log(nm + " breached 12s: HP " + h0 + "->" + sv.computeOnServer(s -> pl(s).getHealth()) + " SP " + s0 + "->" + sv.computeOnServer(s -> Employees.get(pl(s)).sp)
                        + " deaths=" + sv.computeOnServer(He2Test::deaths));
                sv.runOnServer(s -> { for (Entity e : s.overworld().getAllEntities())
                    if (e instanceof BreachingAbnormality ba && ba.profile().id().equals(nm)) ba.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f); });
                ctx.waitTicks(10);
            }
            log("scarecrow brains=" + sv.computeOnServer(s -> find(s, ScarecrowEntity.class).getBrains()));

            // ── 샤덴프로이데 ──
            sv.runCommand(tp[12]); ctx.waitTicks(20);
            ctx.waitTicks(10);
            sv.runOnServer(s -> { sturdy(s, 40); WorkManager.handleAction(pl(s), find(s, SchadenfreudeEntity.class).getId(), 2); });
            ctx.waitTicks(130);
            log("schadenfreude watched during work: q=" + sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).getQliphoth()) + " watched=" + sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).watched()));
            sv.runOnServer(s -> WorkManager.clear(pl(s)));
            sv.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14 * 12 + 0.5) + " 90 10");
            sv.runOnServer(s -> { sturdy(s, 40); WorkManager.handleAction(pl(s), find(s, SchadenfreudeEntity.class).getId(), 2); });
            ctx.waitTicks(130);
            log("schadenfreude NOT watched during work: q=" + sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).getQliphoth()) + " watched=" + sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).watched()));
            sv.runOnServer(s -> { WorkManager.clear(pl(s)); find(s, SchadenfreudeEntity.class).breach(); });
            ctx.waitTicks(80);
            BlockPos q0 = sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).blockPosition());
            ctx.waitTicks(60);
            BlockPos q1 = sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).blockPosition());
            log("schadenfreude breached, unwatched 3s: moved " + String.format("%.1f", Math.sqrt(q0.distSqr(q1))));
            sv.runCommand(tp[12]); ctx.waitTicks(20);
            ctx.waitTicks(60);
            BlockPos q2 = sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).blockPosition());
            ctx.takeScreenshot("x40_schadenfreude_watched");
            log("schadenfreude breached, watched 3s: moved " + String.format("%.1f", Math.sqrt(q1.distSqr(q2))) + " watched=" + sv.computeOnServer(s -> find(s, SchadenfreudeEntity.class).watched()));
            sv.runOnServer(s -> find(s, SchadenfreudeEntity.class).hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(pl(s)), 1000f));
            ctx.waitTicks(10);
            log("DONE");
        }
    }
}
