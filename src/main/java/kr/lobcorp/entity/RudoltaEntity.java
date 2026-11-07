package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;

/**
 * F-02-49 썰매 탄 루돌타 — 뿔이 크리스마스 트리처럼 장식된 사슴 형상.
 * 등 뒤에 썰매와 자루를 끌고 다니며, 온몸이 피로 얼룩져 있다.
 * 탈출하면 아무도 쫓지 않고 시설을 느릿하게 배회하며 눈을 흘린다.
 * 3초마다 몸에서 피를 쏟아내, 10블록 안의 직원 정신력을 깎아먹는다.
 */
public class RudoltaEntity extends BreachingAbnormality {
    private static final EntityDimensions BREACHED_SIZE = EntityDimensions.scalable(0.75f, 1.8f);

    /** 피를 쏟아내는 주기 */
    private static final int BLEED_INTERVAL = 60;
    /** 피가 닿는 거리 */
    private static final double BLEED_RADIUS = 10.0;

    public RudoltaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.RUDOLTA;
    }

    @Override
    protected boolean usesMeleeGoal() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 탈출했을 때만 느릿하게 배회한다. 누구도 쫓지 않는다.
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0, 10) {
            @Override
            public boolean canUse() {
                return isBreached() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isBreached() && super.canContinueToUse();
            }
        });
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isBreached() ? BREACHED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    protected String breachAdvice() {
        return "썰매 탄 루돌타는 아무도 쫓지 않습니다. 다만 3초마다 피를 쏟으니 10블록 밖에서 제압하십시오.";
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  뿔에 매달린 전구들이 하나둘 켜진다. 사슴은 조용히 썰매를 바라본다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal("  사슴이 고개를 돌린다. 붉은 코에서 김이 새어 나온다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 50) lowerQliphoth(p, 1);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal("  썰매 위의 자루가 무겁게 가라앉는다. 발밑에 붉은 얼룩이 번진다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (p.getRandom().nextInt(100) < 70) lowerQliphoth(p, 1);
            }
        }
    }

    // ───────────── 탈출: 눈을 흘리며 배회 ─────────────

    @Override
    protected void onBreach(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] F-02-49가 썰매를 끌고 배회합니다. 피가 닿지 않도록 거리를 두십시오.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!isBreached()) return;

        // 발밑에 쌓이는 눈
        if (tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1, getZ(), 3, 0.6, 0.05, 0.6, 0.01);
            sl.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.2, getZ(), 2, 0.7, 0.1, 0.7, 0.01);
        }

        // 3초마다 피를 쏟는다
        if (tickCount % BLEED_INTERVAL == 0) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.0, getZ(), 10, 0.8, 0.6, 0.8, 0.02);
            sl.sendParticles(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, getX(), getY() + 0.8, getZ(), 6, 0.7, 0.4, 0.7, 0.01);
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.2f, 0.5f);
            sl.playSound(null, blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.HOSTILE, 1.0f, 0.6f);
            for (ServerPlayer p : sl.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
                if (p.distanceToSqr(this) > BLEED_RADIUS * BLEED_RADIUS) continue;
                Employees.damageSp(p, 5 + getRandom().nextInt(3));
            }
        }
    }
}
