package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.ModItems;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.FacilityData;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * O-02-101 불새 — 불꽃으로 이루어진 거대한 새.
 * 클리포트 카운터가 거꾸로 움직이며, 카운터가 낮을수록 작업 실패 피해가 커진다.
 * 탈출하면 주변을 태우고, 공격당하면 불길을 두르고 곧장 돌진한다. 제압하면 명예의 깃털을 남긴다.
 */
public class FirebirdEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.4f, 1.6f);

    /** 주변 화상 피해 주기 */
    private static final int AURA_CD = 20;
    private static final double AURA_RADIUS = 8.0;
    /** 돌진 준비(정지) 시간 */
    private static final int WIND_TICKS = 20;
    /** 돌진 최대 거리(블록) */
    private static final int CHARGE_MAX = 12;
    private static final double CHARGE_STEP = 1.0;
    private static final double CHARGE_HIT_RADIUS = 1.5;
    /** 복귀 최대 시간 */
    private static final int RETURN_MAX = 100;
    private static final double RETURN_DONE = 2.0;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_WIND = 1;
    private static final int PHASE_CHARGE = 2;
    private static final int PHASE_RETURN = 3;

    private int phase = PHASE_IDLE;
    private int timer = 0;
    private int breachTimer = 0;
    private int auraCd = AURA_CD;
    private Vec3 chargeDir = Vec3.ZERO;
    private double charged = 0;
    private UUID chargeTarget = null;
    private int featherDay = -1;

    private final Set<UUID> chargeHits = new HashSet<>();

    public FirebirdEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150 * 1.5 / 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.FIREBIRD;
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
        return "불길에 닿지 마십시오. 공격하면 곧장 돌진해 옵니다.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(p);
        d.firebirdBlind = false;
        // 타오르는 불꽃: 카운터가 1이거나 HP가 위태로우면 불꽃으로 되살아난다
        if (qliphoth == 1 || p.getHealth() < p.getMaxHealth() * 0.2f) {
            p.setHealth(p.getMaxHealth());
            d.blazingTicks = 1200;
            p.sendSystemMessage(Component.literal("  타오르는 불꽃이 온몸을 감싸고 상처가 재가 되어 흩어진다.")
                    .withStyle(st -> st.withColor(0xFFAA00).withItalic(true)));
        }
        switch (result) {
            case GOOD -> {
                p.sendSystemMessage(Component.literal("  불새가 날개를 접고 조용히 고개를 숙인다. 불티가 별처럼 흩어진다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  불새가 낮게 울며 깃털을 부리로 고른다. 열기가 잠시 옅어진다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (getRandom().nextInt(100) < 30) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  불새가 날개를 크게 펼친다. 격리실의 공기가 타들어 간다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                raiseCounter(1);
            }
        }
    }

    private void raiseCounter(int n) {
        qliphoth = Math.min(profile().qliphothMax(), qliphoth + n);
    }

    @Override
    public void onAnyBreach(BreachingAbnormality who) {
        if (who == this || isBreached()) return;
        if (home == null || who.home == null) return;
        if (who.home.distSqr(home) < 40 * 40) lowerQliphoth(null, 1);
    }

    @Override
    public float workDamageMultiplier(ServerPlayer worker) {
        if (qliphoth == 2) return 1.5f;
        if (qliphoth == 1) return 2.0f;
        return super.workDamageMultiplier(worker);
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        resetState();
        breachTimer = 900 + getRandom().nextInt(900);
        sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-02-101 불새가 격리실을 빠져나왔습니다. 불길에 닿지 마십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        resetState();
    }

    private void resetState() {
        phase = PHASE_IDLE;
        timer = 0;
        breachTimer = 0;
        auraCd = AURA_CD;
        chargeDir = Vec3.ZERO;
        charged = 0;
        chargeTarget = null;
        chargeHits.clear();
    }

    // ───────────── 상태 기계 ─────────────

    /** 공격당했을 때: 잠시 몸을 웅크린 뒤 공격자를 향해 돌진한다 */
    private void startWind(ServerPlayer attacker) {
        phase = PHASE_WIND;
        timer = WIND_TICKS;
        chargeTarget = attacker.getUUID();
        chargeHits.clear();
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 30, 0.6, 0.6, 0.6, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4f, 0.8f);
        }
    }

    private void startCharge(ServerLevel sl) {
        phase = PHASE_CHARGE;
        charged = 0;
        chargeHits.clear();
        Vec3 dir = Vec3.ZERO;
        if (chargeTarget != null) {
            ServerPlayer t = sl.getServer().getPlayerList().getPlayer(chargeTarget);
            if (t != null) {
                Vec3 d = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
                if (d.lengthSqr() > 0.01) dir = d.normalize();
            }
        }
        if (dir == Vec3.ZERO) {
            float yaw = getYRot() * (float) Math.PI / 180f;
            dir = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        chargeDir = dir;
        setYRot((float) (Math.atan2(chargeDir.z, chargeDir.x) * 180.0 / Math.PI) - 90f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.6f, 0.5f);
    }

    private void tickCharge(ServerLevel sl) {
        if (charged >= CHARGE_MAX) {
            startReturn(sl);
            return;
        }
        double step = Math.min(CHARGE_STEP, CHARGE_MAX - charged);
        double nx = getX() + chargeDir.x * step;
        double nz = getZ() + chargeDir.z * step;
        BlockPos feet = new BlockPos((int) Math.floor(nx), (int) Math.floor(getY()), (int) Math.floor(nz));
        if (sl.getBlockState(feet).isSolid() || sl.getBlockState(feet.above()).isSolid()) {
            startReturn(sl);
            return;
        }
        teleportTo(nx, getY(), nz);
        charged += step;
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (chargeHits.contains(p.getUUID())) continue;
            if (p.distanceToSqr(this) > CHARGE_HIT_RADIUS * CHARGE_HIT_RADIUS) continue;
            chargeHits.add(p.getUUID());
            Employees.damageSp(p, 15);
            p.sendSystemMessage(Component.literal("  불길이 직원을 삼키고 지나간다.")
                    .withStyle(st -> st.withColor(0xFFAA00).withItalic(true)));
        }
        if (charged >= CHARGE_MAX) startReturn(sl);
    }

    private void startReturn(ServerLevel sl) {
        phase = PHASE_RETURN;
        timer = RETURN_MAX;
        chargeDir = Vec3.ZERO;
        charged = 0;
        if (home != null) {
            getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
        }
    }

    private void tickReturn(ServerLevel sl) {
        if (home == null || --timer <= 0
                || distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) <= RETURN_DONE * RETURN_DONE) {
            getNavigation().stop();
            phase = PHASE_IDLE;
            timer = 0;
        }
    }

    // ───────────── 화상 / 불꽃 ─────────────

    private void tickAura(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 8, 0.8, 0.8, 0.8, 0.01);
        if (auraCd > 0) {
            auraCd--;
            return;
        }
        auraCd = AURA_CD;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > AURA_RADIUS * AURA_RADIUS) continue;
            p.hurtServer(sl, sl.damageSources().magic(), 2f);
        }
    }

    /** 타오르는 불꽃: 남은 틱 동안 서서히 회복한다 */
    private void tickBlazing(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            EmployeeData d = Employees.get(p);
            if (d.blazingTicks <= 0) continue;
            d.blazingTicks -= 20;
            if (d.blazingTicks % 100 == 0) {
                p.heal(1f);
                d.sp = Math.min(d.maxSp(), d.sp + 1);
                Employees.sync(p);
            }
            if (d.blazingTicks <= 0) {
                d.blazingTicks = 0;
                p.sendSystemMessage(Component.literal("  타오르는 불꽃이 잦아들었다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
        }
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) tickBlazing(sl);
        if (!isBreached()) return;

        tickAura(sl);
        if (breachTimer > 0 && --breachTimer == 0) {
            returnToContainment(Component.literal("[관제] O-02-101의 불길이 사그라들며 격리실로 돌아갔습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
            resetQliphoth();
            return;
        }

        switch (phase) {
            case PHASE_WIND -> {
                if (--timer <= 0) startCharge(sl);
            }
            case PHASE_CHARGE -> tickCharge(sl);
            case PHASE_RETURN -> tickReturn(sl);
            default -> getNavigation().stop();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer sp && !Employees.get(sp).hasGift("justitia")) {
            Employees.get(sp).firebirdBlind = true;
        }
        if (isBreached() && (phase == PHASE_CHARGE || phase == PHASE_RETURN)) return false;
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && isBreached() && phase == PHASE_IDLE && source.getEntity() instanceof ServerPlayer attacker) {
            startWind(attacker);
        }
        return hurt;
    }

    // ───────────── 제압 ─────────────

    @Override
    protected void suppress(Entity by) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.0, getZ(), 12, 0.8, 0.8, 0.8, 0.02);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 40, 1.0, 1.0, 1.0, 0.05);
        }
        if (by instanceof ServerPlayer sp) {
            FacilityData f = Facility.get(sp.level().getServer());
            if (featherDay != f.day) {
                featherDay = f.day;
                sp.getInventory().placeItemBackInInventory(new ItemStack(ModItems.FEATHER_OF_HONOR),
                        net.minecraft.util.Prediction.SERVER_ONLY);
                sp.sendSystemMessage(Component.literal("  불새가 타다 남은 깃털 하나를 떨어뜨린다.")
                        .withStyle(st -> st.withColor(0xFFAA00).withItalic(true)));
            }
        }
        super.suppress(by);
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_firebird_phase", phase);
        out.putInt("lc_firebird_timer", timer);
        out.putInt("lc_firebird_breach", breachTimer);
        out.putInt("lc_firebird_feather_day", featherDay);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        phase = in.getIntOr("lc_firebird_phase", PHASE_IDLE);
        timer = in.getIntOr("lc_firebird_timer", 0);
        breachTimer = in.getIntOr("lc_firebird_breach", 0);
        featherDay = in.getIntOr("lc_firebird_feather_day", -1);
        chargeDir = Vec3.ZERO;
        charged = 0;
        chargeTarget = null;
        chargeHits.clear();
    }
}
