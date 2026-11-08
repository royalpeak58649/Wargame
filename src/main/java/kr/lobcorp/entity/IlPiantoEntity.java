package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.UUID;

/**
 * D-01-105 달의 울음 — 뚜껑이 우는 초승달 얼굴인 창백한 그랜드 피아노.
 * 통찰 작업을 마치면 연주가 시작되어 모든 직원이 빨라지지만, 세 번째 연주에서는 연주자가 갇히고
 * 카운터가 0이 되어 곧바로 탈출한다. 탈출하면 달머리 형상으로 일어나 직원을 쫓고,
 * 주기적으로 몸을 웅크렸다가 곧장 돌진한다.
 */
public class IlPiantoEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.4f, 2.4f);

    /** 연주 지속 시간 */
    private static final int PERFORM_TICKS = 1800;
    /** 연주 중 버프/파티클 주기 */
    private static final int PERFORM_PULSE = 40;
    /** 갇힌 연주자 감속 주기 */
    private static final int TRAP_SLOW_CD = 20;
    /** 갇힌 연주자 피해 주기 */
    private static final int TRAP_HURT_CD = 60;
    private static final float TRAP_HURT = 4f;
    private static final int TRAP_SP = 4;

    /** 돌진 준비(정지) 시간 */
    private static final int WIND_TICKS = 30;
    /** 돌진 재사용 대기 */
    private static final int CHARGE_CD = 200;
    /** 돌진 최대 거리(블록) */
    private static final int CHARGE_MAX = 14;
    private static final double CHARGE_STEP = 1.2;
    private static final double CHARGE_HIT_RADIUS = 1.5;
    private static final float CHARGE_DAMAGE = 18f;
    private static final int CHARGE_SP = 10;
    /** 복귀 최대 시간 */
    private static final int RETURN_MAX = 100;
    private static final double RETURN_DONE = 2.0;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_WIND = 1;
    private static final int PHASE_CHARGE = 2;
    private static final int PHASE_RETURN = 3;

    private UUID performer = null;
    private int performTicks = 0;
    private int performCount = 0;

    private int phase = PHASE_IDLE;
    private int timer = 0;
    private int breachTimer = 0;
    private int chargeCd = CHARGE_CD;
    private int trapSlowCd = TRAP_SLOW_CD;
    private int trapHurtCd = TRAP_HURT_CD;
    private Vec3 chargeDir = Vec3.ZERO;
    private double charged = 0;

    public IlPiantoEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150 * 1.5 / 0.6 * 0.8 * 1.1)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.IL_PIANTO;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "연주가 끝날 때까지 버티거나 제압하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (s.type == WorkType.INSIGHT) {
            startPerformance(p);
        }
        switch (result) {
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  뚜껑이 반쯤 열린 채 낮은 화음이 울린다. 달의 얼굴이 조용히 눈을 감는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (getRandom().nextInt(100) < 30) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  건반이 삐걱이며 젖은 소리를 낸다. 달의 얼굴에서 눈물이 흘러내린다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
            default -> {
            }
        }
    }

    // ───────────── 연주 ─────────────

    /** 통찰 작업을 마친 직원이 연주를 시작한다 */
    private void startPerformance(ServerPlayer p) {
        performer = p.getUUID();
        performTicks = PERFORM_TICKS;
        performCount++;
        if (level() instanceof ServerLevel sl) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] D-01-105 달의 울음이 연주를 시작합니다. 피아노가 스스로 노래합니다.")
                    .withStyle(st -> st.withColor(0xAA55FF)));
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.NEUTRAL, 1.4f, 0.8f);
        }
        if (performCount % 3 == 0) {
            if (level() instanceof ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] 세 번째 연주입니다. 연주자가 피아노에 갇혔습니다!")
                        .withStyle(st -> st.withColor(0xFF5555).withBold(true)));
            }
            lowerQliphoth(null, qliphoth);
        }
    }

    /** 연주 중 효과: 모든 직원이 빨라지고, 피아노에서 음표가 흩날린다 */
    private void tickPerformance(ServerLevel sl) {
        if (performTicks % PERFORM_PULSE != 0) return;
        for (ServerPlayer o : sl.getServer().getPlayerList().getPlayers()) {
            o.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, false));
        }
        sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.4, getZ(), 8, 0.7, 0.5, 0.7, 0.1);
    }

    /** 탈출 상태에서 연주자가 갇혀 있는 동안의 속박과 피해 */
    private void tickTrapped(ServerLevel sl, ServerPlayer p) {
        if (--trapSlowCd <= 0) {
            trapSlowCd = TRAP_SLOW_CD;
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 4, false, false));
        }
        if (--trapHurtCd <= 0) {
            trapHurtCd = TRAP_HURT_CD;
            p.hurtServer(sl, sl.damageSources().magic(), TRAP_HURT);
            Employees.damageSp(p, TRAP_SP);
            p.sendSystemMessage(Component.literal("  달의 손가락이 건반을 누른다. 몸이 피아노에 붙어 떨어지지 않는다.")
                    .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
        }
    }

    /** 연주 종료 처리 */
    private void finishPerformance(ServerLevel sl) {
        ServerPlayer p = performer == null ? null : sl.getServer().getPlayerList().getPlayer(performer);
        if (p != null && p.isAlive()) {
            p.setHealth(Math.max(1f, p.getHealth() / 2f));
            p.sendSystemMessage(Component.literal("  마지막 화음이 끊기자 몸이 풀려난다. 힘이 빠져 나간다.")
                    .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
        }
        if (isBreached()) {
            returnToContainment(Component.literal("[관제] 연주가 끝나자 D-01-105가 격리실로 돌아갔습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
        } else {
            lowerQliphoth(p, 1);
        }
        performer = null;
        performTicks = 0;
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        resetState();
        breachTimer = 900 + getRandom().nextInt(900);
        if (performTicks <= 0) performTicks = PERFORM_TICKS;
        sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 1.6f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] D-01-105 달의 울음이 격리실을 빠져나왔습니다. 연주가 끝날 때까지 버티십시오.")
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
        trapSlowCd = TRAP_SLOW_CD;
        trapHurtCd = TRAP_HURT_CD;
        chargeDir = Vec3.ZERO;
        charged = 0;
    }

    // ───────────── 상태 기계 ─────────────

    private void startWind(ServerLevel sl) {
        phase = PHASE_WIND;
        timer = WIND_TICKS;
        chargeCd = CHARGE_CD;
        getNavigation().stop();
        sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.6, getZ(), 20, 0.6, 0.6, 0.6, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 1.2f, 0.7f);
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
        sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 1.6f, 0.5f);
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
        sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.2, getZ(), 6, 0.4, 0.4, 0.4, 0.05);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > CHARGE_HIT_RADIUS * CHARGE_HIT_RADIUS) continue;
            p.hurtServer(sl, sl.damageSources().magic(), CHARGE_DAMAGE);
            Employees.damageSp(p, CHARGE_SP);
            p.sendSystemMessage(Component.literal("  달머리가 직원을 정면으로 들이받는다. 귀에서 화음이 갈라진다.")
                    .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
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

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;

        if (performTicks > 0) {
            tickPerformance(sl);
            if (isBreached() && performer != null) {
                ServerPlayer trapped = sl.getServer().getPlayerList().getPlayer(performer);
                if (trapped != null && trapped.isAlive()) tickTrapped(sl, trapped);
            }
            if (--performTicks <= 0) {
                finishPerformance(sl);
                return;
            }
        }

        if (!isBreached()) return;

        if (breachTimer > 0 && --breachTimer == 0) {
            returnToContainment(Component.literal("[관제] D-01-105의 연주가 잦아들며 격리실로 돌아갔습니다.")
                    .withStyle(st -> st.withColor(0x55FF55)));
            resetQliphoth();
            return;
        }

        switch (phase) {
            case PHASE_WIND -> {
                getNavigation().stop();
                sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.6, getZ(), 4, 0.5, 0.5, 0.5, 0.02);
                if (--timer <= 0) startCharge(sl);
            }
            case PHASE_CHARGE -> tickCharge(sl);
            case PHASE_RETURN -> tickReturn(sl);
            default -> {
                if (chargeCd > 0) chargeCd--;
                if (chargeCd <= 0 && getTarget() != null) startWind(sl);
            }
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("lc_pianto_performer", performer == null ? "" : performer.toString());
        out.putInt("lc_pianto_ticks", performTicks);
        out.putInt("lc_pianto_count", performCount);
        out.putInt("lc_pianto_phase", phase);
        out.putInt("lc_pianto_timer", timer);
        out.putInt("lc_pianto_breach", breachTimer);
        out.putInt("lc_pianto_cd", chargeCd);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        String s = in.getStringOr("lc_pianto_performer", "");
        performer = null;
        if (!s.isEmpty()) {
            try {
                performer = UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
            }
        }
        performTicks = in.getIntOr("lc_pianto_ticks", 0);
        performCount = in.getIntOr("lc_pianto_count", 0);
        phase = in.getIntOr("lc_pianto_phase", PHASE_IDLE);
        timer = in.getIntOr("lc_pianto_timer", 0);
        breachTimer = in.getIntOr("lc_pianto_breach", 0);
        chargeCd = in.getIntOr("lc_pianto_cd", CHARGE_CD);
        chargeDir = Vec3.ZERO;
        charged = 0;
    }
}
