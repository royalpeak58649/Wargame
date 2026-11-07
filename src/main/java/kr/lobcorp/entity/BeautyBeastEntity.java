package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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
 * F-02-44 미녀와 야수 — 초록빛 눈이 잔뜩 박힌 곤충과 짐승의 뒤섞인 몸.
 * 억압 작업은 가장 잘 통하지만 야수를 상처 입히고, 상처 입은 야수에게 또 억압을 마치면
 * 저주가 작업자에게 옮겨 붙어 그 직원이 새로운 야수가 된다.
 */
public class BeautyBeastEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Boolean> WOUNDED =
            SynchedEntityData.defineId(BeautyBeastEntity.class, EntityDataSerializers.BOOLEAN);

    /** 마지막으로 저주를 이어받은 직원의 이름 (비어 있으면 없음) */
    private String cursedName = "";

    public BeautyBeastEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.BEAUTY_BEAST;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WOUNDED, false);
    }

    public boolean isWounded() {
        return entityData.get(WOUNDED);
    }

    private void setWounded(boolean b) {
        entityData.set(WOUNDED, b);
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (s.type == WorkType.REPRESSION) {
            if (!isWounded()) {
                setWounded(true);
                p.sendSystemMessage(Component.literal("  미녀와 야수가 고통스럽게 몸을 웅크린다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                p.level().playSound(null, blockPosition(), SoundEvents.SPIDER_HURT, SoundSource.HOSTILE, 1.2f, 0.6f);
            } else {
                curse(p);
            }
            return;
        }
        if (isWounded()) {
            setWounded(false);
            p.sendSystemMessage(Component.literal("  미녀와 야수의 상태가 나아졌다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    private void curse(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1, getZ(), 60, 0.8, 0.7, 0.8, 0.02);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
        sl.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.7, 0.4, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 1.2f, 1.4f);

        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 저주를 이어받아 새로운 「미녀와 야수」가 되었습니다.")
                        .withStyle(st -> st.withColor(0xFF5555))));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("저주는 끝나지 않는다")
                .withStyle(st -> st.withColor(0xAAAAAA))));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("저주")
                .withStyle(st -> st.withColor(0x2E7D32).withBold(true))));

        cursedName = p.getGameProfile().name();
        setCustomName(Component.literal(cursedName + "였던 것"));
        setCustomNameVisible(true);
        setWounded(false);
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    // ───────────── 연출 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 1.2, getZ(), 1, 0.6, 0.4, 0.6, 0);
        }
        if (isWounded() && tickCount % 40 == 0) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.6, getZ(), 3, 0.5, 0.3, 0.5, 0.01);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_wounded", isWounded());
        out.putString("lc_cursed", cursedName);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setWounded(in.getBooleanOr("lc_wounded", false));
        cursedName = in.getStringOr("lc_cursed", "");
        if (!cursedName.isEmpty()) {
            setCustomName(Component.literal(cursedName + "였던 것"));
            setCustomNameVisible(true);
        }
    }
}
