package kr.lobcorp.ordeal;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 보라색 정오 「사랑을 주소서」 — 제자리에 박힌 석판.
 * 움직이지 않지만 20블록 안의 직원 위로 낙하 지점을 예고한 뒤 내리찍고, 발밑의 직원을 촉수로 휘감는다.
 * 600틱마다 주변 이상의 클리포트 카운터를 하나씩 갉아먹는다.
 */
public class VioletNoonEntity extends OrdealEntity {
    /** 낙하 공격이 떨어질 좌표. null이면 예고 중이 아니다. */
    private Vec3 landing = null;
    /** 예고가 끝날 때까지 남은 틱 */
    private int warn = 0;

    public VioletNoonEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 140)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.VIOLET_NOON;
    }

    @Override
    protected float[] resist() {
        return new float[]{0.8f, 2.0f, 0.8f, 1.0f};
    }

    @Override
    protected void registerGoals() {
        // 석판은 움직이지 않는다. 표적만 잡는다.
        addPlayerTargetGoal();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 2f);
        if (hit && le instanceof ServerPlayer sp) Employees.damageSp(sp, 1);
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);

        if (warn > 0) {
            warn--;
            if (landing != null) {
                for (int i = 0; i < 6; i++) {
                    double a = getRandom().nextDouble() * Math.PI * 2;
                    double r = 2.0 * Math.sqrt(getRandom().nextDouble());
                    sl.sendParticles(ParticleTypes.WITCH, landing.x + Math.cos(a) * r, landing.y + 0.2,
                            landing.z + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0);
                }
            }
            if (warn == 0) slam(sl);
            return;
        }

        if (tickCount % 120 == 0) {
            List<ServerPlayer> near = playersNear(sl, position(), 20.0);
            if (!near.isEmpty()) {
                landing = near.get(getRandom().nextInt(near.size())).position();
                warn = 80;
                sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 0.8f, 0.6f);
            }
        }
        if (tickCount % 20 == 0) {
            for (ServerPlayer p : playersNear(sl, position(), 4.0)) {
                Employees.damageSp(p, 1);
                p.hurtServer(sl, sl.damageSources().magic(), 1f);
            }
        }
        if (tickCount % 600 == 0) {
            BreachingAbnormality pick = null;
            for (Entity e : sl.getAllEntities()) {
                if (!(e instanceof AbnormalityEntity a) || !a.isAlive()) continue;
                if (!(a instanceof BreachingAbnormality b) || b.isBreached() || b.getQliphoth() <= 0) continue;
                if (a.distanceTo(this) > 40) continue;
                if (pick == null || getRandom().nextInt(2) == 0) pick = b;
            }
            if (pick != null) {
                pick.lowerQliphoth(null, 1);
                Facility.broadcast(sl.getServer(), Component.literal("[관제] 석판이 클리포트 카운터를 갉아먹습니다.")
                        .withStyle(s -> s.withColor(0x9B59D0)));
            }
        }
    }

    /** 예고한 지점을 내리찍는다. */
    private void slam(ServerLevel sl) {
        Vec3 at = landing;
        landing = null;
        if (at == null) return;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.4, at.z, 50, 1.2, 0.8, 1.2, 0.05);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 0.6f);
        for (ServerPlayer p : playersNear(sl, at, 2.0)) {
            p.hurtServer(sl, sl.damageSources().magic(), 30f);
        }
    }
}
