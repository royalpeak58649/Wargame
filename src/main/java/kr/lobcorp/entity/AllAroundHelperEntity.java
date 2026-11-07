package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Facility;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * T-05-41 만능 도우미 — 웃는 화면 얼굴을 단 하얀 가사 로봇.
 * 청소 도구를 든 팔 사이에 몇 개의 칼날이 숨어 있다.
 * 탈출하면 제자리에서 회전해 칼날을 펼친 뒤, 직선으로 돌진해 지나가는 길의 모두를 베고 벽에 부딪히면 멈춘다.
 * 그 뒤에는 동력이 떨어져 한동안 멈춰 있는다.
 */
public class AllAroundHelperEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(AllAroundHelperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_T =
            SynchedEntityData.defineId(AllAroundHelperEntity.class, EntityDataSerializers.INT);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.75f, 1.2f);

    public static final int PHASE_REST = 0;
    public static final int PHASE_SPIN = 1;
    public static final int PHASE_CHARGE = 2;

    private static final int REST_AFTER_BREACH = 40;
    private static final int REST_AFTER_CHARGE = 200;
    private static final int SPIN_TICKS = 60;
    private static final int CHARGE_TICKS = 40;
    private static final double CHARGE_SPEED = 0.9;
    private static final double DETECT_RANGE = 24.0;

    private UUID targetId = null;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<UUID> hitThisCharge = new HashSet<>();

    public AllAroundHelperEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 70)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.HELPER;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_REST);
        builder.define(PHASE_T, 0);
    }

    public int getPhase() {
        return entityData.get(PHASE);
    }

    private void setPhase(int p) {
        entityData.set(PHASE, p);
    }

    public int getPhaseT() {
        return entityData.get(PHASE_T);
    }

    private void setPhaseT(int t) {
        entityData.set(PHASE_T, t);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "회전이 멈추면 곧바로 돌진합니다. 벽 뒤로 피하고, 동력이 떨어진 틈에 때리십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  로봇이 화면에 커다란 웃는 얼굴을 띄우고 바닥을 반짝반짝 닦아 낸다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  로봇의 팔 하나가 덜컥거린다. 화면의 미소가 아주 잠깐 일그러졌다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 50) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  로봇이 청소를 멈추고 당신을 빤히 바라본다. 화면 속 미소가 조금 넓어졌다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 70) lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    public void breach() {
        super.breach();
        if (isBreached()) {
            setPhase(PHASE_REST);
            setPhaseT(REST_AFTER_BREACH);
            chargeDir = Vec3.ZERO;
            hitThisCharge.clear();
            getNavigation().stop();
        }
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setPhase(PHASE_REST);
        setPhaseT(0);
        chargeDir = Vec3.ZERO;
        hitThisCharge.clear();
        targetId = null;
        getNavigation().stop();
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            if (getPhase() != PHASE_REST) setPhase(PHASE_REST);
            return;
        }

        int phase = getPhase();
        int t = getPhaseT();

        switch (phase) {
            case PHASE_SPIN -> {
                if (t > 0) {
                    setPhaseT(t - 1);
                    float yaw = getYRot() + 40f;
                    setYRot(yaw);
                    setYBodyRot(yaw);
                    setYHeadRot(yaw);
                    sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.7, getZ(), 6, 0.9, 0.4, 0.9, 0.02);
                    if (t - 1 == 0) beginCharge(sl);
                }
            }
            case PHASE_CHARGE -> {
                if (t > 0) setPhaseT(t - 1);
                Vec3 step = chargeDir.scale(CHARGE_SPEED);
                move(MoverType.SELF, step);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.6, getZ(), 3, 0.4, 0.3, 0.4, 0);
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.6, getZ(), 4, 0.5, 0.3, 0.5, 0.05);
                sliceAround(sl);
                if (horizontalCollision || getPhaseT() <= 0) endCharge(sl);
            }
            default -> {
                if (t > 0) {
                    setPhaseT(t - 1);
                    if (t % 10 == 0) {
                        sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.1, getZ(), 3, 0.3, 0.2, 0.3, 0.01);
                    }
                } else {
                    ServerPlayer target = findTarget(sl);
                    if (target != null) {
                        targetId = target.getUUID();
                        setPhase(PHASE_SPIN);
                        setPhaseT(SPIN_TICKS);
                        sl.playSound(null, blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 1.4f, 0.8f);
                    }
                }
            }
        }
    }

    private ServerPlayer findTarget(ServerLevel sl) {
        ServerPlayer best = null;
        double bd = DETECT_RANGE * DETECT_RANGE;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double d = p.distanceToSqr(this);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    private void beginCharge(ServerLevel sl) {
        Vec3 dir = Vec3.ZERO;
        if (targetId != null) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(targetId);
            if (p != null && p.isAlive()) {
                Vec3 diff = p.position().subtract(position());
                Vec3 horiz = new Vec3(diff.x, 0, diff.z);
                if (horiz.lengthSqr() > 1.0E-4) dir = horiz.normalize();
            }
        }
        if (dir == Vec3.ZERO) {
            float yaw = getYRot();
            dir = new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        }
        chargeDir = dir;
        float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90f;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        hitThisCharge.clear();
        setPhase(PHASE_CHARGE);
        setPhaseT(CHARGE_TICKS);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 1.6f);
    }

    private void endCharge(ServerLevel sl) {
        setPhase(PHASE_REST);
        setPhaseT(REST_AFTER_CHARGE);
        chargeDir = Vec3.ZERO;
        hitThisCharge.clear();
        sl.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1.2f, 0.8f);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 32 * 32) continue;
            p.sendSystemMessage(Component.literal("  만능 도우미가 멈춰 섰다. 동력이 떨어진 것 같다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    private void sliceAround(ServerLevel sl) {
        AABB box = getBoundingBox().inflate(0.6);
        for (Entity e : sl.getEntities(this, box)) {
            if (e == this || !(e instanceof LivingEntity le) || !le.isAlive()) continue;
            if (hitThisCharge.contains(le.getUUID())) continue;
            if (le instanceof Player pl && (pl.isCreative() || pl.isSpectator())) continue;
            if (!(le instanceof Player) && !(le instanceof AbnormalityEntity)) continue;
            hitThisCharge.add(le.getUUID());
            if (le instanceof ServerPlayer sp) {
                sp.hurtServer(sl, sl.damageSources().mobAttack(this), 10f + getRandom().nextInt(5));
            } else {
                le.hurtServer(sl, sl.damageSources().mobAttack(this), 10f);
            }
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, le.getX(), le.getY() + 1, le.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_phase", getPhase());
        out.putInt("lc_phase_t", getPhaseT());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setPhase(in.getIntOr("lc_phase", PHASE_REST));
        setPhaseT(in.getIntOr("lc_phase_t", 0));
    }
}
