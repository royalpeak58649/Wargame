package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * O-01-73 절망의 기사 — 푸른 머리카락이 검게 물들어 가는 여성 기사.
 * 매일 처음으로 좋음 결과를 낸 직원에게 가호를 내린다. 가호를 받은 직원은 작업을 할 수 없고,
 * 그 직원이 쓰러지거나 패닉에 빠지면 기사가 격리실을 나온다.
 */
public class KnightDespairEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.7f, 2.4f);

    /** 순간이동 주기 */
    private static final int TELEPORT_PERIOD = 200;
    /** 순간이동 후 검을 던지기까지 */
    private static final int THROW_DELAY = 30;
    /** 검 사거리 */
    private static final double THROW_RANGE = 24.0;
    /** 검 피해 (PALE) */
    /** PALE: 최대 HP 대비 % */
    private static final int THROW_DAMAGE = 60;

    private UUID blessedId = null;
    private int tpT = 0;
    /** 클라이언트 연출용: 남은 던지기 준비 틱 */
    public int throwT = 0;

    public KnightDespairEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.KNIGHT_DESPAIR;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "그녀의 검이 빛나면 자리를 벗어나십시오. 검은 방호복을 가리지 않습니다.";
    }

    // ───────────── 가호 ─────────────

    public UUID getBlessedId() {
        return blessedId;
    }

    /** 가호를 모두 거둔다 (날짜 종료 / 제압) */
    public void clearBlessing() {
        if (blessedId == null) return;
        if (level() instanceof ServerLevel sl) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(blessedId);
            if (p != null) {
                Employees.get(p).blessed = false;
                Employees.sync(p);
            }
        }
        blessedId = null;
    }

    private void bless(ServerPlayer p) {
        blessedId = p.getUUID();
        EmployeeData d = Employees.get(p);
        d.blessed = true;
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        p.sendSystemMessage(Component.literal("  기사가 눈물 젖은 검을 당신의 어깨에 얹었다. 가호가 깃든다.")
                .withStyle(st -> st.withColor(0x9AD0FF).withItalic(true)));
        p.sendSystemMessage(Component.literal("  가호를 받은 동안에는 작업을 할 수 없습니다. 오직 싸울 수만 있습니다.")
                .withStyle(st -> st.withColor(0xFF7777)));
        Facility.broadcast(p.level().getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 O-01-73의 가호를 받았습니다. 이 직원이 무너지면 기사가 탈출합니다.")
                        .withStyle(st -> st.withColor(0x9AD0FF))));
        Employees.sync(p);
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (result != WorkManager.Result.GOOD) {
            p.sendSystemMessage(Component.literal("  기사는 대답하지 않는다. 검이 조용히 흔들릴 뿐이다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            return;
        }
        if (blessedId != null) {
            ServerPlayer cur = p.level().getServer().getPlayerList().getPlayer(blessedId);
            if (cur != null && cur.isAlive()) {
                p.sendSystemMessage(Component.literal("  기사가 고개를 젓는다. 이미 가호를 받은 이가 있다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                return;
            }
            blessedId = null;
        }
        bless(p);
    }

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (blessedId == null || !blessedId.equals(p.getUUID()) || isBreached()) return;
        blessedId = null;
        Employees.get(p).blessed = false;
        Facility.broadcast(level().getServer(), Component.literal("[관제] 가호를 받은 직원이 무너졌습니다. O-01-73이 탈출합니다.")
                .withStyle(st -> st.withColor(0xFF5555).withBold(true)));
        breach();
    }

    @Override
    public void onAnyPanic(ServerPlayer p) {
        if (blessedId == null || !blessedId.equals(p.getUUID()) || isBreached()) return;
        blessedId = null;
        Employees.get(p).blessed = false;
        Facility.broadcast(level().getServer(), Component.literal("[관제] 가호를 받은 직원이 무너졌습니다. O-01-73이 탈출합니다.")
                .withStyle(st -> st.withColor(0xFF5555).withBold(true)));
        breach();
    }

    @Override
    public void resetQliphoth() {
        super.resetQliphoth();
        clearBlessing();
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        tpT = 0;
        throwT = 0;
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-01-73이 검을 거두고 걸어 나옵니다. 검은 방호복을 가리지 않습니다.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        tpT = 0;
        throwT = 0;
        clearBlessing();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            if (throwT != 0) throwT = 0;
            return;
        }
        if (throwT > 0) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (throwT == 1) throwSword(sl);
            throwT--;
            return;
        }
        tpT++;
        if (tpT >= TELEPORT_PERIOD) {
            tpT = 0;
            teleportNearPlayer(sl);
        }
    }

    // ───────────── 순간이동 ─────────────

    private void teleportNearPlayer(ServerLevel sl) {
        ServerPlayer target = null;
        int n = 0;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            n++;
            if (getRandom().nextInt(n) == 0) target = p;
        }
        if (target == null) return;

        BlockPos spot = findSpotNear(sl, target.blockPosition());
        if (spot == null) spot = target.blockPosition().above(2);

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 20, 0.5, 0.8, 0.5, 0.02);
        teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        setYRot(getRandom().nextFloat() * 360f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 20, 0.5, 0.8, 0.5, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2f, 0.7f);
        throwT = THROW_DELAY;
    }

    /** 중심에서 6블록 안의, 발 디딜 수 있는 공기 자리를 찾는다. 없으면 null. */
    private BlockPos findSpotNear(ServerLevel sl, BlockPos center) {
        for (int i = 0; i < 60; i++) {
            int dx = getRandom().nextInt(13) - 6;
            int dz = getRandom().nextInt(13) - 6;
            if (dx * dx + dz * dz > 36) continue;
            BlockPos found = scanDown(sl, center.getX() + dx, center.getZ() + dz, center.getY());
            if (found != null) return found;
        }
        return null;
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

    // ───────────── 검 ─────────────

    private void throwSword(ServerLevel sl) {
        ServerPlayer target = null;
        double best = THROW_RANGE * THROW_RANGE;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            double d = p.distanceToSqr(this);
            if (d < best) { best = d; target = p; }
        }
        if (target == null) return;

        Vec3 from = new Vec3(getX(), getY() + 1.6, getZ());
        Vec3 to = target.position().add(0, 1, 0);
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len > 1.0E-4) {
            dir = dir.scale(1.0 / len);
            for (double d = 0; d <= len; d += 0.5) {
                Vec3 at = from.add(dir.scale(d));
                sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        setYRot((float) (Math.atan2(dir.z, dir.x) * 180.0 / Math.PI) - 90f);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());

        boolean blessed = Employees.get(target).blessed;
        Employees.damagePale(target, THROW_DAMAGE * (blessed ? 1.5f : 1f));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2, false, false));
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.2, target.getZ(), 10, 0.3, 0.4, 0.3, 0.03);
        sl.playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.2f, 0.8f);
        sl.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.2f, 0.8f);
        target.sendSystemMessage(Component.literal("  눈물로 벼려낸 검이 당신을 꿰뚫는다.")
                .withStyle(st -> st.withColor(0x9AD0FF).withItalic(true)));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (blessedId != null) out.store("lc_blessed", UUIDUtil.CODEC, blessedId);
        out.putInt("lc_tpt", tpT);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        blessedId = in.read("lc_blessed", UUIDUtil.CODEC).orElse(null);
        tpT = in.getIntOr("lc_tpt", 0);
    }
}
