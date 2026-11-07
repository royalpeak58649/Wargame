package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O-01-92 오늘의 수줍은 표정 — 천에 걸린 다섯 개의 얼굴 중 하나 뒤에 숨어 있다.
 * 얼굴이 바뀔 때마다 작업 성공률과 실패 피해가 달라진다. 탈출 없음.
 */
public class TodaysShyLookEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Integer> FACE =
            SynchedEntityData.defineId(TodaysShyLookEntity.class, EntityDataSerializers.INT);

    /** 얼굴이 바뀌는 주기 (30초) */
    public static final int FACE_INTERVAL = 600;

    public TodaysShyLookEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.entityData.set(FACE, 2);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.TODAYS_SHY_LOOK;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FACE, 2);
    }

    public int getFace() {
        return entityData.get(FACE);
    }

    private void setFace(int f) {
        entityData.set(FACE, Math.max(0, Math.min(4, f)));
    }

    // ───────────── 얼굴별 효과 ─────────────

    @Override
    public int workChanceBonus(ServerPlayer p, WorkType type) {
        return switch (getFace()) {
            case 0 -> 30;
            case 1 -> 10;
            case 3 -> -30;
            case 4 -> -50;
            default -> 0;
        };
    }

    @Override
    public float workDamageMultiplier(ServerPlayer p) {
        return switch (getFace()) {
            case 0 -> 0.5f;
            case 3 -> 1.5f;
            case 4 -> 2f;
            default -> 1f;
        };
    }

    @Override
    public void onBoxResult(ServerPlayer p, boolean success) {
        if (!success) return;
        EmployeeData d = Employees.get(p);
        switch (getFace()) {
            case 0 -> {
                p.heal(1f);
                if (d.sp < d.maxSp()) d.sp++;
                p.level().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.6, p.getZ(), 2, 0.3, 0.2, 0.3, 0);
            }
            case 1 -> {
                if (d.sp < d.maxSp()) d.sp++;
                if (d.sp < d.maxSp()) d.sp++;
                p.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.6, p.getZ(), 2, 0.3, 0.2, 0.3, 0);
            }
            default -> {
            }
        }
    }

    // ───────────── 작업 시작 / 종료 ─────────────

    @Override
    public void onWorkStart(ServerPlayer p, WorkType type) {
        p.sendSystemMessage(Component.literal("  천 너머의 얼굴: " + faceName(getFace()) + " (" + faceEffect(getFace()) + ")")
                .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  수줍은 무언가가 천 뒤에서 조용히 손을 흔들었다.";
            case NORMAL -> "  얼굴들이 잠시 일제히 같은 방향을 바라본다.";
            case BAD -> "  천이 흔들리고, 얼굴 하나가 천천히 찡그려진다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    public static String faceName(int face) {
        return switch (face) {
            case 0 -> "활짝 웃는 얼굴";
            case 1 -> "웃는 얼굴";
            case 3 -> "슬픈 얼굴";
            case 4 -> "화난 얼굴";
            default -> "무표정한 얼굴";
        };
    }

    public static String faceEffect(int face) {
        return switch (face) {
            case 0 -> "성공률 +30%, 성공할 때마다 HP·SP 회복, 실패 피해 x0.5";
            case 1 -> "성공률 +10%, 성공할 때마다 SP 회복";
            case 3 -> "성공률 -30%, 실패 피해 x1.5";
            case 4 -> "성공률 -50%, 실패 피해 x2";
            default -> "효과 없음";
        };
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % FACE_INTERVAL == 0 && !WorkManager.isAbnoBusy(this)) {
            int next = getRandom().nextInt(5);
            if (next == getFace()) next = (next + 1 + getRandom().nextInt(4)) % 5;
            setFace(next);
            sl.playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1.2f, 0.7f);
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.6, getZ(), 8, 0.6, 0.3, 0.2, 0.01);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_face", getFace());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setFace(in.getIntOr("lc_face", 2));
    }
}
