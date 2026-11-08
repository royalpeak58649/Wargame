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

    static void ctxWait(ServerPlayer p) {
        try { var fld = net.minecraft.world.entity.Entity.class.getDeclaredField("invulnerableTime"); fld.setAccessible(true); fld.setInt(p, 0); } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
            var sv = sp.getServer();
            sp.getConnection().waitForChunksRender();
            sv.runCommand("difficulty normal");
            sv.runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            String[] names = {"penitence(W)", "fist", "todays_expression(B)", "daredevil(P)"};
            for (int i = 0; i < 4; i++) {
                final int k = i;
                ctx.waitTicks(25);
                String r = sv.computeOnServer(s -> {
                    ServerPlayer p = pl(s);
                    Employees.restoreSp(p);
                    p.setHealth(p.getMaxHealth());
                    FakePlayer f = FakePlayer.get(s.overworld());
                    f.snapTo(p.getX() + 1, p.getY(), p.getZ(), 0, 0);
                    net.minecraft.world.item.Item it = k == 0 ? ModItems.PENITENCE : k == 2 ? ModItems.TODAYS_EXPRESSION : k == 3 ? ModItems.DAREDEVIL : null;
                    f.setItemInHand(InteractionHand.MAIN_HAND, it == null ? ItemStack.EMPTY : new ItemStack(it));
                    int sp0 = Employees.get(p).sp; float hp0 = p.getHealth();
                    p.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(f), 5f);
                    return "sp " + sp0 + "->" + Employees.get(p).sp + " hp " + hp0 + "->" + p.getHealth() + " max " + p.getMaxHealth();
                });
                log("PVP " + names[k] + " " + r);
            }
            ctx.waitTicks(25);
            String r = sv.computeOnServer(s -> {
                ServerPlayer p = pl(s);
                var d = Employees.get(p); d.fortitude += 20; Employees.apply(p);
                p.setHealth(p.getMaxHealth());
                float hp0 = p.getHealth();
                Employees.damagePale(p, 25f);
                return "pale25% on max " + p.getMaxHealth() + ": " + hp0 + "->" + p.getHealth();
            });
            log("PVP " + r);
        }
    }
}
