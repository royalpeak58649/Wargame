package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.DamageColor;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * O-06-20 아무것도 없는 — 사람의 가죽을 덧댄 붉은 살덩이.
 * 직원의 목소리를 흉내 내며 "안녕"이라고 말하고, 정의가 낮은 직원이 작업을 마치면 곧바로 탈출한다.
 * 탈출하면 1형태 → 알 → 완전체 순서로 다시 태어나며, 단계마다 내성과 크기가 달라진다.
 */
public class NothingThereEntity extends BreachingAbnormality {
    /** 0 = 격리 중/위장, 1 = 1형태, 2 = 알, 3 = 완전체 */
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(NothingThereEntity.class, EntityDataSerializers.INT);

    private static final EntityDimensions EGG_SIZE = EntityDimensions.scalable(1.2f, 1.2f);
    private static final EntityDimensions PERFECT_SIZE = EntityDimensions.scalable(1.4f, 3.0f);

    /** 1형태 / 알 지속 시간 (30초) */
    private static final int PHASE_TICKS = 600;
    /** 뼈 조각 경고 시간 */
    private static final int SHARD_WARN = 40;
    /** 뼈 조각 주기 */
    private static final int SHARD_PERIOD = 140;
    /** 뼈 조각 사거리 */
    private static final double SHARD_RANGE = 16.0;
    /** 뼈 조각 판정 반경 */
    private static final double SHARD_HIT_RADIUS = 1.2;

    private static final double BASE_SPEED = 0.32;
    private static final double PERFECT_SPEED = 0.24;

    private int phaseTicks = 0;
    private int deathsInCell = 0;
    /** 나쁨 결과로 죽은 직원의 이름 (탈출 시 흉내 낸다) */
    private String skinName = null;

    /** 뼈 조각 경고 중인 위치 (null이면 없음) */
    private Vec3 shardFrom = null;
    private Vec3 shardTo = null;
    private int shardTicks = 0;
    private int shardCooldown = 0;
    /** 마지막으로 피해를 입은 뒤 지난 틱 */
    private int sinceHurt = 0;
    private int regenTicks = 0;

