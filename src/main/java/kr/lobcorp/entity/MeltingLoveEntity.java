package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.DamageColor;
import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * D-03-109 녹아내리는 사랑 — 분홍빛 점액으로 된 소녀.
 * 작업 결과가 보통이나 나쁨이면 카운터가 내려가고, 처음 작업한 직원이 그녀의 사랑을 받는다.
 * 탈출하면 점액을 뿜어 BLACK 피해를 주고, 죽은 직원은 점액 덩어리가 되어 그녀를 따른다.
 * RED 피해는 오히려 그녀를 회복시킨다.
 */
public class MeltingLoveEntity extends BreachingAbnormality {
    /** 점액 발사 주기 */
    private static final int SHOT_PERIOD = 120;
    /** 점액 발사 사거리 */
    private static final double SHOT_RANGE = 16.0;
    /** 점액 웅덩이 지속 시간 */
    private static final int PUDDLE_TICKS = 200;
    /** 웅덩이 피해 주기 */
    private static final int PUDDLE_PERIOD = 40;
    /** 웅덩이 피해 반경 */
    private static final double PUDDLE_RADIUS = 2.5;
    /** 웅덩이 피해량 */
    private static final float PUDDLE_DAMAGE = 2f;
    /** 체력 60% 이하에서 점액을 흡수하는 주기 */
    private static final int ABSORB_PERIOD = 300;
    /** 흡수 판정 반경 */
    private static final double ABSORB_RADIUS = 12.0;
    /** 흡수 회복량 */
    private static final float ABSORB_HEAL = 40f;
    /** 직원 사망 → 점액 생성 판정 반경 */
    private static final double DEATH_RADIUS = 32.0;
    /** 점액 최대 수 */
    private static final int MAX_SLIMES = 10;
    /** 격노 시 피해 배율 */
    private static final float ENRAGE_MULT = 1.3f;

    /** 축복받은 직원이 회복하는 SP 주기 */
    private static final int BLESS_SP_PERIOD = 300;
    /** 축복받은 직원이 회복하는 SP */
    private static final int BLESS_SP = 30;
    /** 분홍빛 폭발 주기 */
    private static final int BURST_PERIOD = 200;
    /** 분홍빛 폭발의 감염 반경 */
    private static final double BURST_RADIUS = 6.0;
    /** 감염된 직원이 회복하는 SP 주기 */
    private static final int INFECT_SP_PERIOD = 100;
    /** 감염된 직원이 회복하는 SP */
    private static final int INFECT_SP = 10;
    /** 감염 후 얼굴이 물드는 시점 */
    private static final int INFECT_WARN = 1100;
    /** 감염 후 점액 덩어리로 변하는 시점 */
    private static final int INFECT_TRANSFORM = 2200;
    /** 시설 감염 판정 주기 */
    private static final int SPREAD_CHECK_PERIOD = 100;

    /** 점액 웅덩이 (위치 + 남은 틱) */
    private record Puddle(Vec3 pos, int ticks) {}

    private final List<Puddle> puddles = new ArrayList<>();
    private boolean enraged = false;
    private int shotTicks = 0;
    private int absorbTicks = 0;

    /** 그녀의 사랑을 받는 직원 (null = 없음) */
    private UUID blessed = null;
    /** 감염된 직원 → 감염된 뒤 지난 틱 */
    private final Map<UUID, Integer> infected = new HashMap<>();
    /** 폭발이 멈춰 있는 남은 틱 */
    private int pauseTicks = 0;
    /** 축복 SP 회복 주기 카운터 */
    private int blessSpTicks = 0;
    /** 분홍빛 폭발 주기 카운터 */
    private int burstTicks = 0;
    /** 시설 감염 판정 주기 카운터 */
    private int spreadTicks = 0;

