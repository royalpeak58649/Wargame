package kr.lobcorp.ordeal;

import kr.lobcorp.work.Facility;
import kr.lobcorp.work.FacilityData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * 시련의 발생·진행·정산을 관리한다. 클리포트 폭주 레벨이 2(여명)·4(정오)·6(황혼)에 도달하면 하루에 한 번씩 발생한다.
 */
public final class OrdealManager {
    private OrdealManager() {}

    private static final List<Active> ACTIVE = new ArrayList<>();

    /** 진행 중인 시련 하나. 처치되지 않고 사라진 개체가 있으면 failed가 켜진다. */
    private static final class Active {
        final OrdealType type;
        final List<OrdealEntity> entities;
        boolean failed;

        Active(OrdealType type, List<OrdealEntity> entities) {
            this.type = type;
            this.entities = entities;
        }
    }

    private static final EnumSet<OrdealTier> TRIGGERED = EnumSet.noneOf(OrdealTier.class);
    private static int lastDay = -1;

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        FacilityData f = Facility.get(server);
        if (f.day != lastDay) {
            TRIGGERED.clear();
            lastDay = f.day;
        }
        // 시련은 이제 클리포트 폭주 레벨(2/4/6)에서만 발생한다 (Meltdown 참고)
        for (Active a : new ArrayList<>(ACTIVE)) {
            // 살아 있지 않은 개체를 정리한다. 체력이 남은 채 사라졌다면 처치가 아니라 자멸·소멸이므로 실패로 본다.
            a.entities.removeIf(e -> {
                if (e.isAlive()) return false;
                if (e.getHealth() > 0) a.failed = true;
                return true;
            });
            if (!a.entities.isEmpty()) continue;
            ACTIVE.remove(a);
            if (a.failed) {
                Facility.broadcast(server, Component.literal("[관제] " + a.type.ko + " 시련을 막지 못했습니다.")
                        .withStyle(s -> s.withColor(0xFF5555)));
                continue;
            }
            int bonus = Math.max(1, Math.round(f.quota * a.type.tier.reward));
            Facility.addEnergy(server, bonus);
            Facility.broadcast(server, Component.literal("[관제] " + a.type.ko + " 시련을 극복했습니다. 에너지 +" + bonus)
                    .withStyle(s -> s.withColor(0x55FF55)));
        }
    }

    /** 해당 등급의 시련을 오늘 처음으로 발생시킨다. 이미 발생했거나 등급 풀이 비면 false. */
    public static boolean startTier(MinecraftServer server, OrdealTier tier) {
        if (tier == null || TRIGGERED.contains(tier)) return false;
        List<OrdealType> pool = OrdealType.ofTier(tier);
        if (pool.isEmpty()) return false;
        TRIGGERED.add(tier);
        start(server, pool.get(server.overworld().getRandom().nextInt(pool.size())));
        return true;
    }

    /** 오늘 이 등급의 시련이 이미 발생했는지 */
    public static boolean triggered(OrdealTier tier) {
        return TRIGGERED.contains(tier);
    }

    /** 시련을 발생시키고 소환한 개체 수를 돌려준다. */
    public static int start(MinecraftServer server, OrdealType type) {
        ServerLevel level = server.overworld();
        // 환상체가 수용된 부서의 중앙 홀에서만 발생한다. 그런 곳이 없으면 기존 동작(플레이어 위치)으로 대체한다.
        List<BlockPos> centers = new ArrayList<>(Facility.activeCenters(server));
        if (centers.isEmpty()) {
            ServerPlayer any = null;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                any = p;
                break;
            }
            if (any == null) return 0;
            centers.add(any.blockPosition());
        }
        Collections.shuffle(centers, new Random(level.getRandom().nextLong()));
        // 활성 중앙 홀이 type.sites보다 적으면 사이트 수를 줄이지 않고 셔플된 목록을 순환해 재사용한다.
        int siteCount = type.sites;
        List<OrdealEntity> spawned = new ArrayList<>();
        for (int i = 0; i < siteCount; i++) {
            BlockPos site = centers.get(i % centers.size());
            for (int g = 0; g < type.group; g++) {
                OrdealEntity e = type.entity.get().create(level, EntitySpawnReason.EVENT);
                if (e == null) continue;
                BlockPos pos = findSpawnPos(level, site, e.getRandom());
                if (pos == null) continue;
                e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, e.getRandom().nextFloat() * 360f, 0);
                level.addFreshEntity(e);
                spawned.add(e);
            }
        }
        if (spawned.isEmpty()) return 0;
        ACTIVE.add(new Active(type, spawned));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(type.ko)
                    .withStyle(s -> s.withColor(type.color))));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(type.tier.ko + "의 시련")
                    .withStyle(s -> s.withColor(type.color))));
            p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1f, 1f);
        }
        Facility.broadcast(server, Component.literal("[관제] " + type.ko + " 시련이 환상체가 있는 부서에서 시작되었습니다.")
                .withStyle(s -> s.withColor(type.color)));
        return spawned.size();
    }

    /**
     * 중앙 홀에는 표지 기둥이 서 있으므로 정중앙에는 절대 소환하지 않는다.
     * 최대 10번 무작위 오프셋(각 축 -5..5, max(|dx|,|dz|) >= 2)을 시도해 발과 머리 칸이 비어 있고 발밑이 단단한 자리를 찾는다.
     * 찾지 못하면 null (해당 개체는 소환하지 않는다).
     */
    private static BlockPos findSpawnPos(ServerLevel level, BlockPos site, RandomSource r) {
        for (int i = 0; i < 10; i++) {
            int dx = r.nextInt(11) - 5;
            int dz = r.nextInt(11) - 5;
            if (Math.max(Math.abs(dx), Math.abs(dz)) < 2) continue;
            BlockPos pos = site.offset(dx, 0, dz);
            BlockState here = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());
            BlockState below = level.getBlockState(pos.below());
            if (here.isSolid() || above.isSolid() || !below.isSolid()) continue;
            return pos;
        }
        return null;
    }

    /** 부모가 속한 시련에 자식 개체를 편입시킨다. 분열·증식하는 개체가 쓰러질 때 사용한다. */
    public static void adopt(OrdealEntity parent, OrdealEntity child) {
        for (Active a : ACTIVE) {
            if (a.entities.contains(parent)) {
                a.entities.add(child);
                return;
            }
        }
    }

    /** 환상체가 수용된 부서의 중앙 홀 중 하나를 무작위로 고른다. 없으면 전체 중앙 홀, 그것도 없으면 null */
    public static BlockPos randomCenter(MinecraftServer server, RandomSource r) {
        List<BlockPos> centers = Facility.activeCenters(server);
        if (centers.isEmpty()) centers = Facility.centers(server);
        if (centers.isEmpty()) return null;
        return centers.get(r.nextInt(centers.size()));
    }

    /** 모든 시련 개체를 제거하고 진행 상태를 비운다. */
    public static void clearAll(MinecraftServer server) {
        List<Entity> doomed = new ArrayList<>();
        for (ServerLevel l : server.getAllLevels()) {
            for (Entity e : l.getAllEntities()) {
                if (e instanceof OrdealEntity) doomed.add(e);
            }
        }
        for (Entity e : doomed) e.discard();
        ACTIVE.clear();
    }

    /** 진행 중인 시련 요약 */
    public static String status() {
        if (ACTIVE.isEmpty()) return "진행 중인 시련 없음";
        StringBuilder sb = new StringBuilder();
        for (Active a : ACTIVE) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(a.type.ko).append(" 남은 ").append(a.entities.size());
        }
        return sb.toString();
    }
}
