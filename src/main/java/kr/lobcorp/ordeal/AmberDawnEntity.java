package kr.lobcorp.ordeal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 호박색 여명 — 완벽한 음식. 작은 벌레들이 부서 홀을 기어다니며 뛰어올라 물어뜯는다.
 * 일정 시간이 지나면 연기와 함께 다른 부서 중앙 홀로 파고든다.
 */
public class AmberDawnEntity extends OrdealEntity {
    private int life = 800 + getRandom().nextInt(400);

    public AmberDawnEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.AMBER_DAWN;
    }

    @Override
    protected float[] resist() {
        return new float[]{2.0f, 1.0f, 1.0f, 2.0f};
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 2f);
        if (hit) level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.4f, 1.8f);
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        LivingEntity target = getTarget();
        if (tickCount % 40 == 0 && target != null && onGround() && distanceToSqr(target) <= 25.0) {
            Vec3 dir = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
            if (dir.lengthSqr() > 0.0001) dir = dir.normalize().scale(0.6);
            setDeltaMovement(dir.x, 0.4, dir.z);
            sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.3f, 2f);
        }
        if (--life <= 0) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 12, 0.3, 0.2, 0.3, 0.02);
            BlockPos c = OrdealManager.randomCenter(sl.getServer(), sl.getRandom());
            if (c != null) {
                BlockPos to = c.offset(sl.getRandom().nextInt(7) - 3, 0, sl.getRandom().nextInt(7) - 3);
                teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, to.getX() + 0.5, to.getY() + 0.3, to.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
            }
            life = 800 + getRandom().nextInt(400);
        }
    }
}
