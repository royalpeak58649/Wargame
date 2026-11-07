package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * T-02-99 공허한 꿈 — 털로 뒤덮인 작은 양이 늘 잠들어 있다.
 * 탈출하면 낮게 떠다니며 빛무리를 흩뿌려 직원을 재운다. 직접 공격하지는 않는다.
 * 때리면 공허한 꿈-1(수탉)로 변이해 울음으로 부서 전체의 정신력을 깎는다.
 */
public class VoidDreamEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Boolean> ROOSTER =
            SynchedEntityData.defineId(VoidDreamEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 0.7f);

    private static final int MAX_BREACH_TICKS = 1800; // 90초
    private static final int ORB_INTERVAL = 80;
    private static final int ORB_LIFE = 60;
    private static final int CROW_INTERVAL = 120;
    private static final int SLEEP_TICKS = 300;

    /** 빛무리 하나 */
    private static final class Orb {
        Vec3 pos;
        Vec3 vel;
        int life;

        Orb(Vec3 pos, Vec3 vel, int life) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
        }
    }

    private final List<Orb> orbs = new ArrayList<>();
    private Vec3 drift = null;
    private int driftCooldown = 0;
    private net.minecraft.world.level.pathfinder.Path route;
    private int stuckTicks = 0;
    private net.minecraft.world.entity.ai.navigation.FlyingPathNavigation flyNav;

    public VoidDreamEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.VOID_DREAM;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    public double homeYOffset() {
        return 0.5;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROOSTER, false);
    }

    public boolean isRooster() {
        return entityData.get(ROOSTER);
    }

    private void setRooster(boolean b) {
        entityData.set(ROOSTER, b);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "빛무리에 닿지 마십시오. 잠들면 아무것도 할 수 없습니다.";
    }

    // ───────────── 작업 결과 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (result == WorkManager.Result.BAD) {
            lowerQliphoth(p, 1);
        }
        String text = switch (result) {
            case GOOD -> "  양은 잠결에 입을 조금 벌렸다가 다시 다문다. 털 사이로 별가루가 흘러내린다.";
            case NORMAL -> "  양의 얕은 숨소리가 규칙적으로 이어진다.";
            case BAD -> "  양의 꿈이 조금씩 새어 나와 방 안의 공기가 무거워진다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
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
        setRooster(false);
        orbs.clear();
        drift = null;
        driftCooldown = 0;
        route = null;
        stuckTicks = 0;
        setNoGravity(true);
        getNavigation().stop();
    }

    // ───────────── 피격 → 변이 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached() && !isRooster() && source.getEntity() instanceof Player) {
            setRooster(true);
            orbs.clear();
            level.broadcastEntityEvent(this, (byte) 4);
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2f, 1.4f);
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 24, 0.4, 0.4, 0.4, 0.02);
            Facility.broadcast(level.getServer(), Component.literal("[관제] T-02-99가 깨어났습니다. 공허한 꿈-1로 변이.")
                    .withStyle(st -> st.withColor(0xFF5555).withBold(true)));
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            setNoGravity(true);
            return;
        }
        setNoGravity(true);

        if (breachTicks > MAX_BREACH_TICKS) {
            returnToContainment(Component.literal("[관제] T-02-99가 스스로 격리실로 돌아와 다시 잠들었습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
            return;
        }

        if (isRooster()) {
            if (breachTicks % CROW_INTERVAL == 0) crow(sl);
        } else {
            if (breachTicks % ORB_INTERVAL == 0) releaseOrbs(sl);
            tickOrbs(sl);
        }

        drift(sl);
    }

    /** 낮게 떠다닌다. 60틱마다 목적지를 새로 고른다. */
    private void drift(ServerLevel sl) {
        if (driftCooldown > 0) driftCooldown--;
        if (driftCooldown == 0 || drift == null) {
            driftCooldown = 60;
            Player near = sl.getNearestPlayer(this, 24);
            Vec3 base = near != null ? near.position() : (home != null
                    ? new Vec3(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) : position());
            double ang = getRandom().nextDouble() * Math.PI * 2;
            double r = 2 + getRandom().nextDouble() * 6;
            drift = new Vec3(base.x + Math.cos(ang) * r, base.y + 0.6 + getRandom().nextDouble() * 1.2,
                    base.z + Math.sin(ang) * r);
            route = null;
        }

        Vec3 goal = drift;
        boolean direct = sl.clip(new net.minecraft.world.level.ClipContext(position().add(0, 0.25, 0), goal,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
        if (horizontalCollision) stuckTicks = 40;
        if (stuckTicks > 0) stuckTicks--;
        if (!direct || stuckTicks > 0) {
            if (flyNav == null) {
                flyNav = new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, sl);
                flyNav.setCanOpenDoors(false);
                flyNav.getNodeEvaluator().setCanPassDoors(true);
            }
            if (route == null || route.isDone() || tickCount % 20 == 0) {
                route = flyNav.createPath(BlockPos.containing(goal), 1);
            }
            if (route != null && !route.isDone()) {
                BlockPos n = route.getNextNodePos();
                Vec3 node = new Vec3(n.getX() + 0.5, n.getY() + 0.3, n.getZ() + 0.5);
                if (node.distanceToSqr(position()) < 0.5) route.advance();
                goal = node;
            }
        } else {
            route = null;
        }

        Vec3 diff = goal.subtract(position());
        double dist = diff.length();
        double speed = 0.06;
        Vec3 vel = dist > 0.05 ? diff.scale(Math.min(speed, dist) / dist) : Vec3.ZERO;
        setDeltaMovement(vel);
        if (vel.horizontalDistanceSqr() > 0.0004) {
            float yaw = (float) (Math.atan2(vel.z, vel.x) * (180.0 / Math.PI)) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
    }

    // ───────────── 빛무리 ─────────────

    private void releaseOrbs(ServerLevel sl) {
        for (int i = 0; i < 4; i++) {
            double ang = getRandom().nextDouble() * Math.PI * 2;
            Vec3 vel = new Vec3(Math.cos(ang) * 0.35, (getRandom().nextDouble() - 0.5) * 0.08, Math.sin(ang) * 0.35);
            orbs.add(new Orb(position().add(0, 0.4, 0), vel, ORB_LIFE));
        }
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8f, 1.8f);
    }

    private void tickOrbs(ServerLevel sl) {
        List<Orb> dead = new ArrayList<>();
        for (Orb o : orbs) {
            Vec3 next = o.pos.add(o.vel);
            if (sl.getBlockState(BlockPos.containing(next)).isSolid()) {
                dead.add(o);
                continue;
            }
            o.pos = next;
            if (--o.life <= 0) {
                dead.add(o);
                continue;
            }
            sl.sendParticles(ParticleTypes.END_ROD, o.pos.x, o.pos.y, o.pos.z, 2, 0.02, 0.02, 0.02, 0);
            sl.sendParticles(ParticleTypes.GLOW, o.pos.x, o.pos.y, o.pos.z, 1, 0.02, 0.02, 0.02, 0);
            AABB box = new AABB(o.pos, o.pos).inflate(0.3);
            for (ServerPlayer p : sl.players()) {
                if (p.isSpectator() || p.isCreative()) continue;
                if (box.intersects(p.getBoundingBox())) {
                    Employees.sleep(p, SLEEP_TICKS);
                    dead.add(o);
                    break;
                }
            }
        }
        orbs.removeAll(dead);
    }

    // ───────────── 수탉의 울음 ─────────────

    private void crow(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 1.1f);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.6, getZ(), 1, 0, 0, 0, 0);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] T-02-99의 울음이 부서를 뒤덮습니다.")
                .withStyle(st -> st.withColor(0xC9C3E6)));
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) > 32) continue;
            if (Employees.get(p).sleepTicks > 0) {
                Employees.wake(p, true);
            } else {
                Employees.damageSp(p, 3 + getRandom().nextInt(3));
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            // 변이 연출 (클라이언트)
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_rooster", isRooster());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setRooster(in.getBooleanOr("lc_rooster", false));
    }
}
