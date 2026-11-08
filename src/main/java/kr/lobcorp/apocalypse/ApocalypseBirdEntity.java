package kr.lobcorp.apocalypse;

import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 종말새 — 세 마리 새가 하나가 된 ALEPH. 무적이며, 세 개의 알이 모두 부서져야 사라진다.
 * 제자리에 떠서 주기적으로 순간이동하고, 정면의 직원에게 BLACK 피해를 준다.
 */
public class ApocalypseBirdEntity extends PathfinderMob {
    /** 특수 패턴 쿨다운 (파트 B에서 사용) */
    protected int specialCooldown = 0;
    private int teleportIn = 300;
    private int attackTicks = 0;

    public ApocalypseBirdEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (--teleportIn <= 0) {
            teleportIn = 300 + getRandom().nextInt(201);
            teleport(sl);
        }
        if (++attackTicks % 100 == 0) {
            attack(sl);
            if (specialCooldown > 0) specialCooldown--;
            else special(sl);
        }
    }

    /** 무작위 부서 중앙 홀로 순간이동한다 */
    private void teleport(ServerLevel sl) {
        List<BlockPos> centers = Facility.centers(sl.getServer());
        if (centers.isEmpty()) return;
        BlockPos c = centers.get(getRandom().nextInt(centers.size()));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.6, 1.0, 0.6, 0.02);
        snapTo(c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5, getYRot(), 0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.6, 1.0, 0.6, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    /** 정면 6블록 안의 직원에게 BLACK 피해 */
    private void attack(ServerLevel sl) {
        Vec3 look = getLookAngle();
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (distanceTo(p) > 6) continue;
            Vec3 dir = p.position().subtract(position());
            if (dir.lengthSqr() > 1.0E-4 && look.dot(dir.normalize()) <= 0.3) continue;
            float dmg = 14 + getRandom().nextInt(13);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            Employees.damageSp(p, Math.round(dmg));
            sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    /** 특수 패턴 훅 (파트 B에서 채운다) */
    protected void special(ServerLevel sl) {
    }
}
