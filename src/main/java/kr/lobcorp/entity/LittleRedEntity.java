package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.DamageColor;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.FacilityData;
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

/**
 * F-01-57 빨간 두건 용병 — 붉은 두건의 여성 용병.
 * 애착 작업은 시설 에너지를 바친 「의뢰」로 바뀐다. 고용되면 가장 가까운 탈출 환상체를 쫓아가 베어 넘긴다.
 * 카운터가 0이 되면 적대적으로 탈출해 도끼와 권총으로 직원을 덮친다. 늑대의 냄새를 맡으면 광폭화한다.
 */
public class LittleRedEntity extends BreachingAbnormality {
    public static final int MAX_COUNTER = 3;
    public static final double HIRE_RANGE = 64.0;
    public static final double WOLF_RANGE = 24.0;
    public static final String WOLF_ID = "big_bad_wolf";

    /** 클라이언트 전용: 사격 애니메이션 */
    public int shotAnimTicks = 0;

    private int hiredTargetId = -1;
    private boolean hired = false;
    private int shotCd = 0;
    private boolean berserk = false;

    public LittleRedEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.LITTLE_RED;
    }

    public boolean isBerserk() {
        return berserk;
    }

    public boolean isHired() {
        return hired;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // 탈출 시 폭 0.6 — 열린 철문 틈(13/16)을 지날 수 있어야 한다
        return isBreached() ? EntityDimensions.scalable(0.6f, 1.8f) : super.getDefaultDimensions(pose);
    }

    @Override
    protected double chaseSpeed() {
        return 1.35;
    }

    // ───────────── 작업 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        if (type == WorkType.ATTACHMENT) {
            request(p);
            return false;
        }
        EmployeeData d = Employees.get(p);
        if (WOLF_ID.equals(d.lastWorked)) {
            p.sendSystemMessage(Component.literal("  늑대 냄새가 난다… 그녀가 칼자루를 쥔다.")
                    .withStyle(s -> s.withColor(0xFF7777).withItalic(true)));
            lowerQliphoth(p, 1);
        }
        return true;
    }

    // ───────────── 의뢰 (애착 작업 대체) ─────────────

    private void request(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (hired) {
            WorkManager.actionbar(p, Component.literal("그녀는 이미 임무를 수행 중이다.")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return;
        }
        if (isBreached()) {
            WorkManager.actionbar(p, WorkManager.red("탈출한 상태에서는 의뢰할 수 없습니다."));
            return;
        }
        BreachingAbnormality target = null;
        double best = HIRE_RANGE * HIRE_RANGE;
        for (Entity e : sl.getAllEntities()) {
            if (!(e instanceof BreachingAbnormality b) || b == this || !b.isBreached() || b.isFriendly()) continue;
            double dist = b.distanceToSqr(p);
            if (dist < best) {
                best = dist;
                target = b;
            }
        }
        if (target == null) {
            WorkManager.actionbar(p, WorkManager.red("사냥할 대상이 없습니다. 탈출한 환상체가 없습니다."));
            return;
        }
        int cost = hireCost(target.profile().risk());
        FacilityData f = Facility.get(sl.getServer());
        if (f.energy < cost) {
            WorkManager.actionbar(p, WorkManager.red("의뢰할 에너지가 부족합니다. (필요 " + cost + ", 보유 " + f.energy + ")"));
            return;
        }
        f.energy = Math.max(0, f.energy - cost);
        hired = true;
        hiredTargetId = target.getId();
        setFriendly(true);
        setBreached(true);
        breachCountdown = -1;
        setTarget(null);
        getNavigation().stop();

        p.sendSystemMessage(Component.literal("  그녀가 두건을 고쳐 쓰고 도끼를 어깨에 걸친다. \"값은 쳤어. 어디 있지?\"")
                .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
        p.sendSystemMessage(Component.literal("  시설 에너지 -" + cost + "   (남은 에너지 " + f.energy + ")")
                .withStyle(s -> s.withColor(0xFFAA00)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 빨간 두건 용병을 고용했습니다. 목표: " + target.profile().name())
                        .withStyle(s -> s.withColor(0xFF5555))));
        sl.playSound(null, blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.8f, 1.2f);
    }

    private static int hireCost(String risk) {
        return switch (risk) {
            case "ZAYIN", "TETH" -> 80;
            case "HE" -> 120;
            case "WAW" -> 160;
            case "ALEPH" -> 200;
            default -> 120;
        };
    }

    // ───────────── 고용 임무 ─────────────

    private void tickHired(ServerLevel sl) {
        Entity e = sl.getEntity(hiredTargetId);
        boolean valid = e instanceof BreachingAbnormality b && b.isAlive() && b.isBreached() && !b.isFriendly();
        if (!valid) {
            returnHome(sl);
            return;
        }
        LivingEntity target = (LivingEntity) e;
        getNavigation().moveTo(target, 1.3);
        if (distanceTo(target) <= 2.5 && tickCount % 15 == 0) {
            float base = 14 + getRandom().nextInt(5);
            if (target instanceof BreachingAbnormality b) {
                b.takeEgoDamage(DamageColor.RED, base, this);
            } else {
                target.hurtServer(sl, sl.damageSources().mobAttack(this), base);
            }
            sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.9f);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.2, target.getZ(), 4, 0.3, 0.3, 0.3, 0);
        }
    }

    private void returnHome(ServerLevel sl) {
        if (home == null) {
            finishHire(sl);
            return;
        }
        getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.2);
        if (distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) <= 4.0) {
            finishHire(sl);
        }
    }

    private void finishHire(ServerLevel sl) {
        hired = false;
        hiredTargetId = -1;
        setFriendly(false);
        setBreached(false);
        setTarget(null);
        getNavigation().stop();
        if (home != null) snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, homeYaw, 0);
        Facility.broadcast(sl.getServer(), Component.literal("  용병이 임무를 마치고 돌아왔다.")
                .withStyle(s -> s.withColor(0xAAAAAA)));
    }

    // ───────────── 적대 탈출 ─────────────

    private void tickHostile(ServerLevel sl) {
        // 늑대의 냄새 → 광폭화
        if (!berserk && tickCount % 20 == 0) {
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof BreachingAbnormality b && b != this && b.isBreached()
                        && WOLF_ID.equals(b.profile().id()) && distanceTo(b) <= WOLF_RANGE) {
                    berserk = true;
                    setTarget(b);
                    Facility.broadcast(sl.getServer(), Component.literal("[관제] 빨간 두건 용병이 늑대의 냄새를 맡았습니다. 이성을 잃었습니다!")
                            .withStyle(s -> s.withColor(0xFF5555).withBold(true)));
                    sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.7f);
                    break;
                }
            }
        }
        if (shotCd > 0) shotCd--;
        // 사무직·요원도 사격 대상이 된다 (플레이어 전용 처리는 아래에서 분기)
        if (shotCd <= 0 && getTarget() instanceof LivingEntity tp && distanceTo(tp) <= 12 && hasLineOfSight(tp)) {
            shotCd = 80;
            shotAnimTicks = 10;
            sl.broadcastEntityEvent(this, (byte) 5);
            float dmg = 5 + getRandom().nextInt(3);
            if (tp.hasEffect(MobEffects.POISON)) dmg += 5;
            tp.hurtServer(sl, sl.damageSources().mobAttack(this), dmg);
            Vec3 from = getEyePosition();
            Vec3 to = tp.getEyePosition();
            Vec3 dir = to.subtract(from);
            double len = dir.length();
            Vec3 step = dir.normalize().scale(0.5);
            for (double t = 0; t < len; t += 0.5) {
                Vec3 pt = from.add(step.scale(t * 2));
                sl.sendParticles(ParticleTypes.SMOKE, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0);
            }
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.3f, 1.8f);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float dmg = 6 + getRandom().nextInt(4);
        if (berserk) dmg *= 1.5f;
        boolean ok = target.hurtServer(level, level.damageSources().mobAttack(this), dmg);
        if (target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
        }
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.9f);
        return ok;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 5) shotAnimTicks = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (shotAnimTicks > 0) shotAnimTicks--;
    }

    // ───────────── 사건 ─────────────

    @Override
    public void onAnyBreach(BreachingAbnormality who) {
        if (hired) return;
        if (who != null && who.profile().id().equals("punishing_bird")) return;
        lowerQliphoth(null, 1);
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  그녀가 도끼날을 손등으로 닦는다. \"나쁘지 않아. 다음에도 그렇게 해.\"";
            case NORMAL -> "  두건 아래에서 짧은 한숨이 새어 나온다.";
            case BAD -> "  그녀가 벽에 도끼를 박아 넣는다. \"…시간 낭비였어.\"";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (hired) {
            tickHired(sl);
        } else if (isBreached()) {
            tickHostile(sl);
        }
        if (isBreached() && tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.2, getZ(), 2, 0.3, 0.5, 0.3, 0.01);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_hired", hired);
        out.putInt("lc_hired_target", hiredTargetId);
        out.putBoolean("lc_berserk", berserk);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        hired = in.getBooleanOr("lc_hired", false);
        hiredTargetId = in.getIntOr("lc_hired_target", -1);
        berserk = in.getBooleanOr("lc_berserk", false);
    }
}
