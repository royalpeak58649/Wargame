package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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

import java.util.HashMap;
import java.util.Map;

/**
 * F-02-70 흑조의 꿈 — 호숫가에 선 여인과 저주받은 오라비 여섯 마리.
 * 보통·나쁨 결과, 직원의 죽음·패닉, 다른 환상체의 탈출이 겹칠 때마다 백조가 한 마리씩 검게 물든다.
 * 여섯 마리가 모두 물들면 여동생이 탈출한다. 양산은 정면의 공격을 되돌려 보낸다.
 */
public class BlackSwanEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Integer> CHANGED =
            SynchedEntityData.defineId(BlackSwanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRANSFORMED =
            SynchedEntityData.defineId(BlackSwanEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 2.0f);
    private static final double DEFLECT_ANGLE = 70.0;
    private static final double CONTAINMENT_RADIUS = 6.0;
    private static final int CONTAINMENT_COOLDOWN = 1200;
    private static final int SCREECH_INTERVAL = 300;
    private static final double SCREECH_RADIUS = 16.0;

    private int deaths = 0;
    private int panics = 0;
    private int breaches = 0;
    private int screechT = 0;
    private final Map<Integer, Long> touched = new HashMap<>();

    public BlackSwanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.BLACK_SWAN;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHANGED, 0);
        builder.define(TRANSFORMED, false);
    }

    public int getChanged() {
        return entityData.get(CHANGED);
    }

    public boolean isTransformed() {
        return entityData.get(TRANSFORMED);
    }

    private void setTransformed(boolean b) {
        entityData.set(TRANSFORMED, b);
    }

    @Override
    protected double chaseSpeed() {
        return 0.9;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "양산이 정면의 공격을 되돌려 보냅니다. 뒤나 옆에서 때리십시오.";
    }

    @Override
    public void resetQliphoth() {
        super.resetQliphoth();
        deaths = 0;
        panics = 0;
        breaches = 0;
        screechT = 0;
        touched.clear();
        entityData.set(CHANGED, 0);
        setTransformed(false);
    }

    // ───────────── 백조가 물드는 조건 ─────────────

    /** 카운터를 내리고, 백조 한 마리를 검게 물들인다 */
    private void darken(ServerPlayer by) {
        lowerQliphoth(by, 1);
        int changed = Math.max(0, Math.min(6, profile().qliphothMax() - qliphoth));
        entityData.set(CHANGED, changed);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.4, getZ(), 12, 1.6, 0.2, 1.6, 0.01);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(this) <= 24) {
                    p.sendSystemMessage(Component.literal("  호숫가의 백조 한 마리가 검게 물들었다.")
                            .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                }
            }
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  여인은 검은 양산을 천천히 돌린다. 호수 위의 백조들이 날개를 접는다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  백조 한 마리가 물 위로 떠오르다 이내 가라앉는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                darken(p);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  양산 끝에서 검은 물방울이 뚝뚝 떨어진다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                darken(p);
            }
        }
    }

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (isBreached()) return;
        deaths++;
        if (deaths % 5 == 0) darken(null);
    }

    @Override
    public void onAnyPanic(ServerPlayer p) {
        if (isBreached()) return;
        panics++;
        if (panics % 5 == 0) darken(null);
    }

    @Override
    public void onAnyClerkDeath(ClerkEntity c) {
        if (isBreached()) return;
        deaths++;
        if (deaths % 5 == 0) darken(null);
    }

    @Override
    public void onAnyClerkPanic(ClerkEntity c) {
        if (isBreached()) return;
        panics++;
        if (panics % 5 == 0) darken(null);
    }

    @Override
    public void onAnyBreach(BreachingAbnormality who) {
        if (isBreached()) return;
        breaches++;
        if (breaches % 3 == 0) darken(null);
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        screechT = 0;
        touched.clear();
        entityData.set(CHANGED, 6);
        sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.5f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] F-02-70 여동생이 모습을 드러냈습니다. 양산을 정면으로 마주하지 마십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(net.minecraft.network.chat.MutableComponent announcement) {
        super.returnToContainment(announcement);
        setTransformed(false);
        screechT = 0;
        touched.clear();
        entityData.set(CHANGED, 0);
    }

    // ───────────── 양산 방어 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached() && source.getEntity() instanceof LivingEntity attacker) {
            Vec3 toAttacker = attacker.position().subtract(position());
            Vec3 flat = new Vec3(toAttacker.x, 0, toAttacker.z);
            if (flat.lengthSqr() > 1.0E-4) {
                Vec3 facing = new Vec3(-Math.sin(Math.toRadians(getYRot())), 0, Math.cos(Math.toRadians(getYRot())));
                double dot = facing.normalize().dot(flat.normalize());
                double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
                if (angle < DEFLECT_ANGLE) {
                    attacker.hurtServer(level, level.damageSources().thorns(this), amount);
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 1.4, getZ(), 10, 0.4, 0.5, 0.4, 0.05);
                    level.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1f, 1.2f);
                    return false;
                }
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity le) {
            le.hurtServer(level, level.damageSources().mobAttack(this), 5 + getRandom().nextInt(4));
            level.sendParticles(ParticleTypes.SQUID_INK, le.getX(), le.getY() + 1.2, le.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
            return true;
        }
        return super.doHurtTarget(level, target);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        if (tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.8, getZ(), 1, 0.3, 0.2, 0.3, 0.005);
        }

        if (tickCount % 20 == 0) {
            for (Entity e : sl.getAllEntities()) {
                if (!(e instanceof BreachingAbnormality other) || other == this) continue;
                if (other.isBreached() || !other.isAlive()) continue;
                if (distanceToSqr(other) > CONTAINMENT_RADIUS * CONTAINMENT_RADIUS) continue;
                long now = sl.getGameTime();
                Long last = touched.get(other.getId());
                if (last != null && now - last < CONTAINMENT_COOLDOWN) continue;
                touched.put(other.getId(), now);
                other.lowerQliphoth(null, 1);
                sl.sendParticles(ParticleTypes.SQUID_INK, other.getX(), other.getY() + 1.2, other.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
            }
        }

        if (!isTransformed() && getHealth() < getMaxHealth() * 0.5f) {
            setTransformed(true);
            sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.5f, 0.4f);
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.6, getZ(), 40, 0.8, 0.8, 0.8, 0.05);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] F-02-70의 등 뒤에서 검은 날개가 펼쳐집니다. 비명이 곧 시설을 덮칩니다.")
                    .withStyle(st -> st.withColor(0xFF5555)));
        }

        if (isTransformed()) {
            screechT++;
            if (screechT >= SCREECH_INTERVAL) {
                screechT = 0;
                screech(sl);
            }
        }
    }

    private void screech(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 1.5f, 0.5f);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.6, getZ(), 1, 0, 0, 0, 0);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > SCREECH_RADIUS * SCREECH_RADIUS) continue;
            Employees.damageSp(p, 15 + getRandom().nextInt(8));
            p.sendSystemMessage(Component.literal("  검은 날개가 부딪히며 비명이 터져 나온다.")
                    .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_swan_deaths", deaths);
        out.putInt("lc_swan_panics", panics);
        out.putInt("lc_swan_breaches", breaches);
        out.putInt("lc_swan_changed", getChanged());
        out.putBoolean("lc_swan_transformed", isTransformed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        deaths = in.getIntOr("lc_swan_deaths", 0);
        panics = in.getIntOr("lc_swan_panics", 0);
        breaches = in.getIntOr("lc_swan_breaches", 0);
        entityData.set(CHANGED, in.getIntOr("lc_swan_changed", 0));
        setTransformed(in.getBooleanOr("lc_swan_transformed", false));
    }
}
