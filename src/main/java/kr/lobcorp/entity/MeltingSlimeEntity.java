package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Employees;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * D-03-109-1 — 녹아내리는 사랑이 삼킨 직원에게서 태어난 점액 덩어리.
 * 환상체가 아니며, E.G.O 무기로만 제대로 피해가 들어간다. RED 피해는 오히려 몸을 불린다.
 * 거대해진 점액이 죽으면 어미가 격노한다.
 */
public class MeltingSlimeEntity extends PathfinderMob {
    /** 거대한 점액인가 */
    private static final EntityDataAccessor<Boolean> BIG =
            SynchedEntityData.defineId(MeltingSlimeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions SMALL_SIZE = EntityDimensions.scalable(0.8f, 0.8f);
    private static final EntityDimensions BIG_SIZE = EntityDimensions.scalable(1.4f, 1.8f);

    private static final double SMALL_SPEED = 0.2;
    private static final double BIG_SPEED = 0.24;
    private static final float SMALL_HEALTH = 40f;
    private static final float BIG_HEALTH = 160f;
    /** 격노를 알릴 어미 탐색 반경 */
    private static final double ENRAGE_RADIUS = 128.0;

    public MeltingSlimeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SMALL_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, SMALL_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BIG, false);
    }

    public boolean isBig() {
        return entityData.get(BIG);
    }

    public void setBig(boolean big) {
        entityData.set(BIG, big);
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(big ? BIG_HEALTH : SMALL_HEALTH);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(big ? BIG_SPEED : SMALL_SPEED);
        if (big) setHealth(BIG_HEALTH);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (BIG.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBig() ? BIG_SIZE : SMALL_SIZE;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        // 벽 너머의 직원도 찾아낸다 (시야 무시). Goal 판정은 엔티티 ID에 따라 격틱으로 돌기 때문에 tickCount 모듈러를 쓰면 안 된다.
        targetSelector.addGoal(1, new Goal() {
            private final TargetingConditions cond = TargetingConditions.forCombat().range(32).ignoreLineOfSight();

            @Override
            public boolean canUse() {
                if (getRandom().nextInt(4) != 0) return false;
                LivingEntity cur = getTarget();
                return cur == null || !cur.isAlive() || (cur instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
            }

            @Override
            public void start() {
                if (level() instanceof ServerLevel sl) {
                    Player found = sl.getNearestPlayer(cond, MeltingSlimeEntity.this);
                    if (found instanceof ServerPlayer sp && sp.isCreative()) found = null;
                    setTarget(found);
                }
            }

            @Override
            public boolean canContinueToUse() {
                return false;
            }
        });
    }

    // ───────────── 공격 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        float dmg = isBig() ? 9f + getRandom().nextInt(6) : 5f + getRandom().nextInt(5);
        boolean hit = le.hurtServer(level, level.damageSources().magic(), dmg);
        if (hit) {
            if (le instanceof ServerPlayer sp) Employees.damageSp(sp, Math.round(dmg));
            level.playSound(null, blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1f, 0.8f);
            level.sendParticles(ParticleTypes.ITEM_SLIME, le.getX(), le.getY() + 1, le.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
        }
        return hit;
    }

    // ───────────── 내성 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float mult = 0.5f;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                switch (ego.color) {
                    case RED -> {
                        // RED 피해는 점액을 불린다
                        heal(amount);
                        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                        return false;
                    }
                    case WHITE -> mult = 1.0f;
                    case BLACK -> mult = 2.0f;
                    case PALE -> mult = 1.0f;
                }
                mult *= kr.lobcorp.Grades.mult(kr.lobcorp.Grades.ofItem(ego), kr.lobcorp.Grades.ofEntity(this));
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    // ───────────── 죽음 ─────────────

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!isBig()) return;
        if (!(level() instanceof ServerLevel sl)) return;
        MeltingLoveEntity love = sl.getNearestEntity(MeltingLoveEntity.class,
                net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(),
                this, getX(), getY(), getZ(), getBoundingBox().inflate(ENRAGE_RADIUS));
        if (love != null) love.enrage();
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.4, getZ(), 1, 0.3, 0.2, 0.3, 0.005);
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_slime_big", isBig());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        if (in.getBooleanOr("lc_slime_big", false)) setBig(true);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
