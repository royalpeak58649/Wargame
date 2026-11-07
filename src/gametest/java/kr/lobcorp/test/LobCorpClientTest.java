package kr.lobcorp.test;

import kr.lobcorp.ModItems;
import kr.lobcorp.client.screen.WorkScreen;
import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.ForsakenMurdererEntity;
import kr.lobcorp.entity.HappyTeddyBearEntity;
import kr.lobcorp.entity.ScorchedGirlEntity;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.WorkManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** 실제 클라이언트로 신규 환상체 3종 + 살인마 회귀를 돌려 보고 스크린샷을 남긴다. */
public class LobCorpClientTest implements FabricClientGameTest {
    private static void log(String s) {
        System.out.println("[LOBCORP-TEST] " + s);
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static <T extends AbnormalityEntity> T find(MinecraftServer s, Class<T> cls) {
        for (Entity e : s.overworld().getAllEntities()) if (cls.isInstance(e)) return cls.cast(e);
        return null;
    }

    private static void work(TestServerContext server, Class<? extends AbnormalityEntity> cls, int type) {
        server.runOnServer(s -> {
            Employees.get(player(s)).panicTicks = 0; // 테스트가 패닉으로 막히지 않게
            Employees.restoreSp(player(s));
            AbnormalityEntity a = find(s, cls);
            WorkManager.handleAction(player(s), a.getId(), type);
        });
    }

    /** 작업이 시작될 때까지(최대 40틱) 기다린 뒤, 끝날 때까지 기다린다 */
    private static int waitWork(TestServerContext server) {
        try {
            server.waitFor(s -> WorkManager.session(player(s)) != null, 40);
        } catch (AssertionError e) {
            log("  (work never started)");
            return -1;
        }
        return server.waitFor(s -> WorkManager.session(player(s)) == null, 1500);
    }

    private static String playerState(TestServerContext server) {
        return server.computeOnServer(s -> {
            ServerPlayer p = player(s);
            var d = Employees.get(p);
            return "hp=" + p.getHealth() + " alive=" + p.isAlive() + " dead=" + p.isDeadOrDying()
                    + " sp=" + d.sp + "/" + d.maxSp() + " fairy=" + d.fairyTicks + " deaths=" + p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DEATHS));
        });
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var server = sp.getServer();
            sp.getConnection().waitForChunksRender();
            for (String c : new String[]{"difficulty normal", "time set noon", "gamerule advance_time false",
                    "gamerule advance_weather false", "weather clear", "gamerule immediate_respawn true",
                    "gamerule keep_inventory true", "gamemode survival @p"}) server.runCommand(c);