    public MeltingLoveEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.14)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.MELTING_LOVE;
    }

    @Override
    protected String breachAdvice() {
        return "RED 피해는 통하지 않습니다. BLACK E.G.O로 제압하십시오.";
    }

    public boolean isEnraged() {
        return enraged;
    }

    /** 거대한 점액이 죽으면 격노한다 — 체력을 모두 회복하고 더 세게 공격한다 */
    public void enrage() {
        if (!isBreached() || enraged) return;
        enraged = true;
        setHealth(getMaxHealth());
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 60, 0.8, 1.0, 0.8, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.6f, 0.5f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] D-03-109가 격노했습니다.")
                    .withStyle(st -> st.withColor(0xFF3333).withBold(true)));
        }
    }

    // ───────────── 축복 / 감염 ─────────────

    /** 그녀의 사랑을 받는 직원인가 */
    private boolean isBlessed(ServerPlayer p) {
        return blessed != null && blessed.equals(p.getUUID());
    }

    /** 하루가 지나면 사랑과 감염을 모두 초기화한다 */
    public void clearDaily() {
        blessed = null;
        infected.clear();
        pauseTicks = 0;
        blessSpTicks = 0;
        burstTicks = 0;
        spreadTicks = 0;
    }

    /** 감염된 직원을 점액 덩어리로 만든다 */
    private void transform(ServerLevel sl, ServerPlayer p) {
        sl.sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 0.9, 0.6, 0.05);
        sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.4, p.getZ(), 20, 0.5, 0.6, 0.5, 0.03);
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
        MeltingSlimeEntity slime = kr.lobcorp.ModEntities.MELTING_SLIME.create(sl, EntitySpawnReason.EVENT);
        if (slime != null) {
            slime.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
            sl.addFreshEntity(slime);
        }
    }

    /** 분홍빛 폭발 — 주변의 감염되지 않은 직원을 감염시킨다 */
    private void pinkBurst(ServerLevel sl, ServerPlayer source) {
        sl.sendParticles(ParticleTypes.HEART, source.getX(), source.getY() + 1.2, source.getZ(), 24, 0.6, 0.8, 0.6, 0.03);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, source.getX(), source.getY() + 1, source.getZ(), 40, 0.8, 0.8, 0.8, 0.06);
        sl.playSound(null, source.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.7f);
        for (ServerPlayer o : sl.players()) {
            if (o == source || !o.isAlive() || o.isCreative() || o.isSpectator()) continue;
            if (o.distanceTo(source) > BURST_RADIUS) continue;
            if (isBlessed(o) || infected.containsKey(o.getUUID())) continue;
            if (getRandom().nextInt(100) >= 75) continue;
            infected.put(o.getUUID(), 0);
            o.sendSystemMessage(Component.literal("  분홍 점액이 튀었다. 몸이 달콤하게 녹아내린다.")
                    .withStyle(st -> st.withColor(0xFFAAAA).withItalic(true)));
        }
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkStart(ServerPlayer worker, WorkType type) {
        if (type != WorkType.REPRESSION) {
            if (blessed == null) {
                blessed = worker.getUUID();
                worker.sendSystemMessage(Component.literal("  분홍빛 소녀가 당신을 바라보며 웃는다. 그녀의 사랑을 받게 되었다.")
                        .withStyle(st -> st.withColor(0xFFAAAA).withItalic(true)));
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART, worker.getX(), worker.getY() + 1.4, worker.getZ(),
                            20, 0.5, 0.6, 0.5, 0.03);
                }
            }
        } else if (isBlessed(worker)) {
            lowerQliphoth(worker, 1);
            worker.sendSystemMessage(Component.literal("  사랑하는 사람이 그녀를 해치려 한다…")
                    .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
        }
    }

    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        return super.workChanceBonus(worker, type) + (isBlessed(worker) ? 10 : 0);
    }

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        if (result == WorkManager.Result.NORMAL || result == WorkManager.Result.BAD) {
            lowerQliphoth(worker, 1);
        }
        String text = switch (result) {
            case GOOD -> "  분홍빛 점액이 손끝에 닿는다. 따뜻하고, 조금 달콤하다.";
            case NORMAL -> "  그녀가 당신을 오래 바라본다. 점액이 발밑으로 흘러내린다.";
            default -> "  점액이 손을 붙잡는다. 놓아주지 않으려는 것 같다.";
        };
        worker.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xFFAAAA).withItalic(true)));

        if (isBlessed(worker)) {
            EmployeeData d = Employees.get(worker);
            int n = 25 + getRandom().nextInt(11);
            d.sp = Math.min(d.maxSp(), d.sp + n);
            Employees.sync(worker);
        }
        if (s.type == WorkType.INSIGHT && s.good >= 24) {
            pauseTicks = 600;
            worker.sendSystemMessage(Component.literal("  점액이 잠잠해졌다. 30초 동안 폭발이 멈춘다.")
                    .withStyle(st -> st.withColor(0xFFAAAA).withItalic(true)));
        }
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        shotTicks = 0;
        absorbTicks = 0;
        puddles.clear();
        Facility.broadcast(sl.getServer(), Component.literal("[관제] D-03-109가 분홍빛 점액을 흘리며 걸어 나옵니다.")
                .withStyle(st -> st.withColor(0xFF3333)));

        for (UUID id : new ArrayList<>(infected.keySet())) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive()) continue;
            transform(sl, p);
        }
        infected.clear();

        if (blessed != null) {
            ServerPlayer b = sl.getServer().getPlayerList().getPlayer(blessed);
            if (b != null && b.isAlive()) {
                b.hurtServer(sl, sl.damageSources().magic(), 1000f);
                MeltingSlimeEntity slime = kr.lobcorp.ModEntities.MELTING_SLIME.create(sl, EntitySpawnReason.EVENT);
                if (slime != null) {
                    slime.snapTo(b.getX(), b.getY(), b.getZ(), b.getYRot(), 0);
                    slime.setBig(true);
                    sl.addFreshEntity(slime);
                }
                Facility.broadcast(sl.getServer(), Component.literal("[관제] D-03-109의 사랑을 받던 직원이 거대한 점액으로 변했습니다.")
                        .withStyle(st -> st.withColor(0xFF3333)));
            }
            blessed = null;
        }
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        enraged = false;
        shotTicks = 0;
        absorbTicks = 0;
        puddles.clear();
        if (level() instanceof ServerLevel sl) {
            for (MeltingSlimeEntity m : sl.getEntitiesOfClass(MeltingSlimeEntity.class,
                    getBoundingBox().inflate(256.0))) {
                m.discard();
            }
        }
    }

    // ───────────── 공격 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float dmg = 11f + getRandom().nextInt(7);
        if (enraged) dmg *= ENRAGE_MULT;
        boolean hit = target.hurtServer(level, level.damageSources().magic(), dmg);
        if (hit) {
            if (target instanceof ServerPlayer sp) Employees.damageSp(sp, Math.round(dmg));
            level.playSound(null, blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1.2f, 0.7f);
            level.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1.2, target.getZ(),
                    12, 0.3, 0.4, 0.3, 0.05);
        }
        return hit;
    }

    /** 점액 발사 — 대상에게 BLACK 피해를 주고 그 자리에 웅덩이를 남긴다 */
    private void slimeShot(ServerLevel sl, LivingEntity target) {
        float dmg = 18f + getRandom().nextInt(13);
        if (enraged) dmg *= ENRAGE_MULT;
        boolean hit = target.hurtServer(sl, sl.damageSources().magic(), dmg);
        if (hit && target instanceof ServerPlayer sp) Employees.damageSp(sp, Math.round(dmg));
        sl.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1, target.getZ(),
                30, 0.5, 0.6, 0.5, 0.08);
        sl.playSound(null, target.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.4f, 0.6f);
        puddles.add(new Puddle(target.position(), PUDDLE_TICKS));
    }

    /** 웅덩이를 갱신하고, 밟고 있는 직원에게 피해를 준다 */
    private void tickPuddles(ServerLevel sl) {
        for (int i = puddles.size() - 1; i >= 0; i--) {
            Puddle p = puddles.get(i);
            int left = p.ticks() - 1;
            if (left <= 0) {
                puddles.remove(i);
                continue;
            }
            puddles.set(i, new Puddle(p.pos(), left));
            sl.sendParticles(ParticleTypes.ITEM_SLIME, p.pos().x, p.pos().y + 0.1, p.pos().z, 2, 0.6, 0.05, 0.6, 0.0);
            if (left % PUDDLE_PERIOD != 0) continue;
            for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class,
                    net.minecraft.world.phys.AABB.ofSize(p.pos(), PUDDLE_RADIUS * 2, 3.0, PUDDLE_RADIUS * 2))) {
                if (!sp.isAlive() || sp.isCreative() || sp.isSpectator()) continue;
                if (sp.position().distanceTo(p.pos()) > PUDDLE_RADIUS) continue;
                sp.hurtServer(sl, sl.damageSources().magic(), PUDDLE_DAMAGE);
            }
        }
    }

    // ───────────── 내성 ─────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached() && source.getEntity() instanceof LivingEntity attacker
                && attacker.getMainHandItem().getItem() instanceof EgoWeaponItem ego
                && ego.color == DamageColor.RED) {
            // RED 피해는 점액에 흡수되어 오히려 회복된다
            heal(amount);
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.4, getZ(), 8, 0.4, 0.4, 0.4, 0.02);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 직원 사망 → 점액 ─────────────

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (!isBreached() && isBlessed(p)) {
            blessed = null;
            lowerQliphoth(null, qliphoth);
            if (level() instanceof ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] D-03-109의 사랑받던 직원이 죽었습니다.")
                        .withStyle(st -> st.withColor(0xFF3333)));
            }
        }
        infected.remove(p.getUUID());
        if (!isBreached()) return;
        if (p.distanceToSqr(this) > DEATH_RADIUS * DEATH_RADIUS) return;
        if (!(level() instanceof ServerLevel sl)) return;
        if (sl.getEntitiesOfClass(MeltingSlimeEntity.class, getBoundingBox().inflate(256.0)).size() >= MAX_SLIMES) return;
        MeltingSlimeEntity slime = kr.lobcorp.ModEntities.MELTING_SLIME.create(sl, EntitySpawnReason.EVENT);
        if (slime == null) return;
        slime.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        sl.addFreshEntity(slime);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원의 시신이 점액 덩어리가 되어 D-03-109를 따릅니다.")
                        .withStyle(st -> st.withColor(0xFF3333))));
    }

    @Override
    public void onAnyClerkDeath(ClerkEntity c) {
        if (!isBreached()) return;
        if (c.distanceToSqr(this) > DEATH_RADIUS * DEATH_RADIUS) return;
        if (!(level() instanceof ServerLevel sl)) return;
        if (sl.getEntitiesOfClass(MeltingSlimeEntity.class, getBoundingBox().inflate(256.0)).size() >= MAX_SLIMES) return;
        MeltingSlimeEntity slime = kr.lobcorp.ModEntities.MELTING_SLIME.create(sl, EntitySpawnReason.EVENT);
        if (slime == null) return;
        slime.snapTo(c.getX(), c.getY(), c.getZ(), c.getYRot(), 0);
        sl.addFreshEntity(slime);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, c.getX(), c.getY() + 1, c.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 사무직의 시신이 점액 덩어리가 되어 D-03-109를 따릅니다.")
                .withStyle(st -> st.withColor(0xFF3333)));
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            tickBlessing(sl);
            return;
        }
        tickPuddles(sl);

        if (++shotTicks >= SHOT_PERIOD) {
            shotTicks = 0;
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && distanceTo(target) <= SHOT_RANGE) {
                slimeShot(sl, target);
            }
        }

        if (getHealth() < getMaxHealth() * 0.6f && ++absorbTicks >= ABSORB_PERIOD) {
            absorbTicks = 0;
            MeltingSlimeEntity victim = null;
            for (MeltingSlimeEntity m : sl.getEntitiesOfClass(MeltingSlimeEntity.class,
                    getBoundingBox().inflate(ABSORB_RADIUS))) {
                if (m.isBig() || !m.isAlive()) continue;
                victim = m;
                break;
            }
            if (victim != null) {
                victim.discard();
                heal(ABSORB_HEAL);
                sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 40, 0.6, 0.8, 0.6, 0.05);
                Facility.broadcast(sl.getServer(), Component.literal("[관제] D-03-109가 점액 덩어리를 흡수해 상처를 메웁니다.")
                        .withStyle(st -> st.withColor(0xFF5555)));
            }
        }
    }

    /** 격리 중일 때의 축복 / 감염 처리 */
    private void tickBlessing(ServerLevel sl) {
        ServerPlayer b = blessed == null ? null : sl.getServer().getPlayerList().getPlayer(blessed);
        if (b != null && !b.isAlive()) b = null;

        if (b != null && ++blessSpTicks >= BLESS_SP_PERIOD) {
            blessSpTicks = 0;
            EmployeeData d = Employees.get(b);
            d.sp = Math.min(d.maxSp(), d.sp + BLESS_SP);
            Employees.sync(b);
        }

        if (pauseTicks > 0) {
            pauseTicks--;
        } else if (++burstTicks >= BURST_PERIOD) {
            burstTicks = 0;
            if (b != null) pinkBurst(sl, b);
            for (UUID id : new ArrayList<>(infected.keySet())) {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
                if (p == null || !p.isAlive()) continue;
                pinkBurst(sl, p);
            }
        }

        for (UUID id : new ArrayList<>(infected.keySet())) {
            int t = infected.get(id) + 1;
            infected.put(id, t);
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive()) continue;
            if (t % INFECT_SP_PERIOD == 0) {
                EmployeeData d = Employees.get(p);
                d.sp = Math.min(d.maxSp(), d.sp + INFECT_SP);
                Employees.sync(p);
            }
            if (t == INFECT_WARN) {
                p.sendSystemMessage(Component.literal("  얼굴이 분홍빛으로 물들었다…")
                        .withStyle(st -> st.withColor(0xFFAAAA).withItalic(true)));
            }
            if (t >= INFECT_TRANSFORM) {
                transform(sl, p);
                infected.remove(id);
            }
        }

        if (++spreadTicks >= SPREAD_CHECK_PERIOD) {
            spreadTicks = 0;
            if (b != null && !infected.isEmpty()) {
                int online = 0;
                for (ServerPlayer p : sl.players()) {
                    if (p.isCreative() || p.isSpectator()) continue;
                    online++;
                }
                if (infected.size() * 2 >= online) {
                    lowerQliphoth(null, qliphoth);
                    Facility.broadcast(sl.getServer(), Component.literal("[관제] 시설의 절반이 D-03-109에게 물들었습니다.")
                            .withStyle(st -> st.withColor(0xFF3333)));
                }
            }
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_ml_enraged", enraged);
        out.putString("lc_ml_blessed", blessed == null ? "" : blessed.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        enraged = in.getBooleanOr("lc_ml_enraged", false);
        String id = in.getStringOr("lc_ml_blessed", "");
        blessed = id.isEmpty() ? null : UUID.fromString(id);
    }

    @Override
    public boolean isPushable() {
        return isBreached();
    }

    @Override
    public void push(Entity entity) {
        if (isBreached()) super.push(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
