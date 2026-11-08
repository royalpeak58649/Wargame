package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
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
 * O-03-93 푸른 별 — 허공에 떠 있는 푸른 심장.
 * 자제가 낮은 직원은 격리실에 들어서는 순간 빨려 들어가고, 신중이 낮은 직원이 작업을 마치면 카운터가 내려간다.
 * 탈출하면 무작위 부서 중앙 홀에 떠올라 시설 전체에 WHITE 피해를 주기적으로 준다. 공황에 빠진 직원은 별에게 끌려간다.
 */
public class BlueStarEntity extends BreachingAbnormality {
    /** 자제 기준선 — 이보다 낮으면 작업 시작 시 사망 */
    private static final int TEMPERANCE_SAFE = 65;
    /** 신중 기준선 — 이보다 낮으면 작업 완료 시 카운터 감소 */
    private static final int PRUDENCE_SAFE = 85;
    /** 근접 판정: 자제 기준선 */
    private static final int NEAR_TEMPERANCE = 45;
    /** 근접 판정: 신중 기준선 */
    private static final int NEAR_PRUDENCE = 65;
    /** 근접 판정 반경 */
    private static final double NEAR_RADIUS = 16.0;
    /** 파동 기본 주기 */
    private static final int PULSE_PERIOD = 100;

    private int pulseTicks = 0;
    /** 다음 파동까지 필요한 틱 (100 + 0..60) */
    private int nextPulse = PULSE_PERIOD;

    public BlueStarEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 440)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.BLUE_STAR;
    }

    /** 별은 움직이지도, 직접 때리지도 않는다 (목표 없음) */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected boolean leavesRoomOnBreach() {
        return false;
    }

    @Override
    protected String breachAdvice() {
        return "신중과 자제가 높은 직원만 접근하십시오. 공황에 빠진 직원은 별에게 끌려갑니다.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkStart(ServerPlayer worker, WorkType type) {
        EmployeeData d = Employees.get(worker);
        if (d.temperance < TEMPERANCE_SAFE) {
            worker.sendSystemMessage(Component.literal("  푸른 빛이 당신을 끌어당긴다.")
                    .withStyle(st -> st.withColor(0x5555FF).withItalic(true)));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL, worker.getX(), worker.getY() + 1, worker.getZ(), 40, 0.4, 0.8, 0.4, 0.03);
                worker.hurtServer(sl, sl.damageSources().magic(), 1000f);
            }
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(worker);
        if (d.prudence < PRUDENCE_SAFE) {
            lowerQliphoth(worker, 1);
        }
        String text = switch (result) {
            case GOOD -> "  낮은 소리가 잠시 멎는다. 별이 당신을 기억한 것 같다.";
            case NORMAL -> "  푸른 심장이 느리게 뛴다. 소리는 계속 이어진다.";
            default -> "  소리가 거칠어졌다. 별이 당신을 바라본다.";
        };
        worker.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        List<BlockPos> centers = Facility.centers(sl.getServer());
        if (!centers.isEmpty()) {
            BlockPos c = centers.get(getRandom().nextInt(centers.size()));
            snapTo(c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5, getYRot(), 0);
        }
        setNoGravity(true);
        pulseTicks = 0;
        nextPulse = PULSE_PERIOD;
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-03-93 푸른 별이 시설 한가운데에 떠올랐습니다. 낮은 소리가 시설 전체를 덮칩니다.")
                .withStyle(st -> st.withColor(0xFF3333).withBold(true)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setNoGravity(false);
        pulseTicks = 0;
    }

    // ───────────── 파동 ─────────────

    private void pulse(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            Employees.damageSp(p, 15 + getRandom().nextInt(6));
            p.level().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.2f, 0.5f);
        }
        // 사무직도 같은 파동에 휩쓸린다
        for (ClerkEntity c : sl.getEntitiesOfClass(ClerkEntity.class, getBoundingBox().inflate(256.0))) {
            if (!c.isAlive()) continue;
            c.damageSp(15 + getRandom().nextInt(6));
        }
        // 공황에 빠진 사무직은 별에게 끌려간다
        for (ClerkEntity c : sl.getEntitiesOfClass(ClerkEntity.class, getBoundingBox().inflate(256.0))) {
            if (!c.isAlive() || !c.isPanicking()) continue;
            c.teleportTo(getX(), getY(), getZ());
            c.hurtServer(sl, sl.damageSources().magic(), 1000f);
        }
        // 별 주위로 푸른 영혼이 고리 모양으로 맴돈다
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            double r = 3.0;
            sl.sendParticles(ParticleTypes.SOUL, getX() + Math.cos(a) * r, getY() + 0.8, getZ() + Math.sin(a) * r,
                    1, 0.05, 0.2, 0.05, 0.0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.6f, 0.4f);

        // 공황에 빠진 직원은 별에게 끌려간다
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (Employees.get(p).isPanicking()) {
                devour(sl, p);
                continue;
            }
            if (distanceTo(p) > NEAR_RADIUS) continue;
            EmployeeData d = Employees.get(p);
            if (d.temperance < NEAR_TEMPERANCE || d.prudence < NEAR_PRUDENCE) {
                devour(sl, p);
            }
        }
    }

    /** 별에게 끌려가 사라진다 */
    private void devour(ServerLevel sl, ServerPlayer p) {
        p.teleportTo(getX(), getY(), getZ());
        p.sendSystemMessage(Component.literal("  푸른 별이 당신을 불렀다.")
                .withStyle(st -> st.withColor(0x5555FF).withItalic(true)));
        sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 40, 0.5, 0.8, 0.5, 0.03);
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    // ───────────── 내성 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached()) {
            boolean ego = source.getEntity() instanceof LivingEntity le
                    && le.getMainHandItem().getItem() instanceof EgoWeaponItem;
            if (!ego) return false;
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (++pulseTicks >= nextPulse) {
            pulseTicks = 0;
            nextPulse = PULSE_PERIOD + getRandom().nextInt(61);
            pulse(sl);
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_bs_pulse", pulseTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        pulseTicks = in.getIntOr("lc_bs_pulse", 0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
