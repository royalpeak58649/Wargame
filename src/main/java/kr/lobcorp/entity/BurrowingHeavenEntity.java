package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
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

import java.util.ArrayList;
import java.util.List;

/**
 * O-04-72 땅을 파는 천국 — 창백한 나무 몸통에 핏발 선 눈이 수없이 달려 있다.
 * 누군가 바라보는 동안에는 움직이지 못한다. 아무도 보지 않으면 땅속으로 사라졌다가
 * 직원 곁에서 솟아올라 주변을 찢는다.
 */
public class BurrowingHeavenEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.2f, 3.2f);

    /** 시선 판정 거리 */
    private static final double WATCH_RANGE = 24.0;
    /** 시선 판정 각도 (dot) */
    private static final double WATCH_DOT = 0.85;

    /** 작업 중 시선 확인 주기 */
    private static final int WORK_CHECK = 20;
    /** 시선을 떼고 나서 추가로 카운터가 내려가는 주기 */
    private static final int WORK_PENALTY = 200;

    /** 탈출 중 시선 확인 주기 */
    private static final int BREACH_CHECK = 20;
    /** 아무도 보지 않을 때 파고들기까지 걸리는 시간 */
    private static final int BURROW_TICKS = 60;

    private int notWatched = 0;
    private int unwatched = 0;

    public BurrowingHeavenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.BURROWING_HEAVEN;
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
        return "눈을 떼지 마십시오. 아무도 보지 않으면 움직입니다.";
    }

    // ───────────── 시선 ─────────────

    /** p가 이 환상체를 바라보고 있는가 */
    private boolean isWatchedBy(ServerPlayer p) {
        if (p == null || !p.isAlive() || p.isCreative() || p.isSpectator()) return false;
        if (p.distanceToSqr(this) > WATCH_RANGE * WATCH_RANGE) return false;
        if (!p.hasLineOfSight(this)) return false;
        Vec3 look = p.getLookAngle();
        Vec3 to = getEyePosition().subtract(p.getEyePosition());
        if (to.lengthSqr() < 1.0E-6) return false;
        return look.dot(to.normalize()) > WATCH_DOT;
    }

    /** 이 환상체를 바라보는 직원이 한 명이라도 있는가 */
    private boolean watched() {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (ServerPlayer p : sl.players()) {
            if (isWatchedBy(p)) return true;
        }
        return false;
    }

    // ───────────── 클리포트 카운터 ─────────────

    private void raiseCounter(int n) {
        qliphoth = Math.min(profile().qliphothMax(), qliphoth + n);
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        notWatched = 0;
        switch (result) {
            case GOOD -> {
                p.sendSystemMessage(Component.literal("  수많은 눈이 일제히 감겼다가 다시 떠진다. 뿌리가 조용히 흙을 덮는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                raiseCounter(2);
            }
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  나무 몸통이 아주 조금 흔들린다. 눈 하나가 작업자를 따라 굴러간다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                raiseCounter(1);
            }
            case BAD -> p.sendSystemMessage(Component.literal("  눈이 전부 작업자에게로 몰린다. 뿌리가 바닥을 긁어 파헤친다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    @Override
    public void onWorkAborted(ServerPlayer worker, WorkSession session, String reason) {
        notWatched = 0;
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        notWatched = 0;
        unwatched = 0;
        sl.playSound(null, blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 1.5f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-04-72 땅을 파는 천국이 격리실을 빠져나왔습니다. " + breachAdvice())
                .withStyle(st -> st.withColor(0xFF5555)));
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > 16.0) continue;
            p.hurtServer(sl, sl.damageSources().magic(), 1000f);
        }
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        notWatched = 0;
        unwatched = 0;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isBreached()) {
            getNavigation().stop();
            tickBreached(sl);
        } else {
            tickWork(sl);
        }
    }

    /** 작업 중: 작업자가 이 환상체를 바라보고 있는지 확인한다 */
    private void tickWork(ServerLevel sl) {
        if (tickCount % WORK_CHECK != 0) return;
        for (ServerPlayer p : sl.players()) {
            WorkSession s = WorkManager.session(p);
            if (s == null || s.abno != this) continue;
            if (isWatchedBy(p)) {
                notWatched = 0;
                continue;
            }
            p.sendOverlayMessage(Component.literal("땅을 파는 천국이 움직이려 한다. 눈을 떼지 마십시오.")
                    .withStyle(st -> st.withColor(0xFF5555)));
            if (notWatched == 0) {
                lowerQliphoth(p, 1);
            } else if (notWatched % WORK_PENALTY == 0) {
                lowerQliphoth(p, 1);
            }
            notWatched += WORK_CHECK;
        }
    }

    /** 탈출 중: 아무도 보지 않으면 땅속으로 사라졌다가 직원 곁에서 솟아오른다 */
    private void tickBreached(ServerLevel sl) {
        if (tickCount % BREACH_CHECK != 0) return;
        if (watched()) {
            unwatched = 0;
            return;
        }
        unwatched += BREACH_CHECK;
        if (unwatched < BURROW_TICKS) return;

        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            targets.add(p);
        }
        if (targets.isEmpty()) {
            float next = getHealth() - getMaxHealth() * 0.25f;
            if (next <= 0) {
                returnToContainment(Component.literal("[관제] O-04-72가 땅속으로 파고들어 격리실로 돌아갔습니다.")
                        .withStyle(st -> st.withColor(0x55FF55)));
                return;
            }
            setHealth(next);
            unwatched = 0;
            return;
        }

        ServerPlayer target = targets.get(getRandom().nextInt(targets.size()));
        Vec3 old = position();
        teleportTo(target.getX() + 1.5, target.getY(), target.getZ());
        sl.sendParticles(ParticleTypes.SCULK_SOUL, old.x, old.y + 1.0, old.z, 40, 0.6, 1.0, 0.6, 0.02);
        sl.playSound(null, target.blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 1.5f, 0.6f);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.position().distanceToSqr(old) > 36.0) continue;
            p.hurtServer(sl, sl.damageSources().magic(), 25f);
            Employees.damageSp(p, 10);
            p.sendSystemMessage(Component.literal("  뿌리가 바닥을 찢고 솟아올라 살을 파고든다.")
                    .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
        }
        unwatched = 0;
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_heaven_not_watched", notWatched);
        out.putInt("lc_heaven_unwatched", unwatched);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        notWatched = in.getIntOr("lc_heaven_not_watched", 0);
        unwatched = in.getIntOr("lc_heaven_unwatched", 0);
    }
}