            int[] base = server.computeOnServer(s -> new int[]{player(s).getBlockX(), player(s).getBlockY(), player(s).getBlockZ()});
            int bx = base[0], by = base[1], bz = base[2];
            server.runCommand("setworldspawn " + (bx - 6) + " " + by + " " + bz);
            String[] rooms = {"fairy_festival", "happy_teddy_bear", "scorched_girl", "forsaken_murderer"};
            for (int i = 0; i < rooms.length; i++) {
                server.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14 * i) + " run lobcorp build " + rooms[i]);
            }
            ctx.waitTicks(20);

            // ── 1. 요정의 축제 ──
            server.runCommand("tp @p " + (bx + 3.5) + " " + by + " " + (bz + 0.5) + " -90 -5");
            ctx.waitTicks(40);
            sp.getConnection().waitForChunksRender();
            ctx.takeScreenshot("01_fairy_room");
            server.runOnServer(s -> WorkManager.openScreen(player(s), find(s, FairyFestivalEntity.class)));
            ctx.waitForScreen(WorkScreen.class);
            ctx.waitTicks(5);
            ctx.takeScreenshot("02_fairy_screen");
            ctx.clickScreenButton("애착");
            log("fairy work ticks=" + waitWork(server));
            ctx.waitTicks(10);
            ctx.takeScreenshot("03_fairy_blessed");
            log("after fairy: " + playerState(server));

            // 축복받은 채 테디베어 작업 시도 → 잡아먹힘
            server.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14.5) + " -90 5");
            ctx.waitTicks(10);
            work(server, HappyTeddyBearEntity.class, 0);
            ctx.waitTicks(10);
            log("after devour: " + playerState(server));

            // ── 2. 행복한 테디베어 ──
            server.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14.5) + " -90 12");
            ctx.waitTicks(30);
            ctx.takeScreenshot("04_teddy_room");
            work(server, HappyTeddyBearEntity.class, 2);
            log("teddy work#1 ticks=" + waitWork(server) + " " + playerState(server));
            // 같은 직원이 연달아 → 포옹 (저항 V로 버티고 애니메이션 확인)
            server.runCommand("effect give @p resistance 10 4 true");
            work(server, HappyTeddyBearEntity.class, 2);
            ctx.waitTicks(6);
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            ctx.waitTicks(4);
            ctx.takeScreenshot("05_teddy_hug");
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            log("after hug (resistance V): " + playerState(server)
                    + " sessionStarted=" + server.computeOnServer(s -> WorkManager.session(player(s)) != null));
            server.runCommand("effect clear @p");
            // 다른 직원이 사이에 끼면 안전한지: 한 번 더 작업 → 이번엔 lastWorker가 초기화돼 정상 시작
            work(server, HappyTeddyBearEntity.class, 1);
            log("teddy after hug, new work started=" + server.computeOnServer(s -> WorkManager.session(player(s)) != null));
            waitWork(server);
            // 다시 같은 직원 → 이번엔 저항 없이: 사망 확인
            work(server, HappyTeddyBearEntity.class, 1);
            ctx.waitTicks(10);
            log("after real hug: " + playerState(server));

            // ── 3. 불타버린 소녀 ──
            server.runCommand("effect give @p resistance 60 2 true");
            server.runCommand("tp @p " + (bx - 3.5) + " " + by + " " + (bz + 28.5) + " -90 10");
            ctx.waitTicks(20);
            server.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 28.5) + " -45 5");
            ctx.waitTicks(30);
            ctx.takeScreenshot("06_scorched_contained");
            work(server, ScorchedGirlEntity.class, 3);
            log("scorched work ticks=" + waitWork(server) + " q=" + server.computeOnServer(s -> find(s, ScorchedGirlEntity.class).getQliphoth()));
            server.runCommand("tp @p " + (bx - 4.5) + " " + by + " " + (bz + 28.5) + " -90 8");
            float hpBefore = server.computeOnServer(s -> player(s).getHealth());
            server.runOnServer(s -> find(s, ScorchedGirlEntity.class).breach());
            ctx.waitTicks(30);
            ctx.takeScreenshot("07_scorched_breach");
            ctx.waitTicks(25);
            ctx.takeScreenshot("08_scorched_rushing");
            int boomed = server.waitFor(s -> !find(s, ScorchedGirlEntity.class).isBreached(), 700);
            ctx.waitTicks(3);
            ctx.takeScreenshot("09_scorched_after_explosion");
            log("scorched exploded after " + boomed + " ticks; hp " + hpBefore + " -> " + playerState(server)
                    + " girl=" + server.computeOnServer(s -> {
                        var g = find(s, ScorchedGirlEntity.class);
                        return "breached=" + g.isBreached() + " pos=" + g.blockPosition() + " q=" + g.getQliphoth();
                    }));

            // ── 4. 버려진 살인마 회귀 (공통 클래스 리팩터링 후) ──
            server.runCommand("effect give @p resistance 60 4 true");
            server.runCommand("tp @p " + (bx - 2.5) + " " + by + " " + (bz + 42.5) + " -90 10");
            server.runOnServer(s -> {
                find(s, ForsakenMurdererEntity.class).breach();
                player(s).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.REGRET));
            });
            for (int i = 0; i < 10; i++) {
                ctx.waitTicks(40);
                String st = server.computeOnServer(sv -> {
                    var m = find(sv, ForsakenMurdererEntity.class);
                    return "pos=" + m.blockPosition() + " dist=" + m.distanceTo(player(sv)) + " target=" + (m.getTarget() != null)
                            + " path=" + m.getNavigation().getPath() + " running=" + m.goalSelectorDebug().replace("kr.lobcorp.entity.", "");
                });
                log("murderer chase t=" + (i + 1) * 40 + " " + st);
                if (st.contains("dist=1.") || st.contains("dist=0.") || st.contains("dist=2.")) break;
            }
            for (int i = 0; i < 20; i++) {
                boolean still = server.computeOnServer(s -> {
                    var m = find(s, ForsakenMurdererEntity.class);
                    if (!m.isBreached()) return false;
                    m.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(player(s)), 8f);
                    return m.isBreached();
                });
                if (!still) break;
                ctx.waitTicks(12);
            }
            log("murderer: " + server.computeOnServer(s -> {
                var m = find(s, ForsakenMurdererEntity.class);
                return "breached=" + m.isBreached() + " pos=" + m.blockPosition() + " q=" + m.getQliphoth();
            }));

            // E.G.O 아이콘/방어구
            server.runOnServer(s -> {
                ServerPlayer p = player(s);
                p.getInventory().clearContent();
                p.getInventory().add(new ItemStack(ModItems.MATCH_FLAME));
                p.getInventory().add(new ItemStack(ModItems.BEAR_PAWS));
                p.getInventory().add(new ItemStack(ModItems.WINGBEAT));
                p.getInventory().add(new ItemStack(ModItems.MATCH_FLAME_SUIT));
                p.getInventory().add(new ItemStack(ModItems.BEAR_PAWS_SUIT));
                p.getInventory().add(new ItemStack(ModItems.WINGBEAT_SUIT));
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BEAR_PAWS_SUIT));
            });
            server.runCommand("tp @p " + (bx - 3) + " " + by + " " + (bz + 7) + " 0 10");
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            ctx.waitTicks(20);
            ctx.takeScreenshot("10_ego_items");
            log("DONE");
        }
    }
}
