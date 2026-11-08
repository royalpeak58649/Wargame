package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
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

/**
 * O-03-89-1 — O-03-89가 삼킨 직원에게서 태어난 하수인.
 * 환상체가 아니며, E.G.O 무기로만 제대로 피해가 들어간다. 가까이 있는 신중이 낮은 직원을 공황에 빠뜨린다.
 */
public class CensoredMinionEntity extends PathfinderMob {
    /** 공포 오라 주기 */
    private static final int FEAR_PERIOD = 100;
    /** 공포 오라 반경 */
    private static final double FEAR_RADIUS = 8.0;
    /** 공포 기준 신중 */
    private static final int FEAR_PRUDENCE = 65;
    /** 하수인 최대 수 */
    private static final int MAX_MINIONS = 8;

    public CensoredMinionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.FOLLOW_RANGE, 32);
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
                    Player found = sl.getNearestPlayer(cond, CensoredMinionEntity.this);
                    if (found instanceof ServerPlayer sp && ToolState.isSheltered(sp)) found = null;
                    if (found instanceof ServerPlayer sp2 && sp2.isCreative()) found = null;
                    setTarget(found);
                }
            }

            @Override
            public boolean canContinueToUse() {
                return false;
            }
        });
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        float dmg = 9f + getRandom().nextInt(3);
        boolean hit = le.hurtServer(level, level.damageSources().magic(), dmg);
        if (hit) {
            if (le instanceof ServerPlayer sp) Employees.damageSp(sp, Math.round(dmg));
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1f, 0.8f);
            level.sendParticles(ParticleTypes.SQUID_INK, le.getX(), le.getY() + 1, le.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
            // 직원을 죽였다면 그 자리에서 하나 더 태어난다
            if (!le.isAlive() && le instanceof ServerPlayer
                    && level.getEntitiesOfClass(CensoredMinionEntity.class, getBoundingBox().inflate(256.0)).size() < MAX_MINIONS) {
                CensoredMinionEntity minion = kr.lobcorp.ModEntities.CENSORED_MINION.create(level, EntitySpawnReason.EVENT);
                if (minion != null) {
                    minion.snapTo(le.getX(), le.getY(), le.getZ(), le.getYRot(), 0);
                    level.addFreshEntity(minion);
                }
            }
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float mult = 0.5f;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                mult = switch (ego.color) {
                    case RED -> 0.8f;
                    case WHITE -> 1.2f;
                    case BLACK -> 0.5f;
                    case PALE -> 1.0f;
                };
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.2, getZ(), 1, 0.3, 0.3, 0.3, 0.005);
        }
        if (tickCount % FEAR_PERIOD != 0) return;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(FEAR_RADIUS))) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            EmployeeData d = Employees.get(p);
            if (d.prudence >= FEAR_PRUDENCE || d.isPanicking()) continue;
            Employees.panic(p);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}