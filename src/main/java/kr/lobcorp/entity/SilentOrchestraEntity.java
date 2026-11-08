package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.DamageColor;
import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * T-01-31 침묵의 오케스트라 — 무대 위에 선 지휘자.
 * 작업 결과가 좋음/나쁨이면 클리포트 카운터가 내려가고, 탈출하면 가장 가까운 부서 중앙 홀에서 다섯 악장을 연주한다.
 * 악장마다 통하는 피해 속성이 하나뿐이며, 마지막 악장이 끝날 때 정신력이 절반 미만인 직원은 모두 죽는다.
 */
public class SilentOrchestraEntity extends BreachingAbnormality {
    /** 0 = 격리 중, 1..5 = 악장 */
    private static final EntityDataAccessor<Integer> MOVEMENT =
            SynchedEntityData.defineId(SilentOrchestraEntity.class, EntityDataSerializers.INT);

    /** 악장별 지속 시간 (틱) */
    private static final int[] MOVEMENT_TICKS = {0, 420, 300, 240, 520, 400};
    /** 악장별 이름 */
    private static final String[] MOVEMENT_NAMES = {
            "", "아다지오 에 트란퀼로", "소스테누토", "아첼레란도 에 크레셴도", "스트린젠도", "피날레 — 콘 푸오코"
    };
    /** 정신력 파동 주기 */
    private static final int PULSE_PERIOD = 100;
    /** 피날레 판정 반경 */
    private static final double FINALE_RADIUS = 40.0;

    private int movementTicks = 0;
    private int pulseTicks = 0;

