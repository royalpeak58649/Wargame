package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O-07-103 양 — 눈이 하나뿐인 흰 잉어. 도구형이면서 탈출하는 환상체.
 * 사용한 직원은 양을 몸에 두르고, 1초마다 SP가 회복되며 WHITE 작업 피해가 줄어든다.
 * 착용 시간이 30초 쌓일 때마다 음의 카운터가 내려가고, 격리 중인 음에게 다가가면 둘이 만나 용이 지나간다.
 * 탈출은 음과 함께만 한다. 탈출하면 착용자에게서 떨어져 나온다.
 */
public class YangEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Boolean> TAKEN =
            SynchedEntityData.defineId(YangEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PENDANT =
            SynchedEntityData.defineId(YangEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.9f, 0.9f);
    private static final EntityDimensions PENDANT_SIZE = EntityDimensions.scalable(0.4f, 0.4f);

    /** 착용자가 격리 중인 음에게 이만큼 다가가면 합일이 일어난다 */
    private static final double DANGER_RADIUS = 4.0;
    /** 탈출 중 치유 파동 */
    private static final int HEAL_CD = 40;
    private static final double HEAL_RADIUS = 6.0;
    /** 조각 상태 */
    private static final int PENDANT_TICKS = 400;

    private int healCd = HEAL_CD;
    private int pendantT = 0;

    public YangEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.YANG;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAKEN, false);
        builder.define(PENDANT, false);
    }

    /** 누군가 양을 몸에 두르고 있으면 물고기 모델을 감춘다 */
    public boolean isTaken() {
        return entityData.get(TAKEN);
    }

    private void setTaken(boolean b) {
        entityData.set(TAKEN, b);
    }

    /** 조각(태극의 흰 반쪽) 상태인가 */
    public boolean isPendant() {
        return entityData.get(PENDANT);
    }

    private void setPendant(boolean b) {
        entityData.set(PENDANT, b);
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (isPendant()) return PENDANT_SIZE;
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected double chaseSpeed() {
        return 0.6;
    }

    @Override
    protected String breachAdvice() {
        return "느릿하게 떠다니며 직원의 상처를 아물게 합니다. 받은 피해는 정신력으로 되돌아옵니다.";
    }

    // ───────────── 사용 (도구) ─────────────

    @Override
    public void useTool(ServerPlayer p) {
        if (isBreached()) {
            p.sendOverlayMessage(Component.literal("양은 지금 격리실 밖에 있습니다.").withStyle(s -> s.withColor(0xFF5555)));
            return;
        }
        if (ToolState.yangHolder == null) {
            take(p);
            return;
        }
        if (ToolState.isYangHolder(p)) {
            giveBack(p);
            return;
        }
        p.sendOverlayMessage(Component.literal("누군가 양을 몸에 두르고 있습니다.").withStyle(s -> s.withColor(0xFF5555)));
    }

    private void take(ServerPlayer p) {
        ServerLevel sl = p.level();
        ToolState.yangHolder = p.getUUID();
        ToolState.yangTicks = 0;
        setTaken(true);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.2, 0.3, 0.05);
        p.sendSystemMessage(Component.literal("  흰 잉어가 몸을 감싸 안는다. 상처가 서늘하게 아문다.")
                .withStyle(s -> s.withColor(0xFFFFFF).withItalic(true)));
        p.sendSystemMessage(Component.literal("  (1초마다 SP +1, WHITE 작업 피해 감소 · 30초마다 음의 카운터 -1 · 격리 중인 음에게 다가가지 마십시오)")
                .withStyle(s -> s.withColor(0xAAAAAA)));
        Employees.sync(p);
    }

    private void giveBack(ServerPlayer p) {
        ServerLevel sl = p.level();
        ToolState.clearYang();
        setTaken(false);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f);
        p.sendSystemMessage(Component.literal("  흰 잉어가 손에서 미끄러져 물로 돌아간다.")
                .withStyle(s -> s.withColor(0x55FF55).withItalic(true)));
        Employees.sync(p);
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        healCd = HEAL_CD;
        pendantT = 0;
        setPendant(false);
        // 착용자에게서 떨어져 나온다
        if (ToolState.yangHolder != null) {
            ServerPlayer h = sl.getServer().getPlayerList().getPlayer(ToolState.yangHolder);
            ToolState.clearYang();
            setTaken(false);
            if (h != null) {
                h.sendSystemMessage(Component.literal("  몸을 감싸던 흰 잉어가 빠져나가 바닥으로 흘러내린다.")
                        .withStyle(s -> s.withColor(0xFF5555).withItalic(true)));
                Employees.sync(h);
            }
        }
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        healCd = HEAL_CD;
        pendantT = 0;
        setPendant(false);
        setTaken(false);
    }

    // ───────────── 조각 / 제압 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isPendant()) return false;
        boolean result = super.hurtServer(level, source, amount);
        if (isBreached() && result && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            int sp = Math.max(1, Math.round(amount));
            if (attacker instanceof ServerPlayer p) {
                Employees.damageSp(p, sp);
                p.sendSystemMessage(Component.literal("  흰 비늘이 빛나며 상처가 정신으로 되돌아온다.")
                        .withStyle(st -> st.withColor(0xFFFFFF).withItalic(true)));
            } else {
                attacker.hurtServer(level, level.damageSources().thorns(this), amount);
            }
            level.sendParticles(ParticleTypes.END_ROD, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 10, 0.4, 0.5, 0.4, 0.05);
        }
        return result;
    }

    @Override
    protected void suppress(Entity by) {
        if (isBreached() && !isPendant() && yinBreached(level().getServer())) {
            becomePendant();
            return;
        }
        super.suppress(by);
    }

    private void becomePendant() {
        setPendant(true);
        pendantT = PENDANT_TICKS;
        setHealth(1f);
        getNavigation().stop();
        setTarget(null);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.4, getZ(), 30, 0.4, 0.3, 0.4, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.2f, 1.6f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] O-07-103이 작은 흰 조각이 되었습니다. 아직 제압되지 않았습니다.")
                    .withStyle(st -> st.withColor(0xFFAA55)));
        }
    }

    private void revive() {
        setPendant(false);
        setHealth(getMaxHealth());
        pendantT = 0;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 40, 0.6, 0.5, 0.6, 0.03);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 1.0f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 흰 조각이 다시 잉어의 모습으로 일어섰습니다.")
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
    }

    private static boolean yinBreached(net.minecraft.server.MinecraftServer server) {
        if (server == null) return false;
        for (Entity e : server.overworld().getAllEntities()) {
            if (e instanceof YinEntity y && y.isAlive() && y.isBreached()) return true;
        }
        return false;
    }

    private static YinEntity findYin(ServerLevel sl) {
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof YinEntity y && y.isAlive()) return y;
        }
        return null;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;

        if (!isBreached()) {
            if (tickCount % 20 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 1, 0.3, 0.1, 0.3, 0);
            }
            // 착용자가 격리 중인 음에게 다가가면 둘이 만나고, 착용자는 그 자리에서 죽는다
            if (ToolState.yangHolder != null && tickCount % 5 == 0) {
                ServerPlayer h = sl.getServer().getPlayerList().getPlayer(ToolState.yangHolder);
                if (h != null && h.isAlive()) {
                    YinEntity yin = findYin(sl);
                    if (yin != null && !yin.isBreached() && h.distanceToSqr(yin) <= DANGER_RADIUS * DANGER_RADIUS) {
                        unionWithWearer(sl, yin, h);
                    }
                }
            }
            return;
        }

        if (isPendant()) {
            if (tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0.005);
            }
            if (--pendantT <= 0) {
                if (yinBreached(sl.getServer())) revive();
                else {
                    setPendant(false);
                    super.suppress(null);
                }
            }
            return;
        }

        if (tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 1, 0.3, 0.2, 0.3, 0.005);
        }

        // 음에게 다가간다
        YinEntity yin = findYin(sl);
        if (yin != null && yin.isBreached() && !yin.isPendant()) {
            getNavigation().moveTo(yin, 0.8);
            if (distanceToSqr(yin) <= 2.0 * 2.0) {
                YinEntity.union(sl, yin, this);
                return;
            }
        }

        // 치유 파동
        if (healCd > 0) healCd--;
        if (healCd <= 0) {
            healCd = HEAL_CD;
            heal(sl);
        }
    }

    private void heal(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 20, HEAL_RADIUS * 0.5, 0.4, HEAL_RADIUS * 0.5, 0.02);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > HEAL_RADIUS * HEAL_RADIUS) continue;
            p.heal(2f);
            var d = Employees.get(p);
            if (d.sp < d.maxSp()) d.sp = Math.min(d.maxSp(), d.sp + 2);
            Employees.sync(p);
        }
    }

    /** 착용자가 격리 중인 음에게 닿았다 */
    private void unionWithWearer(ServerLevel sl, YinEntity yin, ServerPlayer wearer) {
        ToolState.clearYang();
        setTaken(false);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(wearer.getDisplayName())
                .append(Component.literal(" 직원이 양을 두른 채 음에게 닿았습니다.").withStyle(st -> st.withColor(0xFF5555))));
        YinEntity.union(sl, yin, this);
        wearer.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_yang_taken", isTaken());
        out.putBoolean("lc_yang_pendant", isPendant());
        out.putInt("lc_yang_pendant_t", pendantT);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setTaken(in.getBooleanOr("lc_yang_taken", false));
        setPendant(in.getBooleanOr("lc_yang_pendant", false));
        pendantT = in.getIntOr("lc_yang_pendant_t", 0);
    }
}
