package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * T-03-46 백야 — 눈부신 날개를 단 존재.
 * 90초 동안 아무도 작업하지 않으면 카운터가 내려가고, 좋음은 모든 직원을 치유한다.
 * 탈출하면 부서 중앙 홀에 강림해 사도들을 깨우고, 커지는 PALE의 고리로 직원을 베어낸다.
 * WHITE 피해는 오히려 그를 치유하며, 「하나의 죄와 수백 개의 선」에게 고해하면 빛 속으로 사라진다.
 */
public class WhiteNightEntity extends BreachingAbnormality {
    /** 아무도 작업하지 않을 때 카운터가 내려가는 주기 (90초) */
    private static final int IDLE_LIMIT = 1800;
    /** 보통 결과에서 카운터가 회복될 확률 (%) */
    private static final int NORMAL_HEAL_CHANCE = 40;
    /** 좋음/보통의 치유 반경 */
    private static final double HEAL_RADIUS = 24.0;
    /** 나쁨의 정신 피해 반경 */
    private static final double PUNISH_RADIUS = 24.0;
    /** PALE 고리 주기 */
    private static final int RING_PERIOD = 1200;
    /** 고리가 퍼지는 데 걸리는 틱 */
    private static final int RING_TICKS = 60;
    /** 고리의 최대 반경 */
    private static final double RING_MAX = 32.0;
    /** 고리 판정 두께 */
    private static final double RING_WIDTH = 1.5;
    /** 고리 PALE 피해 (최대 HP 대비 %) */
    private static final float RING_PALE = 30f;
    /** 사도 최대 수 */
    private static final int MAX_APOSTLES = 6;

    /** 마지막 작업 이후 지난 틱 */
    private int idleTicks = 0;
    /** 다음 PALE 고리까지 남은 틱 */
    private int ringTicks = 0;
    /** 고리가 퍼지기 시작한 뒤 지난 틱 (-1 = 고리 없음) */
    private int ringAge = -1;
    /** 이번 고리에 이미 맞은 플레이어 */
    private final java.util.Set<java.util.UUID> ringHit = new java.util.HashSet<>();

