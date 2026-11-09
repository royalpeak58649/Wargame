package kr.lobcorp.work;

import kr.lobcorp.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 시설 에너지 / 할당량 / 날짜 */
public final class Facility {
    private Facility() {}

    private static final ServerBossEvent BAR = new ServerBossEvent(
            UUID.nameUUIDFromBytes("lobcorp:energy".getBytes()),
            Component.literal("에너지"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);

    public static FacilityData get(MinecraftServer server) {
        return server.overworld().getAttachedOrCreate(ModAttachments.FACILITY);
    }

    public static void addEnergy(MinecraftServer server, int amount) {
        FacilityData f = get(server);
        f.energy += amount;
        if (!f.quotaMet && f.energy >= f.quota) {
            f.quotaMet = true;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("관리 업무를 종료할 수 있습니다").withStyle(s -> s.withColor(0xAAAAAA))));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("에너지 할당량 달성").withStyle(s -> s.withColor(0xFFFF55))));
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 0.6f, 1f);
            }
            broadcast(server, Component.literal("[L사] 할당량 달성. 관리자는 /lobcorp day next 로 다음 날을 시작하세요.").withStyle(s -> s.withColor(0xAAAAAA)));
        }
    }

    public static void nextDay(MinecraftServer server) {
        ToolState.nextDay(server);
        FacilityData f = get(server);
        f.day++;
        f.energy = 0;
        f.quotaMet = false;
        f.quota = 20 + 10 * f.day;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            EmployeeData d = Employees.get(p);
            if (d.frost == 2) {
                broadcast(server, Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                        .append(p.getDisplayName())
                        .append(Component.literal(" 직원이 끝내 녹지 못했습니다.").withStyle(s -> s.withColor(0xFF5555))));
                p.hurtServer(p.level(), p.level().damageSources().magic(), 1000f);
            }
        }
        for (ServerLevel l : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity e : l.getAllEntities()) {
                if (e instanceof kr.lobcorp.entity.KnightDespairEntity k) k.clearBlessing();
                if (e instanceof kr.lobcorp.entity.LittlePrinceEntity lp) lp.clearDaily();
                if (e instanceof kr.lobcorp.entity.ParasiteTreeEntity pt) pt.clearDaily();
                if (e instanceof kr.lobcorp.entity.BackwardClockEntity bc) bc.clearDaily();
                if (e instanceof kr.lobcorp.entity.FleshIdolEntity fi) fi.clearDaily();
                if (e instanceof kr.lobcorp.entity.ExpressTrainEntity et) et.clearDaily();
                if (e instanceof kr.lobcorp.entity.MeltingLoveEntity ml) ml.clearDaily();
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            EmployeeData d = Employees.get(p);
            Employees.endPanic(p);
            d.sp = d.maxSp();
            p.setHealth(p.getMaxHealth());
            d.laetitiaHeart = false;
            d.princeSpores = 0;
            d.parasiteBlessed = false;
            d.nestInfectTicks = 0;
            d.blazingTicks = 0;
            d.firebirdBlind = false;
            d.pebble = false;
            d.frost = 0;
            d.frozenAt = null;
            d.blessed = false;
            d.devoured = false;
            d.devouredAt = null;
            d.sheepTicks = false;
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("에너지 할당량 " + f.quota).withStyle(s -> s.withColor(0x55FF55))));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Day " + f.day)));
        }
        Clerks.spawnAll(server);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        FacilityData f = get(server);
        BAR.setName(Component.literal("Day " + f.day + "   에너지 " + f.energy + " / " + f.quota));
        BAR.setProgress(Math.min(1f, f.quota <= 0 ? 1f : (float) f.energy / f.quota));
        BAR.setColor(f.quotaMet ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!BAR.getPlayers().contains(p)) BAR.addPlayer(p);
        }
        if (server.getTickCount() % 100 == 0) regenerate(server);
    }

    /** 부서 중앙 홀에서 5초마다 체력·정신력을 10씩 회복한다. */
    private static void regenerate(MinecraftServer server) {
        List<BlockPos> centers = centers(server);
        if (centers.isEmpty()) return;
        ServerLevel level = server.overworld();
        for (BlockPos c : centers) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.isAlive() || p.level() != level) continue;
                if (Math.abs(p.getX() - (c.getX() + 0.5)) > 6.5) continue;
                if (Math.abs(p.getZ() - (c.getZ() + 0.5)) > 6.5) continue;
                if (Math.abs(p.getY() - c.getY()) > 3) continue;
                EmployeeData d = Employees.get(p);
                if (d.isPanicking()) continue;
                p.heal(10f);
                if (d.sp < d.maxSp()) {
                    d.sp = Math.min(d.maxSp(), d.sp + 10);
                    Employees.sync(p);
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.2, p.getZ(), 4, 0.4, 0.5, 0.4, 0);
            }
        }
    }

    /** 부서 중앙 홀 중심 목록. 예전에 지은 시설은 격리실 자리에서 역산해 캐시한다. */
    public static List<BlockPos> centers(MinecraftServer server) {
        FacilityData f = get(server);
        if (!f.centers.isEmpty()) return f.centers;
        List<BlockPos> derived = deriveCenters(f.slots);
        if (!derived.isEmpty()) f.centers = new ArrayList<>(derived);
        return derived;
    }

    /**
     * 환상체가 실제로 수용된 부서의 중앙 홀만 돌려준다.
     * 각 격리실 자리(abno != "")에서 가장 가까운 중앙 홀을 "사용 중"으로 보고, 그런 중앙 홀만 남긴다.
     * 수용된 격리실이 하나도 없으면 빈 목록을 돌려준다.
     */
    public static List<BlockPos> activeCenters(MinecraftServer server) {
        List<BlockPos> all = centers(server);
        if (all.isEmpty()) return List.of();
        FacilityData f = get(server);
        Set<BlockPos> used = new HashSet<>();
        for (FacilityData.Slot s : f.slots) {
            if (s.abno == null || s.abno.isEmpty()) continue;
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos c : all) {
                double d = c.distSqr(s.origin);
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
            if (best != null) used.add(best);
        }
        if (used.isEmpty()) return List.of();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos c : all) {
            if (used.contains(c)) out.add(c);
        }
        return out;
    }

    private static List<BlockPos> deriveCenters(List<FacilityData.Slot> slots) {
        List<BlockPos> out = new ArrayList<>();
        if (slots.isEmpty()) return out;
        // 모든 격리실 자리에서 가능한 중앙 홀 z 후보를 모은다.
        Set<Integer> candidates = new HashSet<>();
        for (FacilityData.Slot s : slots) {
            for (int k = 0; k <= 4; k++) {
                candidates.add(s.origin.getZ() - 13 - 10 * k);
                candidates.add(s.origin.getZ() + 13 + 10 * k);
            }
        }
        Map<String, List<FacilityData.Slot>> byDept = new LinkedHashMap<>();
        for (FacilityData.Slot s : slots) byDept.computeIfAbsent(s.dept, k -> new ArrayList<>()).add(s);
        for (List<FacilityData.Slot> group : byDept.values()) {
            if (group.isEmpty()) continue;
            BlockPos first = group.get(0).origin;
            int oz = Integer.MIN_VALUE;
            for (int cand : candidates) {
                boolean ok = true;
                for (FacilityData.Slot s : slots) {
                    if (!isCandidate(s.origin.getZ(), cand)) { ok = false; break; }
                }
                if (ok) { oz = cand; break; }
            }
            if (oz == Integer.MIN_VALUE) return List.of();
            out.add(new BlockPos(first.getX(), first.getY(), oz));
        }
        return out;
    }

    private static boolean isCandidate(int slotZ, int oz) {
        for (int k = 0; k <= 4; k++) {
            if (slotZ == oz + 13 + 10 * k || slotZ == oz - 13 - 10 * k) return true;
        }
        return false;
    }

    public static void onLeave(ServerPlayer p) {
        BAR.removePlayer(p);
    }

    public static void broadcast(MinecraftServer server, Component msg) {
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}
