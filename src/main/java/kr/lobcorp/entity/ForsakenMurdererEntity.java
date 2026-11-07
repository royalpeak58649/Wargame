package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** T-01-54 버려진 살인마 — 나쁨 결과로 카운터가 0이 되면 탈출해 직원을 쫓는 TETH */
public class ForsakenMurdererEntity extends BreachingAbnormality {
    /** 클라이언트 전용: 박치기 애니메이션 */
    public int attackAnimTicks = 0;

    public ForsakenMurdererEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.FORSAKEN_MURDERER;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // 탈출 시에는 무릎으로 기어다녀서 키가 낮다.
        // 폭은 0.8 미만이어야 한다: 열린 철문 사이 틈이 13/16 블록이라 0.9면 문간에 끼어 못 나온다.
        return isBreached() ? EntityDimensions.scalable(0.7f, 1.3f) : super.getDefaultDimensions(pose);
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  그는 격리실 구석에서 조각상처럼 서 있다. 오늘은 조용하다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> p.sendSystemMessage(Component.literal("  벽 쪽에서 둔탁한 소리가 한 번 들렸다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case BAD -> {
                p.sendSystemMessage(Component.literal("  머리를 벽에 찧는 소리가 점점 빨라진다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                level().playSound(null, blockPosition(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 1f, 0.7f);
                lowerQliphoth(p, 1);
            }
        }
    }

    /** 디버그용: 현재 실행 중인 목표 */
    public String goalSelectorDebug() {
        StringBuilder sb = new StringBuilder();
        goalSelector.getAvailableGoals().forEach(g -> sb.append(g.getGoal().getClass().getName()).append(g.isRunning() ? "*" : "").append(","));
        targetSelector.getAvailableGoals().forEach(g -> sb.append(g.getGoal().getClass().getName()).append(g.isRunning() ? "*" : "").append(","));
        return sb.toString();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, (byte) 4);
        playSound(SoundEvents.ANVIL_LAND, 0.6f, 0.6f);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackAnimTicks = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackAnimTicks > 0) attackAnimTicks--;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBreached() ? SoundEvents.ZOMBIE_VILLAGER_AMBIENT : null;
    }
}
