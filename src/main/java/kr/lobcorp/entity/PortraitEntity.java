package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O-09-91 다른 세계의 초상 — 도구형. 사용한 직원은 「새겨진 자」가 되고,
 * 접속 중인 다른 직원 한 명이 「대신하는 자」가 되어 새겨진 자의 HP·SP 피해를 대신 받는다.
 * 누적 피해가 커질수록 배율이 오르고, 누군가 새로 사용하면 이전 새겨진 자는 죽는다.
 */
public class PortraitEntity extends AbnormalityEntity {
    public static final int STARE_TICKS = 40;

    public PortraitEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.PORTRAIT;
    }

    // ───────────── 사용 ─────────────

    @Override
    public void useTool(ServerPlayer p) {
        ServerLevel sl = p.level();
        // 이미 누군가 새겨져 있다면, 초상화는 새 얼굴을 원한다
        if (ToolState.etched != null && !ToolState.etched.equals(p.getUUID())) {
            ServerPlayer prev = sl.getServer().getPlayerList().getPlayer(ToolState.etched);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 초상화가 새 얼굴을 원했습니다.")
                    .withStyle(s -> s.withColor(0x8A4AC0)));
            ToolState.resetPortrait();
            if (prev != null && prev.isAlive()) {
                prev.hurtServer(prev.level(), prev.level().damageSources().magic(), 1000f);
            }
        }
        // 대신할 다른 직원을 찾는다
        List<ServerPlayer> others = new ArrayList<>();
        for (ServerPlayer o : sl.getServer().getPlayerList().getPlayers()) {
            if (o != p && o.isAlive() && !o.isSpectator()) others.add(o);
        }
        if (others.isEmpty()) {
            p.sendOverlayMessage(Component.literal("초상화 속 얼굴이 당신을 잠시 바라보다 돌아선다.")
                    .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
            return;
        }
        ServerPlayer victim = others.get(getRandom().nextInt(others.size()));
        ToolState.etched = p.getUUID();
        ToolState.victim = victim.getUUID();
        ToolState.portraitTotal = 0;

        sl.broadcastEntityEvent(this, (byte) 5);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.2f, 0.5f);
        sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.2, getZ(), 40, 0.6, 0.8, 0.6, 0.1);

        p.sendSystemMessage(Component.literal("  초상화 속 얼굴이 당신의 얼굴을 닮아간다.")
                .withStyle(s -> s.withColor(0xB46CFF).withItalic(true)));
        p.sendSystemMessage(Component.literal("  당신이 받을 상처는 ").withStyle(s -> s.withColor(0xAAAAAA).withItalic(true))
                .append(victim.getDisplayName())
                .append(Component.literal(" 직원에게 옮겨 갑니다.").withStyle(s -> s.withColor(0xAAAAAA).withItalic(true))));
        victim.sendSystemMessage(Component.literal("  어딘가에서 당신의 이름이 불렸다.")
                .withStyle(s -> s.withColor(0x8A1A1A).withItalic(true)));
        victim.sendSystemMessage(Component.literal("  초상화 너머의 누군가가 받을 상처가 당신에게 흘러들어옵니다.")
                .withStyle(s -> s.withColor(0x8A1A1A).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-09-91이 사용되었습니다. ")
                .withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 새겨진 자가 되었습니다.").withStyle(s -> s.withColor(0xFF5555))));
        Employees.sync(p);
        Employees.sync(victim);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.4, getZ(), 1, 0.3, 0.2, 0.3, 0);
        }
        if (ToolState.etched == null) return;
        // 새겨진 자가 사라졌다면 초상화는 제자리로 돌아간다
        ServerPlayer e = sl.getServer().getPlayerList().getPlayer(ToolState.etched);
        if (e == null || !e.isAlive()) {
            ToolState.resetPortrait();
            return;
        }
        ServerPlayer v = ToolState.victim == null ? null : sl.getServer().getPlayerList().getPlayer(ToolState.victim);
        if (v == null || !v.isAlive()) {
            ToolState.resetPortrait();
            e.sendSystemMessage(Component.literal("  초상화 속 얼굴이 흐려진다. 대신 받아 줄 사람이 사라졌다.")
                    .withStyle(s -> s.withColor(0x8A4AC0).withItalic(true)));
            Employees.sync(e);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 5) {
            // 클라이언트 연출용 — 별도 상태 없음
            return;
        }
        super.handleEntityEvent(id);
    }
}
