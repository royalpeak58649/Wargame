package kr.lobcorp.test;

import kr.lobcorp.ModEntities;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.work.RoomBuilder;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

public class PanelTest implements FabricClientGameTest {
    static void log(String s) { System.out.println("[LOBCORP-TEST] " + s); }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var sv = sp.getServer();
            sp.getConnection().waitForChunksRender();
            sv.runCommand("time set noon");
            sv.runCommand("gamerule advance_time false");
            int[] b = sv.computeOnServer(s -> {
                var p = s.getPlayerList().getPlayers().get(0);
                return new int[]{p.getBlockX() + 4, p.getBlockY() + 1, p.getBlockZ()};
            });
            sv.runOnServer(s -> {
                var l = s.overworld();
                RoomBuilder.build(l, new BlockPos(b[0], b[1], b[2]), ModEntities.FORSAKEN_MURDERER, true);
                RoomBuilder.build(l, new BlockPos(b[0], b[1], b[2] + 10), ModEntities.ONE_SIN, false);
            });
            ctx.waitTicks(30);
            log("PANEL start " + read(sv, b, 0) + " | " + read(sv, b, 10));
            sv.runOnServer(s -> {
                for (var e : s.overworld().getAllEntities())
                    if (e instanceof BreachingAbnormality m) m.lowerQliphoth(null, 1);
            });
            ctx.waitTicks(30);
            log("PANEL lowered " + read(sv, b, 0));
            sv.runCommand("tp @p " + (b[0] - 2.5) + " " + b[1] + " " + (b[2] + 5.5) + " -70 -5");
            ctx.waitTicks(30);
            sp.getConnection().waitForChunksRender();
            ctx.takeScreenshot("panel_hall");
            sv.runCommand("tp @p " + (b[0] + 0.5) + " " + b[1] + " " + (b[2] + 0.5) + " -90 -25");
            ctx.waitTicks(20);
            ctx.takeScreenshot("panel_close");
            sv.runOnServer(s -> {
                for (var e : s.overworld().getAllEntities())
                    if (e instanceof BreachingAbnormality m) m.breach();
            });
            ctx.waitTicks(30);
            log("PANEL breached " + read(sv, b, 0));
            log("DONE");
        }
    }

    static String read(net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext sv, int[] b, int dz) {
        return sv.computeOnServer(s -> {
            double x = b[0] + 1.97, y = b[1] + 3.02, z = b[2] + dz + 0.5;
            var l = s.overworld().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,
                    new net.minecraft.world.phys.AABB(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1));
            StringBuilder o = new StringBuilder("n=" + l.size() + " ");
            for (var d : l) o.append(d.getText().getString().replace("\n", " | "));
            return o.toString();
        });
    }
}
