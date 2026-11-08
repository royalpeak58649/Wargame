package kr.lobcorp.apocalypse;

import kr.lobcorp.ModEntities;
import kr.lobcorp.ModItems;
import kr.lobcorp.entity.BigBirdEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.entity.JudgementBirdEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.FacilityData;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 종말새 사건. 세 마리 새(징벌의 새·큰 새·심판의 새)가 모두 탈출하면 검은 숲이 열리고 종말새가 나타난다.
 * 종말새는 무적이며, 세 개의 알을 모두 부수면 쓰러지고 살아남은 직원 전원이 「황혼」의 가호를 받는다.
 */
public final class ApocalypseManager {
    private ApocalypseManager() {}

    /** 사건이 시작되는 최소 일차 */
    public static final int MIN_DAY = 21;

    private static boolean active = false;
    private static boolean triggeredToday = false;
    private static int lastDay = -1;
    private static final boolean[] EGG_ALIVE = new boolean[3];
    private static UUID bossId = null;
    /** 재진입 방지 */
    private static boolean busy = false;

    private static final String[] EGG_NAMES = {"작은 부리", "긴 팔", "큰 눈"};

    /** 탈출 사건을 받아 사건 발생 여부를 판정한다. */
    public static void onBreach(MinecraftServer server) {
        if (server == null) return;
        if (busy || active || triggeredToday) return;
        if (Facility.get(server).day < MIN_DAY) return;
        ServerLevel level = server.overworld();
        List<BreachingAbnormality> birds = birds(level);
        List<BreachingAbnormality> breached = new ArrayList<>();
        for (BreachingAbnormality b : birds) {
            if (b.isBreached()) breached.add(b);
        }
        if (breached.size() == 2) {
            busy = true;
            try {
                for (BreachingAbnormality b : birds) {
                    if (!b.isBreached()) {
                        b.breach();
                        break;
                    }
                }
            } finally {
                busy = false;
            }
            breached.clear();
            for (BreachingAbnormality b : birds) {
                if (b.isBreached()) breached.add(b);
            }
        }
        if (breached.size() >= 3) start(server);
    }

