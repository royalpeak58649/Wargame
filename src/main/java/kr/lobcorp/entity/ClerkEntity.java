package kr.lobcorp.entity;

import kr.lobcorp.ordeal.OrdealEntity;
import kr.lobcorp.work.AbnoEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 사무직 — 시설을 배회하는 일반 사무직 직원.
 * 플레이어가 적을 때도 사망/시체를 세는 환상체가 동작하도록, 부서 중앙 홀을 중심으로 돌아다닌다.
 * 위협(탈출 환상체·시련·하수인)을 보면 도망치고, 정신력이 바닥나면 공황에 빠진다.
 */
public class ClerkEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(ClerkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PANIC =
            SynchedEntityData.defineId(ClerkEntity.class, EntityDataSerializers.BOOLEAN);

    /** 최대 정신력 */
    public static final int MAX_SP = 20;
    /** 공황 지속 시간 */
    private static final int PANIC_TICKS = 600;
    /** 소속 부서 중앙 홀 */
    private BlockPos post = null;
    private int sp = MAX_SP;
    private int panicTicks = 0;

    public ClerkEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
        builder.define(PANIC, false);
    }

    /** 외형 번호 (0..3) */
    public int skin() {
        return entityData.get(SKIN);
    }

    public boolean isPanicking() {
        return entityData.get(PANIC);
    }

    public int sp() {
        return sp;
    }

    public BlockPos post() {
        return post;
    }

    /** 배치 직후 호출: 소속 부서, 외형, 이름을 정한다. */
    public void setup(BlockPos post, int skin, String name) {
        this.post = post;
        entityData.set(SKIN, Math.max(0, Math.min(3, skin)));
        setCustomName(Component.literal("사무직 " + name));
        setCustomNameVisible(false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 10f, 1.0, 1.4, ClerkEntity::isThreat));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    /** 사무직이 도망쳐야 하는 대상인가 */
    public static boolean isThreat(LivingEntity e) {
        if (e instanceof BreachingAbnormality b) return b.isBreached() && !b.isFriendly();
        return e instanceof OrdealEntity
                || e instanceof CensoredMinionEntity
                || e instanceof MeltingSlimeEntity
                || e instanceof ApostleEntity;
    }

    /** 정신력 피해. 0이 되면 공황에 빠진다. */
    public void damageSp(int n) {
        if (n <= 0) return;
        sp -= n;
        if (sp <= 0) {
            sp = 0;
            panicTicks = PANIC_TICKS;
            entityData.set(PANIC, true);
            AbnoEvents.clerkPanic(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isPanicking()) {
            tickPanic(sl);
            return;
        }
        if (post == null) return;
        if (tickCount % 60 != 0) return;
        double px = post.getX() + 0.5, py = post.getY(), pz = post.getZ() + 0.5;
        if (distanceToSqr(px, py, pz) <= 12 * 12) return;
        if (!sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10.0), ClerkEntity::isThreat).isEmpty()) return;
        getNavigation().moveTo(px, py, pz, 0.9);
    }

    private void tickPanic(ServerLevel sl) {
        if (panicTicks > 0) panicTicks--;
        if (panicTicks % 20 == 0) {
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2.0, getZ(), 2, 0.3, 0.2, 0.3, 0);
        }
        if (panicTicks % 40 == 0) {
            double tx = getX() + (getRandom().nextDouble() * 2 - 1) * 8;
            double tz = getZ() + (getRandom().nextDouble() * 2 - 1) * 8;
            getNavigation().moveTo(tx, getY(), tz, 1.4);
        }
        if (panicTicks <= 0) {
            sp = MAX_SP / 2;
            entityData.set(PANIC, false);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        AbnoEvents.clerkDeath(this);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (post != null) {
            out.putBoolean("lc_has_post", true);
            out.putInt("lc_post_x", post.getX());
            out.putInt("lc_post_y", post.getY());
            out.putInt("lc_post_z", post.getZ());
        }
        out.putInt("lc_sp", sp);
        out.putInt("lc_skin", skin());
        out.putInt("lc_panic", panicTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        if (in.getBooleanOr("lc_has_post", false)) {
            post = new BlockPos(in.getIntOr("lc_post_x", 0), in.getIntOr("lc_post_y", 0), in.getIntOr("lc_post_z", 0));
        }
        sp = in.getIntOr("lc_sp", MAX_SP);
        entityData.set(SKIN, Math.max(0, Math.min(3, in.getIntOr("lc_skin", 0))));
        panicTicks = in.getIntOr("lc_panic", 0);
        entityData.set(PANIC, panicTicks > 0);
    }
}
