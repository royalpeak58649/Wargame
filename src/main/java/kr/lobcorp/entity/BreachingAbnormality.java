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
    /** 격리실 문 밖 복도 중앙 (탈출 시 큰 히트박스가 문을 통과할 수 없어 순간이동에 사용) */
    private BlockPos exitPos = null;
    /** 플레이어별 누적 위협도 (어그로) */
    private final java.util.Map<java.util.UUID, Float> threat = new java.util.HashMap<>();
    private int retargetIn = 100;
    private int targetTicks = 0;

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
                    // 사무직이 더 가까우면 사무직을 노린다
                    ClerkEntity clerk = nearestClerk(sl, 24.0);
                    if (clerk != null && (found == null || distanceTo(clerk) < distanceTo(found))) {
                        setTarget(clerk);
                        return;
                    }
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

    /** 클리포트 폭주를 막지 못했다: 카운터를 완전히 소진시킨다. */
    public void meltdownFail() {
        lowerQliphoth(null, qliphoth);
    }

    // ───────────── 탈출 ─────────────

    public void breach() {
        if (!(level() instanceof ServerLevel sl) || isBreached()) return;
        breachCountdown = -1;
        breachTicks = 0;
        setBreached(true);
        openDoorsNearHome(sl);
        if (leavesRoomOnBreach() && (getBbWidth() >= 0.95f || getBbHeight() >= 1.95f)) moveToExit();
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
            if (st.getBlock() instanceof DoorBlock door) {
                if (st.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                    // 문 서쪽 2블록 = 복도 중앙
                    exitPos = pos.immutable().offset(-2, 0, 0);
                }
                if (!st.getValue(DoorBlock.OPEN)) {
                    door.setOpen(this, sl, st, pos.immutable(), true);
                }
            }
        }
    }

    /** 탈출 시 격리실을 나가는가 (방 안에서 싸우는 환상체는 false) */
    protected boolean leavesRoomOnBreach() {
        return true;
    }

    /** 문이 좁아 지나갈 수 없는 히트박스를 복도 중앙으로 옮긴다 */
    protected void moveToExit() {
        if (exitPos == null || !(level() instanceof ServerLevel sl)) return;
        BlockPos p = exitPos;
        if (sl.getBlockState(p).isSolid() || sl.getBlockState(p.above()).isSolid()) return;
        teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 20, 0.4, 0.5, 0.4, 0.02);
    }

    // ───────────── 격리실 복귀 (제압/자폭 공통) ─────────────

    /** 탈출 상태를 끝내고 격리실로 되돌린다. 카운터 복구. */
    protected void returnToContainment(MutableComponent announcement) {
        if (!(level() instanceof ServerLevel sl)) return;
        setBreached(false);
        breachCountdown = -1;
        setTarget(null);
        threat.clear();
        targetTicks = 0;
        retargetIn = 100;
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

    /** 외부(사건 관리자 등)에서 격리실로 되돌린다. announcement가 null이면 알리지 않는다. */
    public void forceReturn(MutableComponent msg) {
        returnToContainment(msg);
    }

    protected void suppress(Entity by) {
        var msg = Component.literal("[관제] " + profile().code() + " 제압 완료. ").withStyle(st -> st.withColor(0x55FF55));
        if (by instanceof Player pl) {
            msg.append(pl.getDisplayName()).append(Component.literal(" 직원의 활약으로 ").withStyle(st -> st.withColor(0xAAAAAA)));
        }
        msg.append(Component.literal("격리실에 재수용되었습니다.").withStyle(st -> st.withColor(0xAAAAAA)));
        returnToContainment(msg);
    }

    /** 현재 단계의 피해 내성 배율. 형태가 바뀌는 환상체는 재정의한다. */
    protected float resistMultiplier(DamageColor c) {
        return profile().resistTo(c);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isBreached()) return super.hurtServer(level, source, amount);
        float mult = NON_EGO_MULT;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                mult = resistMultiplier(ego.color);
                if (source.getDirectEntity() == attacker && attacker instanceof ServerPlayer sp
                        && ego.spec().kind() == kr.lobcorp.item.WeaponKind.FAST) {
                    mult *= kr.lobcorp.item.MeleeEffects.comboMultiplier(sp, level);
                }
                // 등급 차이 배율 (E.G.O 무기 등급 vs 환상체 등급)
                mult *= kr.lobcorp.Grades.mult(kr.lobcorp.Grades.ofItem(ego), kr.lobcorp.Grades.of(profile().risk()));
            }
        }
        float dmg = amount * mult;
        if (source.getEntity() instanceof ServerPlayer attacker) {
            threat.merge(attacker.getUUID(), dmg, Float::sum);
            if (getTarget() != attacker && getRandom().nextInt(100) < 35) {
                setTarget(attacker);
                targetTicks = 0;
            }
        }
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
        float dmg = amount * resistMultiplier(color);
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

    /** 반경 안에서 가장 가까운 살아 있는 사무직 (없으면 null) */
    protected ClerkEntity nearestClerk(ServerLevel sl, double radius) {
        ClerkEntity best = null;
        double bd = Double.MAX_VALUE;
        for (ClerkEntity c : sl.getEntitiesOfClass(ClerkEntity.class, getBoundingBox().inflate(radius))) {
            if (!c.isAlive()) continue;
            double d = distanceTo(c);
            if (d > radius) continue;
            if (d < bd) {
                bd = d;
                best = c;
            }
        }
        return best;
    }

    /** 위협도 + 거리 + 약간의 무작위로 추격 대상을 다시 고른다 (한 명이 영구히 어그로를 잡지 못하게) */
    private void retarget(ServerLevel sl) {
        LivingEntity best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            double dist = distanceTo(p);
            if (dist > 40) continue;
            float score = threat.getOrDefault(p.getUUID(), 0f) + 20f / (1f + (float) dist) + getRandom().nextFloat() * 8f;
            if (p == getTarget() && targetTicks > 300) score *= 0.4f;
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        // 사무직은 플레이어보다 조금 낮은 우선순위로 함께 고려한다
        for (ClerkEntity c : sl.getEntitiesOfClass(ClerkEntity.class, getBoundingBox().inflate(40.0))) {
            if (!c.isAlive()) continue;
            double dist = distanceTo(c);
            if (dist > 40) continue;
            float score = 20f / (1f + (float) dist) + getRandom().nextFloat() * 8f - 4f;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        if (best != null && best != getTarget()) {
            setTarget(best);
            targetTicks = 0;
        }
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
            if (!isFriendly()) {
                targetTicks++;
                if (breachTicks % 20 == 0) {
                    threat.values().removeIf(v -> v * 0.9f < 0.5f);
                    threat.replaceAll((k, v) -> v * 0.9f);
                }
                if (--retargetIn <= 0) {
                    retarget(sl);
                    retargetIn = 100 + getRandom().nextInt(60);
                }
            }
            // 문이 좁아 못 나가는 히트박스: 일정 시간이 지나도 방 안에 있으면 복도로 옮긴다
            if (breachTicks == 100 && leavesRoomOnBreach() && home != null
                    && Math.abs(getX() - (home.getX() + 0.5)) < 4.5
                    && Math.abs(getZ() - (home.getZ() + 0.5)) < 4.5
                    && Math.abs(getY() - home.getY()) < 3) {
                LivingEntity t = getTarget();
                if (t != null && distanceTo(t) > 6) {
                    if (exitPos == null) openDoorsNearHome(sl);
                    moveToExit();
                }
            }
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
