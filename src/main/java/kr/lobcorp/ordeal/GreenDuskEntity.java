package kr.lobcorp.ordeal;

import kr.lobcorp.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 녹색 황혼 「우리가 도달해야 할 곳」 — 제자리에 선 거대한 공장.
 * 스스로 움직이지 않고, 몸 안에서 녹색 여명과 녹색 정오를 계속 찍어낸다.
 * 충전이 끝날 때마다 주변에 기계가 12기 미만이면 세 기를 더 내보낸다.
 */
public class GreenDuskEntity extends OrdealEntity {
    /** 충전이 끝날 때까지 남은 틱 */
    private int charge = 0;

    public GreenDuskEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.GREEN_DUSK;
    }

    @Override
    protected float[] resist() {
        return new float[]{0.8f, 1.0f, 2.0f, 1.0f};
    }

    @Override
    protected void registerGoals() {
        // 공장은 움직이지 않는다. 목표를 하나도 두지 않는다.
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);

        charge++;
        if (charge % 20 == 0) {
            // 충전이 쌓일수록 더 많이, 더 높이 솟는다
            int count = charge > 700 ? 6 : 2;
            double top = getBbHeight() + (charge > 700 ? 1.5 : 0.5);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + top, getZ(), count, 0.6, 0.4, 0.6, 0.02);
        }

        if (charge >= 900 + getRandom().nextInt(300)) {
            charge = 0;
            produce(sl);
        }
    }

    /** 주변 기계가 모자라면 녹색 여명·녹색 정오를 세 기 찍어낸다. */
    private void produce(ServerLevel sl) {
        AABB around = getBoundingBox().inflate(64.0);
        List<GreenDawnEntity> dawns = sl.getEntitiesOfClass(GreenDawnEntity.class, around);
        List<GreenNoonEntity> noons = sl.getEntitiesOfClass(GreenNoonEntity.class, around);
        if (dawns.size() + noons.size() >= 12) return;
        for (int i = 0; i < 3; i++) {
            OrdealEntity child;
            if (getRandom().nextBoolean()) {
                child = ModEntities.GREEN_DAWN.create(sl, EntitySpawnReason.EVENT);
            } else {
                child = ModEntities.GREEN_NOON.create(sl, EntitySpawnReason.EVENT);
            }
            if (child == null) continue;
            double dx = (getRandom().nextDouble() - 0.5) * 4.0;
            double dz = (getRandom().nextDouble() - 0.5) * 4.0;
            child.snapTo(getX() + dx, getY(), getZ() + dz, getRandom().nextFloat() * 360f, 0);
            sl.addFreshEntity(child);
            OrdealManager.adopt(this, child);
        }
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 30, 1.2, 0.8, 1.2, 0.04);
    }
}
