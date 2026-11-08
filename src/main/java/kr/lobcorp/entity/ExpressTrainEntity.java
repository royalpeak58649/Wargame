package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
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
import net.minecraft.world.phys.AABB;

/**
 * T-09-86 지옥행 급행열차 — 도구형.
 * 지붕에 등불 네 개가 달린 작은 매표소. 앞에는 선로가 놓여 있다.
 * 표를 사면 등불의 수만큼 값이 매겨지고, 등불이 모두 켜진 채로 시간이 흐르면 열차가 시설을 가로지른다.
 */
public class ExpressTrainEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Integer> LIGHTS =
            SynchedEntityData.defineId(ExpressTrainEntity.class, EntityDataSerializers.INT);

    /** 등불이 하나 켜지는 주기 (틱) */
    private static final int LAMP_CYCLE = 600;
    /** 표를 사는 직원 주변에 함께 혜택을 받는 거리 */
    private static final double SHARE_RANGE = 24.0;

    private int lampTicks = 0;

    public ExpressTrainEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.EXPRESS_TRAIN;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LIGHTS, 0);
    }

    /** 켜진 등불의 수 (0-4) */
    public int lights() {
        return entityData.get(LIGHTS);
    }

    private void setLights(int n) {
        entityData.set(LIGHTS, Math.max(0, Math.min(4, n)));
    }

    // ───────────── 사용 ─────────────

    @Override
    public void useTool(ServerPlayer p) {
        int n = lights();
        if (n <= 0) {
            p.sendSystemMessage(Component.literal("  아직 표를 팔지 않는다.")
                    .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
            return;
        }
        if (n == 1) {
            p.heal(8f);
            addSp(p, 8);
            p.sendSystemMessage(Component.literal("  표 한 장. 몸이 조금 가벼워졌다.")
                    .withStyle(s -> s.withColor(0xAA55FF).withItalic(true)));
        } else if (n == 2) {
            p.heal(16f);
            addSp(p, 16);
            p.sendSystemMessage(Component.literal("  표 두 장. 상처가 아물고 정신이 맑아진다.")
                    .withStyle(s -> s.withColor(0xAA55FF).withItalic(true)));
        } else if (n == 3) {
            if (p.level() instanceof ServerLevel sl) {
                for (ServerPlayer o : sl.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(SHARE_RANGE))) {
                    if (!o.isAlive() || o.isSpectator()) continue;
                    o.heal(10f);
                    addSp(o, 10);
                }
            }
            p.sendSystemMessage(Component.literal("  표 세 장. 주변의 직원들이 함께 회복했다.")
                    .withStyle(s -> s.withColor(0xAA55FF).withItalic(true)));
        } else {
            if (p.level() instanceof ServerLevel sl) {
                for (ServerPlayer o : sl.getServer().getPlayerList().getPlayers()) {
                    if (!o.isAlive() || o.isSpectator()) continue;
                    o.heal(10f);
                    addSp(o, 10);
                }
            }
            p.sendSystemMessage(Component.literal("  표 네 장. 시설의 모든 직원이 회복했다.")
                    .withStyle(s -> s.withColor(0xAA55FF).withItalic(true)));
        }
        setLights(0);
        lampTicks = 0;
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.2, getZ(), 16, 0.5, 0.4, 0.5, 0.03);
        }
    }

    private static void addSp(ServerPlayer p, int amount) {
        EmployeeData d = Employees.get(p);
        if (d.sp < d.maxSp()) {
            d.sp = Math.min(d.maxSp(), d.sp + amount);
            Employees.sync(p);
        }
    }

    /** 하루가 지나면 매표소는 다시 어두워진다 */
    public void clearDaily() {
        setLights(0);
        lampTicks = 0;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;

        lampTicks++;
        if (lampTicks < LAMP_CYCLE) return;
        lampTicks = 0;

        if (lights() < 4) {
            setLights(lights() + 1);
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 0.9f + lights() * 0.1f);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.4, getZ(), 8, 0.4, 0.2, 0.4, 0.01);
        } else {
            train(sl);
            setLights(0);
        }
    }

    /** 등불이 모두 켜진 채로 시간이 흐르면 열차가 매표소를 지나간다 */
    private void train(ServerLevel sl) {
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 지옥행 급행열차가 도착합니다. 선로에서 벗어나십시오!")
                .withStyle(st -> st.withColor(0xFF5555).withBold(true)));

        final double x = getX();
        final double y = getY();
        final double z = getZ();
        final double halfWidth = 1.5;
        final double halfLength = 80.0;

        for (int i = -80; i <= 80; i++) {
            double px = x + i;
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, px, y + 1.0, z, 2, 0.2, 0.4, halfWidth * 0.6, 0.01);
            if (i % 2 == 0) sl.sendParticles(ParticleTypes.FLAME, px, y + 1.4, z, 1, 0.2, 0.3, halfWidth * 0.5, 0.01);
        }
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 0.6f);

        AABB box = new AABB(x - halfLength, y - 3, z - halfWidth, x + halfLength, y + 4, z + halfWidth);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            p.hurtServer(sl, sl.damageSources().magic(), 20f);
            Employees.damageSp(p, 10);
            p.sendSystemMessage(Component.literal("  열차가 지나갔다. 선로 위에 서 있던 몸이 짓눌렸다.")
                    .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_train_lights", lights());
        out.putInt("lc_train_lamp_ticks", lampTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setLights(in.getIntOr("lc_train_lights", 0));
        lampTicks = in.getIntOr("lc_train_lamp_ticks", 0);
    }
}