    public SilentOrchestraEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.SILENT_ORCHESTRA;
    }

    /** 지휘자는 움직이지도, 직접 때리지도 않는다 (목표 없음) */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected boolean leavesRoomOnBreach() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOVEMENT, 0);
    }

    public int movement() {
        return entityData.get(MOVEMENT);
    }

    private void setMovement(int m) {
        entityData.set(MOVEMENT, m);
    }

    @Override
    protected String breachAdvice() {
        return "악장마다 통하는 속성이 바뀝니다. 피날레 전에 정신력을 절반 이상으로 유지하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  지휘봉이 가볍게 튕긴다. 어디선가 박수가 들린다.";
            case NORMAL -> "  지휘자는 아무 말 없이 박자만 센다. 조용하다.";
            default -> "  지휘봉이 거칠게 내리꽂힌다. 불협화음이 벽을 긁고 지나간다.";
        };
        worker.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        if (result == WorkManager.Result.GOOD || result == WorkManager.Result.BAD) {
            lowerQliphoth(worker, 1);
        }
    }

    // ───────────── 탈출 / 복귀 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        List<BlockPos> centers = Facility.centers(sl.getServer());
        if (!centers.isEmpty()) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos c : centers) {
                double d = c.distSqr(blockPosition());
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
            if (best != null) {
                snapTo(best.getX() + 0.5, best.getY() + 1, best.getZ() + 0.5, getYRot(), 0);
            }
        }
        movementTicks = 0;
        pulseTicks = 0;
        setMovement(1);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] T-01-31의 공연이 시작되었습니다. 제1악장 — 아다지오 에 트란퀼로")
                .withStyle(st -> st.withColor(0xFF3333).withBold(true)));
        playMovementSound(sl);
    }

    @Override
    protected void returnToContainment(MutableComponent announcement) {
        super.returnToContainment(announcement);
        setMovement(0);
        movementTicks = 0;
        pulseTicks = 0;
    }

    private void playMovementSound(ServerLevel sl) {
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 1.4f, 0.8f);
        }
    }

    // ───────────── 악장 진행 ─────────────

    private void startMovement(ServerLevel sl, int m) {
        setMovement(m);
        movementTicks = 0;
        pulseTicks = 0;
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 제" + m + "악장 — " + MOVEMENT_NAMES[m])
                .withStyle(st -> st.withColor(0xFF3333)));
        playMovementSound(sl);
        if (m == 4) {
            Facility.get(sl.getServer()).energy = 0;
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 스트린젠도 — 오늘 모은 에너지가 모두 흩어졌습니다.")
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
    }

    private void endFinale(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (distanceTo(p) > FINALE_RADIUS) continue;
            EmployeeData d = Employees.get(p);
            if (d.sp >= d.maxSp() / 2) continue;
            p.sendSystemMessage(Component.literal("  마지막 음이 끝나자, 당신은 더 이상 아무것도 들을 수 없었다.")
                    .withStyle(st -> st.withColor(0xFF5555).withItalic(true)));
            p.hurtServer(sl, sl.damageSources().magic(), 1000f);
        }
        returnToContainment(Component.literal("[관제] 공연이 끝났습니다. T-01-31이 무대에서 사라졌습니다.")
                .withStyle(st -> st.withColor(0x55FF55)));
    }

    // ───────────── 정신력 파동 ─────────────

    private void pulse(ServerLevel sl) {
        int mv = movement();
        if (mv <= 0) return;
        int base = switch (mv) {
            case 1, 2 -> 15 + getRandom().nextInt(6);
            case 3 -> 25 + getRandom().nextInt(6);
            case 4 -> 20 + getRandom().nextInt(6);
            default -> 25 + getRandom().nextInt(6);
        };
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            double d = distanceTo(p);
            double ratio = ringRatio(mv, d);
            if (ratio <= 0) continue;
            int amount = Math.max(1, (int) Math.round(base * ratio));
            Employees.damageSp(p, amount);
            if (mv == 3) {
                p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));
            }
        }
        // 사무직도 같은 음파에 휩쓸린다 (대피소 판정 없음)
        for (ClerkEntity c : sl.getEntitiesOfClass(ClerkEntity.class, getBoundingBox().inflate(40.0))) {
            if (!c.isAlive()) continue;
            double cd = distanceTo(c);
            double cratio = ringRatio(mv, cd);
            if (cratio <= 0) continue;
            c.damageSp(Math.max(1, (int) Math.round(base * cratio)));
        }
        // 지휘자 주위로 음표가 고리 모양으로 맴돈다
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            double r = 2.5 + mv * 0.5;
            sl.sendParticles(ParticleTypes.NOTE, getX() + Math.cos(a) * r, getY() + 1.6, getZ() + Math.sin(a) * r,
                    1, 0.05, 0.2, 0.05, 0.0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.2f, 0.6f + mv * 0.1f);
    }

    /** 거리별 피해 비율 */
    private static double ringRatio(int mv, double d) {
        if (d <= 8) return 1.0;
        if (d <= 16) {
            return switch (mv) {
                case 1 -> 0.0;
                case 2 -> 0.33;
                case 3 -> 0.5;
                default -> 0.77;
            };
        }
        if (d <= 24) {
            return switch (mv) {
                case 1, 2 -> 0.0;
                case 3 -> 0.25;
                default -> 0.66;
            };
        }
        if (d <= 40) {
            return mv >= 4 ? 0.55 : 0.0;
        }
        return 0.0;
    }

    // ───────────── 내성 ─────────────

    @Override
    protected float resistMultiplier(DamageColor c) {
        return switch (movement()) {
            case 1 -> c == DamageColor.PALE ? 2.0f : 0.0f;
            case 2 -> c == DamageColor.BLACK ? 2.0f : 0.0f;
            case 3 -> c == DamageColor.WHITE ? 2.0f : 0.0f;
            case 4 -> c == DamageColor.RED ? 2.0f : 0.0f;
            case 5 -> 0.0f;
            default -> super.resistMultiplier(c);
        };
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBreached()) {
            boolean ego = source.getEntity() instanceof LivingEntity le
                    && le.getMainHandItem().getItem() instanceof EgoWeaponItem;
            if (!ego) return false;
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) {
            if (movement() != 0) setMovement(0);
            return;
        }
        int mv = movement();
        if (mv <= 0 || mv >= MOVEMENT_TICKS.length) return;
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        movementTicks++;
        if (++pulseTicks >= PULSE_PERIOD) {
            pulseTicks = 0;
            pulse(sl);
        }
        if (movementTicks >= MOVEMENT_TICKS[mv]) {
            if (mv >= 5) {
                endFinale(sl);
            } else {
                startMovement(sl, mv + 1);
            }
        }
    }

    // ───────────── 저장 ─────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_so_movement", movement());
        out.putInt("lc_so_movement_ticks", movementTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setMovement(in.getIntOr("lc_so_movement", 0));
        movementTicks = in.getIntOr("lc_so_movement_ticks", 0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** 디버그용 */
    public String movementDebug() {
        return "movement=" + movement() + " t=" + movementTicks;
    }
}
