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
 * 벌거벗은 둥지의 뱀 — 감염이 끝난 직원의 몸을 찢고 나온 창백한 뱀.
 * 환상체가 아니며, E.G.O 무기로만 제대로 피해가 들어간다. 직원을 쫓아가 물어뜯는다.
 */
public class NakedWormEntity extends PathfinderMob {
    public NakedWormEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 0);
    }

    /** 직원의 몸에서 뱀이 기어 나온다 */
    public static void spawnFrom(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        NakedWormEntity worm = kr.lobcorp.ModEntities.NAKED_WORM.create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (worm == null) return;
        worm.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        sl.addFreshEntity(worm);
        sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.SQUID_HURT, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원의 몸에서 벌거벗은 둥지의 뱀이 기어 나왔습니다.").withStyle(s -> s.withColor(0xAA55FF))));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6));
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
                    Player found = sl.getNearestPlayer(cond, NakedWormEntity.this);
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
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 4f);
        if (hit) {
            level.playSound(null, blockPosition(), SoundEvents.SQUID_HURT, SoundSource.HOSTILE, 1f, 0.8f);
            level.sendParticles(ParticleTypes.SQUID_INK, le.getX(), le.getY() + 1, le.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
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
                    case RED -> 0.6f;
                    case WHITE -> 0.8f;
                    case BLACK -> 1.2f;
                    case PALE -> 1.5f;
                };
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.4, getZ(), 1, 0.3, 0.3, 0.3, 0.005);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
