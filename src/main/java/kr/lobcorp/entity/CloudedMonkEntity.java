package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * D-01-110 구름 낀 수도승 — 회색 구름에 휩싸인 수도승.
 * 통찰 작업은 SP를 회복시키지만, 실패하면 카운터가 내려간다. 직원이 세 명 죽을 때마다 카운터가 1 내려가고,
 * 탈출한 용(Yin)이 격리실 근처에 있으면 카운터가 곧바로 0이 된다.
 * 탈출하면 직원을 쫓고, 체력이 70% 아래로 떨어지면 구름을 모은 뒤 정면으로 돌진한다.
 */
public class CloudedMonkEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.0f, 2.4f);

    /** 용이 격리실 근처에 있을 때 카운터를 0으로 만드는 판정 주기 */
    private static final int YIN_CD = 40;
    private static final double YIN_RADIUS = 16.0;
    /** 직원 사망 누적 임계값 */
    private static final int DEATH_LIMIT = 3;
    /** 돌진 준비(정지) 시간 */
    private static final int WIND_TICKS = 30;
    /** 돌진 재사용 대기 */
    private static final int CHARGE_CD = 160;
    /** 돌진 최대 거리(블록) */
    private static final int CHARGE_MAX = 16;
    private static final double CHARGE_STEP = 1.2;
    private static final double CHARGE_HIT_RADIUS = 1.5;
    private static final float CHARGE_DAMAGE = 40f;
    /** 돌진이 명중하면 최대 HP의 이만큼을 회복한다 */
    private static final float CHARGE_HEAL = 0.2f;
    /** 이 비율 아래로 체력이 떨어지면 돌진을 쓸 수 있다 */
    private static final float CHARGE_HP_RATIO = 0.7f;
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
    private int chargeCd = CHARGE_CD;
    private int yinCd = YIN_CD;
    private int deaths = 0;
    private Vec3 chargeDir = Vec3.ZERO;
    private double charged = 0;

    public CloudedMonkEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150 * 1.5 / 0.6 * 0.8)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.CLOUDED_MONK;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "체력이 깎이면 돌진합니다. 정면에 서지 마십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (s.type == WorkType.INSIGHT) {
            EmployeeData d = Employees.get(p);
            d.sp = Math.min(d.maxSp(), d.sp + 5);
            Employees.sync(p);
            p.sendSystemMessage(Component.literal("  수도승이 구름 사이로 손끝을 내밀어 이마를 스친다. 마음이 잠시 맑아진다.")
                    .withStyle(st -> st.withColor(0xCCCCFF).withItalic(true)));
        }
        switch (result) {
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  구름이 낮게 가라앉는다. 수도승이 천천히 고개를 젓는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (getRandom().nextInt(100) < 30) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  구름이 격리실 벽을 타고 번진다. 수도승의 긴 팔이 바닥을 긁는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
            default -> {
            }
        }
    }

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (isBreached()) return;
        deaths++;
        if (deaths < DEATH_LIMIT) return;
        deaths = 0;
        lowerQliphoth(null, 1);
        if (level() instanceof ServerLevel sl) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] D-01-110 구름 낀 수도승이 죽음을 세어 내려앉습니다.")
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        resetState();
        breachTimer = 900 + getRandom().nextInt(900);
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] D-01-110 구름 낀 수도승이 격리실을 빠져나왔습니다. 정면에 서지 마십시오.")
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
        chargeCd = CHARGE_CD;
        chargeDir = Vec3.ZERO;
        charged = 0;
    }

    // ───────────── 상태 기계 ─────────────

    private void startWind(ServerLevel sl) {
        phase = PHASE_WIND;
        timer = WIND_TICKS;
        chargeCd = CHARGE_CD;
        getNavigation().stop();
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.2, getZ(), 30, 0.7, 0.7, 0.7, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2f, 0.7f);
    }

    private void startCharge(ServerLevel sl) {
        phase = PHASE_CHARGE;
        charged = 0;
        Vec3 dir = Vec3.ZERO;
        if (getTarget() != null) {
            Vec3 d = new Vec3(getTarget().getX() - getX(), 0, getTarget().getZ() - getZ());
            if (d.lengthSqr() > 0.01) dir = d.normalize();
        }
        if (dir == Vec3.ZERO) {
            float yaw = getYRot() * (float) Math.PI / 180f;
            dir = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        chargeDir = dir;
        setYRot((float) (Math.atan2(chargeDir.z, chargeDir.x) * 180.0 / Math.PI) - 90f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.6f, 0.5f);
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
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 8, 0.4, 0.4, 0.4, 0.02);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > CHARGE_HIT_RADIUS * CHARGE_HIT_RADIUS) continue;
            p.hurtServer(sl, sl.damageSources().magic(), CHARGE_DAMAGE);
            heal(getMaxHealth() * CHARGE_HEAL);
            p.sendSystemMessage(Component.literal("  구름이 직원을 덮치고, 수도승의 상처가 구름으로 메워진다.")
                    .withStyle(st -> st.withColor(0xCCCCFF).withItalic(true)));
            startReturn(sl);
            return;
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

    // ───────────── 용(Yin) ─────────────

    /** 탈출한 용이 격리실 근처에 있으면 구름이 무너져 내린다 */
    private void tickYin(ServerLevel sl) {
        if (home == null) return;
        double hx = home.getX() + 0.5, hy = home.getY(), hz = home.getZ() + 0.5;
        for (YinEntity yin : sl.getEntitiesOfClass(YinEntity.class,
                new AABB(hx - YIN_RADIUS, hy - YIN_RADIUS, hz - YIN_RADIUS,
                        hx + YIN_RADIUS, hy + YIN_RADIUS, hz + YIN_RADIUS))) {
            if (!yin.isAlive() || !yin.isBreached()) continue;
            if (yin.distanceToSqr(hx, hy, hz) > YIN_RADIUS * YIN_RADIUS) continue;
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.2, getZ(), 20, 0.5, 0.6, 0.5, 0.02);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 용의 그림자가 스치자 D-01-110의 구름이 무너져 내립니다.")
                    .withStyle(st -> st.withColor(0xFF5555)));
            lowerQliphoth(null, qliphoth);
            return;
        }
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            if (--yinCd <= 0) {
                yinCd = YIN_CD;
                tickYin(sl);
            }
            return;
        }

        if (breachTimer > 0 && --breachTimer == 0) {
            returnToContainment(Component.literal("[관제] D-01-110의 구름이 걷히며 격리실로 돌아갔습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
            resetQliphoth();
            return;
        }

        switch (phase) {
            case PHASE_WIND -> {
                getNavigation().stop();
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.2, getZ(), 4, 0.5, 0.5, 0.5, 0.01);
                if (--timer <= 0) startCharge(sl);
            }
            case PHASE_CHARGE -> tickCharge(sl);
            case PHASE_RETURN -> tickReturn(sl);
            default -> {
                if (chargeCd > 0) chargeCd--;
                if (chargeCd <= 0 && getTarget() != null && getHealth() < getMaxHealth() * CHARGE_HP_RATIO) {
                    startWind(sl);
                }
            }
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_monk_phase", phase);
        out.putInt("lc_monk_timer", timer);
        out.putInt("lc_monk_breach", breachTimer);
        out.putInt("lc_monk_cd", chargeCd);
        out.putInt("lc_monk_deaths", deaths);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        phase = in.getIntOr("lc_monk_phase", PHASE_IDLE);
        timer = in.getIntOr("lc_monk_timer", 0);
        breachTimer = in.getIntOr("lc_monk_breach", 0);
        chargeCd = in.getIntOr("lc_monk_cd", CHARGE_CD);
        deaths = in.getIntOr("lc_monk_deaths", 0);
        chargeDir = Vec3.ZERO;
        charged = 0;
    }
}
