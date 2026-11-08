package kr.lobcorp.work;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** 시설 단위 사건 전파 (탈출 / 직원 사망 / 패닉) */
public final class AbnoEvents {
    private AbnoEvents() {}

    private static List<AbnormalityEntity> all(MinecraftServer server) {
        List<AbnormalityEntity> list = new ArrayList<>();
        if (server == null) return list;
        ServerLevel level = server.overworld();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof AbnormalityEntity a && a.isAlive()) list.add(a);
        }
        return list;
    }

    /** who가 탈출했다. who 자신은 제외하고 알린다. */
    public static void breach(BreachingAbnormality who) {
        if (who == null || who.level().getServer() == null) return;
        for (AbnormalityEntity a : all(who.level().getServer())) {
            if (a == who) continue;
            a.onAnyBreach(who);
        }
        kr.lobcorp.apocalypse.ApocalypseManager.onBreach(who.level().getServer());
    }

    public static void death(ServerPlayer p) {
        if (p == null || p.level().getServer() == null) return;
        for (AbnormalityEntity a : all(p.level().getServer())) {
            a.onAnyEmployeeDeath(p);
        }
    }

    public static void panic(ServerPlayer p) {
        if (p == null || p.level().getServer() == null) return;
        for (AbnormalityEntity a : all(p.level().getServer())) {
            a.onAnyPanic(p);
        }
    }

    /** 현재 탈출 중인 환상체 수 */
    public static int breachedCount(MinecraftServer server) {
        int n = 0;
        for (AbnormalityEntity a : all(server)) {
            if (a instanceof BreachingAbnormality b && b.isBreached()) n++;
        }
        return n;
    }
}
