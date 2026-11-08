package kr.lobcorp.ordeal;

import kr.lobcorp.work.Employees;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 녹색 여명 「의심」 — 사람 형상의 기계. 쓰러진 직원의 시체를 향해 창을 내리꽂아 주변 직원의 정신력을 갉아먹는다.
 */
public class GreenDawnEntity extends OrdealEntity {
    public GreenDawnEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.GREEN_DAWN;
    }

    @Override
    protected float[] resist() {
        return new float[]{0.8f, 1.3f, 2.0f, 1.0f};
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 4f);
        if (!hit) return false;
        level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 1.4f);
        if (le instanceof ServerPlayer sp && !sp.isAlive()) {
            for (ServerPlayer p : playersNear(level, sp.position(), 6.0)) {
                Employees.damageSp(p, 3);
                p.sendSystemMessage(Component.literal("  「의심」이 쓰러진 직원의 시체를 찔러댑니다.")
                        .withStyle(s -> s.withColor(0x55CC55).withItalic(true)));
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.4f, 0.8f);
        }
        return true;
    }
}
