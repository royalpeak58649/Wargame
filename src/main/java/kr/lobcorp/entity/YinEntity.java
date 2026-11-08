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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * O-05-102 음 — 눈이 하나뿐인 검은 잉어.
 * 탈출하면 곁을 지나는 직원에게 어둠의 파동을 뿜고, 잠시 바닥을 표시한 뒤 그 자리를 검게 터뜨린다.
 * 생명이 다하면 작은 검은 조각이 되어 20초 뒤 다시 일어난다. 양이 탈출해 있으면 제압되지 않는다.
 * 양과 만나면 둘 다 격리실로 돌아가고, 그 자리를 지나는 선을 따라 용이 지나간다.
 */
public class YinEntity extends BreachingAbnormality {
    private static final EntityDataAccessor<Boolean> PENDANT =
            SynchedEntityData.defineId(YinEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.9f, 0.9f);
    private static final EntityDimensions PENDANT_SIZE = EntityDimensions.scalable(0.4f, 0.4f);

    /** 어둠의 파동 */
    private static final int PULSE_CD = 60;
    private static final double PULSE_RADIUS = 3.0;
    /** 바닥을 표시한 뒤 터뜨리기까지 */
    private static final int ZONE_WARN = 30;
    private static final int ZONE_CD = 100;
    private static final double ZONE_LENGTH = 8.0;
    private static final double ZONE_HALF_WIDTH = 1.6;
    /** 조각 상태 */
    private static final int PENDANT_TICKS = 400;
    /** 양이 탈출해 있지 않을 때 머무는 자리 (격리실 기준) */
    private static final int WAIT_DX = 6;
    private static final int WAIT_DZ = 0;

    private int pulseCd = PULSE_CD;
    private int zoneCd = ZONE_CD;
    private int zoneWarn = -1;
    private int pendantT = 0;
    private Vec3 zoneDir = new Vec3(0, 0, 1);

