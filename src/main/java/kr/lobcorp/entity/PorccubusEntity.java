package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O-02-98 포큐버스 — 창백한 노란 가시로 뒤덮인 긴 초록 줄기. 머리통은 붉은 꽃이고, 그 사이로 작고 동그란 눈 두 개가 빛난다.
 * 작업 결과가 좋음이면 카운터가 회복되지만, 자제가 낮은 직원에게는 그 쾌락이 곧 죽음이다.
 * 탈출하면 시설 어딘가에 숨어 있다가 다가오는 직원을 덮친다. 가시에 찔린 직원은 자제가 낮을수록 더 큰 정신 피해를 입는다.
 */
public class PorccubusEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 1.6f);

    /** 숨어 있는 동안 깨어나는 거리 */
    private static final double WAKE_RADIUS = 6.0;
    /** 가시가 닿는 거리 */
    private static final double PRICK_RADIUS = 2.2;

    private boolean awake = false;
    private int prickCd = 0;

    public PorccubusEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.PORCCUBUS;
    }

    @Override
    protected double chaseSpeed() {
        return 1.1;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // 폭은 0.8 미만이어야 한다: 열린 철문 사이 틈이 13/16 블록이라 0.9면 문간에 끼어 못 나온다.
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "가시에 찔리지 마십시오. 자제가 낮은 직원일수록 위험합니다.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(p);
        switch (result) {
            case GOOD -> {
                if (d.temperance < 65) {
                    overdose(p);
                    return;
                }
                qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
                p.sendSystemMessage(Component.literal("  붉은 꽃잎이 천천히 벌어졌다 닫힌다. 달콤한 냄새가 격리실에 고인다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                p.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                        .withStyle(st -> st.withColor(0x55FF55)));
            }
            case NORMAL -> p.sendSystemMessage(Component.literal("  가시가 아주 조금 떨린다. 꽃은 여전히 당신을 보고 있다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  꽃잎이 시들어 떨어졌다. 줄기가 바닥을 긁으며 몸을 웅크린다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
    }

    /** 자제 IV 미만 직원이 좋음 결과를 냈을 때 — 쾌락 과다 */
    private void overdose(ServerPlayer p) {
        ServerLevel sl = p.level();
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.7, 0.5, 0.02);
        sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 1.2f, 0.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5f, 1.8f);
        p.sendSystemMessage(Component.literal("  너무 달콤하다. 숨을 쉴 수가 없다. 무릎이 풀리고 입가에 웃음이 번진다.")
                .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 쾌락을 견디지 못했습니다.").withStyle(st -> st.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    // ───────────── 가시 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof ServerPlayer p)) return super.doHurtTarget(level, target);
        EmployeeData d = Employees.get(p);
        int sp;
        if (d.temperance < 65) sp = 6 + getRandom().nextInt(5);
        else if (d.temperance < 85) sp = 2 + getRandom().nextInt(3);
        else sp = 1 + getRandom().nextInt(2);

        level.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1, p.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.HOSTILE, 1f, 1.2f);
        p.sendSystemMessage(Component.literal("  가시가 살갗을 파고들었다. 아프지 않다. 그게 문제다.")
                .withStyle(st -> st.withColor(0xFFAAAA).withItalic(true)));
        Employees.damageSp(p, sp);

        if (Employees.get(p).panicTicks > 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1.6, p.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.4f);
            Facility.broadcast(level.getServer(), Component.literal("  … ").withStyle(st -> st.withColor(0xAA0000))
                    .append(p.getDisplayName())
                    .append(Component.literal("의 머리가 터졌다.").withStyle(st -> st.withColor(0xAA0000))));
            p.hurtServer(level, level.damageSources().magic(), 1000f);
        }
        return true;
    }

    // ───────────── 탈출: 숨는다 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        BlockPos from = blockPosition();
        BlockPos hide = findHidingSpot(sl);
        if (hide != null) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, from.getX() + 0.5, from.getY() + 1, from.getZ() + 0.5,
                    30, 0.6, 0.6, 0.6, 0.02);
            teleportTo(hide.getX() + 0.5, hide.getY(), hide.getZ() + 0.5);
            setYRot(getRandom().nextFloat() * 360f);
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1, getZ(), 30, 0.6, 0.6, 0.6, 0.02);
        }
        awake = false;
        setTarget(null);
        getNavigation().stop();
        sl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-02-98이 시설 어딘가로 사라졌습니다. 가시에 주의하십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    /** 격리실 주변 8~20 블록 거리에서 숨을 자리를 찾는다. 없으면 null. */
    private BlockPos findHidingSpot(ServerLevel sl) {
        BlockPos base = home != null ? home : blockPosition();
        for (int i = 0; i < 80; i++) {
            int dx = getRandom().nextInt(41) - 20;
            int dz = getRandom().nextInt(41) - 20;
            int d2 = dx * dx + dz * dz;
            if (d2 < 64 || d2 > 400) continue; // 수평 거리 8~20
            BlockPos found = scanDown(sl, base.getX() + dx, base.getZ() + dz, base.getY());
            if (found != null) return found;
        }
        // 대체: 격리실에서 동쪽으로 10블록
        return scanDown(sl, base.getX() + 10, base.getZ(), base.getY());
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
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        awake = false;
        prickCd = 0;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        if (!awake) {
            setTarget(null);
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.2, getZ(), 2, 0.3, 0.4, 0.3, 0);
            }
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                if (p.distanceToSqr(this) <= WAKE_RADIUS * WAKE_RADIUS) {
                    awake = true;
                    sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SNIFF, SoundSource.HOSTILE, 1.2f, 1.4f);
                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.4, getZ(), 20, 0.4, 0.5, 0.4, 0.02);
                    Facility.broadcast(sl.getServer(), Component.literal("[관제] O-02-98이 깨어났습니다.")
                            .withStyle(st -> st.withColor(0xFF5555).withBold(true)));
                    break;
                }
            }
            return;
        }

        if (prickCd > 0) prickCd--;
        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.4, getZ(), 2, 0.3, 0.4, 0.3, 0);
        }
        if (prickCd == 0) {
            LivingEntity best = null;
            double bd = PRICK_RADIUS * PRICK_RADIUS;
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                double dist = p.distanceToSqr(this);
                if (dist < bd) {
                    bd = dist;
                    best = p;
                }
            }
            if (best != null) {
                prickCd = 20;
                doHurtTarget(sl, best);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_awake", awake);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        awake = in.getBooleanOr("lc_awake", false);
    }
}
