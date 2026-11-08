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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * F-04-42 백설공주의 사과 — 한 입 베어 문 거대한 붉은 사과.
 * 탈출하면 같은 층의 먼 자리로 순간이동해 주변으로 덩굴을 퍼뜨린다.
 * 덩굴 위의 직원은 느려지고, 4초마다 검은 피해를 입는다. 덩굴이 다 자라면 다시 자리를 옮긴다.
 */
public class SnowWhiteAppleEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(1.4f, 1.4f);

    /** 덩굴이 다 자라는 데 걸리는 시간 */
    private static final int GROW_TICKS = 800;
    private static final double MAX_RADIUS = 8.0;
    private static final int DAMAGE_CD = 80;
    /** 덩굴을 깔아 두는 실제 블록 수 상한 */
    private static final int MAX_BLOCKS = 48;

    private int growT = 0;
    private int damageCd = DAMAGE_CD;
    private final List<BlockPos> vines = new ArrayList<>();

    public SnowWhiteAppleEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.SNOW_WHITE_APPLE;
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
        return "덩굴이 깔린 바닥에서는 발이 붙습니다. 덩굴 밖으로 나와 제압하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  사과의 벌레 먹은 자리에서 초록 덩굴이 조용히 움츠러든다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  덩굴 하나가 손목을 스치고 지나간다. 달콤한 냄새가 남는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 50) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  사과가 아주 조금 더 벌어졌다. 덩굴이 바닥을 긁는다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 70) lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        growT = 0;
        damageCd = DAMAGE_CD;
        clearVines(sl);
        sl.playSound(null, blockPosition(), SoundEvents.CHERRY_LEAVES_FALL, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] F-04-42 백설공주의 사과가 격리실을 빠져나왔습니다. 덩굴을 밟지 마십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
        relocate(sl);
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        if (level() instanceof ServerLevel sl) clearVines(sl);
        super.returnToContainment(announcement);
        growT = 0;
        damageCd = DAMAGE_CD;
    }

    /** 같은 층의 먼 자리로 순간이동하고 덩굴을 처음부터 다시 깐다 */
    private void relocate(ServerLevel sl) {
        clearVines(sl);
        growT = 0;
        damageCd = DAMAGE_CD;
        BlockPos from = blockPosition();
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, from.getX() + 0.5, from.getY() + 1.0, from.getZ() + 0.5,
                30, 0.5, 0.8, 0.5, 0.02);
        sl.playSound(null, from, SoundEvents.CHERRY_LEAVES_FALL, SoundSource.HOSTILE, 1.2f, 1.2f);
        BlockPos to = findSpot(sl);
        if (to != null) {
            teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            setYRot(getRandom().nextFloat() * 360f);
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
        }
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.0, getZ(), 30, 0.5, 0.8, 0.5, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.CHERRY_LEAVES_FALL, SoundSource.HOSTILE, 1.2f, 0.8f);
    }

    /** 10~30블록 떨어진, 발밑이 단단하고 위 두 칸이 비어 있는 자리 */
    private BlockPos findSpot(ServerLevel sl) {
        BlockPos base = blockPosition();
        for (int i = 0; i < 80; i++) {
            int dx = getRandom().nextInt(61) - 30;
            int dz = getRandom().nextInt(61) - 30;
            int d2 = dx * dx + dz * dz;
            if (d2 < 100 || d2 > 900) continue;
            BlockPos found = scanDown(sl, base.getX() + dx, base.getZ() + dz, base.getY());
            if (found != null) return found;
        }
        return scanDown(sl, base.getX() + 12, base.getZ(), base.getY());
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

    // ───────────── 덩굴 ─────────────

    private double radius() {
        return Math.min(MAX_RADIUS, 1.0 + growT / (double) GROW_TICKS * (MAX_RADIUS - 1.0));
    }

    private void clearVines(ServerLevel sl) {
        for (BlockPos pos : vines) {
            if (sl.getBlockState(pos).is(Blocks.VINE)) sl.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        vines.clear();
    }

    /** 덩굴을 조금씩 퍼뜨린다 (공기 중인 칸에만 실제 덩굴을 남긴다) */
    private void spread(ServerLevel sl) {
        double r = radius();
        for (int i = 0; i < 6; i++) {
            double ang = getRandom().nextDouble() * Math.PI * 2;
            double dist = getRandom().nextDouble() * r;
            double px = getX() + Math.cos(ang) * dist;
            double pz = getZ() + Math.sin(ang) * dist;
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, px, getY() + 0.15, pz, 1, 0.1, 0.05, 0.1, 0);
            if (vines.size() >= MAX_BLOCKS || getRandom().nextInt(4) != 0) continue;
            BlockPos pos = new BlockPos((int) Math.floor(px), (int) Math.floor(getY()), (int) Math.floor(pz));
            if (!sl.getBlockState(pos).isAir()) continue;
            if (sl.getBlockState(pos.below()).isAir()) continue;
            sl.setBlock(pos, Blocks.VINE.defaultBlockState(), 3);
            vines.add(pos);
        }
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        growT++;
        if (tickCount % 4 == 0) spread(sl);

        double r = radius();
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            if (p.distanceToSqr(this) > r * r) continue;
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
        }

        if (damageCd > 0) damageCd--;
        if (damageCd <= 0) {
            damageCd = DAMAGE_CD;
            lash(sl, r);
        }

        if (growT >= GROW_TICKS) relocate(sl);
    }

    private void lash(ServerLevel sl, double r) {
        sl.playSound(null, blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 1.4f, 0.5f);
        for (int i = 0; i < 40; i++) {
            double ang = getRandom().nextDouble() * Math.PI * 2;
            double dist = getRandom().nextDouble() * r;
            sl.sendParticles(ParticleTypes.SQUID_INK, getX() + Math.cos(ang) * dist, getY() + 0.4,
                    getZ() + Math.sin(ang) * dist, 1, 0.1, 0.2, 0.1, 0.01);
        }
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            if (p.distanceToSqr(this) > r * r) continue;
            p.hurtServer(sl, sl.damageSources().magic(), 16f);
            Employees.damageSp(p, 8);
            p.sendSystemMessage(Component.literal("  덩굴이 발목을 감아 올라와 살을 파고든다.")
                    .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_apple_grow", growT);
        out.putInt("lc_apple_dmg_cd", damageCd);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        growT = in.getIntOr("lc_apple_grow", 0);
        damageCd = in.getIntOr("lc_apple_dmg_cd", DAMAGE_CD);
    }
}
