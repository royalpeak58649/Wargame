package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.DamageColor;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * T-01-75 웃는 시체 산 — 수많은 시체가 엉겨 붙어 만들어진 산. 모든 얼굴이 웃고 있다.
 * 다친 직원이 들어오거나, 격리실에서 직원이 죽거나, 작업 결과가 나쁘면 클리포트 카운터가 내려간다.
 * 탈출하면 시체를 찾아 기어 다니며 먹어 치우고, 시체 셋을 먹을 때마다 몸이 한 덩이씩 커진다.
 */
public class SmilingMountainEntity extends BreachingAbnormality {
    /** 몸집 단계 (1..3) */
    private static final EntityDataAccessor<Integer> PARTS =
            SynchedEntityData.defineId(SmilingMountainEntity.class, EntityDataSerializers.INT);

    private static final EntityDimensions SIZE_1 = EntityDimensions.scalable(1.6f, 1.4f);
    private static final EntityDimensions SIZE_2 = EntityDimensions.scalable(2.4f, 2.2f);
    private static final EntityDimensions SIZE_3 = EntityDimensions.scalable(3.2f, 3.0f);

    /** 단계별 최대 체력 */
    private static final float HP_1 = 120f;
    private static final float HP_2 = 240f;
    private static final float HP_3 = 480f;

    private static final double SPEED_BASE = 0.30;
    private static final double SPEED_HEAVY = 0.18;

    /** 시체를 먹을 때 회복하는 비율 */
    private static final float EAT_HEAL_RATIO = 0.3f;
    /** 기억하는 시체 수 상한 */
    private static final int MAX_CORPSES = 20;
    /** 시체를 찾아가는 최대 거리 */
    private static final double CORPSE_SEEK_RANGE = 6.0;
    /** 시체를 먹는 거리 */
    private static final double CORPSE_EAT_RANGE = 2.0;

    /** 포효 주기 / 사거리 */
    private static final int ROAR_PERIOD = 160;
    private static final double ROAR_RANGE = 12.0;
    /** 산성 침 주기 / 사거리 / 경고 시간 / 판정 반경 */
    private static final int SPIT_PERIOD = 200;
    private static final double SPIT_RANGE = 16.0;
    private static final int SPIT_WARN = 30;
    private static final double SPIT_HIT_RADIUS = 2.5;

    /** 먹어 치운 시체 수 */
    private int eaten = 0;
    /** 시설에서 죽은 직원 수 */
    private int facilityDeaths = 0;
    /** 탈출 중 기억한 시체 위치 */
    private final List<Vec3> corpses = new ArrayList<>();

    private int roarTicks = 0;
    private int spitTicks = 0;
    /** 산성 침 경고 중인 위치 (null이면 없음) */
    private Vec3 spitAt = null;
    private int spitWarnTicks = 0;

