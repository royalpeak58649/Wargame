package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * F-05-32 따뜻한 마음의 나무꾼 — 주석으로 된 몸, 가슴에는 심장이 들어갈 자리가 비어 있다.
 * 카운터가 0이 되어도 스스로 탈출하지 않는다. 다음에 격리실에 들어온 직원을 베어 버린 뒤에야 나온다.
 */
public class WoodsmanEntity extends BreachingAbnormality {
    /** 카운터 0: 다음 방문자를 기다리는 상태 (동기화·저장) */
    private boolean armed = false;
    /** 클라이언트 전용: 도끼를 내려치는 애니메이션 */
    public int swingTicks = 0;

    private int comboCd = 200;
    private int comboT = 0;

    public WoodsmanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.WOODSMAN;
    }

    @Override
    protected double chaseSpeed() {
        return 1.0;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // 탈출 시에도 폭은 0.8 미만이어야 열린 철문 틈(13/16)을 지날 수 있다
        return isBreached() ? EntityDimensions.scalable(0.7f, 2.0f) : super.getDefaultDimensions(pose);
    }

    public boolean isArmed() {
        return armed;
    }

    // ───────────── 카운터: 0이 되어도 탈출하지 않는다 ─────────────

    @Override
    public void lowerQliphoth(ServerPlayer by, int amount) {
        qliphoth = Math.max(0, qliphoth - amount);
        if (by != null) {
            by.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
        if (qliphoth <= 0 && !isBreached()) {
            if (!armed) {
                armed = true;
                if (by != null) {
                    by.sendSystemMessage(Component.literal("  나무꾼이 도끼 자루를 고쳐 쥐었다.")
                            .withStyle(st -> st.withColor(0xFFAA00).withItalic(true)));
                }
                Facility.broadcast(level().getServer(), Component.literal("[관제] 경고: " + profile().code()
                        + " 클리포트 카운터 0. 다음 작업자가 격리실에 들어서는 순간이 위험합니다.")
                        .withStyle(st -> st.withColor(0xFFAA00).withBold(true)));
                level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_STEP, SoundSource.HOSTILE, 1f, 0.6f);
            }
        }
    }

    // ───────────── 작업 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        if (!armed) return true;
        ServerLevel sl = p.level();
        armed = false;
        sl.broadcastEntityEvent(this, (byte) 4);
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
        p.sendSystemMessage(Component.literal("  도끼날이 어디서 나타났는지도 모를 만큼 빨랐다.")
                .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 격리실에 들어서자마자 쓰러졌습니다.").withStyle(st -> st.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
        breach();
        return false;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(p);
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  나무꾼은 도끼를 내려놓고 오래 당신을 바라본다. 가슴의 빈자리에서 바람 소리가 난다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  주석 몸이 삐걱이며 조금 움직였다. 녹슨 팔꿈치에서 가루가 떨어진다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  도끼 자루를 쥔 손에 힘이 들어간다. 나무꾼이 당신 쪽으로 고개를 돌렸다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
        if (result != WorkManager.Result.BAD && d.temperance >= 45) {
            p.sendSystemMessage(Component.literal("  당신의 침착함이 오히려 나무꾼을 불편하게 만들었다. 그는 한 걸음 물러선다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            lowerQliphoth(p, 1);
        }
    }

    // ───────────── 탈출 중 전투 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (comboT > 0) return false;
        if (!(target instanceof ServerPlayer p)) return false;
        int dmg = 5 + level.getRandom().nextInt(6);
        level.broadcastEntityEvent(this, (byte) 4);
        level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.2f, 0.7f);
        p.hurtServer(level, level.damageSources().mobAttack(this), dmg);
        return true;
    }

    private void tickCombo(ServerLevel sl) {
        LivingEntity target = getTarget();
        if (comboT > 0) {
            comboT--;
            if (target != null && target.isAlive() && distanceTo(target) <= 3.5) {
                if (comboT == 1) {
                    int dmg = 15 + sl.getRandom().nextInt(6);
                    sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.5f);
                    sl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                    sl.broadcastEntityEvent(this, (byte) 4);
                    target.hurtServer(sl, sl.damageSources().mobAttack(this), dmg);
                } else if (comboT % 5 == 0) {
                    int dmg = 3 + sl.getRandom().nextInt(2);
                    sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.8f);
                    target.hurtServer(sl, sl.damageSources().mobAttack(this), dmg);
                }
            }
            if (comboT == 0) comboCd = 200;
            return;
        }
        if (comboCd > 0) comboCd--;
        if (comboCd == 0 && target != null && target.isAlive() && distanceTo(target) <= 3.0) {
            comboT = 30;
            getNavigation().stop();
            sl.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_STEP, SoundSource.HOSTILE, 1.2f, 0.5f);
        }
    }

    /** 탈출한 나무꾼 곁에서 직원이 죽으면, 그는 그 심장을 가슴에 밀어 넣는다 */
    public static void onPlayerDeath(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        WoodsmanEntity best = null;
        double bd = 6.0 * 6.0;
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof WoodsmanEntity w && w.isBreached() && w.isAlive()) {
                double dist = w.distanceToSqr(p);
                if (dist <= bd) {
                    bd = dist;
                    best = w;
                }
            }
        }
        if (best == null) return;
        best.heal(40f);
        sl.sendParticles(ParticleTypes.HEART, best.getX(), best.getY() + 1.6, best.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        sl.playSound(null, best.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.HOSTILE, 1f, 0.6f);
        for (ServerPlayer o : sl.players()) {
            if (o.distanceTo(best) <= 16) {
                o.sendSystemMessage(Component.literal("  나무꾼이 가슴의 빈자리에 무언가를 밀어 넣었다.")
                        .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) swingTicks = 12;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (swingTicks > 0) swingTicks--;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isBreached()) {
            tickCombo(sl);
            if (tickCount % 20 == 0) {
                sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.2, getZ(), 2, 0.4, 0.6, 0.4, 0.01);
            }
        }
    }

    @Override
    protected String breachAdvice() {
        return "나무꾼의 도끼는 느리지만 무겁습니다. 가까이 붙지 마십시오.";
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_armed", armed);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        armed = in.getBooleanOr("lc_armed", false);
    }
}
