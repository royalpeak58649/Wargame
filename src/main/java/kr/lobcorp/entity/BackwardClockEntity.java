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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * D-09-104 거꾸로 가는 시계 — 도구형.
 * 네 개의 등불을 하나씩 밝힌 뒤 사용하면 시간이 거꾸로 흘러
 * 탈출한 환상체가 격리실로 돌아가고, 하수인들이 사라지며, 클리포트 카운터가 복구된다.
 * 대신 시계를 돌린 직원은 그 자리에서 죽고, 시계는 그날 하루 멈춘다.
 * 등불을 밝히는 데 실패하거나 감당할 수 없는 직원이 돌리면 시계는 사람을 해친다.
 */
public class BackwardClockEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Integer> LIGHTS =
            SynchedEntityData.defineId(BackwardClockEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GONE =
            SynchedEntityData.defineId(BackwardClockEntity.class, EntityDataSerializers.BOOLEAN);

    /** 등불을 밝히려면 가장 높은 스탯이 III 등급(45) 이상이어야 한다 */
    private static final int LIGHT_STAT = 45;
    /** 시계를 돌리려면 IV 등급 이상 스탯이 세 개 필요하다 */
    private static final int ACTIVATE_LEVEL = 3;

    private boolean usedToday = false;

    public BackwardClockEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.BACKWARD_CLOCK;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LIGHTS, 0);
        builder.define(GONE, false);
    }

    /** 켜진 등불 수 (0~4) */
    public int lights() {
        return entityData.get(LIGHTS);
    }

    private void setLights(int v) {
        entityData.set(LIGHTS, Math.max(0, Math.min(4, v)));
    }

    /** 오늘 이미 멈춘 시계인가 (렌더러가 시계를 감춘다) */
    public boolean isGone() {
        return entityData.get(GONE);
    }

    private void setGone(boolean b) {
        entityData.set(GONE, b);
    }

    /** IV·V 등급인 스탯의 개수 */
    private static int level(EmployeeData d) {
        int n = 0;
        if (isHigh(d.fortitude)) n++;
        if (isHigh(d.prudence)) n++;
        if (isHigh(d.temperance)) n++;
        if (isHigh(d.justice)) n++;
        return n;
    }

    private static boolean isHigh(int v) {
        String g = EmployeeData.grade(v);
        return "IV".equals(g) || "V".equals(g);
    }

    // ───────────── 사용 ─────────────

    @Override
    public void useTool(ServerPlayer p) {
        if (isGone()) {
            p.sendOverlayMessage(Component.literal("시계는 오늘 이미 멈췄다.")
                    .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
            return;
        }
        if (lights() < 4) {
            EmployeeData d = Employees.get(p);
            int best = Math.max(Math.max(d.fortitude, d.prudence), Math.max(d.temperance, d.justice));
            if (best >= LIGHT_STAT) {
                setLights(lights() + 1);
                ServerLevel sl = p.level();
                sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.4, getZ(), 8, 0.3, 0.2, 0.3, 0.02);
                p.sendSystemMessage(Component.literal("  시계 위의 등불이 하나 켜졌다. (" + lights() + "/4)")
                        .withStyle(s -> s.withColor(0xAA55FF).withItalic(true)));
            } else {
                p.sendSystemMessage(Component.literal("  시계는 아무 반응도 하지 않는다.")
                        .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
            }
            return;
        }
        activate(p);
    }

    private void activate(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        if (level(Employees.get(p)) >= ACTIVATE_LEVEL) {
            succeed(p, sl);
        } else {
            fail(p, sl);
        }
    }

    /** 시간이 거꾸로 흐른다 */
    private void succeed(ServerPlayer p, ServerLevel sl) {
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof BreachingAbnormality b && b.isBreached() && !(b instanceof BlackSwanEntity)) {
                b.returnToContainment(Component.literal("[관제] 시간이 거꾸로 흘러 " + b.profile().code() + "가 격리실로 돌아갔습니다.")
                        .withStyle(st -> st.withColor(0x55FF55)));
            }
        }
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof WorkerBeeEntity || e instanceof LittlePrinceMinionEntity
                    || e instanceof SaplingEntity || e instanceof NakedWormEntity) {
                e.discard();
            }
        }
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof AbnormalityEntity a && !a.isBreached() && !a.profile().isTool()) {
                a.resetQliphoth();
            }
        }
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.6, getZ(), 120, 1.2, 1.0, 1.2, 0.15);
        sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.6, getZ(), 80, 1.0, 1.0, 1.0, 0.2);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 시계를 거꾸로 돌렸습니다. 시간이 되감기고 시설이 정리되었습니다.")
                        .withStyle(st -> st.withColor(0x55FF55))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
        setGone(true);
        usedToday = true;
        setLights(0);
    }

    /** 시계를 감당하지 못한 직원 */
    private void fail(ServerPlayer p, ServerLevel sl) {
        for (ServerPlayer o : sl.getServer().getPlayerList().getPlayers()) {
            if (o == p || !o.isAlive() || o.isCreative() || o.isSpectator()) continue;
            if (getRandom().nextInt(2) == 0) {
                o.hurtServer(sl, sl.damageSources().magic(), 1000f);
            } else {
                Employees.panic(o);
            }
        }
        sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 2.0, getZ(), 40, 0.8, 0.8, 0.8, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 시계를 감당하지 못했습니다. 시간이 뒤틀립니다.")
                        .withStyle(st -> st.withColor(0xFF5555))));
        setLights(0);
    }

    /** 하루가 지나면 시계는 다시 제자리로 돌아온다 */
    public void clearDaily() {
        setGone(false);
        usedToday = false;
        setLights(0);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isGone()) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.4, getZ(), lights(), 0.3, 0.1, 0.3, 0);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_clock_lights", lights());
        out.putBoolean("lc_clock_gone", isGone());
        out.putBoolean("lc_clock_used", usedToday);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setLights(in.getIntOr("lc_clock_lights", 0));
        setGone(in.getBooleanOr("lc_clock_gone", false));
        usedToday = in.getBooleanOr("lc_clock_used", false);
    }
}
