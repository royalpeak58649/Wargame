package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.ModEntities;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * D-01-106 검은 군단 — 분홍 하트를 단 작은 군인들.
 * 본능 작업은 「보호」로 바뀌어 군인들이 그 직원을 따라다니며 지킨다.
 * 카운터가 0이 되면 검은 군인 넷이 각 부서 중앙 홀로 행진해 터진다.
 */
public class ArmyInBlackEntity extends BreachingAbnormality {
    /** 보호 지속 시간 */
    private static final int PROTECT_TICKS = 1200;
    /** 보호 중 저항 효과 주기 */
    private static final int RESIST_PERIOD = 100;
    /** 보호 중 회복 주기 */
    private static final int HEAL_PERIOD = 200;
    /** 보호 회복 반경 */
    private static final double HEAL_RADIUS = 8.0;
    /** 보호 회복량 (HP / SP) */
    private static final float HEAL_HP = 12f;
    private static final int HEAL_SP = 12;
    /** 직원 사망 몇 명마다 카운터가 내려가는가 */
    private static final int DEATHS_PER_LOWER = 5;
    /** 탈출 시 행진하는 군인 수 */
    private static final int SOLDIERS = 4;

    /** 보호받는 직원 */
    private UUID protectedId = null;
    /** 보호 남은 틱 */
    private int protectTicks = 0;
    /** 보호 주기 카운터 */
    private int resistTicks = 0;
    private int healTicks = 0;
    /** 시설에서 죽은 직원 수 */
    private int deaths = 0;

    public ArmyInBlackEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.ARMY_IN_BLACK;
    }

    @Override
    protected String breachAdvice() {
        return "군인을 공격하지 않으면 해치지 않습니다. 공격하면 총을 쏩니다.";
    }

    /** 격리실에 머문다 — 나가는 것은 군인들뿐이다 */
    @Override
    protected boolean leavesRoomOnBreach() {
        return false;
    }

    @Override
    protected void registerGoals() {
        // 스스로 움직이지 않는다
    }

    // ───────────── 작업 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer worker, WorkType type) {
        if (type != WorkType.INSTINCT) return super.beforeWorkStart(worker, type);
        protect(worker);
        return false;
    }

    /** 본능 작업 = 「보호」. 군인들이 그 직원을 따라다니며 지킨다. */
    private void protect(ServerPlayer worker) {
        protectedId = worker.getUUID();
        protectTicks = PROTECT_TICKS;
        resistTicks = 0;
        healTicks = 0;
        lowerQliphoth(worker, 1);
        worker.sendSystemMessage(Component.literal("  분홍 군인들이 경례한다. 이제 당신을 지킬 것이다.")
                .withStyle(st -> st.withColor(0xFFAAAA).withItalic(true)));
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, worker.getX(), worker.getY() + 1.4, worker.getZ(),
                    20, 0.5, 0.6, 0.5, 0.03);
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        if (s.type == WorkType.REPRESSION) {
            lowerQliphoth(worker, 1);
            worker.sendSystemMessage(Component.literal("  군인들이 총구를 당신에게 돌린다. 보호가 거두어졌다.")
                    .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
        }
        if (result == WorkManager.Result.BAD) {
            lowerQliphoth(worker, 1);
        }
        String text = switch (result) {
            case GOOD -> "  군인들이 조용히 경례한다. 하트가 조금 더 붉어졌다.";
            case NORMAL -> "  군인들이 대열을 정비한다. 아무도 다치지 않았다.";
            default -> "  군인들이 흩어진다. 누군가를 지키지 못했다.";
        };
        worker.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    // ───────────── 직원 사망 ─────────────

    @Override
    public void onAnyEmployeeDeath(ServerPlayer p) {
        if (isBreached()) return;
        deaths++;
        if (deaths % DEATHS_PER_LOWER == 0) lowerQliphoth(null, 1);
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        protectTicks = 0;
        protectedId = null;
        List<BlockPos> centers = new ArrayList<>(Facility.centers(sl.getServer()));
        Collections.shuffle(centers, new java.util.Random(getRandom().nextLong()));
        int n = Math.min(SOLDIERS, centers.size());
        for (int i = 0; i < n; i++) {
            BlockPos c = centers.get(i);
            ArmySoldierEntity soldier = ModEntities.ARMY_SOLDIER.create(sl, EntitySpawnReason.EVENT);
            if (soldier == null) continue;
            int dx = (i % 2 == 0 ? -12 : 12);
            int dz = (i < 2 ? -3 : 3);
            soldier.snapTo(c.getX() + dx + 0.5, c.getY(), c.getZ() + dz + 0.5, 0, 0);
            soldier.setGoal(c);
            sl.addFreshEntity(soldier);
        }
        Facility.broadcast(sl.getServer(), Component.literal(
                        "[관제] D-01-106 — 검은 군인 넷이 각 부서로 행진합니다. 공격하지 않으면 해치지 않습니다.")
                .withStyle(st -> st.withColor(0xFF3333)));
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        protectTicks = 0;
        protectedId = null;
        if (level() instanceof ServerLevel sl) {
            for (ArmySoldierEntity s : sl.getEntitiesOfClass(ArmySoldierEntity.class,
                    getBoundingBox().inflate(256.0))) {
                s.discard();
            }
        }
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isBreached()) {
            if (sl.getEntitiesOfClass(ArmySoldierEntity.class, getBoundingBox().inflate(256.0)).isEmpty()) {
                returnToContainment(Component.literal("[관제] 검은 군단의 행진이 끝났습니다.")
                        .withStyle(st -> st.withColor(0x55FF55)));
            }
            return;
        }
        if (protectTicks <= 0) return;
        if (--protectTicks <= 0) {
            protectedId = null;
            return;
        }
        ServerPlayer target = protectedId == null ? null : sl.getServer().getPlayerList().getPlayer(protectedId);
        if (target == null || !target.isAlive()) {
            protectTicks = 0;
            protectedId = null;
            return;
        }
        if (++resistTicks >= RESIST_PERIOD) {
            resistTicks = 0;
            target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, false, false));
            sl.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.6, target.getZ(),
                    6, 0.4, 0.4, 0.4, 0.02);
        }
        if (++healTicks >= HEAL_PERIOD) {
            healTicks = 0;
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, target.getBoundingBox().inflate(HEAL_RADIUS))) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                p.heal(HEAL_HP);
                EmployeeData d = Employees.get(p);
                d.sp = Math.min(d.maxSp(), d.sp + HEAL_SP);
                Employees.sync(p);
                sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.4, p.getZ(), 4, 0.4, 0.5, 0.4, 0.02);
            }
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_aib_protect", protectTicks);
        out.putInt("lc_aib_deaths", deaths);
        out.putString("lc_aib_protected", protectedId == null ? "" : protectedId.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        protectTicks = in.getIntOr("lc_aib_protect", 0);
        deaths = in.getIntOr("lc_aib_deaths", 0);
        String id = in.getStringOr("lc_aib_protected", "");
        protectedId = id.isEmpty() ? null : UUID.fromString(id);
    }
}