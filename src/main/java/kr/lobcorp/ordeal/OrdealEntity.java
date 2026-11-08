package kr.lobcorp.ordeal;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.entity.ClerkEntity;
import kr.lobcorp.work.ToolState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 시련 개체의 공통 뼈대. 시련끼리는 서로를 해치지 않고, E.G.O 무기의 피해 유형에 따라 저항이 갈린다.
 */
public abstract class OrdealEntity extends PathfinderMob {
    /** 표적 재탐색 쿨다운 (틱) */
    private int retargetCd = 0;

    public OrdealEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public abstract OrdealType ordealType();

    /** RED, WHITE, BLACK, PALE 순서의 피해 배율 */
    protected abstract float[] resist();

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        addPlayerTargetGoal();
    }

    @Override
    public void tick() {
        super.tick();
        if (retargetCd > 0) retargetCd--;
        // 너무 멀어진 표적은 버린다
        LivingEntity cur = getTarget();
        if (cur != null && distanceTo(cur) > 48.0) setTarget(null);
    }

    /**
     * 표적 후보를 평가해 가장 적합한 대상을 고른다.
     * 플레이어는 점수에 0.6을 곱해 우선하고, 사무직은 거리 그대로 평가한다.
     */
    private LivingEntity pickTarget(ServerLevel sl) {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            double d = distanceTo(p);
            if (d > 40.0) continue;
            double score = d * 0.6;
            if (score < bestScore) {
                bestScore = score;
                best = p;
            }
        }
        for (ClerkEntity c : sl.getEntitiesOfClass(ClerkEntity.class, getBoundingBox().inflate(24.0))) {
            if (!c.isAlive()) continue;
            double d = distanceTo(c);
            if (d > 24.0) continue;
            if (d < bestScore) {
                bestScore = d;
                best = c;
            }
        }
        return best;
    }

    /** 벽 너머의 직원도 찾아내는 표적 목표를 등록한다 (시야 무시). */
    protected void addPlayerTargetGoal() {
        // Goal 판정은 엔티티 ID에 따라 격틱으로 돌기 때문에 tickCount 모듈러를 쓰면 안 된다.
        targetSelector.addGoal(1, new Goal() {
            @Override
            public boolean canUse() {
                LivingEntity cur = getTarget();
                // 재탐색 쿨다운이 끝났으면 다시 고른다
                if (retargetCd <= 0) return true;
                if (getRandom().nextInt(4) != 0) return false;
                return cur == null || !cur.isAlive() || (cur instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
            }

            @Override
            public void start() {
                if (level() instanceof ServerLevel sl) {
                    setTarget(pickTarget(sl));
                    retargetCd = 40;
                }
            }

            @Override
            public boolean canContinueToUse() {
                return false;
            }
        });
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof OrdealEntity) return false;
        // 때린 플레이어에게 즉시 반격한다
        if (source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isCreative() && !attacker.isSpectator()) {
            setTarget(attacker);
            retargetCd = 100;
        }
        float mult = 0.5f;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                float[] r = resist();
                int idx = ego.color.ordinal();
                if (idx >= 0 && idx < r.length) mult = r[idx];
                // 등급 차이 배율 (E.G.O 무기 등급 vs 시련 등급)
                mult *= kr.lobcorp.Grades.mult(kr.lobcorp.Grades.ofItem(ego), kr.lobcorp.Grades.ofEntity(this));
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** 반경 안의 살아 있는 직원 (창작·관전·대피소 제외) */
    protected static List<ServerPlayer> playersNear(ServerLevel sl, Vec3 at, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (p.distanceToSqr(at.x, at.y, at.z) > r * r) continue;
            out.add(p);
        }
        return out;
    }
}
