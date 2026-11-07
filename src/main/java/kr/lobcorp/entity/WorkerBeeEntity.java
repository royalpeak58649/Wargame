package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
 * 일벌 — 여왕벌의 포자에 감염된 채 죽은 직원의 몸에서 기어 나오는 괴물.
 * 환상체가 아니며, E.G.O 무기로만 제대로 피해가 들어간다. 쓰러뜨린 직원도 일벌이 된다.
 */
public class WorkerBeeEntity extends PathfinderMob {
    public WorkerBeeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 0);
    }

    /** 직원의 몸에서 일벌이 태어난다 */
    public static void spawnFrom(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        WorkerBeeEntity bee = kr.lobcorp.ModEntities.WORKER_BEE.create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (bee == null) return;
        bee.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        sl.addFreshEntity(bee);
        sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEE_HURT, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원의 몸에서 일벌이 태어났습니다.").withStyle(s -> s.withColor(0xFFC040))));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        // 벽 너머의 직원도 찾아낸다 (시야 무시). Goal 판정은 엔티티 ID에 따라 격틱으로 돌기 때문에 tickCount 모듈러를 쓰면 안 된다.
        targetSelector.addGoal(1, new Goal() {
            private final TargetingConditions cond = TargetingConditions.forCombat().range(40).ignoreLineOfSight();

            @Override
            public boolean canUse() {
                if (getRandom().nextInt(4) != 0) return false;
                LivingEntity cur = getTarget();
                return cur == null || !cur.isAlive() || (cur instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
            }

            @Override
            public void start() {
                if (level() instanceof ServerLevel sl) {
                    Player found = sl.getNearestPlayer(cond, WorkerBeeEntity.this);
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
        int dmg = 6 + getRandom().nextInt(5);
        boolean hit = le.hurtServer(level, level.damageSources().mobAttack(this), dmg);
        if (hit) {
            level.playSound(null, blockPosition(), SoundEvents.BEE_STING, SoundSource.HOSTILE, 1f, 1f);
            level.sendParticles(ParticleTypes.CRIT, le.getX(), le.getY() + 1, le.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
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
                    case RED -> 1.2f;
                    case WHITE -> 1.5f;
                    case BLACK -> 0.8f;
                    case PALE -> 2.0f;
                };
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, getX(), getY() + 1, getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.BEE_DEATH, SoundSource.HOSTILE, 1.2f, 0.8f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, getX(), getY() + 1.2, getZ(), 1, 0.3, 0.3, 0.3, 0.005);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
