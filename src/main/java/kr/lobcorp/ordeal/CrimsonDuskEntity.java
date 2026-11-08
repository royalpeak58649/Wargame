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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 진홍색 황혼 「막이 내리면」 — 거대한 광대.
 * 몸과 정신을 함께 짓밟고, 가까이 붙은 직원을 땅으로 내리찍어 묶어버린다.
 * 쓰러져도 광대들은 끝나지 않는다. 진홍색 정오 두 기가 남는다.
 */
public class CrimsonDuskEntity extends OrdealEntity {
    public CrimsonDuskEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 350)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.CRIMSON_DUSK;
    }

    @Override
    protected float[] resist() {
        return new float[]{1.0f, 0.8f, 1.2f, 1.5f};
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 10f);
        if (!hit) return false;
        level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 1.0f);
        if (le instanceof ServerPlayer sp) Employees.damageSp(sp, 5);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 140 != 0) return;
        LivingEntity target = getTarget();
        if (target == null || distanceToSqr(target) > 36.0) return;
        slam(sl);
    }

    /** 땅을 내리찍어 주변 직원을 짓밟고 묶는다. */
    private void slam(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 0.6f);
        for (int i = 0; i < 40; i++) {
            double a = getRandom().nextDouble() * Math.PI * 2;
            double r = 5.0 * Math.sqrt(getRandom().nextDouble());
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + Math.cos(a) * r, getY() + 0.3,
                    getZ() + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0.02);
        }
        for (ServerPlayer p : playersNear(sl, position(), 5.0)) {
            p.hurtServer(sl, sl.damageSources().magic(), 12f);
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 50, 1.2, 1.0, 1.2, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.2f);
        for (int i = 0; i < 2; i++) {
            CrimsonNoonEntity child = ModEntities.CRIMSON_NOON.create(sl, EntitySpawnReason.EVENT);
            if (child == null) continue;
            double dx = (getRandom().nextDouble() - 0.5) * 3.0;
            double dz = (getRandom().nextDouble() - 0.5) * 3.0;
            child.snapTo(getX() + dx, getY(), getZ() + dz, getRandom().nextFloat() * 360f, 0);
            sl.addFreshEntity(child);
            OrdealManager.adopt(this, child);
        }
        Facility.broadcast(sl.getServer(), Component.literal("막이 내려도 광대들은 끝나지 않는다")
                .withStyle(s -> s.withColor(0xDC143C)));
    }
}
