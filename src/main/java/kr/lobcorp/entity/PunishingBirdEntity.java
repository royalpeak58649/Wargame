package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * O-02-56 처벌새 — 검은 횃대 위에 앉아 있는 작은 흰 새.
 * 다른 환상체에 작업이 시작되거나 직원이 패닉에 빠지면 클리포트 카운터가 내려간다.
 * 탈출하면 시설을 날아다니며 부리로 직원을 쪼고, 맞으면 격노해 거대한 아가리를 벌려 물어뜯는다.
 */
public class PunishingBirdEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Boolean> ANGRY =
            SynchedEntityData.defineId(PunishingBirdEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.5f, 0.5f);

    private static final int ANGRY_TICKS = 200;
    private static final int PECK_COOLDOWN = 20;
    private static final int BITE_COOLDOWN = 30;
    private static final int MAX_BREACH_TICKS = 1800; // 90초

    private int angryTicks = 0;
    private int attackCooldown = 0;
    private net.minecraft.world.level.pathfinder.Path route;
    private int stuckTicks = 0;
    private net.minecraft.world.entity.ai.navigation.FlyingPathNavigation flyNav;

    /** 디버그용 */
    public String routeDebug() {
        return "stuck=" + stuckTicks + " hcol=" + horizontalCollision + " route=" + (route == null ? "null"
                : (route.isDone() ? "done" : route.getNextNodeIndex() + "/" + route.getNodeCount() + "->" + route.getNextNodePos()));
    }

    /** 클라이언트 전용: 쪼기/물기 애니메이션 */
    public int peckAnimTicks = 0;

    public PunishingBirdEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.PUNISHING_BIRD;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGRY, false);
    }

    public boolean isAngry() {
        return entityData.get(ANGRY);
    }

    private void setAngry(boolean b) {
        entityData.set(ANGRY, b);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "처벌새를 함부로 때리지 마십시오. 격노하면 아가리가 열립니다.";
    }

    // ───────────── 작업 결과 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  새는 횃대 위에서 조용히 깃을 다듬는다. 붉은 가슴팍이 잠깐 반짝였다.";
            case NORMAL -> "  새가 고개를 갸웃하며 당신을 빤히 바라본다.";
            case BAD -> "  새의 부리가 횃대를 한 번 또각하고, 다시 움직이지 않는다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    // ───────────── 다른 곳의 사건 ─────────────

    /** 다른 환상체에 작업이 시작될 때마다 50% 확률로 카운터가 내려간다 (처벌새 자신이 작업 중이면 내려가지 않는다) */
    public static void onAnyWorkStarted(ServerLevel level, AbnormalityEntity target, ServerPlayer p) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof PunishingBirdEntity bird && bird != target && !bird.isBreached()
                    && !kr.lobcorp.work.WorkManager.isAbnoBusy(bird)) {
                if (bird.getRandom().nextInt(2) == 0) bird.lowerQliphoth(null, 1);
            }
        }
    }

    /** 누군가 패닉에 빠지면 카운터가 내려간다 */
    public static void onAnyPanic(ServerLevel level) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof PunishingBirdEntity bird && !bird.isBreached()) {
                bird.lowerQliphoth(null, 1);
            }
        }
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    public void breach() {
        super.breach();
        if (isBreached()) {
            setNoGravity(true);
            getNavigation().stop();
        }
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setAngry(false);
        angryTicks = 0;
        attackCooldown = 0;
        setNoGravity(false);
        getNavigation().stop();
    }

    // ───────────── 피격 → 격노 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached() && source.getEntity() instanceof Player pl) {
            setAngry(true);
            angryTicks = ANGRY_TICKS;
            setTarget(pl);
            level.broadcastEntityEvent(this, (byte) 4);
            level.playSound(null, blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 1.4f, 0.6f);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 0.4, getZ(), 6, 0.2, 0.2, 0.2, 0);
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 비행 / 공격 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            if (peckAnimTicks > 0) peckAnimTicks--;
            return;
        }
        if (!isBreached()) {
            setNoGravity(false);
            return;
        }
        setNoGravity(true);

        if (angryTicks > 0) {
            if (--angryTicks == 0) setAngry(false);
        }
        if (attackCooldown > 0) attackCooldown--;
        if (stuckTicks > 0) stuckTicks--;

        if (breachTicks > MAX_BREACH_TICKS) {
            returnToContainment(Component.literal("[관제] O-02-56이 스스로 격리실로 돌아왔습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
            return;
        }

        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || (target instanceof Player pl && (pl.isCreative() || pl.isSpectator())))) {
            setTarget(null);
            target = null;
        }

        Vec3 goal;
        boolean following = false;
        if (target != null) {
            // 가슴 높이를 노린다 (눈높이를 노리면 문틀 위 벽에 걸린다)
            goal = new Vec3(target.getX(), target.getY() + 0.6, target.getZ());
            // 벽에 막혀 있으면(격리실 안 등) 지상 경로를 받아 문을 통해 빠져나간다
            boolean direct = sl.clip(new net.minecraft.world.level.ClipContext(position().add(0, 0.25, 0), goal,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                    .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
            if (horizontalCollision) stuckTicks = 40;
            if (!direct || stuckTicks > 0) {
                // 지상 내비게이션은 땅에 서 있지 않으면 경로를 만들지 않는다 → 비행용 내비게이터로 경로만 계산
                if (flyNav == null) {
                    flyNav = new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, sl);
                    flyNav.setCanOpenDoors(false);
                    flyNav.getNodeEvaluator().setCanPassDoors(true);
                }
                if (route == null || route.isDone() || tickCount % 20 == 0) route = flyNav.createPath(target, 1);
                if (route != null && !route.isDone()) {
                    net.minecraft.core.BlockPos n = route.getNextNodePos();
                    // 문 높이(2칸) 안으로 낮게 지나가도록 바닥 위 0.3에 맞춘다
                    Vec3 node = new Vec3(n.getX() + 0.5, n.getY() + 0.3, n.getZ() + 0.5);
                    following = true;
                    if (node.distanceToSqr(position()) < 0.5) {
                        route.advance();
                    }
                    goal = node;
                }
            } else {
                route = null;
            }
        } else if (home != null) {
            double a = tickCount * 0.05;
            goal = new Vec3(home.getX() + 0.5 + Math.cos(a) * 2.5, home.getY() + 1.6 + Math.sin(a * 1.7) * 0.4,
                    home.getZ() + 0.5 + Math.sin(a) * 2.5);
        } else {
            goal = position().add(0, 1.5, 0);
        }

        Vec3 diff = goal.subtract(position());
        double dist = diff.length();
        double speed = isAngry() ? 0.45 : 0.25;
        Vec3 vel = dist > 0.05 ? diff.scale(Math.min(speed, dist) / dist) : Vec3.ZERO;
        if (horizontalCollision && !following) vel = vel.add(0, 0.08, 0);
        setDeltaMovement(vel);
        if (vel.horizontalDistanceSqr() > 0.0004) {
            float yaw = (float) (Math.atan2(vel.z, vel.x) * (180.0 / Math.PI)) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }

        if (target != null && attackCooldown <= 0 && distanceTo(target) <= 1.6) {
            if (isAngry()) {
                target.hurtServer(sl, sl.damageSources().mobAttack(this), 12f);
                attackCooldown = BITE_COOLDOWN;
                sl.playSound(null, blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.2f, 1.3f);
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(),
                        10, 0.3, 0.3, 0.3, 0.05);
            } else {
                target.hurtServer(sl, sl.damageSources().mobAttack(this), 2f);
                attackCooldown = PECK_COOLDOWN;
                sl.playSound(null, blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 1f, 1.6f);
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2, target.getZ(),
                        6, 0.2, 0.2, 0.2, 0.05);
            }
            sl.broadcastEntityEvent(this, (byte) 4);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) peckAnimTicks = 8;
        else super.handleEntityEvent(id);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBreached() ? SoundEvents.PARROT_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_angry", isAngry());
        out.putInt("lc_angry_ticks", angryTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setAngry(in.getBooleanOr("lc_angry", false));
        angryTicks = in.getIntOr("lc_angry_ticks", 0);
    }
}
