package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * T-02-71 꿈꾸는 조류 — 몸이 비쳐 보이는 긴 무지개빛 물고기.
 * 탈출하면 같은 층에 무지개빛 물웅덩이 세 개를 만들고, 그 사이를 오가며 돌진한다.
 * 물웅덩이 위의 직원은 느려지고 계속 WHITE 피해를 입는다. 돌진 중에는 피해가 통하지 않는다.
 */
public class DreamingCurrentEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.0f, 1.0f);

    /** 물웅덩이 개수 */
    private static final int POOL_COUNT = 3;
    private static final double POOL_RADIUS = 3.0;
    private static final int POOL_DAMAGE_CD = 20;
    /** 돌진 준비(정지) 시간 */
    private static final int WIND_TICKS = 60;
    /** 물웅덩이 사이를 쉬어 가는 시간 */
    private static final int IDLE_TICKS = 100;
    /** 돌진 최대 거리(블록) */
    private static final int CHARGE_MAX = 25;
    private static final double CHARGE_STEP = 1.2;
    private static final double CHARGE_HIT_RADIUS = 1.5;

    private static final int PHASE_WIND = 0;
    private static final int PHASE_CHARGE = 1;
    private static final int PHASE_IDLE = 2;

    private int phase = PHASE_WIND;
    private int timer = WIND_TICKS;
    private int poolIndex = 0;
    private int dir = 1;
    private Vec3 chargeDir = Vec3.ZERO;
    private double charged = 0;
    private int poolDamageCd = POOL_DAMAGE_CD;

    private final List<BlockPos> pools = new ArrayList<>();
    private final Set<UUID> chargeHits = new HashSet<>();

    public DreamingCurrentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.DREAMING_CURRENT;
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
        return "돌진을 준비할 때만 피해가 들어갑니다. 물웅덩이를 피해 준비 동작을 노리십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  물고기가 꿈결 같은 물결을 남기고 조용히 미끄러진다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  비늘 사이로 무지개빛이 번뜩이고, 발밑이 잠시 젖는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  물고기가 입을 크게 벌린 채 작업자를 노려본다. 물이 차오른다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
        }
        if (kr.lobcorp.work.EmployeeData.grade(Employees.get(p).temperance).equals("I")) lowerQliphoth(p, 1);
    }

    @Override
    public void onWorkAborted(ServerPlayer worker, WorkSession session, String reason) {
        if (Employees.get(worker).isPanicking()) lowerQliphoth(worker, 1);
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        resetState();
        sl.playSound(null, blockPosition(), SoundEvents.CHERRY_LEAVES_FALL, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] T-02-71 꿈꾸는 조류가 격리실을 빠져나왔습니다. 물웅덩이를 밟지 마십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
        pickPools(sl);
        startWind(sl);
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        pools.clear();
        resetState();
    }

    private void resetState() {
        phase = PHASE_WIND;
        timer = WIND_TICKS;
        poolIndex = 0;
        dir = 1;
        chargeDir = Vec3.ZERO;
        charged = 0;
        poolDamageCd = POOL_DAMAGE_CD;
        chargeHits.clear();
    }

    /** 같은 층의 15~40블록 떨어진 자리에 물웅덩이 세 개를 만든다 */
    private void pickPools(ServerLevel sl) {
        pools.clear();
        for (int i = 0; i < POOL_COUNT; i++) {
            BlockPos found = findSpot(sl, 15, 40);
            if (found != null) pools.add(found);
        }
        if (pools.isEmpty()) {
            BlockPos here = blockPosition();
            pools.add(here);
        }
    }

    /** min~max블록 떨어진, 발밑이 단단하고 위 두 칸이 비어 있는 자리 */
    private BlockPos findSpot(ServerLevel sl, int min, int max) {
        BlockPos base = blockPosition();
        int lo = min * min;
        int hi = max * max;
        for (int i = 0; i < 80; i++) {
            int dx = getRandom().nextInt(max * 2 + 1) - max;
            int dz = getRandom().nextInt(max * 2 + 1) - max;
            int d2 = dx * dx + dz * dz;
            if (d2 < lo || d2 > hi) continue;
            BlockPos found = scanDown(sl, base.getX() + dx, base.getZ() + dz, base.getY());
            if (found != null) return found;
        }
        return scanDown(sl, base.getX() + min, base.getZ(), base.getY());
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

    // ───────────── 상태 기계 ─────────────

    /** 다음 물웅덩이 옆으로 순간이동하고 돌진을 준비한다 */
    private void startWind(ServerLevel sl) {
        phase = PHASE_WIND;
        timer = WIND_TICKS;
        chargeHits.clear();
        if (pools.isEmpty()) pickPools(sl);
        if (pools.isEmpty()) return;
        poolIndex = Math.max(0, Math.min(pools.size() - 1, poolIndex));
        BlockPos target = pools.get(poolIndex);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.8, getZ(), 20, 0.4, 0.4, 0.4, 0.02);
        teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        setYRot(getRandom().nextFloat() * 360f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.8, getZ(), 20, 0.4, 0.4, 0.4, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.CHERRY_LEAVES_FALL, SoundSource.HOSTILE, 1.2f, 1.2f);
    }

    /** 가장 멀리 뚫려 있는 축 방향을 골라 돌진을 시작한다 */
    private void startCharge(ServerLevel sl) {
        phase = PHASE_CHARGE;
        charged = 0;
        chargeHits.clear();
        Vec3[] dirs = {new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 0, -1)};
        Vec3 best = dirs[0];
        int bestFree = -1;
        for (Vec3 d : dirs) {
            int free = freeBlocks(sl, d);
            if (free > bestFree) {
                bestFree = free;
                best = d;
            }
        }
        chargeDir = best;
        setYRot((float) (Math.atan2(chargeDir.z, chargeDir.x) * 180.0 / Math.PI) - 90f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        sl.playSound(null, blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    /** 해당 방향으로 몇 블록까지 뚫려 있는가 (최대 CHARGE_MAX) */
    private int freeBlocks(ServerLevel sl, Vec3 d) {
        int n = 0;
        for (int i = 1; i <= CHARGE_MAX; i++) {
            BlockPos pos = new BlockPos((int) Math.floor(getX() + d.x * i),
                    (int) Math.floor(getY()), (int) Math.floor(getZ() + d.z * i));
            if (sl.getBlockState(pos).isSolid()) break;
            if (sl.getBlockState(pos.above()).isSolid()) break;
            n++;
        }
        return n;
    }

    private void tickCharge(ServerLevel sl) {
        if (charged >= CHARGE_MAX) {
            endCharge(sl);
            return;
        }
        double step = Math.min(CHARGE_STEP, CHARGE_MAX - charged);
        double nx = getX() + chargeDir.x * step;
        double nz = getZ() + chargeDir.z * step;
        BlockPos feet = new BlockPos((int) Math.floor(nx), (int) Math.floor(getY()), (int) Math.floor(nz));
        if (sl.getBlockState(feet).isSolid() || sl.getBlockState(feet.above()).isSolid()) {
            endCharge(sl);
            return;
        }
        teleportTo(nx, getY(), nz);
        charged += step;
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 4, 0.3, 0.3, 0.3, 0.01);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (chargeHits.contains(p.getUUID())) continue;
            if (p.distanceToSqr(this) > CHARGE_HIT_RADIUS * CHARGE_HIT_RADIUS) continue;
            chargeHits.add(p.getUUID());
            p.hurtServer(sl, sl.damageSources().magic(), 30f);
            p.sendSystemMessage(Component.literal("  무지개빛 몸통이 직원을 꿰뚫고 지나간다.")
                    .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
        }
        if (charged >= CHARGE_MAX) endCharge(sl);
    }

    private void endCharge(ServerLevel sl) {
        poolIndex += dir;
        if (poolIndex < 0 || poolIndex >= pools.size()) {
            poolIndex = Math.max(0, Math.min(pools.size() - 1, poolIndex));
            dir = -dir;
            phase = PHASE_IDLE;
            timer = IDLE_TICKS;
            return;
        }
        startWind(sl);
    }

    // ───────────── 물웅덩이 ─────────────

    private void tickPools(ServerLevel sl) {
        for (BlockPos pool : pools) {
            sl.sendParticles(ParticleTypes.END_ROD, pool.getX() + 0.5, pool.getY() + 0.2, pool.getZ() + 0.5,
                    6, POOL_RADIUS * 0.5, 0.1, POOL_RADIUS * 0.5, 0.01);
        }
        if (poolDamageCd > 0) {
            poolDamageCd--;
            return;
        }
        poolDamageCd = POOL_DAMAGE_CD;
        for (BlockPos pool : pools) {
            double cx = pool.getX() + 0.5;
            double cy = pool.getY();
            double cz = pool.getZ() + 0.5;
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                if (p.distanceToSqr(cx, cy, cz) > POOL_RADIUS * POOL_RADIUS) continue;
                p.hurtServer(sl, sl.damageSources().magic(), 2f);
                EmployeeData d = Employees.get(p);
                d.sp = Math.min(d.maxSp(), d.sp + 2);
                Employees.sync(p);
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
            }
        }
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        if (pools.isEmpty()) pickPools(sl);
        tickPools(sl);

        switch (phase) {
            case PHASE_WIND -> {
                if (--timer <= 0) startCharge(sl);
            }
            case PHASE_CHARGE -> tickCharge(sl);
            case PHASE_IDLE -> {
                if (--timer <= 0) startWind(sl);
            }
            default -> {
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached() && phase == PHASE_CHARGE) return false;
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_current_phase", phase);
        out.putInt("lc_current_timer", timer);
        out.putInt("lc_current_pool", poolIndex);
        out.putInt("lc_current_dir", dir);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        phase = in.getIntOr("lc_current_phase", PHASE_WIND);
        timer = in.getIntOr("lc_current_timer", WIND_TICKS);
        poolIndex = in.getIntOr("lc_current_pool", 0);
        dir = in.getIntOr("lc_current_dir", 1);
        chargeDir = Vec3.ZERO;
        charged = 0;
        chargeHits.clear();
    }
}
