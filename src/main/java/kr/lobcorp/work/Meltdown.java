package kr.lobcorp.work;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.ordeal.OrdealManager;
import kr.lobcorp.ordeal.OrdealTier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 클리포트 폭주 (Qliphoth Meltdown).
 * 환상체에 작업을 시작할 때마다 게이지가 차오르고, 게이지가 가득 차면 레벨이 오른다.
 * 레벨이 오르면 무작위 격리실에 폭주가 발생하며, 60초 안에 그 환상체에 작업을 시작해 진압해야 한다.
 * 레벨 2·4·6에서는 대신 시련이 발생한다.
 */
public final class Meltdown {
    private Meltdown() {}

    /** 폭주 하나의 제한 시간 (틱) */
    public static final int TIMER = 1200;

    private static int gauge = 0;
    private static int level = 0;
    private static int lastDay = -1;
    /** 폭주 중인 환상체 UUID → 남은 틱 */
    private static final Map<UUID, Integer> MELTING = new HashMap<>();

    /** 게이지 최대치: 3 + 일차/3 + 수용된 격리실 수/5 */
    public static int needed(MinecraftServer s) {
        if (s == null) return 3;
        FacilityData f = Facility.get(s);
        int occupied = 0;
        for (FacilityData.Slot slot : f.slots) {
            if (slot.abno != null && !slot.abno.isEmpty()) occupied++;
        }
        return 3 + f.day / 3 + occupied / 5;
    }

