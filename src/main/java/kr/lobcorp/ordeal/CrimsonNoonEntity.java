package kr.lobcorp.ordeal;

import kr.lobcorp.ModEntities;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 진홍색 정오 「흥겨운 행렬」 — 거대한 광대.
 * 느리지만 묵직하게 직원의 몸과 정신을 함께 짓밟고, 쓰러지면 작은 광대 세 마리로 흩어진다.
 */
public class CrimsonNoonEntity extends OrdealEntity {
    public CrimsonNoonEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.CRIMSON_NOON;
    }

    @Override
    protected float[] resist() {
        return new float[]{1.0f, 0.8f, 1.2f, 1.5f};
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 6f);
        if (!hit) return false;
        level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 1.2f);
        if (le instanceof ServerPlayer sp) Employees.damageSp(sp, 3);
        return true;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 40, 1.0, 0.8, 1.0, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 1.4f);
        for (int i = 0; i < 3; i++) {
            CrimsonDawnEntity child = ModEntities.CRIMSON_DAWN.create(sl, EntitySpawnReason.EVENT);
            if (child == null) continue;
            double dx = (getRandom().nextDouble() - 0.5) * 3.0;
            double dz = (getRandom().nextDouble() - 0.5) * 3.0;
            child.snapTo(getX() + dx, getY(), getZ() + dz, getRandom().nextFloat() * 360f, 0);
            sl.addFreshEntity(child);
            OrdealManager.adopt(this, child);
        }
        Facility.broadcast(sl.getServer(), Component.literal("광대가 쓰러지자 작은 광대들이 흩어진다")
                .withStyle(s -> s.withColor(0xDC143C)));
    }
}
