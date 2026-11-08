package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
 * T-03-46-1 — 백야의 사도.
 * 환상체가 아니며, 백야가 강림하면 그 주위에 깨어난다. E.G.O 무기로만 제대로 피해가 들어가고, WHITE 피해는 오히려 그를 회복시킨다.
 */
public class ApostleEntity extends PathfinderMob {
    /** PALE 피해로 바뀔 확률 (%) */
    private static final int PALE_CHANCE = 10;
    /** PALE 피해량 (최대 HP 대비 %) */
    private static final float PALE_PERCENT = 15f;

    public ApostleEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 12)
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
                    Player found = sl.getNearestPlayer(cond, ApostleEntity.this);
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
        // 가끔 사도의 빛이 직원의 영혼을 직접 꿰뚫는다
        if (le instanceof ServerPlayer sp && getRandom().nextInt(100) < PALE_CHANCE) {
            Employees.damagePale(sp, PALE_PERCENT);
            level.playSound(null, blockPosition(), SoundEvents.BEACON_HIT, SoundSource.HOSTILE, 1f, 1.4f);
            level.sendParticles(ParticleTypes.END_ROD, le.getX(), le.getY() + 1.2, le.getZ(), 12, 0.3, 0.5, 0.3, 0.03);
            return true;
        }
        float dmg = 12f + getRandom().nextInt(5);
        boolean hit = le.hurtServer(level, level.damageSources().magic(), dmg);
        if (hit) {
            level.playSound(null, blockPosition(), SoundEvents.BEACON_HIT, SoundSource.HOSTILE, 0.8f, 1.2f);
            level.sendParticles(ParticleTypes.END_ROD, le.getX(), le.getY() + 1, le.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float mult = 0.5f;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                switch (ego.color) {
                    case WHITE -> {
                        // WHITE 피해는 사도를 회복시킨다
                        heal(amount);
                        return false;
                    }
                    case RED -> mult = 0.6f;
                    case BLACK -> mult = 0.6f;
                    case PALE -> mult = 0.3f;
                }
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 1, 0.25, 0.3, 0.25, 0.005);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