    public YinEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.YIN;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PENDANT, false);
    }

    /** 조각(태극의 검은 반쪽) 상태인가 */
    public boolean isPendant() {
        return entityData.get(PENDANT);
    }

    private void setPendant(boolean b) {
        entityData.set(PENDANT, b);
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (isPendant()) return PENDANT_SIZE;
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected double chaseSpeed() {
        return 0.9;
    }

    @Override
    protected String breachAdvice() {
        return "가까이 가면 어둠의 파동에 휩쓸립니다. 바닥이 검게 표시되면 즉시 벗어나십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  검은 잉어가 손끝을 스치고 지나간다. 물비늘이 차갑다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  외눈이 천천히 당신을 따라간다. 짝을 찾는 눈이다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  잉어가 수면을 박차고 물 밖으로 솟구친다. 물방울이 검게 물든다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        pulseCd = PULSE_CD;
        zoneCd = ZONE_CD;
        zoneWarn = -1;
        pendantT = 0;
        setPendant(false);
        sl.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.2f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-05-102 음이 격리실을 빠져나왔습니다. 양이 함께 움직입니다.")
                .withStyle(st -> st.withColor(0xFF5555)));
        // 양이 있으면 함께 탈출한다
        YangEntity yang = findYang(sl);
        if (yang != null && !yang.isBreached()) yang.breach();
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        pulseCd = PULSE_CD;
        zoneCd = ZONE_CD;
        zoneWarn = -1;
        pendantT = 0;
        setPendant(false);
    }

    // ───────────── 조각 / 제압 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isPendant()) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (isBreached() && yangBreached(level().getServer())) {
            becomePendant();
            return;
        }
        super.die(source);
    }

    private void becomePendant() {
        setPendant(true);
        pendantT = PENDANT_TICKS;
        setHealth(1f);
        getNavigation().stop();
        setTarget(null);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.4, getZ(), 30, 0.4, 0.3, 0.4, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.2f, 0.5f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] O-05-102가 작은 검은 조각이 되었습니다. 아직 제압되지 않았습니다.")
                    .withStyle(st -> st.withColor(0xFFAA55)));
        }
    }

    private void revive() {
        setPendant(false);
        setHealth(getMaxHealth());
        pendantT = 0;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.6, getZ(), 40, 0.6, 0.5, 0.6, 0.03);
            sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.2f, 0.7f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 검은 조각이 다시 잉어의 모습으로 일어섰습니다.")
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
    }

    // ───────────── 양 ─────────────

    private static YangEntity findYang(ServerLevel sl) {
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof YangEntity y && y.isAlive()) return y;
        }
        return null;
    }

    private static boolean yangBreached(MinecraftServer server) {
        if (server == null) return false;
        for (Entity e : server.overworld().getAllEntities()) {
            if (e instanceof YangEntity y && y.isAlive() && y.isBreached()) return true;
        }
        return false;
    }

    /** 양의 착용 시간이 30초 쌓였다: 음의 카운터를 1 내린다 */
    public static void lowerCounter(MinecraftServer server, ServerPlayer by) {
        if (server == null) return;
        for (Entity e : server.overworld().getAllEntities()) {
            if (e instanceof YinEntity yin && yin.isAlive() && !yin.isBreached()) {
                yin.lowerQliphoth(by, 1);
                return;
            }
        }
    }

    /** 음과 양이 만났다: 둘 다 격리실로 돌아가고 용이 지나간다 */
    public static void union(ServerLevel sl, YinEntity yin, YangEntity yang) {
        double x = (yin.getX() + yang.getX()) / 2.0;
        double y = (yin.getY() + yang.getY()) / 2.0;
        double z = (yin.getZ() + yang.getZ()) / 2.0;
        sl.sendParticles(ParticleTypes.SQUID_INK, x, y + 0.8, z, 60, 1.0, 0.8, 1.0, 0.05);
        sl.sendParticles(ParticleTypes.END_ROD, x, y + 0.8, z, 60, 1.0, 0.8, 1.0, 0.05);
        sl.playSound(null, new BlockPos((int) x, (int) y, (int) z), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 음과 양이 만났습니다. 용이 시설을 가로지릅니다!")
                .withStyle(st -> st.withColor(0xFF5555).withBold(true)));
        dragonSweep(sl, x, y, z);
        yang.returnToContainment(Component.literal("[관제] O-07-103 양이 격리실로 돌아갔습니다.")
                .withStyle(st -> st.withColor(0x55FF55)));
        yin.returnToContainment(Component.literal("[관제] O-05-102 음이 격리실로 돌아갔습니다.")
                .withStyle(st -> st.withColor(0x55FF55)));
    }

    /** 합일 지점을 지나는 X축 직선(폭 5, 길이 200)을 따라 용이 지나간다 */
    private static void dragonSweep(ServerLevel sl, double x, double y, double z) {
        final double halfWidth = 2.5;
        final double halfLength = 100.0;
        for (int i = -100; i <= 100; i++) {
            double px = x + i;
            sl.sendParticles(ParticleTypes.SQUID_INK, px, y + 1.0, z, 2, 0.2, 0.4, halfWidth * 0.6, 0.01);
            if (i % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, px, y + 1.4, z, 1, 0.2, 0.3, halfWidth * 0.5, 0.01);
        }
        AABB box = new AABB(x - halfLength, y - 6, z - halfWidth, x + halfLength, y + 6, z + halfWidth);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            invertPlayer(p);
        }
        for (Entity e : sl.getAllEntities()) {
            if (!(e instanceof BreachingAbnormality b) || !b.isBreached() || !b.isAlive()) continue;
            if (b instanceof BlackSwanEntity) continue; // 흑조는 용의 길을 비껴간다
            if (!box.contains(b.position())) continue;
            int max = b.profile().qliphothMax();
            b.qliphoth = Math.max(0, max - b.qliphoth);
            sl.sendParticles(ParticleTypes.SQUID_INK, b.getX(), b.getY() + 1.2, b.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
        }
    }

    /** HP·SP 비율을 뒤집는다 (80% → 20%) */
    private static void invertPlayer(ServerPlayer p) {
        float maxHp = p.getMaxHealth();
        float hp = p.getHealth();
        float newHp = Math.max(1f, maxHp - hp);
        p.setHealth(Math.min(maxHp, newHp));
        var d = Employees.get(p);
        int maxSp = d.maxSp();
        d.sp = Math.max(1, maxSp - d.sp);
        Employees.sync(p);
        p.sendSystemMessage(Component.literal("  용이 지나간 자리에서 당신의 안과 밖이 뒤집혔다.")
                .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        if (isPendant()) {
            if (tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0.005);
            }
            if (--pendantT <= 0) {
                if (yangBreached(sl.getServer())) revive();
                else suppress(null);
            }
            return;
        }

        YangEntity yang = findYang(sl);
        boolean yangOut = yang != null && yang.isBreached();

        if (yangOut) {
            // 서로를 향해 다가간다
            getNavigation().moveTo(yang, 1.0);
            if (distanceToSqr(yang) <= 2.0 * 2.0) {
                union(sl, this, yang);
                return;
            }
        } else {
            // 양이 없으면 정해진 자리로 걸어가 멈춘다
            BlockPos base = home != null ? home : blockPosition();
            double wx = base.getX() + WAIT_DX + 0.5, wy = base.getY(), wz = base.getZ() + WAIT_DZ + 0.5;
            if (distanceToSqr(wx, wy, wz) > 2.0) getNavigation().moveTo(wx, wy, wz, 0.8);
            else getNavigation().stop();
            if (tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 1, 0.3, 0.2, 0.3, 0.005);
            }
            return;
        }

        if (tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 1, 0.3, 0.2, 0.3, 0.005);
        }

        // 어둠의 파동
        if (pulseCd > 0) pulseCd--;
        if (pulseCd <= 0) {
            pulseCd = PULSE_CD;
            pulse(sl);
        }

        // 바닥 표시 → 폭발
        if (zoneWarn > 0) {
            zoneWarn--;
            drawZone(sl);
            if (zoneWarn == 0) {
                zoneWarn = -1;
                erupt(sl);
            }
        } else if (zoneCd > 0) {
            zoneCd--;
        } else if (getTarget() != null && getTarget().isAlive()) {
            zoneCd = ZONE_CD;
            zoneWarn = ZONE_WARN;
            Vec3 look = getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            zoneDir = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            sl.playSound(null, blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.2f, 0.7f);
        }
    }

    private void pulse(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 40, PULSE_RADIUS * 0.6, 0.4, PULSE_RADIUS * 0.6, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 1.2f, 0.6f);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            if (p.distanceToSqr(this) > PULSE_RADIUS * PULSE_RADIUS) continue;
            p.hurtServer(sl, sl.damageSources().magic(), 6f);
            Employees.damageSp(p, 4);
            p.sendSystemMessage(Component.literal("  검은 물결이 벽을 뚫고 밀려온다.")
                    .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
        }
    }

    private void drawZone(ServerLevel sl) {
        for (double t = 1.0; t <= ZONE_LENGTH; t += 0.5) {
            for (double w = -ZONE_HALF_WIDTH; w <= ZONE_HALF_WIDTH; w += 0.8) {
                double px = getX() + zoneDir.x * t - zoneDir.z * w;
                double pz = getZ() + zoneDir.z * t + zoneDir.x * w;
                sl.sendParticles(ParticleTypes.SQUID_INK, px, getY() + 0.1, pz, 1, 0.05, 0.02, 0.05, 0);
            }
        }
    }

    private void erupt(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.6f);
        for (double t = 1.0; t <= ZONE_LENGTH; t += 0.5) {
            for (double w = -ZONE_HALF_WIDTH; w <= ZONE_HALF_WIDTH; w += 0.8) {
                double px = getX() + zoneDir.x * t - zoneDir.z * w;
                double pz = getZ() + zoneDir.z * t + zoneDir.x * w;
                sl.sendParticles(ParticleTypes.SQUID_INK, px, getY() + 0.6, pz, 3, 0.2, 0.4, 0.2, 0.02);
            }
        }
        AABB box = new AABB(getX() - ZONE_LENGTH - 2, getY() - 2, getZ() - ZONE_LENGTH - 2,
                getX() + ZONE_LENGTH + 2, getY() + 3, getZ() + ZONE_LENGTH + 2);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == this) continue;
            if (le instanceof AbnormalityEntity a && !a.isBreached()) continue;
            if (!inZone(le)) continue;
            if (le instanceof ServerPlayer p) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
                p.hurtServer(sl, sl.damageSources().magic(), 18f);
                Employees.damageSp(p, 10);
                p.sendSystemMessage(Component.literal("  검은 바닥이 터져 올랐다.")
                        .withStyle(st -> st.withColor(0xC9C3E6).withItalic(true)));
            } else if (le instanceof BreachingAbnormality b) {
                b.hurtServer(sl, sl.damageSources().magic(), 20f);
            }
        }
    }

    private boolean inZone(LivingEntity le) {
        Vec3 rel = new Vec3(le.getX() - getX(), 0, le.getZ() - getZ());
        double along = rel.dot(zoneDir);
        if (along < 0.5 || along > ZONE_LENGTH) return false;
        double side = Math.abs(rel.x * zoneDir.z - rel.z * zoneDir.x);
        return side <= ZONE_HALF_WIDTH;
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_yin_pendant", isPendant());
        out.putInt("lc_yin_pendant_t", pendantT);
        out.putInt("lc_yin_pulse_cd", pulseCd);
        out.putInt("lc_yin_zone_cd", zoneCd);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setPendant(in.getBooleanOr("lc_yin_pendant", false));
        pendantT = in.getIntOr("lc_yin_pendant_t", 0);
        pulseCd = in.getIntOr("lc_yin_pulse_cd", PULSE_CD);
        zoneCd = in.getIntOr("lc_yin_zone_cd", ZONE_CD);
    }
}
