package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * O-01-15 이름 없는 태아 — 거대한 살덩어리가 잠들어 있다.
 * 클리포트 카운터가 0이 되면 울기 시작해 주변의 정신을 갉아먹고, 가까운 환상체의 격리를 무너뜨린다.
 * 누군가 먹이가 되어야 울음이 그친다. 탈출하지는 않는다.
 */
public class FetusEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Integer> CRY =
            SynchedEntityData.defineId(FetusEntity.class, EntityDataSerializers.INT);

    /** 울음이 시작된 뒤 이 틱이 지나면 아무나 한 명을 끌어당겨 먹는다 */
    public static final int FORCE_FEED_TICKS = 1200;
    public static final int CRY_RADIUS = 24;
    public static final int PULL_RADIUS = 48;

    public FetusEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.FETUS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CRY, 0);
    }

    /** 0이면 조용하다 */
    public int getCry() {
        return entityData.get(CRY);
    }

    private void setCry(int t) {
        entityData.set(CRY, Math.max(0, t));
    }

    // ───────────── 작업 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        if (getCry() <= 0) return true;
        devour(p);
        return false;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  살덩어리가 잠결에 손가락을 빨아 본다. 숨소리가 고르다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  표면 아래에서 무언가가 꿈틀거린다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 30) lower(p);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  태아의 입술이 벌어지며, 아주 작게 소리가 새어 나온다.")
                        .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
                if (p.getRandom().nextInt(100) < 90) lower(p);
            }
        }
    }

    private void lower(ServerPlayer p) {
        qliphoth = Math.max(0, qliphoth - 1);
        p.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                .withStyle(st -> st.withColor(0xFF5555)));
        if (qliphoth <= 0 && getCry() <= 0) startCrying();
    }

    private void startCrying() {
        setCry(1);
        if (level() instanceof ServerLevel sl) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] O-01-15가 울기 시작했습니다. 누군가 먹이가 되어야 그칩니다.")
                    .withStyle(st -> st.withColor(0xFF5555).withBold(true)));
            sl.playSound(null, blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.5f, 0.6f);
            sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 1.2, getZ(), 30, 0.8, 0.6, 0.8, 0.02);
        }
    }

    // ───────────── 울음 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        int cry = getCry();
        if (cry <= 0) {
            if (tickCount % 40 == 0) {
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 1, 0.6, 0.2, 0.6, 0.005);
            }
            return;
        }
        setCry(cry + 1);
        cry = getCry();

        // 눈물
        if (cry % 10 == 0) {
            sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 1.1, getZ(), 4, 0.7, 0.3, 0.7, 0.01);
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 3, 0.8, 0.1, 0.8, 0.02);
        }
        if (cry % 60 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
        // 10초마다 주변 직원의 정신력
        if (cry % 200 == 0) {
            for (ServerPlayer p : sl.players()) {
                if (p.isCreative() || p.isSpectator()) continue;
                if (p.distanceTo(this) > CRY_RADIUS) continue;
                Employees.damageSp(p, 3);
                WorkManager.actionbar(p, Component.literal("갓난아이의 울음이 머릿속을 파고든다")
                        .withStyle(st -> st.withColor(0xFFAAAA)));
            }
        }
        // 20초마다 주변 환상체의 격리가 흔들린다
        if (cry % 400 == 0) {
            List<BreachingAbnormality> near = new ArrayList<>();
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof BreachingAbnormality b && !b.isBreached()
                        && b.distanceTo(this) <= CRY_RADIUS) {
                    near.add(b);
                }
            }
            for (BreachingAbnormality b : near) {
                b.lowerQliphoth(null, 1);
            }
        }
        // 아무도 나서지 않으면 누군가 끌려간다
        if (cry >= FORCE_FEED_TICKS) {
            ServerPlayer victim = pickVictim(sl);
            if (victim != null) {
                devour(victim);
            } else {
                setCry(1);
            }
        }
    }

    private ServerPlayer pickVictim(ServerLevel sl) {
        List<ServerPlayer> pool = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (Employees.get(p).isPanicking()) continue;
            if (p.distanceTo(this) > PULL_RADIUS) continue;
            pool.add(p);
        }
        if (pool.isEmpty()) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    /** 누군가 먹이가 된다. 울음이 그치고 카운터가 1로 돌아온다. */
    public void devour(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) return;
        setCry(0);
        qliphoth = 1;
        double yaw = Math.toRadians(getYRot());
        p.teleportTo(getX() - Math.sin(yaw) * 1.2, getY(), getZ() + Math.cos(yaw) * 1.2);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 2f, 0.5f);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
        sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 1.2, getZ(), 20, 0.8, 0.5, 0.8, 0.02);
        p.sendSystemMessage(Component.literal("  거대한 입이 당신을 삼킨다. 울음소리가 목구멍 안쪽에서 멎는다.")
                .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 O-01-15에게 먹혔습니다. 울음이 그쳤습니다.").withStyle(st -> st.withColor(0xAA0000))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_cry", getCry());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setCry(in.getIntOr("lc_cry", 0));
    }
}