    /** 사건을 시작한다. 시설 중앙 홀이 하나도 없으면 시작하지 않는다. */
    public static boolean start(MinecraftServer server) {
        if (server == null) return false;
        ServerLevel level = server.overworld();
        List<BlockPos> centers = new ArrayList<>(Facility.centers(server));
        if (centers.isEmpty()) return false;
        active = true;
        triggeredToday = true;
        for (int i = 0; i < EGG_ALIVE.length; i++) EGG_ALIVE[i] = true;
        // 세 마리 새는 검은 숲으로 들어간다
        for (BreachingAbnormality b : birds(level)) {
            if (b.isBreached()) b.forceReturn(null);
        }
        Collections.shuffle(centers, new Random(level.getRandom().nextLong()));
        BlockPos bossPos = centers.get(0);
        ApocalypseBirdEntity boss = ModEntities.APOCALYPSE_BIRD.create(level, EntitySpawnReason.EVENT);
        if (boss != null) {
            boss.snapTo(bossPos.getX() + 0.5, bossPos.getY() + 1, bossPos.getZ() + 0.5, 0, 0);
            level.addFreshEntity(boss);
            bossId = boss.getUUID();
        }
        for (int k = 0; k < 3; k++) {
            BlockPos c = centers.get(Math.min(centers.size() - 1, k + 1));
            ApocalypseEggEntity egg = ModEntities.APOCALYPSE_EGG.create(level, EntitySpawnReason.EVENT);
            if (egg == null) continue;
            egg.setKind(k);
            egg.snapTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 0, 0);
            level.addFreshEntity(egg);
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("세 마리 새가 하나가 되었다")
                    .withStyle(s -> s.withColor(0xAA0000))));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("종말새")
                    .withStyle(s -> s.withColor(0xFF3333).withBold(true))));
            p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1f, 0.8f);
        }
        Facility.broadcast(server, Component.literal("[관제] 검은 숲의 문이 열렸습니다. 세 개의 알을 모두 부수십시오.")
                .withStyle(s -> s.withColor(0xFF3333).withBold(true)));
        return true;
    }

    /** 알 하나가 부서졌다. 모두 부서지면 승리 처리. */
    public static void eggDestroyed(MinecraftServer server, int kind) {
        if (server == null) return;
        if (kind < 0 || kind >= EGG_ALIVE.length) return;
        EGG_ALIVE[kind] = false;
        Facility.broadcast(server, Component.literal("[관제] " + EGG_NAMES[kind] + "의 알이 부서졌습니다.")
                .withStyle(s -> s.withColor(0xFFAA55)));
        for (boolean b : EGG_ALIVE) {
            if (b) return;
        }
        victory(server);
    }

    /** 종말새 격파. 보상을 뿌리고 사건을 끝낸다. */
    private static void victory(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof ApocalypseBirdEntity) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY() + 1, e.getZ(), 6, 1.0, 1.0, 1.0, 0);
                drop(level, e.getX(), e.getY() + 1, e.getZ(), new ItemStack(ModItems.TWILIGHT));
                drop(level, e.getX(), e.getY() + 1, e.getZ(), new ItemStack(ModItems.TWILIGHT_SUIT));
                e.discard();
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Employees.get(p).gifts.add("twilight");
            Employees.apply(p);
            Employees.sync(p);
        }
        Facility.broadcast(server, Component.literal("[관제] 종말새가 쓰러졌습니다. 검은 숲에 다시 빛이 듭니다. 살아남은 직원 모두가 「황혼」의 가호를 받았습니다.")
                .withStyle(s -> s.withColor(0x55FF55)));
        active = false;
        bossId = null;
    }

    private static void drop(ServerLevel level, double x, double y, double z, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, x, y, z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    public static boolean eggAlive(int kind) {
        if (kind < 0 || kind >= EGG_ALIVE.length) return false;
        return EGG_ALIVE[kind];
    }

    public static boolean isActive() {
        return active;
    }

    /** 1초마다 날짜 갱신과 사건 정리 */
    public static void tick(MinecraftServer server) {
        if (server == null) return;
        if (server.getTickCount() % 20 != 0) return;
        FacilityData f = Facility.get(server);
        if (f.day != lastDay) {
            lastDay = f.day;
            triggeredToday = false;
        }
        if (!active) return;
        ServerLevel level = server.overworld();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof ApocalypseBirdEntity) return;
        }
        active = false;
        bossId = null;
    }

    /** 종말새와 알을 모두 제거하고 사건을 끝낸다. */
    public static void clearAll(MinecraftServer server) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        List<Entity> doomed = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof ApocalypseBirdEntity || e instanceof ApocalypseEggEntity) doomed.add(e);
        }
        for (Entity e : doomed) e.discard();
        active = false;
        bossId = null;
    }

    /** 사건 요약 */
    public static String status() {
        if (!active) return "진행 중인 종말새 사건 없음";
        StringBuilder sb = new StringBuilder("종말새 사건 진행 중 — 남은 알: ");
        boolean first = true;
        for (int i = 0; i < EGG_ALIVE.length; i++) {
            if (!EGG_ALIVE[i]) continue;
            if (!first) sb.append(", ");
            sb.append(EGG_NAMES[i]);
            first = false;
        }
        if (first) sb.append("없음");
        return sb.toString();
    }

    /** 세 마리 새 목록 */
    private static List<BreachingAbnormality> birds(ServerLevel level) {
        List<BreachingAbnormality> out = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof PunishingBirdEntity || e instanceof BigBirdEntity || e instanceof JudgementBirdEntity) {
                if (e instanceof BreachingAbnormality b && b.isAlive()) out.add(b);
            }
        }
        return out;
    }
}
