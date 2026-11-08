package kr.lobcorp.ordeal;

import kr.lobcorp.work.Employees;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 진홍색 여명 「광대」 — 작은 광대들이 홀을 뛰어다니며 직원의 정신을 흔든다.
 * 가끔 제자리에서 춤을 추듯 깡충 뛰고, 쓰러지면 터지면서 주변 직원의 정신력을 크게 깎는다.
 */
public class CrimsonDawnEntity extends OrdealEntity {
    public CrimsonDawnEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.CRIMSON_DAWN;
    }

    @Override
    protected float[] resist() {
        return new float[]{1.0f, 0.8f, 1.2f, 1.5f};
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 2f);
        if (!hit) return false;
        level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.3f, 2f);
        if (le instanceof ServerPlayer sp) Employees.damageSp(sp, 2);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 60 == 0 && onGround()) {
            setDeltaMovement(0, 0.4, 0);
            sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.4, getZ(), 6, 0.5, 0.4, 0.5, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 0.4f, 1.8f);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 40, 0.8, 0.6, 0.8, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5f, 1.6f);
        for (ServerPlayer p : playersNear(sl, position(), 3.0)) {
            Employees.damageSp(p, 6);
            p.hurtServer(sl, sl.damageSources().magic(), 3f);
        }
    }
}
