package kr.lobcorp.ordeal;

import kr.lobcorp.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 호박색 황혼 「먹이사슬」 — 거대한 벌레.
 * 표적을 향해 기어가며 정면 4블록 안의 직원을 한 번에 물어뜯고, 크게 다치면 몸이 굳어 느려진다.
 * 600틱 남짓마다 땅을 파고들어 주변에 호박색 여명을 토해내거나, 다른 부서 중앙 홀로 파고든다.
 */
public class AmberDuskEntity extends OrdealEntity {
    /** 다음 굴 파기가 실행될 틱 */
    private int burrowAt = 600 + getRandom().nextInt(200);
    /** 소환 직후 땅에서 솟아오르는 연출을 한 번만 하기 위한 표시 */
    private boolean emerged = false;

    public AmberDuskEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.14)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.AMBER_DUSK;
    }

    @Override
    protected float[] resist() {
        return new float[]{1.2f, 0.8f, 0.5f, 2.0f};
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // 실제로 들어간 피해가 크면 몸이 굳어 잠시 느려진다
        if (hurt && amount >= 8f) {
            addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, false, false));
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;

        if (!emerged) {
            emerged = true;
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 30, 1.0, 0.4, 1.0, 0.04);
            sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 0.6f);
            for (ServerPlayer p : playersNear(sl, position(), 3.0)) {
                p.hurtServer(sl, sl.damageSources().magic(), 8f);
            }
        }

        if (tickCount % 35 == 0) bite(sl);

        if (tickCount >= burrowAt) {
            burrow(sl);
            burrowAt = tickCount + 600 + getRandom().nextInt(200);
        }
    }

    /** 정면 4블록 안의 직원을 한꺼번에 물어뜯는다. */
    private void bite(ServerLevel sl) {
        Vec3 look = getLookAngle();
        boolean any = false;
        for (ServerPlayer p : playersNear(sl, position(), 4.0)) {
            Vec3 to = new Vec3(p.getX() - getX(), p.getY() - getY(), p.getZ() - getZ());
            if (to.lengthSqr() < 0.0001) continue;
            if (look.dot(to.normalize()) <= 0.5) continue;
            p.hurtServer(sl, sl.damageSources().magic(), 14f);
            any = true;
        }
        if (any) sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9f, 0.6f);
    }

    /** 땅을 파고든다. 주변에 호박색 여명이 적으면 토해내고, 아니면 다른 부서 중앙 홀로 이동한다. */
    private void burrow(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 20, 0.6, 0.3, 0.6, 0.03);
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 0.5f);
        AABB around = getBoundingBox().inflate(24.0);
        List<AmberDawnEntity> worms = sl.getEntitiesOfClass(AmberDawnEntity.class, around);
        if (worms.size() < 3) {
            for (int i = 0; i < 3; i++) {
                AmberDawnEntity child = ModEntities.AMBER_DAWN.create(sl, EntitySpawnReason.EVENT);
                if (child == null) continue;
                double dx = (getRandom().nextDouble() - 0.5) * 3.0;
                double dz = (getRandom().nextDouble() - 0.5) * 3.0;
                child.snapTo(getX() + dx, getY(), getZ() + dz, getRandom().nextFloat() * 360f, 0);
                sl.addFreshEntity(child);
                OrdealManager.adopt(this, child);
            }
            return;
        }
        BlockPos c = OrdealManager.randomCenter(sl.getServer(), sl.getRandom());
        if (c == null) return;
        BlockPos to = c.offset(getRandom().nextInt(7) - 3, 0, getRandom().nextInt(7) - 3);
        teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, to.getX() + 0.5, to.getY() + 0.4, to.getZ() + 0.5, 20, 0.6, 0.3, 0.6, 0.03);
    }
}
