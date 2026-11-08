package kr.lobcorp.ordeal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 녹색 정오 「이해의 과정」 — 총과 톱을 든 느린 골렘.
 * 16블록 안의 직원을 향해 일직선으로 총을 쏘고, 가까이 붙은 직원은 톱으로 갈아버린다.
 * 총을 8번 쏘면 과열되어 100틱 동안 식히느라 아무것도 하지 않는다.
 */
public class GreenNoonEntity extends OrdealEntity {
    /** 총을 쏜 횟수. 8에 닿으면 과열된다. */
    private int heat = 0;
    /** 과열 냉각이 끝날 때까지 남은 틱 */
    private int cooling = 0;

    public GreenNoonEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.GREEN_NOON;
    }

    @Override
    protected float[] resist() {
        return new float[]{0.8f, 1.3f, 2.0f, 1.0f};
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (cooling > 0) {
            cooling--;
            if (cooling % 10 == 0) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.6, getZ(), 8, 0.4, 0.3, 0.4, 0.02);
            }
            if (cooling == 0) heat = 0;
            return;
        }
        if (heat >= 8) {
            cooling = 100;
            sl.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8f, 0.8f);
            return;
        }
        if (tickCount % 25 == 0 && getTarget() instanceof ServerPlayer target
                && distanceTo(target) <= 16 && hasLineOfSight(target)) {
            shoot(sl, target);
        }
        if (tickCount % 10 == 0) {
            for (ServerPlayer p : playersNear(sl, position(), 2.5)) {
                p.hurtServer(sl, sl.damageSources().magic(), 2f);
            }
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.4f, 0.7f);
        }
    }

    /** 눈에서 대상의 눈까지 한 줄. 단단한 블록에서 멈추고, 처음 맞은 직원만 아프다. */
    private void shoot(ServerLevel sl, ServerPlayer target) {
        heat++;
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.01) return;
        Vec3 step = dir.normalize().scale(0.5);
        ServerPlayer hit = null;
        for (double t = 0.5; t <= len; t += 0.5) {
            Vec3 pt = from.add(step.scale(t * 2));
            if (sl.getBlockState(BlockPos.containing(pt)).isSolid()) break;
            sl.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0);
            AABB box = new AABB(pt.x - 0.3, pt.y - 0.3, pt.z - 0.3, pt.x + 0.3, pt.y + 0.3, pt.z + 0.3);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (p == this || !p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                hit = p;
                break;
            }
            if (hit != null) break;
        }
        sl.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.8f, 1.6f);
        if (hit != null) hit.hurtServer(sl, sl.damageSources().magic(), 3f);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean ok = le.hurtServer(level, level.damageSources().magic(), 3f);
        if (ok) level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.4f, 1.6f);
        return ok;
    }
}
