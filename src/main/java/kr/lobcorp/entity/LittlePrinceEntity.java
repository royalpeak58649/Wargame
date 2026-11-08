package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * O-04-66 어린 왕자 — 망토를 쓴 아이 모양의 푸른 회색 버섯 덩어리. 주변으로 포자가 흩날린다.
 * 탈출하지 않는다. 대신 포자에 잠식된 직원을 격리실로 끌어들여 어린 왕자-1로 만든다.
 */
public class LittlePrinceEntity extends AbnormalityEntity {
    public static final int MAX_SPORES = 5;
    public static final int LURE_TICKS = 200;
    public static final int LURE_MAX = 4;
    public static final double LURE_RADIUS = 4.0;
    public static final double KILL_RADIUS = 2.0;

    /** 통찰이 아닌 작업을 연달아 몇 번 했는가 */
    private int streak = 0;
    /** 오늘 이미 끌어들인 직원 */
    private final Set<UUID> luredToday = new HashSet<>();
    /** 끌려가는 중인 직원 (저장 안 함) */
    private final List<UUID> lureTargets = new ArrayList<>();
    private int lureTicks = 0;

    public LittlePrinceEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.LITTLE_PRINCE;
    }

    // ───────────── 클리포트 카운터 ─────────────

    /** 하루가 지나면 유인 기록을 초기화한다 */
    public void clearDaily() {
        luredToday.clear();
    }

    private void lowerCounter(ServerPlayer by) {
        qliphoth = Math.max(0, qliphoth - 1);
        if (by != null) {
            by.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
        if (qliphoth <= 0) {
            lure();
            resetQliphoth();
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        // 포자: 이 환상체에 작업을 마칠 때마다 1씩 쌓인다
        EmployeeData d = Employees.get(p);
        if (d.princeSpores < MAX_SPORES) {
            d.princeSpores++;
            Employees.sync(p);
        }
        if (s.type == WorkType.INSIGHT) {
            streak = 0;
        } else {
            streak++;
            if (streak >= 3) {
                streak = 0;
                p.sendSystemMessage(Component.literal("  포자가 목구멍 안쪽에서 몰려든다. 어린 왕자가 고개를 갸웃한다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerCounter(p);
            }
        }
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  어린 왕자가 조용히 손을 내민다. 손바닥에 푸른 포자가 앉는다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  망토 아래에서 작은 숨소리가 들린다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  버섯 갓이 갈라지며 검붉은 포자가 뿜어져 나온다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerCounter(p);
            }
        }
    }

    @Override
    public void onOtherWorkFinished(ServerPlayer worker, AbnormalityEntity other, WorkManager.Result r) {
        EmployeeData d = Employees.get(worker);
        if (d.princeSpores > 0) {
            d.princeSpores--;
            Employees.sync(worker);
        }
    }

    // ───────────── 포자 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.4, getZ(), 2, 0.8, 0.4, 0.8, 0.005);
        }
        if (tickCount % 40 == 0) {
            for (ServerPlayer p : sl.players()) {
                if (Employees.get(p).princeSpores >= 3) {
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1.2, p.getZ(),
                            4, 0.4, 0.6, 0.4, 0.01);
                }
            }
        }
        if (tickCount % 20 == 0) checkSporeApproach(sl);
        if (lureTicks > 0) tickLure(sl);
    }

    /** 포자가 5까지 찬 직원이 격리실 가까이 오면 카운터가 내려간다 (직원당 하루 한 번) */
    private void checkSporeApproach(ServerLevel sl) {
        if (home == null) return;
        double hx = home.getX() + 0.5, hy = home.getY() + homeYOffset(), hz = home.getZ() + 0.5;
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            if (Employees.get(p).princeSpores < MAX_SPORES) continue;
            if (luredToday.contains(p.getUUID())) continue;
            if (p.distanceToSqr(hx, hy, hz) > LURE_RADIUS * LURE_RADIUS) continue;
            luredToday.add(p.getUUID());
            p.sendSystemMessage(Component.literal("  어린 왕자가 손짓한다. 포자가 몸속에서 노래하기 시작한다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            lowerCounter(p);
            return;
        }
    }

    // ───────────── 유인 ─────────────

    /** 포자가 가장 많이 쌓인 직원 몇 명을 격리실로 끌어들인다 */
    private void lure() {
        if (!(level() instanceof ServerLevel sl) || home == null) return;
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            if (Employees.get(p).princeSpores <= 0) continue;
            candidates.add(p);
        }
        if (candidates.isEmpty()) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] O-04-66의 포자가 허공으로 흩어졌습니다.")
                    .withStyle(st -> st.withColor(0xAA55FF)));
            return;
        }
        java.util.Collections.shuffle(candidates, new java.util.Random(getRandom().nextLong()));
        candidates.sort((a, b) -> Integer.compare(Employees.get(b).princeSpores, Employees.get(a).princeSpores));
        lureTargets.clear();
        int n = Math.min(LURE_MAX, candidates.size());
        for (int i = 0; i < n; i++) {
            ServerPlayer p = candidates.get(i);
            lureTargets.add(p.getUUID());
            p.sendSystemMessage(Component.literal("  발이 저절로 격리실 쪽으로 향한다. 포자가 방향을 정한다.")
                    .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
        }
        lureTicks = LURE_TICKS;
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.4, getZ(), 60, 2.0, 1.0, 2.0, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.BEE_LOOP, SoundSource.HOSTILE, 1.2f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-04-66의 포자가 " + n + "명의 직원을 격리실로 끌어들이고 있습니다.")
                .withStyle(st -> st.withColor(0xAA55FF)));
    }

    private void tickLure(ServerLevel sl) {
        lureTicks--;
        double hx = home.getX() + 0.5, hy = home.getY() + homeYOffset(), hz = home.getZ() + 0.5;
        for (UUID id : new ArrayList<>(lureTargets)) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive() || p.level() != sl) {
                lureTargets.remove(id);
                continue;
            }
            if (p.distanceToSqr(hx, hy, hz) <= KILL_RADIUS * KILL_RADIUS) {
                lureTargets.clear();
                lureTicks = 0;
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
                p.hurtServer(sl, sl.damageSources().magic(), 1000f);
                LittlePrinceMinionEntity.spawnFrom(p);
                return;
            }
            if (lureTicks % 10 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));
                double dx = hx - p.getX(), dz = hz - p.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.001) {
                    double nx = p.getX() + dx / len * 2.0, nz = p.getZ() + dz / len * 2.0;
                    BlockPos target = new BlockPos((int) Math.floor(nx), (int) Math.floor(p.getY()), (int) Math.floor(nz));
                    if (!sl.getBlockState(target).isSolid() && !sl.getBlockState(target.above()).isSolid()) {
                        p.teleportTo(nx, p.getY(), nz);
                    }
                }
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.5, 0.3, 0.01);
            }
        }
        if (lureTicks <= 0) {
            lureTargets.clear();
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_prince_streak", streak);
        out.putInt("lc_prince_lured", luredToday.size());
        int i = 0;
        for (UUID id : luredToday) {
            out.putString("lc_prince_lured_" + i, id.toString());
            i++;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        streak = in.getIntOr("lc_prince_streak", 0);
        luredToday.clear();
        int n = in.getIntOr("lc_prince_lured", 0);
        for (int i = 0; i < n; i++) {
            String s = in.getStringOr("lc_prince_lured_" + i, "");
            if (!s.isEmpty()) {
                try {
                    luredToday.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
