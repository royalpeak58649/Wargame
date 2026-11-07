package kr.lobcorp.entity;

import kr.lobcorp.DamageColor;
import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 클리포트 카운터가 0이 되면 탈출하는 환상체의 공통 기반.
 * 탈출 → 문 열기 + 경보 + 보스바 → 가장 가까운 직원 추격(벽 무시) → 체력 0이면 죽지 않고 제압되어 격리실로 복귀.
 */
public abstract class BreachingAbnormality extends AbnormalityEntity {
    private static final EntityDataAccessor<Boolean> BREACHED =
            SynchedEntityData.defineId(BreachingAbnormality.class, EntityDataSerializers.BOOLEAN);
    /** 고용/임무 등으로 적대하지 않고 격리실을 나선 상태 (탈출 판정은 유지) */
    private static final EntityDataAccessor<Boolean> FRIENDLY =
            SynchedEntityData.defineId(BreachingAbnormality.class, EntityDataSerializers.BOOLEAN);

    public static final float NON_EGO_MULT = 0.5f;
    protected static final int BREACH_DELAY = 60;

    protected int breachCountdown = -1;
    protected int breachTicks = 0;
    private ServerBossEvent bossBar;

    protected BreachingAbnormality(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BREACHED, false);
        builder.define(FRIENDLY, false);
    }

    /** 적대하지 않는 탈출 상태인가 (빨간 두건 용병의 고용 등) */
    public boolean isFriendly() {
        return entityData.get(FRIENDLY);
    }

    public void setFriendly(boolean b) {
        entityData.set(FRIENDLY, b);
    }

    @Override
    public boolean isBreached() {
        return entityData.get(BREACHED);
    }

    protected void setBreached(boolean b) {
        entityData.set(BREACHED, b);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (BREACHED.equals(key)) refreshDimensions();
    }

    /** 탈출 중 이동 속도 배율 (MeleeAttackGoal) */
    protected double chaseSpeed() {
        return 1.0;
    }

    /** 지상 추격 대신 직접 비행/조종하는 환상체는 false. (registerGoals는 생성자에서 돌므로 인스턴스 필드에 의존하면 안 된다) */
    protected boolean usesMeleeGoal() {
        return true;
    }

    @Override
    protected void registerGoals() {
        if (usesMeleeGoal()) {
            goalSelector.addGoal(1, new MeleeAttackGoal(this, chaseSpeed(), true) {
                @Override
                public boolean canUse() {
                    return isBreached() && !isFriendly() && super.canUse();
                }

                @Override
                public boolean canContinueToUse() {
                    return isBreached() && !isFriendly() && super.canContinueToUse();
                }
            });
        }
        // 벽 너머의 직원도 찾아낸다 (시야 무시).
        // 주의: Mob의 Goal 판정은 엔티티 ID에 따라 격틱으로만 돌기 때문에 tickCount 모듈러를 쓰면 안 된다.
        targetSelector.addGoal(1, new Goal() {
            private final TargetingConditions cond = TargetingConditions.forCombat().range(40).ignoreLineOfSight();

            @Override
            public boolean canUse() {
                if (!isBreached() || isFriendly() || getRandom().nextInt(4) != 0) return false;
                LivingEntity cur = getTarget();
                return cur == null || !cur.isAlive() || (cur instanceof Player pl && (pl.isCreative() || pl.isSpectator()));
            }

            @Override
            public void start() {
                if (level() instanceof ServerLevel sl) {
                    Player found = sl.getNearestPlayer(cond, BreachingAbnormality.this);
                    if (found instanceof ServerPlayer sp && ToolState.isSheltered(sp)) found = null;
                    if (found instanceof ServerPlayer sp2 && sp2.isCreative()) found = null;
                    setTarget(found);
                }
            }

            @Override
            public boolean canContinueToUse() {
                return false;
            }
        });
    }

    // ───────────── 클리포트 카운터 ─────────────

    /** 카운터를 내리고, 0이 되면 탈출 카운트다운을 시작한다 */
    public void lowerQliphoth(ServerPlayer by, int amount) {
        qliphoth = Math.max(0, qliphoth - amount);
        if (by != null) {
            by.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
        if (qliphoth <= 0 && breachCountdown < 0 && !isBreached()) {
            breachCountdown = BREACH_DELAY;
            Facility.broadcast(level().getServer(), Component.literal("[관제] 경고: " + profile().code()
                    + " 클리포트 카운터 0. 격리 붕괴가 임박했습니다!").withStyle(st -> st.withColor(0xFF5555).withBold(true)));
        }
    }

    // ───────────── 탈출 ─────────────

    public void breach() {
        if (!(level() instanceof ServerLevel sl) || isBreached()) return;
        breachCountdown = -1;
        breachTicks = 0;
        setBreached(true);
        openDoorsNearHome(sl);
        bossBar = newBossBar();
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(profile().name() + "가 격리실을 탈출했습니다")
                    .withStyle(st -> st.withColor(0xFFAAAA))));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("격리 실패").withStyle(st -> st.withColor(0xFF5555).withBold(true))));
            p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1f, 1f);
        }
        Facility.broadcast(sl.getServer(), Component.literal("[관제] " + profile().code() + " 탈출. " + breachAdvice())
                .withStyle(st -> st.withColor(0xFF5555)));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.5, 0.5, 0.5, 0.02);
        onBreach(sl);
        kr.lobcorp.work.AbnoEvents.breach(this);
    }

    protected String breachAdvice() {
        return "전 직원은 E.G.O를 장비하고 제압하십시오.";
    }

    protected void onBreach(ServerLevel sl) {}

    private ServerBossEvent newBossBar() {
        return new ServerBossEvent(getUUID(), Component.literal(profile().name() + "  " + profile().code()),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    }

    private void openDoorsNearHome(ServerLevel sl) {
        BlockPos c = home != null ? home : blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-10, -1, -6), c.offset(4, 3, 6))) {
            BlockState st = sl.getBlockState(pos);
            if (st.getBlock() instanceof DoorBlock door && !st.getValue(DoorBlock.OPEN)) {
                door.setOpen(this, sl, st, pos.immutable(), true);
            }
        }
    }

    // ───────────── 격리실 복귀 (제압/자폭 공통) ─────────────

    /** 탈출 상태를 끝내고 격리실로 되돌린다. 카운터 복구. */
    protected void returnToContainment(MutableComponent announcement) {
        if (!(level() instanceof ServerLevel sl)) return;
        setBreached(false);
        breachCountdown = -1;
        setTarget(null);
        getNavigation().stop();
        setHealth(getMaxHealth());
        resetQliphoth();
        if (home != null) snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, homeYaw, 0);
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1f, 0.8f);
        if (announcement != null) Facility.broadcast(sl.getServer(), announcement);
    }

    protected void suppress(Entity by) {
        var msg = Component.literal("[관제] " + profile().code() + " 제압 완료. ").withStyle(st -> st.withColor(0x55FF55));
        if (by instanceof Player pl) {
            msg.append(pl.getDisplayName()).append(Component.literal(" 직원의 활약으로 ").withStyle(st -> st.withColor(0xAAAAAA)));
        }
        msg.append(Component.literal("격리실에 재수용되었습니다.").withStyle(st -> st.withColor(0xAAAAAA)));
        returnToContainment(msg);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isBreached()) return super.hurtServer(level, source, amount);
        float mult = NON_EGO_MULT;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) mult = profile().resistTo(ego.color);
        }
        float dmg = amount * mult;
        if (getHealth() - dmg <= 0) {
            suppress(source.getEntity());
            return true;
        }
        return super.hurtServer(level, source, dmg);
    }

    /**
     * E.G.O 피해를 저항 배율만 적용해 입힌다 (비 E.G.O 절반 감소 없음).
     * 빨간 두건 용병의 고용 임무 등에서 사용.
     */
    public void takeEgoDamage(DamageColor color, float amount, Entity source) {
        if (!(level() instanceof ServerLevel sl)) return;
        float dmg = amount * profile().resistTo(color);
        if (getHealth() - dmg <= 0) {
            suppress(source);
            return;
        }
        super.hurtServer(sl, sl.damageSources().mobAttack(source instanceof LivingEntity le ? le : this), dmg);
    }

    @Override
    public void die(DamageSource source) {
        // 환상체는 죽지 않는다 — 제압되어 격리실로 돌아갈 뿐
        suppress(source.getEntity());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (breachCountdown > 0) {
            if (breachCountdown % 20 == 0) onBreachWarning(sl);
            if (--breachCountdown == 0) breach();
        }
        if (isBreached()) {
            breachTicks++;
            // 대피소로 숨은 직원은 추격 대상에서 제외된다
            if (getTarget() instanceof ServerPlayer tp && ToolState.isSheltered(tp)) setTarget(null);
            if (bossBar == null) bossBar = newBossBar(); // 서버 재시작 직후 등
            bossBar.setProgress(getHealth() / getMaxHealth());
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(this) < 48) bossBar.addPlayer(p);
                else bossBar.removePlayer(p);
            }
        }
    }

    /** 탈출 직전 1초마다 */
    protected void onBreachWarning(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 1.2f, 0.6f);
        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2, getZ(), 2, 0.2, 0.1, 0.2, 0);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (bossBar != null) bossBar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_breached", isBreached());
        out.putBoolean("lc_friendly", isFriendly());
        out.putInt("lc_breach_cd", breachCountdown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setBreached(in.getBooleanOr("lc_breached", false));
        setFriendly(in.getBooleanOr("lc_friendly", false));
        breachCountdown = in.getIntOr("lc_breach_cd", -1);
    }
}
