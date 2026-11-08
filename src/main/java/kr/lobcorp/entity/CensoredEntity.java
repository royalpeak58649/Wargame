package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * O-03-89 ███████ — 모습을 기록할 수 없는 ALEPH 환상체.
 * 신중이 낮은 직원은 격리실에 들어서는 순간 공황에 빠지고, 억압 작업은 「희생」으로 바뀐다.
 * 탈출하면 죽인 직원마다 같은 모습의 하수인이 태어난다.
 */
public class CensoredEntity extends BreachingAbnormality {
    /** 신중 기준선 */
    private static final int PRUDENCE_SAFE = 85;
    /** 탈출 중 공포 오라 주기 */
    private static final int FEAR_PERIOD = 100;
    /** 공포 오라 반경 */
    private static final double FEAR_RADIUS = 10.0;
    /** 하수인 생성 판정 반경 */
    private static final double DEATH_RADIUS = 24.0;
    /** 하수인 최대 수 */
    private static final int MAX_MINIONS = 8;

    public CensoredEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 16)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.CENSORED;
    }

    @Override
    protected String breachAdvice() {
        return "신중이 높은 직원만 접근하십시오. 하수인이 늘기 전에 제압하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer worker, WorkType type) {
        if (type != WorkType.REPRESSION) return super.beforeWorkStart(worker, type);
        sacrifice(worker);
        return false;
    }

    /** 억압 작업 = 「희생」. 무작위 직원 한 명이 격리실로 끌려가 삼켜진다. */
    private void sacrifice(ServerPlayer worker) {
        if (!(level() instanceof ServerLevel sl)) return;
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p == worker || p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            if (Employees.get(p).isPanicking()) continue;
            candidates.add(p);
        }
        ServerPlayer victim = candidates.isEmpty() ? worker
                : candidates.get(getRandom().nextInt(candidates.size()));
        if (home != null) victim.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 1.5);
        sl.sendParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY() + 1.2, victim.getZ(), 40, 0.5, 0.8, 0.5, 0.03);
        sl.playSound(null, victim.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.2f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 희생: ").withStyle(st -> st.withColor(0xAA0000))
                .append(victim.getDisplayName())
                .append(Component.literal(" 직원이 O-03-89의 격리실로 끌려갔습니다.").withStyle(st -> st.withColor(0xFF3333))));
        victim.hurtServer(sl, sl.damageSources().magic(), 1000f);
        qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
        worker.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    public void onWorkStart(ServerPlayer worker, WorkType type) {
        EmployeeData d = Employees.get(worker);
        if (d.prudence < PRUDENCE_SAFE) {
            worker.sendSystemMessage(Component.literal("  보아서는 안 될 것을 보았다. 정신이 산산조각 난다.")
                    .withStyle(st -> st.withColor(0xFF3333).withItalic(true)));
            Employees.panic(worker);
            lowerQliphoth(worker, 1);
        } else {
            Employees.damageSp(worker, d.maxSp() * 6 / 10);
            worker.sendSystemMessage(Component.literal("  보아서는 안 될 것을 보았다. 정신이 크게 깎여 나간다.")
                    .withStyle(st -> st.withColor(0xFF3333).withItalic(true)));
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        if (result == WorkManager.Result.BAD) {
            lowerQliphoth(worker, 1);
        }
        String text = switch (result) {
            case GOOD -> "  검은 얼룩이 벽에서 천천히 흘러내린다. 아무것도 기억나지 않는다.";
            case NORMAL -> "  기록지가 새까맣게 타 버렸다. 무엇을 보았는지 떠오르지 않는다.";
            default -> "  검은 것이 고개를 돌렸다. 그 순간의 기억이 지워졌다.";
        };
        worker.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        if (level() instanceof ServerLevel sl) {
            for (CensoredMinionEntity m : sl.getEntitiesOfClass(CensoredMinionEntity.class,
                    getBoundingBox().inflate(256.0))) {
                m.discard();
            }
        }
    }

    // ───────────── 공격 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float dmg = 16f + getRandom().nextInt(3);
        boolean hit = target.hurtServer(level, level.damageSources().magic(), dmg);
        if (hit) {
            if (target instanceof ServerPlayer sp) Employees.damageSp(sp, Math.round(dmg));
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.2f, 0.7f);
            level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1.2, target.getZ(),
                    10, 0.3, 0.4, 0.3, 0.03);
        }
        return hit;
    }

    // ───────────── 직원 사망 → 하수인 ─────────────

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (!isBreached() || home == null) return;
        if (p.blockPosition().distSqr(home) > DEATH_RADIUS * DEATH_RADIUS) return;
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer o : sl.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(FEAR_RADIUS))) {
            if (!o.isAlive() || o.isCreative() || o.isSpectator()) continue;
            Employees.damageSp(o, 12 + getRandom().nextInt(6));
        }
        if (sl.getEntitiesOfClass(CensoredMinionEntity.class, getBoundingBox().inflate(256.0)).size() >= MAX_MINIONS) return;
        CensoredMinionEntity minion = kr.lobcorp.ModEntities.CENSORED_MINION.create(sl, EntitySpawnReason.EVENT);
        if (minion == null) return;
        minion.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        sl.addFreshEntity(minion);
        sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.03);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원의 시신에서 같은 모습의 하수인이 태어났습니다.")
                        .withStyle(st -> st.withColor(0xFF3333))));
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;
        if (breachTicks % FEAR_PERIOD != 0) return;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(FEAR_RADIUS))) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            EmployeeData d = Employees.get(p);
            if (d.prudence >= PRUDENCE_SAFE || d.isPanicking()) continue;
            Employees.panic(p);
        }
    }

    @Override
    public boolean isPushable() {
        return isBreached();
    }

    @Override
    public void push(Entity entity) {
        if (isBreached()) super.push(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}