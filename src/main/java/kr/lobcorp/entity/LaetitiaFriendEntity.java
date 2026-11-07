package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Facility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 라티샤의 친구 — 선물이 터진 자리에서 기어 나오는, 입이 찢어진 거미 같은 괴물.
 * 환상체가 아니며, E.G.O 무기로만 제대로 피해가 들어간다.
 */
public class LaetitiaFriendEntity extends PathfinderMob {
    public LaetitiaFriendEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 0);
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
                if (level() instanceof ServerLevel sl) setTarget(sl.getNearestPlayer(cond, LaetitiaFriendEntity.this));
            }

            @Override
            public boolean canContinueToUse() {
                return false;
            }
        });
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float mult = 0.5f;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                mult = switch (ego.color) {
                    case RED -> 0.8f;
                    case WHITE -> 0.8f;
                    case BLACK -> 1.2f;
                    case PALE -> 1.0f;
                };
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 라티샤의 친구를 제압했습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_DEATH, SoundSource.HOSTILE, 1.2f, 0.6f);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
