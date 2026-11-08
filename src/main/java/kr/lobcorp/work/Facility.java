package kr.lobcorp.work;

import kr.lobcorp.ModAttachments;
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
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            EmployeeData d = Employees.get(p);
            d.panicTicks = 0;
            d.sp = d.maxSp();
            d.laetitiaHeart = false;
            d.princeSpores = 0;
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
    }

    public static void onLeave(ServerPlayer p) {
        BAR.removePlayer(p);
    }

    public static void broadcast(MinecraftServer server, Component msg) {
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}
