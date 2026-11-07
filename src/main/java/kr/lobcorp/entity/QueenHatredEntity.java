package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * O-01-04 증오의 여왕 — 분홍 드레스를 입은 마법소녀.
 * 희망을 잃으면 거대한 검은 뱀이 되어 시설을 옮겨 다니며 레이저를 쏜다.
 */
public class QueenHatredEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Integer> LASER =
            SynchedEntityData.defineId(QueenHatredEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HYSTERIC =
            SynchedEntityData.defineId(QueenHatredEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.2f, 2.0f);

    /** 순간이동 주기 */
    private static final int TELEPORT_PERIOD = 300;
    /** 순간이동 후 레이저를 쏘기까지 */
    private static final int LASER_DELAY = 40;
    /** 레이저 지속 */
    private static final int LASER_TICKS = 100;
    /** 레이저 사거리 */
    private static final double LASER_LENGTH = 24.0;
    /** 레이저 판정 반경 */
    private static final double LASER_RADIUS = 1.0;

    private int tpT = 0;
    private int laserDelay = -1;
    private Vec3 laserDir = null;

    public QueenHatredEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 220)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.QUEEN_HATRED;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LASER, 0);
        builder.define(HYSTERIC, false);
    }

    /** 남은 레이저 틱 (클라이언트 연출용) */
    public int getLaserTicks() {
        return entityData.get(LASER);
    }

    private void setLaserTicks(int t) {
        entityData.set(LASER, t);
    }

    /** 히스테리 상태인가 (카운터 1) */
    public boolean isHysteric() {
        return entityData.get(HYSTERIC);
    }

    private void setHysteric(boolean b) {
        entityData.set(HYSTERIC, b);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "그녀가 나타나는 곳마다 폭발이 일어납니다. 레이저가 빛나면 옆으로 비키십시오.";
    }

    // ───────────── 클리포트 카운터 ─────────────

    @Override
    public void lowerQliphoth(ServerPlayer by, int amount) {
        super.lowerQliphoth(by, amount);
        setHysteric(qliphoth == 1);
    }

    @Override
    public void resetQliphoth() {
        super.resetQliphoth();
        setHysteric(false);
    }

    // ───────────── 작업 ─────────────

    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        return isHysteric() ? -20 : 0;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (qliphoth == 1) {
            if (s.good >= 16) {
                qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
                setHysteric(false);
                p.sendSystemMessage(Component.literal("  여왕이 다시 미소 짓는다. 별 지팡이 끝에서 반짝임이 돌아왔다.")
                        .withStyle(st -> st.withColor(0xFFB0E0).withItalic(true)));
                Facility.broadcast(p.level().getServer(), Component.literal("[관제] O-01-04가 희망을 되찾았습니다. 클리포트 카운터 "
                        + qliphoth + "/" + profile().qliphothMax()).withStyle(st -> st.withColor(0x55FF55)));
            } else {
                lowerQliphoth(p, 1);
                p.sendSystemMessage(Component.literal("  여왕의 웃음이 일그러진다. 별빛이 하나둘 꺼져 간다.")
                        .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
            }
            return;
        }
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  여왕이 지팡이를 빙글 돌린다. 반짝이는 가루가 작업자를 감싼다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  여왕은 리본을 고쳐 매며 조용히 웃고 있다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  여왕의 눈빛이 흔들린다. 무언가가 드레스 밑에서 꿈틀거린다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 탈출: 뱀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        tpT = 0;
        laserDelay = -1;
        laserDir = null;
        setLaserTicks(0);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-01-04가 검은 뱀의 모습으로 뒤틀렸습니다. 순간이동과 레이저를 조심하십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setLaserTicks(0);
        laserDelay = -1;
        laserDir = null;
        tpT = 0;
        setHysteric(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            if (getLaserTicks() != 0) setLaserTicks(0);
            return;
        }

        int laser = getLaserTicks();
        if (laser > 0) {
            tickLaser(sl, laser);
            return;
        }

        if (laserDelay > 0) {
            laserDelay--;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (laserDelay == 0) startLaser(sl);
            return;
        }

        tpT++;
        if (tpT >= TELEPORT_PERIOD) {
            tpT = 0;
            teleportNearPlayer(sl);
        }
    }

    // ───────────── 순간이동 ─────────────

    private void teleportNearPlayer(ServerLevel sl) {
        ServerPlayer target = null;
        int n = 0;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            n++;
            if (getRandom().nextInt(n) == 0) target = p;
        }
        if (target == null) return;

        BlockPos spot = findSpotNear(sl, target.blockPosition());
        if (spot == null) spot = target.blockPosition().above(2);

        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 3, 0.4, 0.4, 0.4, 0);
        sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 30, 0.6, 0.8, 0.6, 0.05);
        teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        setYRot(getRandom().nextFloat() * 360f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 3, 0.4, 0.4, 0.4, 0);
        sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 30, 0.6, 0.8, 0.6, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.7f);

        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > 25) continue;
            int d = 20 + getRandom().nextInt(7);
            p.hurtServer(sl, sl.damageSources().mobAttack(this), d);
            Employees.damageSp(p, d / 2);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.4, 0.5, 0.4, 0.05);
        }
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-01-04가 ").withStyle(st -> st.withColor(0xAA0000))
                .append(target.getDisplayName())
                .append(Component.literal(" 직원 곁에 나타났습니다.").withStyle(st -> st.withColor(0xFF5555))));
        laserDelay = LASER_DELAY;
    }

    /** 중심에서 8블록 안의, 발 디딜 수 있는 공기 자리를 찾는다. 없으면 null. */
    private BlockPos findSpotNear(ServerLevel sl, BlockPos center) {
        for (int i = 0; i < 60; i++) {
            int dx = getRandom().nextInt(17) - 8;
            int dz = getRandom().nextInt(17) - 8;
            if (dx * dx + dz * dz > 64) continue;
            BlockPos found = scanDown(sl, center.getX() + dx, center.getZ() + dz, center.getY());
            if (found != null) return found;
        }
        return null;
    }

    private BlockPos scanDown(ServerLevel sl, int x, int z, int baseY) {
        for (int y = baseY + 2; y >= baseY - 3; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = sl.getBlockState(pos.below());
            if (!below.isSolid()) continue;
            if (!sl.getBlockState(pos).isAir()) continue;
            if (!sl.getBlockState(pos.above()).isAir()) continue;
            return pos;
        }
        return null;
    }

    // ───────────── 레이저 ─────────────

    private void startLaser(ServerLevel sl) {
        ServerPlayer near = null;
        double best = 32 * 32;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double d = p.distanceToSqr(this);
            if (d < best) { best = d; near = p; }
        }
        if (near == null) return;
        Vec3 diff = near.position().subtract(position());
        Vec3 horiz = new Vec3(diff.x, 0, diff.z);
        if (horiz.lengthSqr() < 1.0E-4) horiz = new Vec3(0, 0, 1);
        laserDir = horiz.normalize();
        setYRot((float) (Math.atan2(laserDir.z, laserDir.x) * 180.0 / Math.PI) - 90f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        setLaserTicks(LASER_TICKS);
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5f, 0.6f);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 40 * 40) continue;
            p.sendSystemMessage(Component.literal("  여왕의 입이 벌어지고, 검붉은 빛이 모인다…")
                    .withStyle(st -> st.withColor(0xFF9AD0).withItalic(true)));
        }
    }

    private void tickLaser(ServerLevel sl, int laser) {
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (laserDir == null) {
            setLaserTicks(0);
            return;
        }
        Vec3 origin = new Vec3(getX(), getY() + 1.4, getZ());
        if (laser % 2 == 0) {
            for (double d = 0; d <= LASER_LENGTH; d += 0.5) {
                Vec3 at = origin.add(laserDir.scale(d));
                sl.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        if (laser % 10 == 0) {
            int healed = 0;
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                Vec3 rel = p.position().add(0, 1, 0).subtract(origin);
                double along = rel.dot(laserDir);
                if (along < 0 || along > LASER_LENGTH) continue;
                Vec3 closest = origin.add(laserDir.scale(along));
                if (closest.distanceTo(p.position().add(0, 1, 0)) > LASER_RADIUS) continue;
                int d = 4 + getRandom().nextInt(2);
                p.hurtServer(sl, sl.damageSources().mobAttack(this), d);
                Employees.damageSp(p, d);
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 8, 0.3, 0.4, 0.3, 0.03);
                healed += d;
            }
            if (healed > 0) heal(healed);
        }
        if (laser % 20 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.5f);
        }
        setLaserTicks(laser - 1);
        if (laser - 1 <= 0) {
            laserDir = null;
            tpT = 0;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_laser", getLaserTicks());
        out.putBoolean("lc_hysteric", isHysteric());
        out.putInt("lc_tpt", tpT);
        out.putInt("lc_laser_delay", laserDelay);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setLaserTicks(in.getIntOr("lc_laser", 0));
        setHysteric(in.getBooleanOr("lc_hysteric", false));
        tpT = in.getIntOr("lc_tpt", 0);
        laserDelay = in.getIntOr("lc_laser_delay", -1);
    }
}
