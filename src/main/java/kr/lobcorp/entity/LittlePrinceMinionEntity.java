package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Employees;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 어린 왕자-1 — 포자에 잠식된 채 격리실로 끌려간 직원의 몸에서 자라난 괴물.
 * 환상체가 아니며, E.G.O 무기로만 제대로 피해가 들어간다. 쓰러지면 주변에 포자를 남긴다.
 */
public class LittlePrinceMinionEntity extends PathfinderMob {
    public static final int DEATH_PULSES = 5;
    public static final int DEATH_PULSE_TICKS = 40;
    public static final double DEATH_RADIUS = 6.0;

    /** 죽은 자리에 남은 포자 구름 (저장 안 함) */
    private static final List<SporeCloud> CLOUDS = new ArrayList<>();

    private record SporeCloud(ServerLevel level, Vec3 pos, int ticksLeft) {}

    public LittlePrinceMinionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 0);
    }

    /** 직원의 몸에서 어린 왕자-1이 태어난다 */
    public static void spawnFrom(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        LittlePrinceMinionEntity minion = kr.lobcorp.ModEntities.LITTLE_PRINCE_MINION.create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (minion == null) return;
        minion.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        sl.addFreshEntity(minion);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEE_HURT, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원의 몸에서 어린 왕자-1이 태어났습니다.").withStyle(s -> s.withColor(0xAA55FF))));
    }

    /** 매 틱: 남은 포자 구름을 터뜨린다 (ToolState.tick에서 호출) */
    public static void tickClouds() {
        if (CLOUDS.isEmpty()) return;
        for (SporeCloud c : new ArrayList<>(CLOUDS)) {
            int left = c.ticksLeft() - 1;
            if (left <= 0) {
                CLOUDS.remove(c);
                continue;
            }
            CLOUDS.set(CLOUDS.indexOf(c), new SporeCloud(c.level(), c.pos(), left));
            if (left % DEATH_PULSE_TICKS != 0) continue;
            ServerLevel sl = c.level();
            Vec3 at = c.pos();
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 1, at.z, 20, 2.0, 0.8, 2.0, 0.02);
            for (ServerPlayer p : sl.players()) {
                if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
                if (p.distanceToSqr(at.x, at.y, at.z) > DEATH_RADIUS * DEATH_RADIUS) continue;
                Employees.damageSp(p, 2);
                p.sendSystemMessage(Component.literal("  남은 포자가 폐 속에서 터진다.")
                        .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
            }
        }
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
                    Player found = sl.getNearestPlayer(cond, LittlePrinceMinionEntity.this);
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
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 6f);
        if (hit) {
            if (le instanceof ServerPlayer sp) Employees.damageSp(sp, 3);
            level.playSound(null, blockPosition(), SoundEvents.BEE_STING, SoundSource.HOSTILE, 1f, 0.8f);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, le.getX(), le.getY() + 1, le.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
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
                    case RED -> 1.3f;
                    case WHITE -> 0.5f;
                    case BLACK -> 1.1f;
                    case PALE -> 1.5f;
                };
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1, getZ(), 30, 0.5, 0.8, 0.5, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.BEE_DEATH, SoundSource.HOSTILE, 1.2f, 0.7f);
            CLOUDS.add(new SporeCloud(sl, position(), DEATH_PULSES * DEATH_PULSE_TICKS));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.2, getZ(), 1, 0.3, 0.3, 0.3, 0.005);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
