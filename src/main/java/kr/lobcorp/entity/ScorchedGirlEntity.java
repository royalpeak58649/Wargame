package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * F-01-02 불타버린 소녀 — 카운터가 0이 되면 탈출해 가장 가까운 직원에게 달려가 자폭한다.
 * 자폭 후에는 격리실에 다시 나타난다. 도착 전에 제압할 수도 있다.
 */
public class ScorchedGirlEntity extends BreachingAbnormality {
    private static final int FUSE = 25;
    private static final int MAX_BREACH_TICKS = 600; // 30초 안에 못 닿아도 그 자리에서 터진다

    private int fuse = -1;
    /** 클라이언트 전용: 터지기 직전 떨림 */
    public int fuseClient = 0;

    public ScorchedGirlEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.SCORCHED_GIRL;
    }

    @Override
    protected double chaseSpeed() {
        return 1.25;
    }

    @Override
    protected String breachAdvice() {
        return "불타버린 소녀가 직원에게 닿기 전에 제압하십시오!";
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  소녀는 구석에 웅크린 채 움직이지 않는다. 성냥만이 조용히 탄다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  성냥불이 잠깐 크게 일렁였다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (getRandom().nextBoolean()) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  타다 남은 성냥 끝이 짧아진 것 같다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 자폭 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // 닿으면 바로 때리지 않고 짧은 도화선을 켠다
        if (fuse < 0) {
            fuse = FUSE;
            level.broadcastEntityEvent(this, (byte) 10);
            level.playSound(null, blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1f, 1.4f);
        }
        return true;
    }

    private void explode(ServerLevel sl) {
        fuse = -1;
        double x = getX(), y = getY() + 0.6, z = getZ();
        sl.explode(this, x, y, z, 3.0f, false, Level.ExplosionInteraction.NONE);
        sl.sendParticles(ParticleTypes.FLAME, x, y, z, 80, 1.2, 0.8, 1.2, 0.05);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(x - 4, y - 3, z - 4, x + 4, y + 3, z + 4))) {
            p.igniteForSeconds(4);
        }
        returnToContainment(Component.literal("[관제] F-01-02 자폭. 잠시 후 격리실에서 다시 확인되었습니다.").withStyle(st -> st.withColor(0xFFAA55)));
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) fuseClient = FUSE;
        else super.handleEntityEvent(id);
    }

    @Override
    protected void onBreachWarning(ServerLevel sl) {
        super.onBreachWarning(sl);
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 15, 0.3, 0.4, 0.3, 0.02);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (fuseClient > 0) fuseClient--;
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        // 가슴을 꿰뚫은 성냥은 늘 타고 있다
        if (tickCount % 8 == 0) {
            double yaw = Math.toRadians(getYRot());
            sl.sendParticles(ParticleTypes.FLAME, getX() - Math.sin(yaw) * 0.25, getY() + 1.3, getZ() + Math.cos(yaw) * 0.25, 1, 0.02, 0.02, 0.02, 0.005);
        }
        if (!isBreached()) {
            fuse = -1;
            return;
        }
        if (fuse > 0) {
            if (fuse % 5 == 0) sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 3, 0.2, 0.3, 0.2, 0);
            if (--fuse == 0) explode(sl);
        } else if (breachTicks > MAX_BREACH_TICKS && fuse < 0) {
            fuse = FUSE;
            sl.broadcastEntityEvent(this, (byte) 10);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBreached() ? SoundEvents.FIRE_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