    /** 작업이 시작될 때마다 호출한다. 폭주 중인 환상체라면 진압하고, 아니면 게이지를 올린다. */
    public static void onWorkStarted(MinecraftServer s, AbnormalityEntity a) {
        if (s == null || a == null) return;
        if (MELTING.remove(a.getUUID()) != null) {
            Facility.broadcast(s, Component.literal("[관제] " + a.profile().name() + "의 클리포트 폭주를 진압했습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
            return;
        }
        if (a.profile().isTool()) return;
        gauge++;
        int need = needed(s);
        Component bar = Component.literal("클리포트 폭주 게이지 " + gauge + "/" + need + "  (레벨 " + level + ")")
                .withStyle(st -> st.withColor(0xFFAA00));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.sendOverlayMessage(bar);
        }
        if (gauge >= need) {
            gauge = 0;
            levelUp(s);
        }
    }

    /** 레벨을 하나 올린다. 시련 등급에 해당하면 시련을, 아니면 폭주를 발생시킨다. */
    public static void levelUp(MinecraftServer s) {
        if (s == null) return;
        level = level >= 10 ? 10 : level + 1;
        OrdealTier tier = switch (level) {
            case 2 -> OrdealTier.DAWN;
            case 4 -> OrdealTier.NOON;
            case 6 -> OrdealTier.DUSK;
            default -> null;
        };
        FacilityData f = Facility.get(s);
        if (tier != null && f.day >= tier.minDay && !OrdealManager.triggered(tier)) {
            if (OrdealManager.startTier(s, tier)) {
                Facility.broadcast(s, Component.literal("[관제] 클리포트 폭주 레벨 " + level + " — 시련이 다가옵니다.")
                        .withStyle(st -> st.withColor(tier == OrdealTier.DAWN ? 0xFFFF55 : 0xFF5555)));
                return;
            }
        }
        startMeltdowns(s);
    }

    /** 무작위 격리실에 폭주를 발생시킨다. */
    private static void startMeltdowns(MinecraftServer s) {
        ServerLevel level0 = s.overworld();
        List<AbnormalityEntity> eligible = new ArrayList<>();
        for (Entity e : level0.getAllEntities()) {
            if (!(e instanceof AbnormalityEntity a) || !a.isAlive()) continue;
            if (a.profile().isTool()) continue;
            if (a.isBreached()) continue;
            if (MELTING.containsKey(a.getUUID())) continue;
            eligible.add(a);
        }
        if (eligible.isEmpty()) return;
        int count = Math.max(1, Math.round(eligible.size() * (0.08f + 0.03f * level)));
        count = Math.min(count, eligible.size());
        Collections.shuffle(eligible, new Random(level0.getRandom().nextLong()));
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < count; i++) {
            AbnormalityEntity a = eligible.get(i);
            MELTING.put(a.getUUID(), TIMER);
            if (names.length() > 0) names.append(", ");
            names.append(a.profile().name());
        }
        Facility.broadcast(s, Component.literal("[관제] 클리포트 폭주 레벨 " + level
                        + "! 다음 격리실에 폭주가 발생했습니다 (60초 안에 작업하십시오): " + names)
                .withStyle(st -> st.withColor(0xFF5555)));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("레벨 " + level)
                    .withStyle(st -> st.withColor(0xFF5555))));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("클리포트 폭주")
                    .withStyle(st -> st.withColor(0xFF5555))));
            p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1f, 1f);
        }
    }

    /** 매 틱 호출. 날짜가 바뀌면 초기화하고, 폭주 타이머를 진행시킨다. */
    public static void tick(MinecraftServer s) {
        if (s == null) return;
        FacilityData f = Facility.get(s);
        if (f.day != lastDay) {
            lastDay = f.day;
            gauge = 0;
            level = 0;
            MELTING.clear();
        }
        if (MELTING.isEmpty()) return;
        if (s.getTickCount() % 10 != 0) return;
        ServerLevel level0 = s.overworld();
        for (UUID id : new ArrayList<>(MELTING.keySet())) {
            Entity e = level0.getEntity(id);
            if (!(e instanceof AbnormalityEntity a) || !a.isAlive() || a.isBreached()) {
                MELTING.remove(id);
                continue;
            }
            level0.sendParticles(ParticleTypes.FLAME, a.getX(), a.getY() + 1.0, a.getZ(), 6, 0.5, 0.6, 0.5, 0.01);
            level0.sendParticles(ParticleTypes.CRIMSON_SPORE, a.getX(), a.getY() + 1.2, a.getZ(), 4, 0.5, 0.6, 0.5, 0.01);
            // 작업 중이 아닐 때만 시간이 흐른다
            if (WorkManager.isAbnoBusy(a)) continue;
            int left = MELTING.get(id) - 10;
            if (left <= 0) {
                MELTING.remove(id);
                fail(s, a);
                continue;
            }
            MELTING.put(id, left);
            if (s.getTickCount() % 200 == 0) {
                Component msg = Component.literal(a.profile().name() + " 폭주 — 남은 시간 " + (left / 20) + "초")
                        .withStyle(st -> st.withColor(0xFF5555));
                for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                    if (p.level() != level0) continue;
                    if (p.distanceTo(a) > 24) continue;
                    p.sendOverlayMessage(msg);
                }
            }
        }
    }

    /** 폭주를 막지 못했다. 에너지를 잃고 클리포트 카운터가 붕괴한다. */
    private static void fail(MinecraftServer s, AbnormalityEntity a) {
        FacilityData f = Facility.get(s);
        int loss = 5 * level;
        f.energy = Math.max(0, f.energy - loss);
        if (a instanceof BreachingAbnormality b) b.meltdownFail();
        Facility.broadcast(s, Component.literal("[관제] " + a.profile().name() + "의 폭주를 막지 못했습니다! 에너지 -"
                        + loss + " · 클리포트 카운터 붕괴")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    public static boolean isMelting(AbnormalityEntity a) {
        return a != null && MELTING.containsKey(a.getUUID());
    }

    /** 현재 상태 요약 */
    public static String status(MinecraftServer s) {
        StringBuilder sb = new StringBuilder();
        sb.append("레벨 ").append(level).append(" · 게이지 ").append(gauge).append('/').append(needed(s))
                .append(" · 폭주 중 ").append(MELTING.size()).append("곳");
        if (!MELTING.isEmpty()) {
            sb.append(": ");
            boolean first = true;
            for (UUID id : MELTING.keySet()) {
                if (!first) sb.append(", ");
                first = false;
                Entity e = s == null ? null : s.overworld().getEntity(id);
                sb.append(e instanceof AbnormalityEntity a ? a.profile().name() : id.toString());
            }
        }
        return sb.toString();
    }

    /** 모든 폭주와 게이지를 초기화한다. */
    public static void clearAll() {
        gauge = 0;
        level = 0;
        MELTING.clear();
    }

    /** 관리자 명령용: 레벨을 강제로 올린다. */
    public static void forceLevelUp(MinecraftServer s) {
        levelUp(s);
    }
}