    public WhiteNightEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1600)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.WHITENIGHT;
    }

    /** 백야는 움직이지도, 직접 때리지도 않는다 (목표 없음) */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected boolean leavesRoomOnBreach() {
        return false;
    }

    @Override
    protected String breachAdvice() {
        return "WHITE 피해는 통하지 않습니다. 사도부터 처리하거나 「하나의 죄」에게 고해하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkStart(ServerPlayer worker, WorkType type) {
        idleTicks = 0;
    }

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        idleTicks = 0;
        if (!(level() instanceof ServerLevel sl)) return;
        switch (result) {
            case GOOD -> {
                qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
                for (ServerPlayer p : sl.players()) {
                    if (!p.isAlive()) continue;
                    p.setHealth(p.getMaxHealth());
                    EmployeeData d = Employees.get(p);
                    d.sp = d.maxSp();
                    Employees.sync(p);
                }
                Facility.broadcast(sl.getServer(), Component.literal("[관제] T-03-46의 빛이 시설을 감쌉니다. 모두가 회복되었습니다.")
                        .withStyle(st -> st.withColor(0xFFFFAA)));
            }
            case NORMAL -> {
                if (getRandom().nextInt(100) < NORMAL_HEAL_CHANCE) {
                    qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
                }
                for (ServerPlayer p : sl.players()) {
                    if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                    if (p.distanceTo(this) > HEAL_RADIUS) continue;
                    p.heal(p.getMaxHealth() * 0.5f);
                    EmployeeData d = Employees.get(p);
                    d.sp = Math.min(d.maxSp(), d.sp + d.maxSp() / 2);
                    Employees.sync(p);
                }
            }
            default -> {
                lowerQliphoth(worker, 1);
                for (ServerPlayer p : sl.players()) {
                    if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                    if (p.distanceTo(this) > PUNISH_RADIUS) continue;
                    Employees.damageSp(p, 50 + getRandom().nextInt(11));
                }
            }
        }
    }

    /** 탈출 중 「하나의 죄와 수백 개의 선」에게 작업을 마치면, 그 직원의 고해로 백야가 사라진다 */
    @Override
    public void onOtherWorkFinished(ServerPlayer worker, AbnormalityEntity other, WorkManager.Result result) {
        if (!isBreached() || !(other instanceof OneSinEntity)) return;
        if (!(level() instanceof ServerLevel sl)) return;
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(worker.getDisplayName())
                .append(Component.literal(" 직원이 고해했습니다. 백야가 빛 속으로 사라집니다.")
                        .withStyle(st -> st.withColor(0xFFFFAA))));
        worker.hurtServer(sl, sl.damageSources().magic(), 1000f);
        returnToContainment(null);
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        List<BlockPos> centers = Facility.centers(sl.getServer());
        if (!centers.isEmpty()) {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (BlockPos c : centers) {
                double dist = distanceToSqr(c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5);
                if (dist < bd) {
                    bd = dist;
                    best = c;
                }
            }
            if (best != null) snapTo(best.getX() + 0.5, best.getY() + 3, best.getZ() + 0.5, getYRot(), 0);
        }
        setNoGravity(true);
        idleTicks = 0;
        ringTicks = 0;
        ringAge = -1;
        spawnApostles(sl, MAX_APOSTLES);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 백야가 강림했습니다. 사도들이 깨어납니다.")
                .withStyle(st -> st.withColor(0xFF3333).withBold(true)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setNoGravity(false);
        idleTicks = 0;
        ringTicks = 0;
        ringAge = -1;
        if (level() instanceof ServerLevel sl) {
            // 사도들은 무릎을 꿇고 사라진다
            for (ApostleEntity a : sl.getEntitiesOfClass(ApostleEntity.class, getBoundingBox().inflate(256.0))) {
                sl.sendParticles(ParticleTypes.END_ROD, a.getX(), a.getY() + 1.2, a.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
                a.discard();
            }
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.4f, 0.6f);
        }
    }

    /** 주위에 사도를 깨운다 (살아 있는 사도가 MAX_APOSTLES를 넘지 않게) */
    private void spawnApostles(ServerLevel sl, int count) {
        int alive = sl.getEntitiesOfClass(ApostleEntity.class, getBoundingBox().inflate(256.0)).size();
        for (int i = 0; i < count && alive < MAX_APOSTLES; i++) {
            ApostleEntity a = kr.lobcorp.ModEntities.APOSTLE.create(sl, EntitySpawnReason.EVENT);
            if (a == null) continue;
            double ang = Math.PI * 2 * i / Math.max(1, count);
            a.snapTo(getX() + Math.cos(ang) * 3.0, getY(), getZ() + Math.sin(ang) * 3.0, getYRot(), 0);
            sl.addFreshEntity(a);
            alive++;
        }
    }

    // ───────────── PALE 고리 ─────────────

    /** 고리 하나를 시작한다 — 사도를 되살리고, 퍼지기 시작한다 */
    private void startRing(ServerLevel sl) {
        ringAge = 0;
        ringHit.clear();
        spawnApostles(sl, MAX_APOSTLES);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.6f, 0.5f);
    }

    /** 고리를 한 틱 진행한다 */
    private void tickRing(ServerLevel sl) {
        double radius = RING_MAX * ringAge / RING_TICKS;
        int points = Math.max(8, (int) (radius * 4));
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            double x = getX() + Math.cos(a) * radius;
            double z = getZ() + Math.sin(a) * radius;
            sl.sendParticles(ParticleTypes.END_ROD, x, getY() + 0.4, z, 1, 0.05, 0.1, 0.05, 0.0);
            sl.sendParticles(ParticleTypes.FLAME, x, getY() + 0.2, z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ringHit.contains(p.getUUID())) continue;
            double dist = Math.sqrt(Math.pow(p.getX() - getX(), 2) + Math.pow(p.getZ() - getZ(), 2));
            if (Math.abs(dist - radius) > RING_WIDTH) continue;
            Employees.damagePale(p, RING_PALE);
            ringHit.add(p.getUUID());
        }
        if (++ringAge > RING_TICKS) ringAge = -1;
    }

    // ───────────── 내성 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached()) {
            boolean ego = source.getEntity() instanceof LivingEntity le
                    && le.getMainHandItem().getItem() instanceof EgoWeaponItem;
            if (!ego) return false;
            if (source.getEntity() instanceof LivingEntity attacker
                    && attacker.getMainHandItem().getItem() instanceof EgoWeaponItem w
                    && w.color == kr.lobcorp.DamageColor.WHITE) {
                // WHITE 피해는 오히려 그를 치유한다
                heal(amount * 2f);
                return false;
            }
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            // 90초 동안 아무도 작업하지 않으면 카운터가 내려간다 (관전·크리에이티브만 있으면 세지 않는다)
            boolean anyone = false;
            for (ServerPlayer p : sl.players()) {
                if (!p.isSpectator() && !p.isCreative()) { anyone = true; break; }
            }
            if (anyone && ++idleTicks >= IDLE_LIMIT) {
                idleTicks = 0;
                lowerQliphoth(null, 1);
            }
            return;
        }
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (ringAge >= 0) {
            tickRing(sl);
        } else if (++ringTicks >= RING_PERIOD) {
            ringTicks = 0;
            startRing(sl);
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_wn_idle", idleTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        idleTicks = in.getIntOr("lc_wn_idle", 0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
