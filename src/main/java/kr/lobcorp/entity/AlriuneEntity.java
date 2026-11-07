package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * T-04-53 알리우네 — 창백한 분홍 꽃잎으로 빚은 키 큰 여인.
 * 걷지 않는다. 탈출하면 시설 곳곳으로 순간이동하며, 곁에 머무는 직원에게 꽃잎을 하나씩 피운다.
 * 세 번째 꽃잎이 필 때 향기가 퍼져 정신력을 앗아가고, 정신력이 바닥난 직원은 패닉 대신 그 자리에서 잠든다.
 */
public class AlriuneEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Integer> PETALS =
            SynchedEntityData.defineId(AlriuneEntity.class, EntityDataSerializers.INT);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 2.4f);

    /** 꽃잎이 피는 데 필요한 근접 틱 (5초) */
    private static final int PETAL_TICKS = 100;
    /** 아무도 곁에 없을 때 다시 순간이동하기까지 (60초) */
    private static final int ALONE_TELEPORT = 1200;
    private static final double NEAR_RADIUS = 6.0;
    private static final double BLOOM_RADIUS = 8.0;

    private int nearT = 0;
    private int aloneT = 0;

    public AlriuneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.ALRIUNE;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PETALS, 0);
    }

    /** 피어난 꽃잎 수 (0~3) */
    public int getPetals() {
        return entityData.get(PETALS);
    }

    private void setPetals(int n) {
        entityData.set(PETALS, Math.max(0, Math.min(3, n)));
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "6블록 안에 머물면 꽃잎이 피어납니다. 꽃잎이 세 장이 되기 전에 물러나십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  꽃잎이 아주 조금 떨린다. 달콤한 냄새가 격리실을 가득 채운다.";
            case NORMAL -> "  알리우네는 아무 말 없이 서 있다. 드레스의 꽃들이 조용히 흔들린다.";
            case BAD -> "  꽃잎 하나가 바닥으로 떨어졌다. 그 자리에서 향기가 짙어진다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        if (result == WorkManager.Result.GOOD || result == WorkManager.Result.BAD) {
            if (p.getRandom().nextInt(100) < 70) lowerQliphoth(p, 1);
        }
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        setPetals(0);
        nearT = 0;
        aloneT = 0;
        teleportRandom(sl);
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setPetals(0);
        nearT = 0;
        aloneT = 0;
    }

    /** 시설 어딘가의 빈 자리로 순간이동한다. */
    private void teleportRandom(ServerLevel sl) {
        BlockPos from = blockPosition();
        BlockPos to = findSpot(sl);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, from.getX() + 0.5, from.getY() + 1.2, from.getZ() + 0.5,
                30, 0.5, 0.8, 0.5, 0.02);
        sl.playSound(null, from, SoundEvents.CHERRY_LEAVES_FALL, SoundSource.HOSTILE, 1.2f, 1.2f);
        if (to != null) {
            teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            setYRot(getRandom().nextFloat() * 360f);
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
        }
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.2, getZ(), 30, 0.5, 0.8, 0.5, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.CHERRY_LEAVES_FALL, SoundSource.HOSTILE, 1.2f, 0.8f);
    }

    /** 격리실 주변에서 설 자리를 찾는다. 없으면 null. */
    private BlockPos findSpot(ServerLevel sl) {
        BlockPos base = home != null ? home : blockPosition();
        for (int i = 0; i < 60; i++) {
            int dx = getRandom().nextInt(37) - 18;
            int dz = getRandom().nextInt(37) - 18;
            if (dx * dx + dz * dz < 49) continue; // 수평 거리 7 미만 제외
            BlockPos found = scanDown(sl, base.getX() + dx, base.getZ() + dz, base.getY());
            if (found != null) return found;
        }
        // 대체: 격리실에서 동쪽으로 8블록
        return scanDown(sl, base.getX() + 8, base.getZ(), base.getY());
    }

    private BlockPos scanDown(ServerLevel sl, int x, int z, int baseY) {
        for (int y = baseY + 2; y >= baseY - 3; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = sl.getBlockState(pos.below());
            if (!below.isSolid()) continue;
            if (!sl.getBlockState(pos).isAir()) continue;
            if (!sl.getBlockState(pos.above()).isAir()) continue;
            return pos;
        }
        return null;
    }

    // ───────────── 향기 ─────────────

    private void bloom(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.4, getZ(), 80, 1.2, 1.4, 1.2, 0.05);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.4, getZ(), 40, 1.0, 1.2, 1.0, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.6f, 0.6f);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) > BLOOM_RADIUS * BLOOM_RADIUS) continue;
            Employees.damageSp(p, 30 + getRandom().nextInt(11));
            if (Employees.get(p).panicTicks > 0) {
                Employees.get(p).panicTicks = 0;
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2f, 0.5f);
                Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                        .append(p.getDisplayName())
                        .append(Component.literal(" 직원이 꽃향기 속에 잠들었습니다.").withStyle(st -> st.withColor(0xFF9AD0))));
                p.hurtServer(sl, sl.damageSources().magic(), 1000f);
            }
        }
        setPetals(0);
        nearT = 0;
        teleportRandom(sl);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            nearT = 0;
            aloneT = 0;
            return;
        }

        setDeltaMovement(0, getDeltaMovement().y, 0);
        getNavigation().stop();

        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.6, getZ(), 2, 0.5, 0.8, 0.5, 0.01);
        }

        boolean near = false;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(this) <= NEAR_RADIUS * NEAR_RADIUS) {
                near = true;
                break;
            }
        }

        if (near) {
            aloneT = 0;
            nearT++;
            if (nearT >= PETAL_TICKS) {
                nearT = 0;
                int petals = getPetals() + 1;
                setPetals(petals);
                sl.playSound(null, blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.2f, 1.4f);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.8, getZ(), 20, 0.6, 0.8, 0.6, 0.02);
                if (petals >= 3) bloom(sl);
            }
        } else {
            nearT = 0;
            setPetals(0);
            aloneT++;
            if (aloneT >= ALONE_TELEPORT) {
                aloneT = 0;
                teleportRandom(sl);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_petals", getPetals());
        out.putInt("lc_near_t", nearT);
        out.putInt("lc_alone_t", aloneT);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setPetals(in.getIntOr("lc_petals", 0));
        nearT = in.getIntOr("lc_near_t", 0);
        aloneT = in.getIntOr("lc_alone_t", 0);
    }
}
