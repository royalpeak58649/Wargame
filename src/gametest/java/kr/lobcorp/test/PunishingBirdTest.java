package kr.lobcorp.test;

import kr.lobcorp.ModItems;
import kr.lobcorp.client.screen.WorkScreen;
import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.WorkManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PunishingBirdTest implements FabricClientGameTest {
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

    private static String bird(TestServerContext server) {
        return server.computeOnServer(s -> {
            var b = find(s, PunishingBirdEntity.class);
            return "q=" + b.getQliphoth() + " breached=" + b.isBreached() + " angry=" + b.isAngry()
                    + " pos=" + String.format("%.1f,%.1f,%.1f", b.getX(), b.getY(), b.getZ())
                    + " dist=" + String.format("%.1f", b.distanceTo(player(s))) + " hp=" + b.getHealth()
                    + " | player hp=" + String.format("%.1f", player(s).getHealth()) + " | " + b.routeDebug()
                    + " door=" + s.overworld().getBlockState(b.blockPosition().atY(player(s).getBlockY()).west(1));
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
            int[] b = server.computeOnServer(s -> new int[]{player(s).getBlockX(), player(s).getBlockY(), player(s).getBlockZ()});
            int bx = b[0], by = b[1], bz = b[2];
            server.runCommand("execute positioned " + bx + " " + by + " " + bz + " run lobcorp build punishing_bird");
            server.runCommand("execute positioned " + bx + " " + by + " " + (bz + 14) + " run lobcorp build fairy_festival");
            server.runCommand("tp @p " + (bx + 3.5) + " " + by + " " + (bz + 0.5) + " -90 2");
            ctx.waitTicks(40);
            sp.getConnection().waitForChunksRender();
            ctx.takeScreenshot("b01_contained");
            server.runOnServer(s -> WorkManager.openScreen(player(s), find(s, PunishingBirdEntity.class)));
            ctx.waitForScreen(WorkScreen.class);
            ctx.waitTicks(5);
            ctx.takeScreenshot("b02_screen");
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
            log("start: " + bird(server));

            // 카운터 규칙 1: 다른 환상체 작업 시작 (50%) — 10번 시도해 감소 횟수 확인
            int drops = server.computeOnServer(s -> {
                var bd = find(s, PunishingBirdEntity.class);
                var fairy = find(s, FairyFestivalEntity.class);
                int n = 0;
                for (int i = 0; i < 10; i++) {
                    bd.resetQliphoth();
                    PunishingBirdEntity.onAnyWorkStarted(s.overworld(), fairy, player(s));
                    if (bd.getQliphoth() < 3) n++;
                }
                bd.resetQliphoth();
                // 자기 자신에게 작업할 때는 내려가지 않아야 한다
                PunishingBirdEntity.onAnyWorkStarted(s.overworld(), bd, player(s));
                return n * 100 + bd.getQliphoth();
            });
            log("other-work drops in 10 tries=" + drops / 100 + ", self-work leaves q=" + drops % 100);
            // 실제 경로: 요정 작업 시작 → 훅 호출 여부
            server.runCommand("tp @p " + (bx + 4.5) + " " + by + " " + (bz + 14.5) + " -90 2");
            server.runOnServer(s -> WorkManager.handleAction(player(s), find(s, FairyFestivalEntity.class).getId(), 2));
            ctx.waitTicks(5);
            log("after real fairy work start: " + bird(server));
            server.runOnServer(s -> WorkManager.clear(player(s)));

            // 카운터 규칙 2: 패닉 → -1. 0이 되면 3초 뒤 탈출
            server.runOnServer(s -> {
                find(s, PunishingBirdEntity.class).resetQliphoth();
                for (int i = 0; i < 3; i++) Employees.panic(player(s));
                Employees.get(player(s)).panicTicks = 0;
                Employees.restoreSp(player(s));
            });
            log("after 3 panics: " + bird(server));
            server.runCommand("effect clear @p");
            server.runCommand("effect give @p resistance 120 1 true");
            server.runCommand("tp @p " + (bx - 3.5) + " " + by + " " + (bz + 0.5) + " -90 -8");
            ctx.waitTicks(70);
            log("t+70 (breach expected): " + bird(server));
            for (int i = 0; i < 8; i++) {
                ctx.waitTicks(20);
                log("chase t+" + (90 + i * 20) + ": " + bird(server));
                if (i == 2) ctx.takeScreenshot("b03_flying");
            }
            ctx.takeScreenshot("b04_pecking");

            // 격노: 맨손으로 때린다
            server.runOnServer(s -> {
                var bd = find(s, PunishingBirdEntity.class);
                bd.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(player(s)), 1f);
            });
            ctx.waitTicks(8);
            ctx.takeScreenshot("b05_angry");
            for (int i = 0; i < 4; i++) {
                ctx.waitTicks(15);
                log("angry t+" + (i + 1) * 15 + ": " + bird(server));
            }

            // 제압: 부리(E.G.O)로 타격
            server.runOnServer(s -> player(s).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BEAK)));
            for (int i = 0; i < 15; i++) {
                boolean still = server.computeOnServer(s -> {
                    var bd = find(s, PunishingBirdEntity.class);
                    if (!bd.isBreached()) return false;
                    bd.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(player(s)), 6f);
                    return bd.isBreached();
                });
                if (!still) break;
                ctx.waitTicks(12);
            }
            log("after suppression: " + bird(server));
            server.runCommand("tp @p " + (bx + 3.5) + " " + by + " " + (bz + 0.5) + " -90 2");
            server.runOnServer(s -> {
                player(s).getInventory().add(new ItemStack(ModItems.BEAK_SUIT));
                player(s).setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BEAK_SUIT));
            });
            ctx.waitTicks(30);
            ctx.takeScreenshot("b06_back_home");
            log("DONE");
        }
    }
}