    public SmilingMountainEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, HP_1)
                .add(Attributes.MOVEMENT_SPEED, SPEED_BASE)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.SMILING_MOUNTAIN;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARTS, 1);
    }

    /** 몸집 단계 (1..3) */
    public int parts() {
        return entityData.get(PARTS);
    }

    private void setParts(int p) {
        entityData.set(PARTS, Math.max(1, Math.min(3, p)));
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PARTS.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return switch (parts()) {
            case 2 -> SIZE_2;
            case 3 -> SIZE_3;
            default -> SIZE_1;
        };
    }

    @Override
    protected String breachAdvice() {
        return "HP·SP가 가득 찬 직원만 보내십시오. 시체를 먹지 못하게 막으십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        EmployeeData d = Employees.get(worker);
        int avg = (d.fortitude + d.prudence + d.temperance + d.justice) / 4;
        return super.workChanceBonus(worker, type) + (avg < 65 ? -40 : 0);
    }

    @Override
    public void onWorkStart(ServerPlayer worker, WorkType type) {
        EmployeeData d = Employees.get(worker);
        if (worker.getHealth() < worker.getMaxHealth() || d.sp < d.maxSp()) {
            lowerQliphoth(worker, 1);
            worker.sendSystemMessage(Component.literal("  다친 냄새를 맡은 얼굴들이 일제히 고개를 돌린다.")
                    .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        if (result == WorkManager.Result.BAD) {
            lowerQliphoth(worker, 1);
        }
        String text = switch (result) {
            case GOOD -> "  산이 낮게 웃는다. 얼굴들이 만족스럽게 입을 다물었다.";
            case NORMAL -> "  시체 산이 느리게 부풀었다가 가라앉는다. 웃음소리가 잦아든다.";
            default -> "  얼굴들이 일제히 입을 벌린다. 웃음소리가 벽을 타고 번진다.";
        };
        worker.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (!isBreached()) {
            if (home != null && p.blockPosition().distSqr(home) <= 36) {
                lowerQliphoth(null, 1);
            }
            facilityDeaths++;
            if (facilityDeaths % 10 == 0) lowerQliphoth(null, 1);
            return;
        }
        addCorpse(p.position());
    }

    @Override
    public void onAnyClerkDeath(ClerkEntity c) {
        if (!isBreached()) {
            if (home != null && c.blockPosition().distSqr(home) <= 36) {
                lowerQliphoth(null, 1);
            }
            facilityDeaths++;
            if (facilityDeaths % 10 == 0) lowerQliphoth(null, 1);
            return;
        }
        addCorpse(c.position());
    }

    /** 시체 위치를 기억한다 (최대 20개, 오래된 것부터 버린다) */
    private void addCorpse(Vec3 at) {
        corpses.add(at);
        while (corpses.size() > MAX_CORPSES) corpses.remove(0);
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        setParts(1);
        eaten = 0;
        corpses.clear();
        roarTicks = 0;
        spitTicks = 0;
        spitAt = null;
        spitWarnTicks = 0;
        setMaxHealthValue(HP_1);
        setHealth(getMaxHealth());
        setSpeed(SPEED_BASE);
        refreshDimensions();
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setParts(1);
        eaten = 0;
        corpses.clear();
        roarTicks = 0;
        spitTicks = 0;
        spitAt = null;
        spitWarnTicks = 0;
        setMaxHealthValue(HP_1);
        setHealth(HP_1);
        setSpeed(SPEED_BASE);
        refreshDimensions();
    }

    private void setSpeed(double value) {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(value);
    }

    private void setMaxHealthValue(float value) {
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(value);
    }

    // ───────────── 공격 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (parts() <= 1) {
            // 1단계: 물어뜯기
            float dmg = 7 + getRandom().nextInt(5);
            boolean ok = target.hurtServer(level, level.damageSources().magic(), dmg);
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.2f, 1.0f);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.2, target.getZ(),
                    8, 0.3, 0.4, 0.3, 0.03);
            return ok;
        }
        // 2단계 이상: 내리찍기 — 주변 3블록의 모든 직원
        boolean any = false;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(3.0))) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            int dmg = 15 + getRandom().nextInt(3);
            p.hurtServer(level, level.damageSources().magic(), dmg);
            Employees.damageSp(p, dmg);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(),
                    10, 0.4, 0.5, 0.4, 0.05);
            any = true;
        }
        level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.4f, 0.6f);
        return any;
    }

    /** 포효: 12블록 안의 직원에게 BLACK 피해 */
    private void roar(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.7f);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.6, 0.8, 0.6, 0.02);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (p.distanceToSqr(this) > ROAR_RANGE * ROAR_RANGE) continue;
            int dmg = 20 + getRandom().nextInt(8);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            Employees.damageSp(p, dmg);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(),
                    12, 0.4, 0.5, 0.4, 0.05);
        }
    }

    /** 산성 침: 30틱 경고 후 그 자리에 BLACK 피해 */
    private void tickSpit(ServerLevel sl) {
        if (spitWarnTicks > 0) {
            spitWarnTicks--;
            if (spitAt != null) {
                sl.sendParticles(ParticleTypes.SMOKE, spitAt.x, spitAt.y + 0.2, spitAt.z, 3, 0.6, 0.2, 0.6, 0.01);
            }
            if (spitWarnTicks == 0) fireSpit(sl);
            return;
        }
        if (spitTicks > 0) {
            spitTicks--;
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceTo(target) > SPIT_RANGE) return;
        spitAt = target.position();
        spitWarnTicks = SPIT_WARN;
        spitTicks = SPIT_PERIOD;
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SNIFF, SoundSource.HOSTILE, 1.2f, 0.7f);
    }

    private void fireSpit(ServerLevel sl) {
        if (spitAt == null) return;
        Vec3 at = spitAt;
        spitAt = null;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z, 30, 0.8, 0.4, 0.8, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1.4f, 0.6f);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (p.position().distanceToSqr(at) > SPIT_HIT_RADIUS * SPIT_HIT_RADIUS) continue;
            int dmg = 60 + getRandom().nextInt(16);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            Employees.damageSp(p, dmg);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(),
                    20, 0.5, 0.6, 0.5, 0.05);
        }
    }

    // ───────────── 시체 먹기 / 성장 ─────────────

    /** 가장 가까운 시체를 찾는다 (없으면 null) */
    private Vec3 nearestCorpse() {
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (Vec3 c : corpses) {
            double d = position().distanceToSqr(c);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private void tickCorpses(ServerLevel sl) {
        if (corpses.isEmpty()) return;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && distanceTo(target) <= CORPSE_SEEK_RANGE) return;
        Vec3 corpse = nearestCorpse();
        if (corpse == null) return;
        if (position().distanceTo(corpse) <= CORPSE_EAT_RANGE) {
            corpses.remove(corpse);
            eatCorpse(sl, corpse);
            return;
        }
        getNavigation().moveTo(corpse.x, corpse.y, corpse.z, 1.0);
    }

    private void eatCorpse(ServerLevel sl, Vec3 at) {
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y + 0.5, at.z, 20, 0.5, 0.4, 0.5, 0.05);
        setHealth(Math.min(getMaxHealth(), getHealth() + getMaxHealth() * EAT_HEAL_RATIO));
        eaten++;
        if (eaten % 3 == 0) grow(sl);
    }

    /** 시체 셋을 먹을 때마다 한 덩이 커진다 */
    private void grow(ServerLevel sl) {
        if (parts() >= 3) return;
        int p = parts() + 1;
        setParts(p);
        float max = p == 2 ? HP_2 : HP_3;
        setMaxHealthValue(max);
        setHealth(max);
        setSpeed(p >= 2 ? SPEED_HEAVY : SPEED_BASE);
        refreshDimensions();
        Facility.broadcast(sl.getServer(), Component.literal("[관제] T-01-75가 몸을 한 덩이 더 키웠습니다.")
                .withStyle(st -> st.withColor(0xFF3333).withBold(true)));
    }

    /** 체력이 절반 미만이면 한 덩이 줄어든다 */
    private void tickShrink(ServerLevel sl) {
        if (parts() < 2) return;
        if (getHealth() >= getMaxHealth() / 2f) return;
        int p = parts() - 1;
        setParts(p);
        float max = p == 1 ? HP_1 : HP_2;
        setMaxHealthValue(max);
        setHealth(Math.min(getHealth(), max));
        setSpeed(p == 1 ? SPEED_BASE : SPEED_HEAVY);
        refreshDimensions();
        Facility.broadcast(sl.getServer(), Component.literal("[관제] T-01-75의 몸에서 시체가 흘러내립니다. 한 덩이 작아졌습니다.")
                .withStyle(st -> st.withColor(0xFFAA00)));
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;
        tickCorpses(sl);
        tickShrink(sl);
        LivingEntity target = getTarget();
        if (parts() >= 2 && target != null && target.isAlive() && distanceTo(target) <= ROAR_RANGE) {
            if (++roarTicks >= ROAR_PERIOD) {
                roarTicks = 0;
                roar(sl);
            }
        }
        if (parts() >= 3) tickSpit(sl);
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_sm_parts", parts());
        out.putInt("lc_sm_eaten", eaten);
        out.putInt("lc_sm_deaths", facilityDeaths);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setParts(in.getIntOr("lc_sm_parts", 1));
        eaten = in.getIntOr("lc_sm_eaten", 0);
        facilityDeaths = in.getIntOr("lc_sm_deaths", 0);
        setMaxHealthValue(parts() == 3 ? HP_3 : parts() == 2 ? HP_2 : HP_1);
        setSpeed(parts() >= 2 ? SPEED_HEAVY : SPEED_BASE);
        refreshDimensions();
    }
}
