package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O-01-12 늙은 여인 — 흔들의자에 앉아 아무도 찾아오지 않는 방을 지킨다.
 * 다른 환상체의 작업이 끝날 때마다 클리포트 카운터가 줄고, 0이 되면 격리실이 고독으로 가득 찬다.
 */
public class OldLadyEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Boolean> SOLITUDE =
            SynchedEntityData.defineId(OldLadyEntity.class, EntityDataSerializers.BOOLEAN);

    public OldLadyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.OLD_LADY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOLITUDE, false);
    }

    public boolean isSolitude() {
        return entityData.get(SOLITUDE);
    }

    private void setSolitude(boolean b) {
        entityData.set(SOLITUDE, b);
    }

    // ───────────── 클리포트 카운터 ─────────────

    @Override
    public void onOtherWorkFinished(ServerPlayer worker, AbnormalityEntity other, WorkManager.Result result) {
        if (isSolitude()) return;
        qliphoth = Math.max(0, qliphoth - 1);
        if (qliphoth <= 0) enterSolitude();
    }

    private void enterSolitude() {
        if (isSolitude()) return;
        setSolitude(true);
        if (level() instanceof ServerLevel sl) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] O-01-12의 격리실이 고독으로 가득 찼습니다.")
                    .withStyle(st -> st.withColor(0x8A9BB8)));
            sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1f, 0.6f);
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 40, 0.6, 0.8, 0.6, 0.01);
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> {
                qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
                if (qliphoth > 0) setSolitude(false);
                p.sendSystemMessage(Component.literal("  여인이 흔들의자를 천천히 밀어 준다. 방 안의 공기가 조금은 가벼워졌다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
            case NORMAL -> {
                qliphoth = Math.min(profile().qliphothMax(), qliphoth + 1);
                if (qliphoth > 0) setSolitude(false);
                p.sendSystemMessage(Component.literal("  여인은 창밖만 바라본다. 대답은 없다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
            case BAD -> p.sendSystemMessage(Component.literal("  흔들의자가 삐걱이고, 방이 한 뼘 더 어두워진다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    // ───────────── 고독 상태 ─────────────

    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        return isSolitude() ? -20 : 0;
    }

    @Override
    public float workDamageMultiplier(ServerPlayer worker) {
        return isSolitude() ? 2.0f : 1f;
    }

    @Override
    public void onWorkStart(ServerPlayer p, WorkType type) {
        if (!isSolitude()) return;
        p.sendSystemMessage(Component.literal("  방 안을 메운 고독이 숨통을 조여 온다.")
                .withStyle(st -> st.withColor(0x8A9BB8).withItalic(true)));
        Employees.damageSp(p, 4);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isSolitude() && tickCount % 40 == 0) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(this) <= 6) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                }
            }
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.2, getZ(), 6, 0.5, 0.6, 0.5, 0.01);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_solitude", isSolitude());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setSolitude(in.getBooleanOr("lc_solitude", false));
    }
}
