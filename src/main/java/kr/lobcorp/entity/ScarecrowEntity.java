package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * F-01-87 지혜를 찾는 허수아비 — 머릿속이 비어 있는 허수아비. 지혜를 찾아 격리실을 나선다.
 * 탈출하면 마지막으로 본 직원을 쫓고, 쓰러뜨린 직원의 지혜를 가슴에 채워 넣는다 (최대 7번).
 */
public class ScarecrowEntity extends BreachingAbnormality {
    /** 지금까지 가져간 지혜의 수 (최대 7) */
    private int brains = 0;
    /** 클라이언트 전용: 낫을 휘두르는 애니메이션 */
    public int swingTicks = 0;

    public ScarecrowEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 110)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.SCARECROW;
    }

    @Override
    protected double chaseSpeed() {
        return 1.0;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // 탈출 시에도 폭은 0.8 미만이어야 열린 철문 틈(13/16)을 지날 수 있다
        return isBreached() ? EntityDimensions.scalable(0.7f, 2.0f) : super.getDefaultDimensions(pose);
    }

    public int getBrains() {
        return brains;
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(p);
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  허수아비가 고개를 갸웃한다. 머릿속에서 짚이 바스락거리는 소리가 난다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  허수아비의 단추 눈이 당신을 따라 움직인다. 무언가를 배우려는 것 같다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  허수아비가 당신의 이마를 손끝으로 훑는다. 아무것도 얻지 못한 채 고개를 떨군다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                lowerQliphoth(p, 1);
            }
        }
        if (d.prudence >= 45) {
            p.sendSystemMessage(Component.literal("  당신의 머릿속이 너무 환해, 허수아비가 눈을 가린다. 그는 그 빛을 견디지 못한다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            lowerQliphoth(p, 1);
        }
    }

    // ───────────── 탈출 중 전투 ─────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof ServerPlayer p)) return false;
        int d = 3 + level.getRandom().nextInt(5);
        level.broadcastEntityEvent(this, (byte) 4);
        level.playSound(null, blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
        p.hurtServer(level, level.damageSources().mobAttack(this), d);
        Employees.damageSp(p, d);
        if (!p.isAlive() && brains < 7) {
            brains++;
            heal(25f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.8, getZ(), 10, 0.4, 0.4, 0.4, 0.02);
            Facility.broadcast(level.getServer(), Component.literal("[관제] 허수아비가 ").withStyle(st -> st.withColor(0xAA0000))
                    .append(p.getDisplayName())
                    .append(Component.literal(" 직원의 지혜를 가져갔습니다.").withStyle(st -> st.withColor(0xAA0000))));
        }
        return true;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) swingTicks = 12;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (swingTicks > 0) swingTicks--;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isBreached() && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.4, getZ(), 2, 0.4, 0.6, 0.4, 0.01);
        }
    }

    @Override
    protected String breachAdvice() {
        return "허수아비는 지혜를 원합니다. 정신력이 낮은 직원은 특히 조심하십시오.";
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_brains", brains);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        brains = in.getIntOr("lc_brains", 0);
    }
}
