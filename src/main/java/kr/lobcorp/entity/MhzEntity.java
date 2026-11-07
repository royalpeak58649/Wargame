package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * T-06-27 1.76 MHz — 잡음만 흘러나오는 낡은 라디오.
 * 클리포트 카운터가 0이 되면 격리실에서 왜곡이 퍼져 나가 반경 안의 직원의 정신력을 갉아먹는다.
 * 탈출하지는 않으며, 카운터를 다시 채우면 왜곡이 통째로 사라진다.
 */
public class MhzEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Integer> RADIUS =
            SynchedEntityData.defineId(MhzEntity.class, EntityDataSerializers.INT);

    public static final int MAX_RADIUS = 32;

    public MhzEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.MHZ;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RADIUS, 0);
    }

    /** 0이면 왜곡 없음 */
    public int getRadius() {
        return entityData.get(RADIUS);
    }

    private void setRadius(int r) {
        entityData.set(RADIUS, Math.max(0, Math.min(MAX_RADIUS, r)));
    }

    // ───────────── 클리포트 카운터 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        int before = qliphoth;
        switch (result) {
            case GOOD -> {
                qliphoth = Math.min(profile().qliphothMax(), qliphoth + (s.good >= 12 ? 2 : 1));
                p.sendSystemMessage(Component.literal("  잡음이 잠시 잦아들었다. 주파수가 조금씩 맞춰진다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
            case NORMAL -> {
                if (p.getRandom().nextInt(100) < 40) qliphoth = Math.max(0, qliphoth - 1);
                p.sendSystemMessage(Component.literal("  다이얼을 돌려도 방송은 잡음뿐이다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
            case BAD -> {
                if (p.getRandom().nextInt(100) < 80) qliphoth = Math.max(0, qliphoth - 1);
                p.sendSystemMessage(Component.literal("  스피커 그릴 사이로 무언가가 지나갔다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
        }
        if (qliphoth != before) {
            p.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xAAAAAA)));
        }
        if (qliphoth <= 0 && getRadius() == 0) {
            setRadius(4);
            if (level() instanceof ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] T-06-27 주파수 이탈. 시설에 왜곡이 번지기 시작합니다.")
                        .withStyle(st -> st.withColor(0xFF5555)));
                sl.playSound(null, blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1f, 0.4f);
            }
        } else if (qliphoth >= profile().qliphothMax() && getRadius() > 0) {
            setRadius(0);
            if (level() instanceof ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] T-06-27 주파수 안정. 왜곡이 사라졌습니다.")
                        .withStyle(st -> st.withColor(0x55FF55)));
                sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1f, 1.2f);
            }
        }
    }

    // ───────────── 왜곡 구역 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        int r = getRadius();
        if (r <= 0) {
            if (tickCount % 40 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.9, getZ(), 3, 0.25, 0.15, 0.25, 0.01);
            }
            return;
        }
        if (tickCount % 100 == 0) setRadius(Math.min(MAX_RADIUS, r + 3));
        if (tickCount % 20 == 0) {
            for (int i = 0; i < 12; i++) {
                double theta = random.nextDouble() * Math.PI * 2;
                double phi = Math.acos(2 * random.nextDouble() - 1);
                double x = getX() + r * Math.sin(phi) * Math.cos(theta);
                double y = getY() + 1 + r * Math.cos(phi) * 0.5;
                double z = getZ() + r * Math.sin(phi) * Math.sin(theta);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
                sl.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.1, 0.1, 0.1, 0.005);
            }
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(this) <= r) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.2, p.getZ(), 2, 0.4, 0.5, 0.4, 0.02);
                }
            }
        }
        if (tickCount % 60 == 0) {
            for (ServerPlayer p : sl.players()) {
                if (p.isSpectator() || p.isCreative()) continue;
                if (p.distanceTo(this) > r) continue;
                Employees.damageSp(p, 2);
                WorkManager.actionbar(p, Component.literal("지직… 왜곡 구역 (반경 " + r + ")")
                        .withStyle(st -> st.withColor(0xAAAAAA)));
                sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.6f, 0.5f);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_radius", getRadius());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setRadius(in.getIntOr("lc_radius", 0));
    }
}
