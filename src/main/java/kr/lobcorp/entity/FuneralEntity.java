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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * T-01-68 죽은 나비들의 장례식 — 관을 머리에 쓴 상복 차림의 사내.
 * 탈출하면 직원과 거리를 두며 걷고, 나비 떼를 쏘아 정신력을 갉아먹는다.
 * 이따금 멈춰 서서 앞쪽으로 나비 무리를 풀어놓는다.
 */
public class FuneralEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 2.0f);

    /** 나비 떼 사격 간격 */
    private static final int VOLLEY_CD = 60;
    private static final double VOLLEY_RANGE = 16.0;
    /** 무리 공격: 탈출 후 첫 사용까지, 이후 간격 */
    private static final int SWARM_FIRST = 200;
    private static final int SWARM_CD = 400;
    private static final int SWARM_TICKS = 60;
    private static final double SWARM_RANGE = 12.0;
    /** 부채꼴 각도 (라디안) */
    private static final double SWARM_CONE = Math.toRadians(60);

    private int volleyCd = VOLLEY_CD;
    private int swarmCd = SWARM_FIRST;
    private int swarmTicks = 0;
    /** 플레이어별 마지막으로 무리에 맞은 틱 */
    private final java.util.Map<java.util.UUID, Integer> swarmHit = new java.util.HashMap<>();

    public FuneralEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.FUNERAL;
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
        return "나비 떼를 쏘아 정신력을 깎습니다. 거리를 좁혀 제압하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        var d = Employees.get(p);
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  관 뚜껑 틈으로 흰 나비 한 마리가 기어 나와 손등에 앉는다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  나비들이 조용히 날갯짓을 멈춘다. 관 안쪽에서 낮은 숨소리가 들린다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> p.sendSystemMessage(Component.literal("  관이 아주 조금 열렸다가 닫힌다. 나비들이 일제히 당신을 바라본다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
        if (d.justice < 45) lowerQliphoth(p, 1);
        if (d.fortitude >= 65) lowerQliphoth(p, 1);
        if (result == WorkManager.Result.BAD && p.getRandom().nextInt(100) < 80) lowerQliphoth(p, 1);
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        volleyCd = VOLLEY_CD;
        swarmCd = SWARM_FIRST;
        swarmTicks = 0;
        swarmHit.clear();
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        swarmTicks = 0;
        swarmHit.clear();
    }

    /** 정신 피해. 이 피해로 패닉에 빠지면 그대로 장례가 치러진다. */
    private void mourn(ServerPlayer p, int amount) {
        if (!(level() instanceof ServerLevel sl)) return;
        Employees.damageSp(p, amount);
        if (Employees.get(p).panicTicks > 0) {
            Employees.get(p).panicTicks = 0;
            sl.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
            sl.playSound(null, p.blockPosition(), SoundEvents.ALLAY_DEATH, SoundSource.HOSTILE, 1.2f, 0.8f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                    .append(p.getDisplayName())
                    .append(Component.literal(" 직원의 장례가 치러졌습니다.").withStyle(st -> st.withColor(0xAAAAAA))));
            p.hurtServer(sl, sl.damageSources().magic(), 1000f);
        }
    }

    private void volley(ServerLevel sl, LivingEntity target) {
        Vec3 from = new Vec3(getX(), getY() + 1.6, getZ());
        Vec3 to = new Vec3(target.getX(), target.getY() + 1.0, target.getZ());
        Vec3 diff = to.subtract(from);
        int steps = (int) Math.max(4, diff.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(diff.scale((double) i / steps));
            sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
            if (i % 2 == 0) sl.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.ALLAY_HURT, SoundSource.HOSTILE, 1.2f, 1.6f);
        if (target instanceof ServerPlayer p) mourn(p, 8 + getRandom().nextInt(5));
    }

    private void swarmTick(ServerLevel sl) {
        Vec3 look = getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();

        // 부채꼴 모양으로 나비를 흩뿌린다
        for (int i = 0; i < 6; i++) {
            double ang = (getRandom().nextDouble() * 2 - 1) * SWARM_CONE;
            double dist = 1.5 + getRandom().nextDouble() * (SWARM_RANGE - 1.5);
            double dx = flat.x * Math.cos(ang) - flat.z * Math.sin(ang);
            double dz = flat.x * Math.sin(ang) + flat.z * Math.cos(ang);
            double px = getX() + dx * dist;
            double pz = getZ() + dz * dist;
            double py = getY() + 0.6 + getRandom().nextDouble() * 1.6;
            sl.sendParticles(ParticleTypes.WHITE_ASH, px, py, pz, 2, 0.2, 0.2, 0.2, 0.01);
            if (i % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.1, 0.1, 0.1, 0);
        }

        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > SWARM_RANGE * SWARM_RANGE) continue;
            Vec3 toP = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
            if (toP.lengthSqr() < 1.0E-4) continue;
            toP = toP.normalize();
            if (flat.dot(toP) < Math.cos(SWARM_CONE)) continue;
            Integer last = swarmHit.get(p.getUUID());
            if (last != null && tickCount - last < 20) continue;
            swarmHit.put(p.getUUID(), tickCount);
            mourn(p, 2 + getRandom().nextInt(2));
        }
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            swarmTicks = 0;
            return;
        }

        LivingEntity target = getTarget();

        // 무리 공격 중에는 멈춰 선다
        if (swarmTicks > 0) {
            swarmTicks--;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (target != null) getLookControl().setLookAt(target, 30f, 30f);
            if (swarmTicks % 6 == 0) {
                swarmTick(sl);
                sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.4f, 1.1f);
            }
            return;
        }

        // 이동: 8블록 거리 유지
        if (target != null && target.isAlive()) {
            double dist = distanceTo(target);
            if (dist > 9) {
                getNavigation().moveTo(target, 1.0);
            } else if (dist < 6) {
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
            }
            getLookControl().setLookAt(target, 30f, 30f);
        }

        // 나비 떼 사격
        if (volleyCd > 0) volleyCd--;
        if (volleyCd <= 0) {
            if (target != null && target.isAlive() && distanceToSqr(target) <= VOLLEY_RANGE * VOLLEY_RANGE
                    && hasLineOfSight(target)) {
                volley(sl, target);
                volleyCd = VOLLEY_CD;
            } else {
                volleyCd = 10;
            }
        }

        // 나비 무리
        if (swarmCd > 0) swarmCd--;
        if (swarmCd <= 0) {
            swarmTicks = SWARM_TICKS;
            swarmCd = SWARM_CD;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.6f, 0.9f);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) > 32 * 32) continue;
                p.sendSystemMessage(Component.literal("  관이 열리고, 흰 나비 떼가 쏟아져 나온다.")
                        .withStyle(st -> st.withColor(0xDDDDDD).withItalic(true)));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_volley_cd", volleyCd);
        out.putInt("lc_swarm_cd", swarmCd);
        out.putInt("lc_swarm_ticks", swarmTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        volleyCd = in.getIntOr("lc_volley_cd", VOLLEY_CD);
        swarmCd = in.getIntOr("lc_swarm_cd", SWARM_FIRST);
        swarmTicks = in.getIntOr("lc_swarm_ticks", 0);
    }
}
