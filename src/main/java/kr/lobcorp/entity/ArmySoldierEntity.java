package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Employees;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * D-01-106-1 — 검은 군단의 군인.
 * 부서 중앙 홀로 행진하다 도착하면 터진다. 공격받기 전까지는 직원을 해치지 않는다.
 */
public class ArmySoldierEntity extends PathfinderMob {
    /** 행진 재탐색 주기 */
    private static final int MARCH_PERIOD = 40;
    /** 도착 판정 거리 */
    private static final double ARRIVE_DIST = 2.5;
    /** 폭발 반경 */
    private static final double BLAST_RADIUS = 24.0;
    /** 폭발 SP 피해 */
    private static final int BLAST_SP = 30;
    /** 격노 사격 주기 */
    private static final int SHOOT_PERIOD = 60;
    /** 격노 사격 반경 */
    private static final double SHOOT_RADIUS = 6.0;

    /** 행진 목표 */
    private BlockPos goal = null;
    /** 공격받아 격노했는가 */
    private boolean provoked = false;

    public ArmySoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public void setGoal(BlockPos pos) {
        this.goal = pos;
    }

    public boolean isProvoked() {
        return provoked;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // 공격받았을 때만 직원을 쫓는다. Goal 판정은 엔티티 ID에 따라 격틱으로 돌기 때문에 tickCount 모듈러를 쓰면 안 된다.
        targetSelector.addGoal(1, new Goal() {
            private final TargetingConditions cond = TargetingConditions.forCombat().range(24).ignoreLineOfSight();

            @Override
            public boolean canUse() {
                if (!provoked || getRandom().nextInt(4) != 0) return false;
                LivingEntity cur = getTarget();
                return cur == null || !cur.isAlive() || (cur instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
            }

            @Override
            public void start() {
                if (level() instanceof ServerLevel sl) {
                    Player found = sl.getNearestPlayer(cond, ArmySoldierEntity.this);
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

    // ───────────── 행진 / 폭발 ─────────────

    /** 목표에 도착하면 터진다 */
    private void explode(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 20, 0.6, 0.6, 0.6, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.8f);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(BLAST_RADIUS))) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            Employees.damageSp(p, BLAST_SP + getRandom().nextInt(11));
        }
        for (BreachingAbnormality a : sl.getEntitiesOfClass(BreachingAbnormality.class,
                getBoundingBox().inflate(BLAST_RADIUS))) {
            if (a.isBreached()) continue;
            a.lowerQliphoth(null, 1);
        }
        discard();
    }

    /** 격노 상태의 사격 */
    private void shoot(ServerLevel sl) {
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(SHOOT_RADIUS))) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            int dmg = getRandom().nextInt(100) < 20 ? 25 + getRandom().nextInt(4) : 12 + getRandom().nextInt(4);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            Employees.damageSp(p, dmg);
            sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        }
        sl.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1f, 1.4f);
    }

    // ───────────── 내성 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer) provoked = true;
        float mult = 0.5f;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                mult = switch (ego.color) {
                    case RED -> 1.2f;
                    case WHITE -> 0.6f;
                    case BLACK -> 1.0f;
                    case PALE -> 0.8f;
                };
                mult *= kr.lobcorp.Grades.mult(kr.lobcorp.Grades.ofItem(ego), kr.lobcorp.Grades.ofEntity(this));
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (provoked) {
            if (tickCount % SHOOT_PERIOD == 0) shoot(sl);
            return;
        }
        if (goal == null) return;
        if (tickCount % MARCH_PERIOD == 0) {
            getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 1.0);
        }
        double dx = getX() - (goal.getX() + 0.5);
        double dz = getZ() - (goal.getZ() + 0.5);
        if (dx * dx + dz * dz <= ARRIVE_DIST * ARRIVE_DIST) explode(sl);
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_soldier_provoked", provoked);
        if (goal != null) {
            out.putBoolean("lc_soldier_has_goal", true);
            out.putInt("lc_soldier_gx", goal.getX());
            out.putInt("lc_soldier_gy", goal.getY());
            out.putInt("lc_soldier_gz", goal.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        provoked = in.getBooleanOr("lc_soldier_provoked", false);
        if (in.getBooleanOr("lc_soldier_has_goal", false)) {
            goal = new BlockPos(in.getIntOr("lc_soldier_gx", 0), in.getIntOr("lc_soldier_gy", 0),
                    in.getIntOr("lc_soldier_gz", 0));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
