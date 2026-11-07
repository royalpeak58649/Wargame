package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
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

/**
 * O-03-60 우주의 파편 — 밤하늘의 색을 한 덩어리로 뭉친 검은 구체.
 * 표면에는 노랗고 붉고 보랏빛인 하트 모양 반점이 박혀 있고, 네 개의 긴 촉수가 늘어져 있다.
 * 탈출하면 느릿하게 배회하다가 촉수로 직원을 찌르고, 이따금 멈춰 서서 형용할 수 없는 노래를 부른다.
 */
public class FragmentEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Integer> SING =
            SynchedEntityData.defineId(FragmentEntity.class, EntityDataSerializers.INT);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 1.6f);

    /** 노래 한 소절 길이 */
    private static final int SING_TICKS = 60;
    /** 노래를 부르는 소리가 들리는 거리 */
    private static final double SING_HEAR = 8.0;
    /** 노래를 시작할 수 있는 거리 */
    private static final double SING_START = 12.0;

    public FragmentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.FRAGMENT;
    }

    @Override
    protected double chaseSpeed() {
        return 1.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SING, 0);
    }

    /** 남은 노래 틱 (클라이언트 연출용) */
    public int getSingTicks() {
        return entityData.get(SING);
    }

    private void setSingTicks(int t) {
        entityData.set(SING, t);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "느릿하게 다가오니 거리를 두고 제압하십시오. 노래가 들리면 즉시 벗어나십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  검은 구체 표면의 하트 하나가 아주 잠깐 밝아졌다가 다시 잦아든다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  촉수 하나가 바닥을 훑고 지나간다. 무언가 작은 소리가 섞여 있다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 50) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  파편이 낮게 웅웅거린다. 귀를 막아도 소리는 머리 안에서 울린다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 70) lowerQliphoth(p, 1);
            }
        }
    }

    @Override
    public void onWorkAborted(ServerPlayer p, WorkSession s, String reason) {
        if ("패닉".equals(reason)) lowerQliphoth(p, 1);
    }

    // ───────────── 탈출: 촉수로 찌르기 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (getSingTicks() > 0) return false; // 노래하는 동안에는 촉수를 뻗지 않는다
        if (target instanceof ServerPlayer p) {
            int d = 2 + getRandom().nextInt(3);
            level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.1, p.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            level.playSound(null, blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1f, 0.5f);
            p.hurtServer(level, level.damageSources().mobAttack(this), d);
            Employees.damageSp(p, d);
            return true;
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setSingTicks(0);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            if (getSingTicks() != 0) setSingTicks(0);
            return;
        }

        int sing = getSingTicks();
        if (sing > 0) {
            sing--;
            setSingTicks(sing);
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);

            sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.6, getZ(), 3, 0.6, 0.4, 0.6, 0.02);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 4, 0.7, 0.6, 0.7, 0.01);
            sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.0, getZ(), 6, 0.8, 0.5, 0.8, 0.01);

            if (sing == 55 || sing == 35 || sing == 15) {
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE,
                        2f, 0.5f + getRandom().nextFloat());
                sl.playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE,
                        1.5f, 0.5f);
                for (ServerPlayer p : sl.players()) {
                    if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                    if (p.distanceToSqr(this) > SING_HEAR * SING_HEAR) continue;
                    Employees.damageSp(p, 2 + getRandom().nextInt(3));
                }
            }
            return;
        }

        // 노래 시작 판정
        if (getRandom().nextInt(240) == 0) {
            ServerPlayer near = null;
            double best = SING_START * SING_START;
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                double d = p.distanceToSqr(this);
                if (d < best) { best = d; near = p; }
            }
            if (near != null) {
                setSingTicks(SING_TICKS);
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                setTarget(null);
                for (ServerPlayer p : sl.players()) {
                    if (p.distanceToSqr(this) > 32 * 32) continue;
                    p.sendSystemMessage(Component.literal("  어디선가 형용할 수 없는 노래가 보인다…")
                            .withStyle(st -> st.withColor(0xC9A8FF).withItalic(true)));
                }
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.6f);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_sing", getSingTicks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setSingTicks(in.getIntOr("lc_sing", 0));
    }
}
