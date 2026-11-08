package kr.lobcorp.ordeal;

import kr.lobcorp.work.Employees;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 쪽빛 정오 「청소부」 — 빠르게 달려들어 직원을 통째로 삼키는 청소부.
 * 8블록 안의 표적을 향해 몸을 날려 덮치고, 죽인 직원을 먹어 치워 상처를 회복한다.
 */
public class IndigoNoonEntity extends OrdealEntity {
    /** 돌진 판정이 남은 틱 */
    private int charge = 0;
    /** 이번 돌진에서 이미 맞은 직원 */
    private final List<ServerPlayer> chargeHits = new ArrayList<>();

    public IndigoNoonEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.INDIGO_NOON;
    }

    @Override
    protected float[] resist() {
        return new float[]{1.0f, 1.2f, 0.5f, 0.8f};
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 3f);
        if (!hit) return false;
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.6f, 1.2f);
        if (le instanceof ServerPlayer sp) {
            Employees.damageSp(sp, 3);
            if (!sp.isAlive()) devour(level);
        }
        return true;
    }

    /** 쓰러진 직원을 먹어 치워 상처를 회복한다. */
    private void devour(ServerLevel sl) {
        setHealth(getMaxHealth());
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 20, 0.5, 0.5, 0.5, 0.03);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 1f, 0.8f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (charge > 0) {
            charge--;
            for (ServerPlayer p : playersNear(sl, position(), 2.0)) {
                if (chargeHits.contains(p)) continue;
                chargeHits.add(p);
                p.hurtServer(sl, sl.damageSources().magic(), 8f);
                Employees.damageSp(p, 4);
            }
            if (charge == 0) chargeHits.clear();
            return;
        }
        if (tickCount % 120 == 0 && onGround() && getTarget() instanceof ServerPlayer target
                && distanceTo(target) <= 8) {
            Vec3 dir = target.position().subtract(position());
            Vec3 flat = new Vec3(dir.x, 0, dir.z);
            if (flat.lengthSqr() > 0.01) {
                Vec3 d = flat.normalize().scale(1.0);
                setDeltaMovement(d.x, 0.2, d.z);
                charge = 10;
                chargeHits.clear();
                sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.6f, 1.6f);
            }
        }
    }
}
