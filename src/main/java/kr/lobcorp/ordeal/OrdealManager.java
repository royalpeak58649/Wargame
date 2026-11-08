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
 * 시련의 발생·진행·정산을 관리한다. 할당량 대비 에너지 비율이 등급 기준을 넘으면 하루에 한 번씩 발생한다.
 */
public final class OrdealManager {
    private OrdealManager() {}

    private static final List<Active> ACTIVE = new ArrayList<>();

    private record Active(OrdealType type, List<OrdealEntity> entities) {}

    private static final EnumSet<OrdealTier> TRIGGERED = EnumSet.noneOf(OrdealTier.class);
    private static int lastDay = -1;

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        FacilityData f = Facility.get(server);
        if (f.day != lastDay) {
            TRIGGERED.clear();
            lastDay = f.day;
        }
        if (kr.lobcorp.work.Extraction.hasFacility(server) && f.quota > 0) {
            float frac = (float) f.energy / f.quota;
            for (OrdealTier tier : OrdealTier.values()) {
                if (TRIGGERED.contains(tier)) continue;
                if (f.day < tier.minDay) continue;
                if (frac < tier.threshold) continue;
                TRIGGERED.add(tier);
                List<OrdealType> pool = OrdealType.ofTier(tier);
                if (pool.isEmpty()) continue;
                start(server, pool.get(server.overworld().getRandom().nextInt(pool.size())));
            }
        }
        for (Active a : new ArrayList<>(ACTIVE)) {
            a.entities().removeIf(e -> !e.isAlive());
            if (!a.entities().isEmpty()) continue;
            ACTIVE.remove(a);
            int bonus = Math.max(1, Math.round(f.quota * a.type().tier.reward));
            Facility.addEnergy(server, bonus);
            Facility.broadcast(server, Component.literal("[관제] " + a.type().ko + " 시련을 극복했습니다. 에너지 +" + bonus)
                    .withStyle(s -> s.withColor(0x55FF55)));
        }
    }

    /** 시련을 발생시키고 소환한 개체 수를 돌려준다. */
    public static int start(MinecraftServer server, OrdealType type) {
        ServerLevel level = server.overworld();
        List<BlockPos> centers = new ArrayList<>(Facility.centers(server));
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
        int siteCount = Math.min(type.sites, centers.size());
        List<OrdealEntity> spawned = new ArrayList<>();
        for (int i = 0; i < siteCount; i++) {
            BlockPos site = centers.get(i);
            for (int g = 0; g < type.group; g++) {
                OrdealEntity e = type.entity.get().create(level, EntitySpawnReason.EVENT);
                if (e == null) continue;
                BlockPos pos = site.offset(e.getRandom().nextInt(9) - 4, 0, e.getRandom().nextInt(9) - 4);
                BlockState here = level.getBlockState(pos);
                BlockState above = level.getBlockState(pos.above());
                BlockState below = level.getBlockState(pos.below());
                if (here.isSolid() || above.isSolid() || !below.isSolid()) pos = site;
                e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, e.getRandom().nextFloat() * 360f, 0);
                level.addFreshEntity(e);
                spawned.add(e);
            }
        }
        ACTIVE.add(new Active(type, spawned));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(type.ko)
                    .withStyle(s -> s.withColor(type.color))));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(type.tier.ko + "의 시련")
                    .withStyle(s -> s.withColor(type.color))));
            p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1f, 1f);
        }
        Facility.broadcast(server, Component.literal("[관제] " + type.ko + " 시련이 시작되었습니다.")
                .withStyle(s -> s.withColor(type.color)));
        return spawned.size();
    }

    /** 부서 중앙 홀 중 하나를 무작위로 고른다. 없으면 null */
    public static BlockPos randomCenter(MinecraftServer server, RandomSource r) {
        List<BlockPos> centers = Facility.centers(server);
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
            sb.append(a.type().ko).append(" 남은 ").append(a.entities().size());
        }
        return sb.toString();
    }
}
