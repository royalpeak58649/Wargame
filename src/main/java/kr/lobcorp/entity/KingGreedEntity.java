package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * O-01-64 탐욕의 왕 — 황금 드레스를 입은 마법소녀.
 * 탈출하면 거대한 황금빛 아가리가 되어 시설 어딘가에 나타난다.
 * 앞으로만 느릿하게 나아가며 정면의 직원을 통째로 삼킨다. 벽에 부딪히면 문이 열려 다른 곳으로 이어진다.
 * 뒤나 옆에서 공격하면 안전하다.
 */
public class KingGreedEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.4f, 1.4f);

    private static final double MOVE_SPEED = 0.12;
    private static final double DEVOUR_REACH = 1.0;
    private static final double DEVOUR_INFLATE = 0.3;
    private static final int PORTAL_COOLDOWN = 20;

    private Direction dir = Direction.NORTH;
    private int portalCd = 0;

    public KingGreedEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.KING_GREED;
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
        return "아가리는 앞으로만 나아갑니다. 뒤나 옆에서 공격하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  그녀가 치맛자락을 살짝 들어 보이며 미소 짓는다. 보석들이 조용히 반짝였다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  그녀의 미소가 조금 굳었다. 손끝이 보석을 매만진다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 50) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  드레스의 보석 하나가 툭 떨어져 바닥을 굴렀다. 그녀가 그것을 오래 내려다본다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 70) lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        teleportToRandomSpot(sl);
        dir = pickDirection(sl);
        portalCd = 0;
        faceDir();
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-01-64가 시설 어딘가에 나타났습니다. 정면에 서지 마십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        portalCd = 0;
        getNavigation().stop();
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        if (portalCd > 0) portalCd--;

        faceDir();
        move(MoverType.SELF, new Vec3(dir.getStepX() * MOVE_SPEED, 0, dir.getStepZ() * MOVE_SPEED));

        // 황금빛 부스러기가 흘러내린다
        if (tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.WAX_ON, getX() + (getRandom().nextDouble() - 0.5) * 1.2,
                    getY() + 0.4 + getRandom().nextDouble() * 0.8,
                    getZ() + (getRandom().nextDouble() - 0.5) * 1.2, 2, 0.1, 0.1, 0.1, 0.01);
        }

        devourAhead(sl);

        if (horizontalCollision && portalCd == 0) openPortal(sl);
    }

    private void faceDir() {
        float yaw = switch (dir) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> getYRot();
        };
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void devourAhead(ServerLevel sl) {
        AABB front = getBoundingBox().move(dir.getStepX() * DEVOUR_REACH, 0, dir.getStepZ() * DEVOUR_REACH)
                .inflate(DEVOUR_INFLATE);
        for (Player pl : sl.players()) {
            if (!(pl instanceof ServerPlayer sp)) continue;
            if (!sp.isAlive() || sp.isCreative() || sp.isSpectator()) continue;
            if (!front.intersects(sp.getBoundingBox())) continue;
            devour(sp, sl);
        }
    }

    private void devour(ServerPlayer p, ServerLevel sl) {
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 2f, 0.4f);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
        sl.sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.6, 0.5, 0.05);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 탐욕의 왕에게 삼켜졌습니다.").withStyle(st -> st.withColor(0xE8B830))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    private void openPortal(ServerLevel sl) {
        portalCd = PORTAL_COOLDOWN;
        Vec3 from = position();
        sl.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 0.7, from.z, 40, 0.7, 0.7, 0.7, 0.1);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4f, 0.7f);
        teleportToRandomSpot(sl);
        dir = pickDirection(sl);
        faceDir();
        Vec3 to = position();
        sl.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 0.7, to.z, 40, 0.7, 0.7, 0.7, 0.1);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4f, 1.1f);
    }

    // ───────────── 위치 / 방향 ─────────────

    /** 격리실 주변에서 바닥 위로 나올 자리를 찾아 이동한다. */
    private void teleportToRandomSpot(ServerLevel sl) {
        BlockPos base = home != null ? home : blockPosition();
        for (int i = 0; i < 60; i++) {
            int dx = getRandom().nextInt(49) - 24;
            int dz = getRandom().nextInt(49) - 24;
            if (dx * dx + dz * dz < 64) continue; // 수평 거리 8 미만 제외
            BlockPos found = scanDown(sl, base.getX() + dx, base.getZ() + dz, base.getY());
            if (found != null) {
                teleportTo(found.getX() + 0.5, found.getY(), found.getZ() + 0.5);
                return;
            }
        }
        BlockPos found = scanDown(sl, base.getX() + 10, base.getZ(), base.getY());
        if (found != null) teleportTo(found.getX() + 0.5, found.getY(), found.getZ() + 0.5);
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

    /** 사방 중 가장 멀리 트여 있는 방향을 고른다. */
    private Direction pickDirection(ServerLevel sl) {
        Direction best = Direction.NORTH;
        int bestFree = -1;
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            int free = 0;
            for (int i = 1; i <= 24; i++) {
                BlockPos pos = blockPosition().offset(d.getStepX() * i, 0, d.getStepZ() * i);
                if (!sl.getBlockState(pos).isAir() || !sl.getBlockState(pos.above()).isAir()) break;
                free = i;
            }
            if (free > bestFree) {
                bestFree = free;
                best = d;
            }
        }
        return best;
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_dir", dir.get3DDataValue());
        out.putInt("lc_portal_cd", portalCd);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        dir = Direction.from3DDataValue(in.getIntOr("lc_dir", Direction.NORTH.get3DDataValue()));
        portalCd = in.getIntOr("lc_portal_cd", 0);
    }
}