    public NothingThereEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.NOTHING_THERE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 0);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    private void setPhase(int p) {
        entityData.set(PHASE, p);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PHASE.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return switch (phase()) {
            case 2 -> EGG_SIZE;
            case 3 -> PERFECT_SIZE;
            default -> super.getDefaultDimensions(pose);
        };
    }

    @Override
    protected String breachAdvice() {
        return "정의와 용기가 낮은 직원은 접근하지 마십시오. 알이 되면 30초 안에 제압하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        return super.workChanceBonus(worker, type) + (Employees.get(worker).fortitude < 85 ? -20 : 0);
    }

    @Override
    public void onWorkStart(ServerPlayer worker, WorkType type) {
        if (Employees.get(worker).fortitude < 65) {
            worker.sendSystemMessage(Component.literal("  문을 열자마자 붉은 살덩이가 고개를 돌린다. 숨이 막힌다.")
                    .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
            Employees.panic(worker);
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        if (result == WorkManager.Result.BAD) {
            skinName = worker.getName().getString();
            if (worker.level() instanceof ServerLevel sl) {
                worker.hurtServer(sl, sl.damageSources().magic(), 1000f);
            }
            Facility.broadcast(level().getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                    .append(Component.literal(skinName))
                    .append(Component.literal(" 직원이 O-06-20에게 삼켜졌습니다. 살덩이가 그 목소리를 흉내 내기 시작합니다.")
                            .withStyle(st -> st.withColor(0xFF5555))));
        } else {
            String text = switch (result) {
                case GOOD -> "  살덩이가 당신의 목소리로 \"안녕\"이라고 말한다. 등골이 서늘해진다.";
                case NORMAL -> "  붉은 살덩이가 조용히 숨을 쉰다. 가죽이 느리게 부풀었다가 가라앉는다.";
                default -> "  살덩이가 흉내 낸 목소리가 격리실 벽에 부딪혀 되돌아온다.";
            };
            worker.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
        if (Employees.get(worker).justice < 65) {
            lowerQliphoth(worker, 1);
        }
    }

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (isBreached() || home == null) return;
        if (p.blockPosition().distSqr(home) > 36) return;
        deathsInCell++;
        if (deathsInCell % 2 == 0) lowerQliphoth(null, 1);
    }

    @Override
    public void onAnyClerkDeath(ClerkEntity c) {
        if (isBreached() || home == null) return;
        if (c.blockPosition().distSqr(home) > 36) return;
        deathsInCell++;
        if (deathsInCell % 2 == 0) lowerQliphoth(null, 1);
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        setPhase(1);
        phaseTicks = 0;
        sinceHurt = 0;
        shardFrom = null;
        shardTo = null;
        shardTicks = 0;
        shardCooldown = 0;
        if (skinName != null && !skinName.isEmpty()) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 「" + skinName + "」 직원이 복도에 서 있습니다… 아니, 그것은 직원이 아닙니다.")
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setPhase(0);
        phaseTicks = 0;
        sinceHurt = 0;
        regenTicks = 0;
        shardFrom = null;
        shardTo = null;
        shardTicks = 0;
        shardCooldown = 0;
        setSpeed(BASE_SPEED);
        refreshDimensions();
    }

    private void setSpeed(double value) {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(value);
    }

    // ───────────── 단계별 내성 ─────────────

    @Override
    protected float resistMultiplier(DamageColor c) {
        return switch (phase()) {
            case 2 -> switch (c) {
                case RED -> 0.0f;
                case WHITE -> 0.6f;
                case BLACK -> 0.6f;
                case PALE -> 1.0f;
            };
            case 3 -> switch (c) {
                case RED -> 0.0f;
                case WHITE -> 0.4f;
                case BLACK -> 0.4f;
                case PALE -> 0.8f;
            };
            default -> super.resistMultiplier(c);
        };
    }

    // ───────────── 공격 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (phase() == 2) return false;
        boolean perfect = phase() == 3;
        float dmg;
        if (perfect) {
            dmg = getRandom().nextInt(10) == 0 ? 50f : 18f + getRandom().nextInt(6);
        } else {
            dmg = 12f + getRandom().nextInt(3);
        }
        boolean ok = target.hurtServer(level, level.damageSources().magic(), dmg);
        level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.2f, perfect ? 0.7f : 1.0f);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.2, target.getZ(),
                8, 0.3, 0.4, 0.3, 0.03);
        return ok;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            if (phase() != 0) setPhase(0);
            return;
        }
        sinceHurt++;
        switch (phase()) {
            case 1 -> {
                phaseTicks++;
                if (phaseTicks >= PHASE_TICKS) {
                    setPhase(2);
                    phaseTicks = 0;
                    setHealth(getMaxHealth());
                    getNavigation().stop();
                    Facility.broadcast(sl.getServer(), Component.literal("[관제] O-06-20이 알이 되었습니다. 30초 안에 제압하십시오.")
                            .withStyle(st -> st.withColor(0xFF5555)));
                }
            }
            case 2 -> {
                phaseTicks++;
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y, 0);
                if (phaseTicks % 20 == 0) {
                    sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.8, getZ(), 4, 0.4, 0.3, 0.4, 0.01);
                    sl.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4f, 0.8f);
                }
                if (phaseTicks >= PHASE_TICKS) {
                    setPhase(3);
                    phaseTicks = 0;
                    setHealth(getMaxHealth());
                    setSpeed(PERFECT_SPEED);
                    Facility.broadcast(sl.getServer(), Component.literal("[관제] O-06-20이 완전한 형태로 다시 태어났습니다.")
                            .withStyle(st -> st.withColor(0xFF3333).withBold(true)));
                }
            }
            case 3 -> {
                phaseTicks++;
                tickShard(sl);
                tickRegen(sl);
            }
            default -> {
            }
        }
    }

    /** 뼈 조각: 40틱 경고 후 16블록 직선 위의 직원을 꿰뚫는다 */
    private void tickShard(ServerLevel sl) {
        if (shardTicks > 0) {
            shardTicks--;
            if (shardFrom != null && shardTo != null) {
                Vec3 dir = shardTo.subtract(shardFrom);
                double len = dir.length();
                if (len > 1.0E-4) {
                    Vec3 d = dir.scale(1.0 / len);
                    for (double t = 0; t <= len; t += 1.0) {
                        Vec3 at = shardFrom.add(d.scale(t));
                        sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
            }
            if (shardTicks == 0) fireShard(sl);
            return;
        }
        if (shardCooldown > 0) {
            shardCooldown--;
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceTo(target) > SHARD_RANGE) return;
        shardFrom = new Vec3(getX(), getY() + 1.2, getZ());
        shardTo = new Vec3(target.getX(), target.getY() + 1.0, target.getZ());
        shardTicks = SHARD_WARN;
        shardCooldown = SHARD_PERIOD;
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SNIFF, SoundSource.HOSTILE, 1.2f, 0.7f);
    }

    private void fireShard(ServerLevel sl) {
        if (shardFrom == null || shardTo == null) return;
        Vec3 from = shardFrom;
        Vec3 to = shardTo;
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len > 1.0E-4) {
            Vec3 d = dir.scale(1.0 / len);
            for (double t = 0; t <= len; t += 0.5) {
                Vec3 at = from.add(d.scale(t));
                sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.02);
            }
            AABB box = new AABB(from, to).inflate(SHARD_HIT_RADIUS + 1.0);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
                if (distanceToLine(from, to, p.position().add(0, 1, 0)) > SHARD_HIT_RADIUS) continue;
                p.hurtServer(sl, sl.damageSources().magic(), 32f);
                p.sendSystemMessage(Component.literal("  뼈 조각이 허공을 꿰뚫고 지나갔다.")
                        .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
            }
        }
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.4f, 0.8f);
        shardFrom = null;
        shardTo = null;
    }

    /** 점 p에서 선분 ab까지의 거리 */
    private static double distanceToLine(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double lenSqr = ab.lengthSqr();
        if (lenSqr < 1.0E-6) return p.distanceTo(a);
        double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / lenSqr));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** 재생: 15초 동안 맞지 않으면 초당 1, 체력이 30% 미만이면 초당 2 */
    private void tickRegen(ServerLevel sl) {
        boolean low = getHealth() < getMaxHealth() * 0.3f;
        if (!low && sinceHurt < 300) {
            regenTicks = 0;
            return;
        }
        if (++regenTicks < 20) return;
        regenTicks = 0;
        if (getHealth() < getMaxHealth()) {
            setHealth(Math.min(getMaxHealth(), getHealth() + (low ? 2f : 1f)));
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.6, getZ(), 2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        sinceHurt = 0;
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_nt_phase", phase());
        out.putInt("lc_nt_phase_ticks", phaseTicks);
        out.putInt("lc_nt_deaths", deathsInCell);
        out.putString("lc_nt_skin", skinName == null ? "" : skinName);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setPhase(in.getIntOr("lc_nt_phase", 0));
        phaseTicks = in.getIntOr("lc_nt_phase_ticks", 0);
        deathsInCell = in.getIntOr("lc_nt_deaths", 0);
        String skin = in.getStringOr("lc_nt_skin", "");
        skinName = skin.isEmpty() ? null : skin;
        setSpeed(phase() == 3 ? PERFECT_SPEED : BASE_SPEED);
    }

    /** 디버그용 */
    public String phaseDebug() {
        return "phase=" + phase() + " t=" + phaseTicks + " deaths=" + deathsInCell;
    }

    @Override
    public boolean isPushable() {
        return isBreached() && phase() != 2;
    }

    @Override
    public void push(Entity entity) {
        if (isBreached() && phase() != 2) super.push(entity);
    }
}