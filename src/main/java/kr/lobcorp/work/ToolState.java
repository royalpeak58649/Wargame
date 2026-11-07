package kr.lobcorp.work;

import net.minecraft.network.chat.Component;
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

    // ───────────── 다른 세계의 초상 (O-09-91) ─────────────

    /** 새겨진 자: 받을 피해가 대신하는 자에게 옮겨 간다 */
    public static UUID etched = null;
    /** 대신하는 자 */
    public static UUID victim = null;
    /** 지금까지 옮겨 간 피해 누적 */
    public static float portraitTotal = 0;

    public static boolean isEtched(ServerPlayer p) {
        return p != null && etched != null && etched.equals(p.getUUID());
    }

    public static boolean isVictim(ServerPlayer p) {
        return p != null && victim != null && victim.equals(p.getUUID());
    }

    /** 누적 피해에 따른 피해 배율 */
    public static float portraitMult() {
        if (portraitTotal >= 200f) return 2.5f;
        if (portraitTotal >= 100f) return 2.0f;
        return 1.5f;
    }

    public static void resetPortrait() {
        etched = null;
        victim = null;
        portraitTotal = 0;
    }

    public static void nextDay(MinecraftServer server) {
        sapChance = 0;
        shelterUser = null;
        shelterTicks = 0;
        shelterEntityId = -1;
        resetPortrait();
    }

    /** 직원이 죽었을 때 (도구형 환상체 관련 후속 처리) */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer p)) return;
        if (isEtched(p)) {
            resetPortrait();
            if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] 초상화 속 얼굴이 지워졌습니다. 초상화가 제자리로 돌아갑니다.")
                        .withStyle(s -> s.withColor(0x8A4AC0)));
            }
        } else if (isVictim(p)) {
            UUID e = etched;
            resetPortrait();
            if (e != null && p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                ServerPlayer ep = sl.getServer().getPlayerList().getPlayer(e);
                if (ep != null) {
                    ep.sendSystemMessage(Component.literal("  초상화 속 얼굴이 흐려진다. 대신 받아 줄 사람이 사라졌다.")
                            .withStyle(s -> s.withColor(0x8A4AC0).withItalic(true)));
                }
            }
        }
    }
}
