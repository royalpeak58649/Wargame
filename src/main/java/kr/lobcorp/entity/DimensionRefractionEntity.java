package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O-03-88 차원 굴절 변이체 — 텅 빈 중심 주위로 굴절된 파편이 떠다니는, 겨우 눈에 보이는 공간의 일그러짐.
 * 작업이 길어지면 카운터가 내려가고, 탈출하면 투명해진 채로 직원 곁을 느리게 떠돌며 닿은 이를 갈아버린다.
 */
public class DimensionRefractionEntity extends BreachingAbnormality {
    /** 이동 목적지 재선정 주기 */
    private static final int MOVE_INTERVAL = 20;
    /** 추격 반경 */
    private static final double CHASE_RANGE = 48.0;
    /** 접촉 판정 거리 */
    private static final double TOUCH_RANGE = 1.5;
    /** 접촉 피해 한 번의 간격 */
    private static final int BURST_INTERVAL = 4;
    /** 접촉 피해 횟수 (5회 × 6 = 30) */
    private static final int BURST_HITS = 5;
    /** 접촉 피해량 */
    private static final float TOUCH_DAMAGE = 6f;
    /** 접촉 피해 재발동 대기 */
    private static final int BURST_COOLDOWN = 40;
    /** 희미한 빛 입자 주기 */
    private static final int SHIMMER_INTERVAL = 10;
    /** 긴 작업 판정 (40초) */
    private static final long LONG_WORK_TICKS = 800L;

    /** 작업 시작 시각 (직원 UUID → gameTime) */
    private final Map<UUID, Long> workStart = new HashMap<>();
    /** 접촉 피해 버스트 진행도 (직원 UUID → 남은 틱, 0 이하 = 대기) */
    private final Map<UUID, Integer> touchTimers = new HashMap<>();
    /** 접촉 피해 재발동 대기 (직원 UUID → 남은 틱) */
    private final Map<UUID, Integer> touchCooldowns = new HashMap<>();

    public DimensionRefractionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 187.5)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.DIMENSION_REFRACTION;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected String breachAdvice() {
        return "보이지 않습니다. 희미한 빛을 쫓아 직접 공격하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkStart(ServerPlayer worker, kr.lobcorp.work.WorkType type) {
        if (worker == null) return;
        workStart.put(worker.getUUID(), level().getGameTime());
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  공간이 잠시 반듯하게 접힌다. 파편들이 조용히 제자리로 돌아간다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  굴절된 파편 하나가 작업자의 손끝을 스쳐 지나간다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  중심의 빈자리가 일렁이며 작업자의 그림자를 삼킨다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
        checkLongWork(p);
    }

    @Override
    public void onWorkAborted(ServerPlayer worker, WorkSession session, String reason) {
        checkLongWork(worker);
    }

    /** 작업이 40초를 넘겼다면 카운터를 1 내린다 */
    private void checkLongWork(ServerPlayer p) {
        if (p == null) return;
        Long start = workStart.remove(p.getUUID());
        if (start == null) return;
        if (level().getGameTime() - start > LONG_WORK_TICKS) {
            p.sendSystemMessage(Component.literal("  너무 오래 들여다보았다. 시야가 굴절되어 어지러워진다.")
                    .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
            lowerQliphoth(p, 1);
        }
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        workStart.clear();
        touchTimers.clear();
        touchCooldowns.clear();
        setInvisible(true);
        setNoGravity(true);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-03-88 차원 굴절 변이체가 격리실을 빠져나왔습니다. " + breachAdvice())
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setInvisible(false);
        setNoGravity(false);
        workStart.clear();
        touchTimers.clear();
        touchCooldowns.clear();
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;
        if (tickCount % MOVE_INTERVAL == 0) drift(sl);
        if (tickCount % SHIMMER_INTERVAL == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 1, 0.15, 0.15, 0.15, 0.002);
        }
        tickTouch(sl);
    }

    /** 20틱마다 가장 가까운 직원을 향해 느리게 떠간다 */
    private void drift(ServerLevel sl) {
        ServerPlayer target = nearestPlayer(sl);
        if (target == null) {
            getNavigation().stop();
            return;
        }
        getNavigation().moveTo(target, 0.6);
    }

    private ServerPlayer nearestPlayer(ServerLevel sl) {
        ServerPlayer best = null;
        double bestDist = CHASE_RANGE * CHASE_RANGE;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double d = p.distanceToSqr(this);
            if (d > bestDist) continue;
            bestDist = d;
            best = p;
        }
        return best;
    }

    /** 닿은 직원에게 4틱 간격으로 5회, 총 30 피해 */
    private void tickTouch(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            UUID id = p.getUUID();
            int cd = touchCooldowns.getOrDefault(id, 0);
            if (cd > 0) touchCooldowns.put(id, cd - 1);

            Integer timer = touchTimers.get(id);
            if (timer == null) {
                if (cd > 0) continue;
                if (p.distanceToSqr(this) > TOUCH_RANGE * TOUCH_RANGE) continue;
                timer = BURST_HITS * BURST_INTERVAL;
                p.sendSystemMessage(Component.literal("  보이지 않는 파편이 살갗을 갈아낸다.")
                        .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
            }
            if (timer % BURST_INTERVAL == 0) {
                p.hurtServer(sl, sl.damageSources().magic(), TOUCH_DAMAGE);
            }
            timer--;
            if (timer <= 0) {
                touchTimers.remove(id);
                touchCooldowns.put(id, BURST_COOLDOWN);
            } else {
                touchTimers.put(id, timer);
            }
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_refraction_invisible", isInvisible());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setInvisible(in.getBooleanOr("lc_refraction_invisible", false));
    }
}
