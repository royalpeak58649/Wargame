package kr.lobcorp.work;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** 도구형 환상체의 시설 단위 상태 */
public final class ToolState {
    private ToolState() {}

    /** 거대한 나무 수액: 항아리가 터질 확률 (%) */
    public static int sapChance = 0;

    public static void nextDay(MinecraftServer server) {
        sapChance = 0;
    }

    /** 직원이 죽었을 때 (도구형 환상체 관련 후속 처리) */
    public static void onDeath(LivingEntity entity, DamageSource source) {
    }
}
