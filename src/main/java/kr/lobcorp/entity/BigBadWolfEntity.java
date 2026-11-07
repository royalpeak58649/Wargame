package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * F-02-58 크고 나빠질 늑대 — 두 발로 선 거대한 회색 늑대.
 * 작업 결과가 나쁘면 작업자를 통째로 삼킨다. 빨간 두건 용병과 작업한 직원도 삼킨다.
 * 용병이 격리실 밖에서 곁에 다가오면 카운터가 곧바로 0이 되고, 늑대는 빠르게 탈출해 물고 할퀸다.
 * 20초마다 울부짖어 정신력을 깎고, 체력이 절반 아래면 다른 환상체의 격리까지 흔든다.
 */
public class BigBadWolfEntity extends BreachingAbnormality {
    public static final double LITTLE_RED_RANGE = 6.0;
    public static final int HOWL_INTERVAL = 400;
    public static final int FLEE_INVULN = 150;

    /** 클라이언트 전용: 울부짖음 / 물기 애니메이션 */
    public int howlAnimTicks = 0;
    public int biteAnimTicks = 0;

    private final Set<UUID> eaten = new HashSet<>();
    private int howlT = HOWL_INTERVAL;
    private int invulnT = 0;
    private float fleeMark = -1f;

    public BigBadWolfEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.BIG_BAD_WOLF;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // 탈출 시 폭 0.75 — 열린 철문 틈(13/16)을 지날 수 있어야 한다
        return isBreached() ? EntityDimensions.scalable(0.75f, 2.4f) : super.getDefaultDimensions(pose);
    }

    @Override
    protected double chaseSpeed() {
        return 1.4;
    }

    // ───────────── 작업 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        EmployeeData d = Employees.get(p);
        if ("little_red".equals(d.lastWorked)) {
            p.sendSystemMessage(Component.literal("  늑대가 코를 벌름거린다. 붉은 두건의 냄새를 기억하고 있다.")
                    .withStyle(s -> s.withColor(0xFF7777).withItalic(true)));
            lowerQliphoth(p, 1);
            devour(p);
            return false;
        }
        return true;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (result == WorkManager.Result.BAD) {
            lowerQliphoth(p, 1);
            devour(p);
            return;
        }
        String text = switch (result) {
            case GOOD -> "  늑대가 입에 문 양털을 천천히 씹는다. 슬픈 눈이 잠시 당신을 따라간다.";
            case NORMAL -> "  늑대의 꼬리가 바닥을 한 번 쓸고 지나간다.";
            case BAD -> "";
        };
        if (!text.isEmpty()) p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    // ───────────── 삼킴 ─────────────

    public void devour(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) return;
        EmployeeData d = Employees.get(p);
        if (d.devoured) return;
        eaten.add(p.getUUID());
        d.devoured = true;
        WorkManager.clear(p);
        BlockPos c = home != null ? home : blockPosition();
        d.devouredAt = new Vec3(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5);
        p.connection.teleport(d.devouredAt.x, d.devouredAt.y, d.devouredAt.z, p.getYRot(), p.getXRot());
        p.setDeltaMovement(0, 0, 0);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.7f);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        p.sendSystemMessage(Component.literal("  거대한 아가리가 당신을 통째로 삼킨다. 축축하고 어두운 곳이다.")
                .withStyle(s -> s.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 늑대에게 삼켜졌습니다.").withStyle(s -> s.withColor(0x8A0000))));
        Employees.sync(p);
    }

    /** 뱃속의 직원을 토해 낸다. bonus면 양의 가호(+9 최대 HP)를 준다. */
    public void release(ServerPlayer p, boolean bonus) {
        EmployeeData d = Employees.get(p);
        if (!d.devoured) return;
        d.devoured = false;
        d.devouredAt = null;
        eaten.remove(p.getUUID());
        p.removeEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
        p.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        p.sendSystemMessage(Component.literal("  늑대의 뱃속에서 빠져나왔다.").withStyle(s -> s.withColor(0x55FF55)));
        if (bonus) {
            d.sheepTicks = true;
            Employees.apply(p);
            p.sendSystemMessage(Component.literal("  양털 냄새가 옷에 배었다. 오늘 하루는 몸이 조금 더 버틴다. (최대 HP +9)")
                    .withStyle(s -> s.withColor(0xF0E6C8)));
        }
        Employees.sync(p);
    }

    private void releaseAll(ServerLevel sl, boolean bonus) {
        for (UUID id : new ArrayList<>(eaten)) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null) release(p, bonus);
        }
        eaten.clear();
    }

    // ───────────── 탈출 ─────────────

    @Override
    protected String breachAdvice() {
        return "늑대의 아가리를 피하고 옆에서 공격하십시오. 20초마다 울부짖습니다.";
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean bite = getRandom().nextBoolean();
        float dmg = bite ? 10 + getRandom().nextInt(5) : 7 + getRandom().nextInt(10);
        biteAnimTicks = 10;
        level.broadcastEntityEvent(this, (byte) 6);
        boolean ok = target.hurtServer(level, level.damageSources().mobAttack(this), dmg);
        level.playSound(null, blockPosition(), bite ? SoundEvents.RAVAGER_ATTACK : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.2f, bite ? 0.8f : 1.1f);
        return ok;
    }

    private void howl(ServerLevel sl) {
        howlAnimTicks = 20;
        sl.broadcastEntityEvent(this, (byte) 5);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.8f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(Component.literal("늑대가 길게 울부짖습니다. 정신을 붙드십시오.").withStyle(s -> s.withColor(0xFF5555))));
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) <= 20) {
                Employees.damageSp(p, 15 + getRandom().nextInt(6));
            }
        }
        if (getHealth() < getMaxHealth() / 2f) {
            List<AbnormalityEntity> others = new ArrayList<>();
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof AbnormalityEntity a && a != this && a.isAlive() && a.distanceTo(this) <= 24) others.add(a);
            }
            for (AbnormalityEntity a : others) {
                if (a instanceof BreachingAbnormality b && !b.isBreached()) {
                    b.lowerQliphoth(null, 1);
                }
            }
        }
    }

    private void flee(ServerLevel sl) {
        BlockPos spot = findHidingSpot(sl);
        if (spot != null) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.5, 0.8, 0.5, 0.02);
            snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, getYRot(), 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.5, 0.8, 0.5, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.6f);
        }
        invulnT = FLEE_INVULN;
        fleeMark = getHealth();
        setTarget(null);
        getNavigation().stop();
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 늑대가 상처를 핥으며 자취를 감췄습니다. 잠시 공격이 통하지 않습니다.")
                .withStyle(s -> s.withColor(0xFFAA00)));
    }

    /** MeatLanternEntity의 숨는 자리 탐색과 같은 방식 */
    private BlockPos findHidingSpot(ServerLevel sl) {
        BlockPos origin = home != null ? home : blockPosition();
        for (int i = 0; i < 24; i++) {
            int dx = getRandom().nextInt(33) - 16;
            int dz = getRandom().nextInt(33) - 16;
            int dy = getRandom().nextInt(7) - 3;
            BlockPos pos = origin.offset(dx, dy, dz);
            if (!sl.isLoaded(pos)) continue;
            if (!sl.getBlockState(pos).isAir()) continue;
            if (!sl.getBlockState(pos.above()).isAir()) continue;
            if (sl.getBlockState(pos.below()).isAir()) continue;
            return pos;
        }
        return null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (invulnT > 0 && isBreached()) {
            level.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 0.6f, 1.4f);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 제압 ─────────────

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            boolean byRed = source.getEntity() instanceof BreachingAbnormality b
                    && "little_red".equals(b.profile().id());
            releaseAll(sl, byRed);
            if (byRed) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] 빨간 두건 용병이 늑대를 베어 넘겼습니다. 뱃속의 직원들이 양털을 뒤집어쓰고 나왔습니다.")
                        .withStyle(s -> s.withColor(0x55FF55)));
            }
        }
        super.die(source);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 5) howlAnimTicks = 20;
        else if (id == 6) biteAnimTicks = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (howlAnimTicks > 0) howlAnimTicks--;
        if (biteAnimTicks > 0) biteAnimTicks--;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (invulnT > 0) invulnT--;
        if (!isBreached()) {
            // 빨간 두건 용병이 격리실 밖에서 곁에 다가오면 카운터가 곧바로 0이 된다
            if (tickCount % 20 == 0 && qliphoth > 0) {
                BlockPos c = home != null ? home : blockPosition();
                for (Entity e : sl.getAllEntities()) {
                    if (e instanceof BreachingAbnormality b && b != this && b.isBreached()
                            && "little_red".equals(b.profile().id())
                            && b.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5) <= LITTLE_RED_RANGE * LITTLE_RED_RANGE) {
                        qliphoth = 0;
                        Facility.broadcast(sl.getServer(), Component.literal("[관제] 빨간 두건 용병이 늑대의 격리실 앞에 섰습니다. 늑대가 이성을 잃습니다!")
                                .withStyle(s -> s.withColor(0xFF5555).withBold(true)));
                        breach();
                        break;
                    }
                }
            }
            return;
        }
        if (--howlT <= 0) {
            howlT = HOWL_INTERVAL;
            howl(sl);
        }
        if (fleeMark < 0) fleeMark = getHealth();
        if (getHealth() <= fleeMark - getMaxHealth() * 0.15f) {
            flee(sl);
        }
        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.4, getZ(), 2, 0.4, 0.6, 0.4, 0.01);
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.store("lc_eaten", UUIDUtil.CODEC.listOf(), new ArrayList<>(eaten));
        out.putInt("lc_howl_t", howlT);
        out.putInt("lc_invuln", invulnT);
        out.putFloat("lc_flee_mark", fleeMark);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        eaten.clear();
        eaten.addAll(in.read("lc_eaten", UUIDUtil.CODEC.listOf()).orElse(List.of()));
        howlT = in.getIntOr("lc_howl_t", HOWL_INTERVAL);
        invulnT = in.getIntOr("lc_invuln", 0);
        fleeMark = in.getFloatOr("lc_flee_mark", -1f);
    }
}
