package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * D-04-108 기생 나무 — 창백한 나무. 가지마다 하얀 꽃봉오리가 달려 있고, 봉오리가 다 열리면 열매 대신 해골이 맺힌다.
 * 탈출하지 않는다. 대신 축복을 받은 직원의 몸에서 묘목이 자라난다.
 */
public class ParasiteTreeEntity extends AbnormalityEntity {
    public static final int MAX_BULBS = 5;
    public static final int WILT_TICKS = 100;
    /** 축복받은 직원이 100틱마다 회복하는 SP */
    public static final int BLESS_SP = 1;

    private static final EntityDataAccessor<Integer> BULBS =
            SynchedEntityData.defineId(ParasiteTreeEntity.class, EntityDataSerializers.INT);

    /** 억압이 아닌 작업을 연달아 몇 번 했는가 */
    private int otherStreak = 0;
    /** 시들기까지 남은 틱 (0이면 시들지 않음) */
    private int wiltTicks = 0;

    public ParasiteTreeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.PARASITE_TREE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BULBS, 0);
    }

    /** 열린 꽃봉오리 수 (0-5) */
    public int bulbs() {
        return entityData.get(BULBS);
    }

    private void setBulbs(int n) {
        entityData.set(BULBS, Math.max(0, Math.min(MAX_BULBS, n)));
    }

    // ───────────── 클리포트 카운터 ─────────────

    /** 하루가 지나면 봉오리와 시들기를 초기화한다 */
    public void clearDaily() {
        setBulbs(0);
        wiltTicks = 0;
    }

    private void lowerCounter(ServerPlayer by) {
        qliphoth = Math.max(0, qliphoth - 1);
        if (by != null) {
            by.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
        if (qliphoth <= 0) {
            pull();
            resetQliphoth();
        }
    }

    @Override
    public void onAnyBreach(BreachingAbnormality who) {
        if (who instanceof PunishingBirdEntity) return;
        if (getRandom().nextInt(100) < 30) lowerCounter(null);
    }

    @Override
    public void onOtherWorkFinished(ServerPlayer worker, AbnormalityEntity other, WorkManager.Result r) {
        otherStreak++;
        if (otherStreak >= 5) {
            otherStreak = 0;
            lowerCounter(worker);
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        otherStreak = 0;
        if (s.type == WorkType.REPRESSION) {
            lowerCounter(p);
        }
        EmployeeData d = Employees.get(p);
        if (!d.parasiteBlessed) {
            bless(p);
        }
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  하얀 꽃봉오리가 조용히 벌어진다. 달콤한 냄새가 목덜미를 스친다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  나무껍질 아래에서 무언가 꿈틀거린다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> p.sendSystemMessage(Component.literal("  봉오리 하나가 터지며 마른 잎이 쏟아진다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        return super.workChanceBonus(worker, type) + (Employees.get(worker).parasiteBlessed ? 5 : 0);
    }

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (Employees.get(p).parasiteBlessed && wiltTicks <= 0) {
            setBulbs(bulbs() - 1);
        }
    }

    // ───────────── 축복 ─────────────

    private void bless(ServerPlayer p) {
        EmployeeData d = Employees.get(p);
        d.parasiteBlessed = true;
        Employees.sync(p);
        setBulbs(bulbs() + 1);
        p.sendSystemMessage(Component.literal("  기생 나무의 축복이 몸속에 뿌리를 내린다. 숨 쉴 때마다 잎이 흔들린다.")
                .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        }
        if (bulbs() >= MAX_BULBS) startWilt();
    }

    /** 카운터 0: 축복받지 않은 직원 한 명을 격리실 앞으로 끌어와 축복한다 */
    private void pull() {
        if (!(level() instanceof ServerLevel sl) || home == null) return;
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            if (Employees.get(p).parasiteBlessed) continue;
            candidates.add(p);
        }
        if (candidates.isEmpty()) return;
        ServerPlayer p = candidates.get(getRandom().nextInt(candidates.size()));
        p.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 1.5);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.8, 0.5, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEE_LOOP, SoundSource.HOSTILE, 1.0f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] D-04-108의 덩굴이 ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원을 격리실로 끌어들였습니다.").withStyle(st -> st.withColor(0xAA55FF))));
        bless(p);
    }

    private void startWilt() {
        if (wiltTicks > 0) return;
        wiltTicks = WILT_TICKS;
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            if (Employees.get(p).parasiteBlessed) Employees.panic(p);
        }
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 2.0, getZ(), 80, 1.5, 1.5, 1.5, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] D-04-108의 꽃봉오리가 모두 열렸습니다. 축복받은 직원들의 몸이 부풀어 오릅니다.")
                .withStyle(st -> st.withColor(0xAA55FF)));
    }

    private void tickWilt(ServerLevel sl) {
        wiltTicks--;
        if (wiltTicks > 0) return;
        wiltTicks = 0;
        for (ServerPlayer p : new ArrayList<>(sl.players())) {
            if (!Employees.get(p).parasiteBlessed || !p.isAlive()) continue;
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
            p.hurtServer(sl, sl.damageSources().magic(), 1000f);
            SaplingEntity.spawnFrom(p);
        }
        for (ServerPlayer p : sl.players()) {
            Employees.get(p).parasiteBlessed = false;
            Employees.sync(p);
        }
        setBulbs(0);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 2.2, getZ(), 2, 0.9, 0.6, 0.9, 0.005);
        }
        if (tickCount % 100 == 0) {
            for (ServerPlayer p : sl.players()) {
                EmployeeData d = Employees.get(p);
                if (!d.parasiteBlessed || !p.isAlive()) continue;
                if (d.sp < d.maxSp()) {
                    d.sp = Math.min(d.maxSp(), d.sp + BLESS_SP);
                    Employees.sync(p);
                }
            }
        }
        if (wiltTicks > 0) tickWilt(sl);
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_parasite_bulbs", bulbs());
        out.putInt("lc_parasite_streak", otherStreak);
        out.putInt("lc_parasite_wilt", wiltTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setBulbs(in.getIntOr("lc_parasite_bulbs", 0));
        otherStreak = in.getIntOr("lc_parasite_streak", 0);
        wiltTicks = in.getIntOr("lc_parasite_wilt", 0);
    }
}
