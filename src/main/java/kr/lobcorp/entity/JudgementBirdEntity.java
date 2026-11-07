package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * O-02-62 심판 새 — 검은 사제복을 걸친 키 크고 마른 새. 금빛 저울을 들고 다닌다.
 * 탈출하면 직원을 향해 걸어가다가, 8블록 안에 보이는 직원이 있으면 저울을 2초간 들어 올린 뒤
 * 그 자리의 모든 직원에게 PALE 피해를 내린다. 방호복은 아무 소용이 없다.
 */
public class JudgementBirdEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Integer> JUDGE =
            SynchedEntityData.defineId(JudgementBirdEntity.class, EntityDataSerializers.INT);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 3.2f);

    private static final int WINDUP = 40;          // 2초
    private static final int COOLDOWN = 200;       // 10초
    private static final double JUDGE_RADIUS = 8.0;
    private static final double CHASE_DIST = 6.0;

    private int cd = 0;

    public JudgementBirdEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.JUDGEMENT_BIRD;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(JUDGE, 0);
    }

    public int getJudgeTicks() {
        return entityData.get(JUDGE);
    }

    private void setJudgeTicks(int v) {
        entityData.set(JUDGE, v);
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "심판 새의 저울이 빛나면 8블록 밖으로 벗어나십시오. 방호복으로는 막을 수 없습니다.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  저울이 아주 조금 기울었다가 제자리로 돌아온다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  접시 위에서 무언가가 조용히 굴러떨어졌다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 50) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  저울이 한쪽으로 기운 채 멈추지 않는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 70) lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        cd = 0;
        setJudgeTicks(0);
        sl.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-02-62가 저울을 들고 나왔습니다. 8블록 안에 서 있지 마십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(net.minecraft.network.chat.MutableComponent announcement) {
        super.returnToContainment(announcement);
        cd = 0;
        setJudgeTicks(0);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        int judge = getJudgeTicks();
        if (judge > 0) {
            // 저울을 들어 올리는 동안 멈춰 선다
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (tickCount % 2 == 0) {
                sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.6, getZ(), 4, 0.5, 0.4, 0.5, 0.05);
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 2, 0.4, 0.3, 0.4, 0.01);
            }
            if (judge == 1) {
                passJudgement(sl);
                cd = COOLDOWN;
            }
            setJudgeTicks(judge - 1);
            return;
        }

        if (cd > 0) cd--;

        ServerPlayer target = nearestVisiblePlayer(sl);
        if (target == null) {
            getNavigation().stop();
            return;
        }
        if (cd <= 0) {
            setJudgeTicks(WINDUP);
            sl.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5f, 0.5f);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(this) <= 24) {
                    p.sendSystemMessage(Component.literal("  심판 새가 저울을 들어 올렸다.")
                            .withStyle(st -> st.withColor(0xD8B040).withItalic(true)));
                }
            }
            return;
        }
        // 저울을 내린 상태에서는 느릿하게 다가간다
        if (distanceTo(target) > CHASE_DIST) {
            getNavigation().moveTo(target, 1.0);
        } else {
            getNavigation().stop();
        }
    }

    private ServerPlayer nearestVisiblePlayer(ServerLevel sl) {
        ServerPlayer best = null;
        double bd = JUDGE_RADIUS * JUDGE_RADIUS;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            double d = p.distanceToSqr(this);
            if (d > bd) continue;
            if (!hasLineOfSight(p)) continue;
            bd = d;
            best = p;
        }
        return best;
    }

    private void passJudgement(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.4f);
        sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.6, getZ(), 20, 0.6, 0.6, 0.6, 0.02);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            if (p.distanceTo(this) > JUDGE_RADIUS) continue;
            int dmg = 30 + p.getRandom().nextInt(11);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            if (!p.isAlive()) {
                sl.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.6, p.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
                Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                        .append(p.getDisplayName())
                        .append(Component.literal(" 직원이 심판을 받았습니다.").withStyle(st -> st.withColor(0xAAAAAA))));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_judge_cd", cd);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        cd = in.getIntOr("lc_judge_cd", 0);
    }
}
