package kr.lobcorp.test;

import kr.lobcorp.ModItems;
import kr.lobcorp.work.Employees;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class PvpWhiteTest implements FabricClientGameTest {
    static void log(String s) { System.out.println("[LOBCORP-TEST] " + s); }
    static ServerPlayer pl(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var sv = sp.getServer();
            sp.getConnection().waitForChunksRender();
            sv.runCommand("difficulty normal");
            sv.runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            String r = sv.computeOnServer(s -> {
                ServerPlayer p = pl(s);
                Employees.restoreSp(p);
                p.setHealth(p.getMaxHealth());
                FakePlayer f = FakePlayer.get(s.overworld());
                f.snapTo(p.getX() + 1, p.getY(), p.getZ(), 0, 0);
                StringBuilder out = new StringBuilder();
                // WHITE 무기
                f.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PENITENCE));
                int sp0 = Employees.get(p).sp; float hp0 = p.getHealth();
                p.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(f), 5f);
                out.append("white sp ").append(sp0).append("->").append(Employees.get(p).sp)
                        .append(" hp ").append(hp0).append("->").append(p.getHealth()).append("; ");
                // 맨손(일반 피해)
                f.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                sp0 = Employees.get(p).sp; hp0 = p.getHealth();
                p.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(f), 5f);
                out.append("fist sp ").append(sp0).append("->").append(Employees.get(p).sp)
                        .append(" hp ").append(hp0).append("->").append(p.getHealth());
                return out.toString();
            });
            log("PVP " + r);
        }
    }
}
