package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
 * O-04-84 고기 등불 — 하얀 털로 뒤덮인 덩어리. 머리 위 가느다란 줄기 끝에 푸르게 빛나는 꽃이 달려 있다.
 * 탈출하면 시설 바닥을 파고들어 숨는다. 흰 털을 밟은 직원은 거대한 아가리에 물려 찢긴다.
 * 스스로 움직이지 않으며, 털 바깥에서 공격하면 안전하다.
 */
public class MeatLanternEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Integer> BITE =
            SynchedEntityData.defineId(MeatLanternEntity.class, EntityDataSerializers.INT);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.6f, 0.6f);

    /** 작업이 이 틱 수 이하로 끝나면 "너무 빨리 끝났다" */
    private static final int FAST_WORK_TICKS = 300;
    private static final int BITE_SHOW_TICKS = 30;
    private static final int BITE_COOLDOWN = 40;
    private static final double BITE_RADIUS = 1.5;

    private int biteCd = 0;

    public MeatLanternEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 70)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.MEAT_LANTERN;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BITE, 0);
    }

    /** 아가리가 드러나 있는 남은 틱 (클라이언트 연출용) */
    public int getBiteTicks() {
        return entityData.get(BITE);
    }

    private void setBiteTicks(int t) {
        entityData.set(BITE, t);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "빛나는 꽃 주변의 흰 털을 밟지 마십시오. 털 바깥에서 공격하면 안전합니다.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        boolean fast = s.interval * s.box <= FAST_WORK_TICKS;
        if (result == WorkManager.Result.BAD) {
            lowerQliphoth(p, 1);
        }
        if (fast) {
            p.sendSystemMessage(Component.literal("  작업이 너무 빨리 끝났다… 고기 등불이 몸을 뒤척인다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            lowerQliphoth(p, 1);
        }
        String text = switch (result) {
            case GOOD -> "  하얀 털 사이로 검은 눈알이 느릿하게 깜빡인다. 줄기 끝의 꽃이 아주 조금 밝아졌다.";
            case NORMAL -> "  고기 등불은 아무 말 없이 바닥에 납작하게 엎드려 있다.";
            case BAD -> "  털 아래에서 젖은 소리가 났다. 아가리가 조금 벌어졌다가 닫힌다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    @Override
    public void onWorkAborted(ServerPlayer p, WorkSession s, String reason) {
        lowerQliphoth(p, 1);
    }

    // ───────────── 탈출: 바닥 밑에 숨는다 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        BlockPos from = blockPosition();
        BlockPos hide = findHidingSpot(sl);
        if (hide != null) {
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5,
                    20, 0.6, 0.3, 0.6, 0.01);
            sl.sendParticles(ParticleTypes.ASH, from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5,
                    40, 0.8, 0.4, 0.8, 0.02);
            teleportTo(hide.getX() + 0.5, hide.getY(), hide.getZ() + 0.5);
            setYRot(getRandom().nextFloat() * 360f);
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(), 20, 0.6, 0.2, 0.6, 0.01);
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.2, getZ(), 40, 0.8, 0.2, 0.8, 0.02);
        }
        sl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.4f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-04-84가 격리실 바닥을 파고 사라졌습니다. 위치를 특정할 수 없습니다.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    /** 격리실 주변에서 바닥 아래로 숨을 자리를 찾는다. 없으면 null. */
    private BlockPos findHidingSpot(ServerLevel sl) {
        BlockPos base = home != null ? home : blockPosition();
        for (int i = 0; i < 60; i++) {
            int dx = getRandom().nextInt(37) - 18;
            int dz = getRandom().nextInt(37) - 18;
            if (dx * dx + dz * dz < 49) continue; // 수평 거리 7 미만 제외
            BlockPos found = scanDown(sl, base.getX() + dx, base.getZ() + dz, base.getY());
            if (found != null) return found;
        }
        // 대체: 격리실에서 동쪽으로 8블록
        BlockPos found = scanDown(sl, base.getX() + 8, base.getZ(), base.getY());
        return found;
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

    @Override
    protected void returnToContainment(net.minecraft.network.chat.MutableComponent announcement) {
        super.returnToContainment(announcement);
        setBiteTicks(0);
        biteCd = 0;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        setDeltaMovement(0, getDeltaMovement().y, 0);
        getNavigation().stop();

        if (tickCount % 10 == 0) {
            for (int i = 0; i < 6; i++) {
                double ang = getRandom().nextDouble() * Math.PI * 2;
                double r = 1.4;
                sl.sendParticles(ParticleTypes.WHITE_ASH, getX() + Math.cos(ang) * r, getY() + 0.1,
                        getZ() + Math.sin(ang) * r, 1, 0.05, 0.02, 0.05, 0);
            }
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.6, getZ(), 2, 0.1, 0.1, 0.1, 0);
        }

        int bite = getBiteTicks();
        if (bite > 0) setBiteTicks(bite - 1);
        if (biteCd > 0) biteCd--;

        if (biteCd == 0) {
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                double dx = p.getX() - getX();
                double dz = p.getZ() - getZ();
                if (dx * dx + dz * dz >= BITE_RADIUS * BITE_RADIUS) continue;
                if (Math.abs(p.getY() - getY()) >= 1.5) continue;
                bite(p, sl);
                break;
            }
        }
    }

    private void bite(ServerPlayer p, ServerLevel sl) {
        setBiteTicks(BITE_SHOW_TICKS);
        biteCd = BITE_COOLDOWN;
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_BIG_FALL, SoundSource.HOSTILE, 1.2f, 0.7f);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 고기 등불에게 물렸습니다.").withStyle(st -> st.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().mobAttack(this), 1000f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_bite", getBiteTicks());
        out.putInt("lc_bite_cd", biteCd);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setBiteTicks(in.getIntOr("lc_bite", 0));
        biteCd = in.getIntOr("lc_bite_cd", 0);
    }
}
