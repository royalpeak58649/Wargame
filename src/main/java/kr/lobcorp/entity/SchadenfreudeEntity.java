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
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * O-05-76 샤덴프로이데 — 자물쇠와 사슬, 톱날로 뒤덮인 상자형 몸.
 * 가슴 한가운데에 열쇠 구멍 모양의 거대한 눈이 박혀 있다. 보이는 것을 견디지 못한다.
 * 누군가 바라보는 동안에만 움직이고 공격한다. 아무도 보지 않으면 완전히 멈춰 선다.
 */
public class SchadenfreudeEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Boolean> WATCHED =
            SynchedEntityData.defineId(SchadenfreudeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.8f, 1.9f);

    /** 작업 중 시선을 느끼는 주기 (5초) */
    private static final int WATCH_INTERVAL = 100;
    /** 시선 판정 거리 */
    private static final double WATCH_RANGE = 32.0;
    /** 시선 판정 각도 (dot) */
    private static final double WATCH_DOT = 0.85;

    private static final int SPIN_COOLDOWN = 200;
    private static final int SPIN_TICKS = 60;
    private static final double SPIN_RANGE = 3.0;

    private int watchTicks = 0;
    private int spinCd = 0;
    private int spinT = 0;
    private final Set<UUID> working = new HashSet<>();

    public SchadenfreudeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.SCHADENFREUDE;
    }

    @Override
    protected double chaseSpeed() {
        return 1.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WATCHED, false);
    }

    public boolean isWatched() {
        return entityData.get(WATCHED);
    }

    private void setWatched(boolean b) {
        entityData.set(WATCHED, b);
    }

    /** 남은 톱날 회전 틱 (클라이언트 연출용) */
    public int getSpinTicks() {
        return spinT;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "바라보는 동안에만 움직입니다. 눈을 돌려 멈춰 세우고, 톱날이 돌기 전에 거리를 두십시오.";
    }

    // ───────────── 시선 ─────────────

    /** p가 e를 바라보고 있는가 */
    public static boolean watchedBy(Entity e, ServerPlayer p) {
        if (p == null || !p.isAlive() || p.isSpectator()) return false;
        if (p.distanceToSqr(e) > WATCH_RANGE * WATCH_RANGE) return false;
        if (!p.hasLineOfSight(e)) return false;
        Vec3 look = p.getLookAngle();
        Vec3 to = e.getEyePosition().subtract(p.getEyePosition());
        if (to.lengthSqr() < 1.0E-6) return false;
        return look.dot(to.normalize()) > WATCH_DOT;
    }

    /** 이 환상체를 바라보는 직원이 한 명이라도 있는가 */
    public boolean watched() {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (ServerPlayer p : sl.players()) {
            if (watchedBy(this, p)) return true;
        }
        return false;
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkStart(ServerPlayer worker, kr.lobcorp.work.WorkType type) {
        working.add(worker.getUUID());
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        working.remove(p.getUUID());
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  자물쇠 하나가 저절로 풀렸다가 다시 채워진다. 열쇠 구멍이 아주 잠깐 어두워졌다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  사슬이 바닥을 긁으며 조금 물러난다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> p.sendSystemMessage(Component.literal("  톱날이 헛돌며 날카로운 소리를 낸다. 무언가가 당신을 노려보는 기분이 든다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    @Override
    public void onWorkAborted(ServerPlayer p, WorkSession s, String reason) {
        working.remove(p.getUUID());
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    public void breach() {
        super.breach();
        if (isBreached()) {
            spinCd = SPIN_COOLDOWN;
            spinT = 0;
            watchTicks = 0;
            working.clear();
        }
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        spinCd = 0;
        spinT = 0;
        watchTicks = 0;
        working.clear();
        setWatched(false);
    }

    // ───────────── 공격 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!isBreached() || !isWatched()) return false;
        if (target instanceof ServerPlayer p) {
            int d = 5 + getRandom().nextInt(3);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.1, p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            level.playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1f, 0.7f);
            p.hurtServer(level, level.damageSources().mobAttack(this), d);
            return true;
        }
        return super.doHurtTarget(level, target);
    }

    private void spin(ServerLevel sl) {
        spinT = SPIN_TICKS;
        sl.playSound(null, blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 1.6f, 0.7f);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 32 * 32) continue;
            p.sendSystemMessage(Component.literal("  샤덴프로이데의 팔에서 톱날이 돌기 시작한다.")
                    .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        }
    }

    private void sliceAround(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > SPIN_RANGE * SPIN_RANGE) continue;
            p.hurtServer(sl, sl.damageSources().mobAttack(this), 2f);
            sl.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, 0.4, 0.4, 0.05);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 2, 1.2, 0.3, 1.2, 0);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;

        if (!isBreached()) {
            // 격리 중: 작업 중인 직원이 바라보면 카운터가 내려간다
            if (tickCount % 5 == 0) setWatched(watched());
            if (working.isEmpty()) {
                watchTicks = 0;
                return;
            }
            if (!isWatched()) return;
            watchTicks++;
            if (watchTicks >= WATCH_INTERVAL) {
                watchTicks = 0;
                ServerPlayer worker = null;
                for (UUID id : working) {
                    ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
                    if (p != null && p.isAlive()) { worker = p; break; }
                }
                if (worker != null) {
                    worker.sendSystemMessage(Component.literal("  시선을 느낀 샤덴프로이데가 몸을 비튼다. (눈을 돌리세요)")
                            .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
                    lowerQliphoth(worker, 1);
                }
            }
            return;
        }

        // 탈출 중: 매 틱 시선을 갱신
        boolean seen = watched();
        setWatched(seen);

        if (!seen) {
            // 아무도 보지 않으면 완전히 멈춘다
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            setTarget(null);
            spinT = 0;
            return;
        }

        if (spinCd > 0) spinCd--;
        if (spinT > 0) {
            spinT--;
            if (spinT % 2 == 0) sliceAround(sl);
            if (spinT % 10 == 0) {
                sl.playSound(null, blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 1.2f, 1.1f);
            }
            return;
        }
        if (spinCd <= 0) {
            ServerPlayer near = null;
            double best = SPIN_RANGE * SPIN_RANGE;
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                double d = p.distanceToSqr(this);
                if (d < best) { best = d; near = p; }
            }
            if (near != null) {
                spinCd = SPIN_COOLDOWN;
                spin(sl);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 맞는 순간에도 시선이 필요하다 — 보지 않으면 때릴 수 없다
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_watch_ticks", watchTicks);
        out.putInt("lc_spin_cd", spinCd);
        out.putInt("lc_spin_t", spinT);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        watchTicks = in.getIntOr("lc_watch_ticks", 0);
        spinCd = in.getIntOr("lc_spin_cd", 0);
        spinT = in.getIntOr("lc_spin_t", 0);
    }
}
