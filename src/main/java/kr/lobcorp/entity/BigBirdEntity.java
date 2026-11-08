package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.AbnoEvents;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * O-02-40 큰 새 — 온몸이 노란 눈으로 뒤덮인 거대한 검은 새.
 * 탈출하면 주변을 어둡게 만들고 느릿하게 배회한다. 20초마다 등불을 흔들어
 * 살아 있는 직원 중 최대 HP가 가장 낮은 자를 표시하고, 두 번 흔들면 그 직원을 불러들여 머리를 물어뜯는다.
 */
public class BigBirdEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.8f, 3.0f);

    private static final int SHAKE_INTERVAL = 400;   // 20초
    private static final int MARK_TICKS = 900;       // 45초
    private static final double MARK_RADIUS = 24.0;
    private static final double EAT_RADIUS = 2.0;
    private static final int EAT_TICKS = 20;

    private UUID marked = null;
    private int lampT = 0;
    private int shakes = 0;
    private int eatT = 0;
    private int deathTally = 0;

    public BigBirdEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.BIG_BIRD;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 탈출했을 때만 느릿하게 배회한다. 누구도 쫓지 않는다.
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8, 10) {
            @Override
            public boolean canUse() {
                return isBreached() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isBreached() && super.canContinueToUse();
            }
        });
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "큰 새는 아무도 쫓지 않습니다. 등불이 비추는 직원은 40초 뒤 이끌려 가니, 그 전에 제압하십시오.";
    }

    @Override
    public void resetQliphoth() {
        super.resetQliphoth();
        deathTally = 0;
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> {
                qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
                p.sendSystemMessage(Component.literal("  수십 개의 눈이 한꺼번에 깜빡인다. 등불이 조금 밝아졌다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
            case NORMAL -> p.sendSystemMessage(Component.literal("  큰 새는 목을 길게 늘어뜨린 채 조용히 서 있다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  새장 같은 부리가 딱딱거린다. 등불의 불꽃이 흔들렸다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
    }

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (isBreached()) return;
        deathTally++;
        if (deathTally % 5 == 0) {
            ServerPlayer any = null;
            if (level() instanceof ServerLevel sl && !sl.players().isEmpty()) any = sl.players().get(0);
            lowerQliphoth(any, 1);
        }
    }

    @Override
    public void onAnyClerkDeath(ClerkEntity c) {
        if (isBreached()) return;
        deathTally++;
        if (deathTally % 5 == 0) {
            ServerPlayer any = null;
            if (level() instanceof ServerLevel sl && !sl.players().isEmpty()) any = sl.players().get(0);
            lowerQliphoth(any, 1);
        }
    }

    @Override
    public void onAnyBreach(BreachingAbnormality who) {
        if (isBreached() || level().getServer() == null) return;
        if (AbnoEvents.breachedCount(level().getServer()) >= 3) {
            breach();
        }
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        lampT = 0;
        shakes = 0;
        eatT = 0;
        marked = null;
        sl.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.5f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-02-40이 등불을 들고 나왔습니다. 빛이 닿는 곳에서 벗어나십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(net.minecraft.network.chat.MutableComponent announcement) {
        super.returnToContainment(announcement);
        marked = null;
        shakes = 0;
        eatT = 0;
        lampT = 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (eatT > 0) return false;
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        if (eatT > 0) eatT--;

        // 주변을 어둡게
        if (tickCount % 4 == 0) {
            for (ServerPlayer p : sl.players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                if (p.distanceToSqr(this) > 16 * 16) continue;
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            }
        }

        // 등불 연출
        if (tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.4, getZ(), 1, 0.15, 0.1, 0.15, 0.005);
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 2.4, getZ(), 1, 0.2, 0.15, 0.2, 0.005);
        }

        lampT++;
        if (lampT >= SHAKE_INTERVAL) {
            lampT = 0;
            shakeLamp(sl);
        }

        ServerPlayer target = markedPlayer(sl);
        if (marked != null && target == null) {
            clearMark();
        }
        if (target != null && shakes >= 2) {
            lure(sl, target);
        }
    }

    private void shakeLamp(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.4, getZ(), 20, 0.4, 0.3, 0.4, 0.02);
        sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 2.4, getZ(), 20, 0.5, 0.4, 0.5, 0.02);

        if (marked == null) {
            ServerPlayer best = null;
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                if (p.distanceToSqr(this) > MARK_RADIUS * MARK_RADIUS) continue;
                if (best == null || p.getMaxHealth() < best.getMaxHealth()) best = p;
            }
            if (best == null) return;
            marked = best.getUUID();
            shakes = 0;
            best.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_TICKS, 0, false, false));
            best.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MARK_TICKS, 1, false, false));
            best.sendSystemMessage(Component.literal("  흔들리는 등불이 당신을 비춘다…")
                    .withStyle(st -> st.withColor(0xFFD040).withItalic(true)));
            Facility.broadcast(sl.getServer(), Component.literal("[관제] O-02-40의 등불이 ").withStyle(st -> st.withColor(0xAA0000))
                    .append(best.getDisplayName())
                    .append(Component.literal(" 직원을 비추고 있습니다.").withStyle(st -> st.withColor(0xFF5555))));
        }
        if (marked != null) shakes++;
    }

    private ServerPlayer markedPlayer(ServerLevel sl) {
        if (marked == null) return null;
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(marked);
        if (p == null || !p.isAlive() || p.isCreative() || p.isSpectator()) return null;
        if (p.distanceToSqr(this) > MARK_RADIUS * MARK_RADIUS) return null;
        return p;
    }

    private void clearMark() {
        marked = null;
        shakes = 0;
    }

    private void lure(ServerLevel sl, ServerPlayer p) {
        if (eatT > 0) return;
        double dist = p.distanceTo(this);
        if (dist <= EAT_RADIUS) {
            eat(sl, p);
            return;
        }
        Vec3 diff = position().subtract(p.position());
        Vec3 horiz = new Vec3(diff.x, 0, diff.z);
        if (horiz.lengthSqr() > 1.0E-4) horiz = horiz.normalize().scale(0.25);
        double vy = p.getDeltaMovement().y;
        if (p.onGround() && getY() - p.getY() > 1.0) vy = 0.42;
        kr.lobcorp.work.Employees.push(p, horiz.x, vy, horiz.z);
        WorkManager.actionbar(p, Component.literal("등불이 당신을 부른다…").withStyle(st -> st.withColor(0xFFD040)));
    }

    private void eat(ServerLevel sl, ServerPlayer p) {
        eatT = EAT_TICKS;
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 1.5f, 0.7f);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.6, p.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 큰 새에게 이끌려 갔습니다.").withStyle(st -> st.withColor(0xAA0000))));
        clearMark();
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_lamp_t", lampT);
        out.putInt("lc_shakes", shakes);
        out.putInt("lc_eat", eatT);
        out.putInt("lc_death_tally", deathTally);
        if (marked != null) out.store("lc_marked", UUIDUtil.CODEC, marked);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        lampT = in.getIntOr("lc_lamp_t", 0);
        shakes = in.getIntOr("lc_shakes", 0);
        eatT = in.getIntOr("lc_eat", 0);
        deathTally = in.getIntOr("lc_death_tally", 0);
        marked = in.read("lc_marked", UUIDUtil.CODEC).orElse(null);
    }
}
