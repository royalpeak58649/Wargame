package kr.lobcorp.work;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** 도구형 환상체의 시설 단위 상태 */
public final class ToolState {
    private ToolState() {}

    /** 거대한 나무 수액: 항아리가 터질 확률 (%) */
    public static int sapChance = 0;

    /** 3월 27일의 대피소: 안에 있는 직원 */
    public static UUID shelterUser = null;
    public static int shelterTicks = 0;
    public static int shelterEntityId = -1;

    /** 대피소 안에 있는가 (피해 무효) */
    public static boolean isSheltered(ServerPlayer p) {
        return p != null && shelterUser != null && shelterUser.equals(p.getUUID());
    }

    public static void nextDay(MinecraftServer server) {
        sapChance = 0;
        shelterUser = null;
        shelterTicks = 0;
        shelterEntityId = -1;
    }

    /** 직원이 죽었을 때 (도구형 환상체 관련 후속 처리) */
    public static void onDeath(LivingEntity entity, DamageSource source) {
    }
}
