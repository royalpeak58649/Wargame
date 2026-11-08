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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O-02-74 벌거벗은 둥지 — 살점으로 된 둥지. 구멍마다 창백한 뱀이 고개를 내민다.
 * 탈출하지 않고 클리포트 카운터도 없다. 대신 작업 중 잃은 HP에 비례해 직원을 감염시킨다.
 */
public class NakedNestEntity extends AbnormalityEntity {
    /** 감염 1단계가 끝나는 틱 (갈색 입자) */
    public static final int STAGE2_TICKS = 240;
    /** 감염 2단계가 끝나는 틱 (죽음) */
    public static final int DEATH_TICKS = 960;
    /** 감염이 옮겨 가는 주기 */
    public static final int SPREAD_INTERVAL = 100;
    public static final double SPREAD_RADIUS = 4.0;

    /** 작업 시작 시점의 작업자 HP (저장 안 함) */
    private final Map<UUID, Float> startHealth = new HashMap<>();

    public NakedNestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.NAKED_NEST;
    }

    // ───────────── 감염 ─────────────

    @Override
    public void onWorkStart(ServerPlayer worker, WorkType type) {
        startHealth.put(worker.getUUID(), worker.getHealth());
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        Float start = startHealth.remove(p.getUUID());
        if (start == null) return;
        int base = switch (result) {
            case GOOD -> 0;
            case NORMAL -> 30;
            case BAD -> 60;
        };
        float lost = Math.max(0f, start - p.getHealth());
        int chance = (int) (base * lost / p.getMaxHealth());
        if (getRandom().nextInt(100) < chance) {
            infect(p);
        }
    }

    /** 직원을 감염시킨다 (이미 감염되어 있으면 아무 일도 없다) */
    public void infect(ServerPlayer p) {
        EmployeeData d = Employees.get(p);
        if (d.nestInfectTicks != 0) return;
        d.nestInfectTicks = 1;
        p.sendSystemMessage(Component.literal("  둥지의 구멍에서 흘러나온 창백한 뱀이 목덜미를 파고든다.")
                .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            sl.playSound(null, p.blockPosition(), SoundEvents.SQUID_AMBIENT, SoundSource.HOSTILE, 1.0f, 0.6f);
        }
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.2, getZ(), 2, 0.7, 0.5, 0.7, 0.005);
        }
        if (tickCount % 20 != 0) return;
        for (ServerPlayer p : sl.players()) {
            EmployeeData d = Employees.get(p);
            if (d.nestInfectTicks <= 0) continue;
            if (!p.isAlive()) {
                d.nestInfectTicks = 0;
                continue;
            }
            d.nestInfectTicks += 20;
            tickInfection(sl, p, d);
        }
    }

    private void tickInfection(ServerLevel sl, ServerPlayer p, EmployeeData d) {
        if (d.nestInfectTicks >= DEATH_TICKS) {
            d.nestInfectTicks = 0;
            sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
            p.hurtServer(sl, sl.damageSources().magic(), 1000f);
            NakedWormEntity.spawnFrom(p);
            return;
        }
        if (d.nestInfectTicks >= STAGE2_TICKS) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
            sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.4, 0.6, 0.4, 0.01);
        } else {
            sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
        }
        if (d.nestInfectTicks % SPREAD_INTERVAL == 0 && getRandom().nextInt(100) < 30) {
            spread(sl, p);
        }
    }

    /** 감염된 직원의 곁에 있는 다른 직원 한 명에게 뱀이 옮겨 간다 */
    private void spread(ServerLevel sl, ServerPlayer from) {
        for (ServerPlayer other : sl.players()) {
            if (other == from) continue;
            if (other.isCreative() || other.isSpectator() || !other.isAlive()) continue;
            if (Employees.get(other).nestInfectTicks != 0) continue;
            if (other.distanceToSqr(from.getX(), from.getY(), from.getZ()) > SPREAD_RADIUS * SPREAD_RADIUS) continue;
            infect(other);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] O-02-74의 뱀이 ").withStyle(st -> st.withColor(0xAA0000))
                    .append(other.getDisplayName())
                    .append(Component.literal(" 직원에게 옮겨 갔습니다.").withStyle(st -> st.withColor(0xAA55FF))));
            return;
        }
    }
}
