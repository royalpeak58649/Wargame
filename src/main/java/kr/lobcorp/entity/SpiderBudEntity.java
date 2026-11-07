package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * T-02-43 거미 봉오리 — 천장에 매달려 있는 어미 거미.
 * 통찰 작업을 하거나 신중 I등급인 직원이 작업을 마치면 내려와 고치로 감싼다.
 * 고치가 하나 늘 때마다 이 환상체의 작업이 끝날 때마다 PE-BOX가 1씩 더 쌓인다.
 */
public class SpiderBudEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Integer> COCOONS =
            SynchedEntityData.defineId(SpiderBudEntity.class, EntityDataSerializers.INT);

    public SpiderBudEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.SPIDER_BUD;
    }

    @Override
    public double homeYOffset() {
        return 3.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COCOONS, 0);
    }

    public int getCocoons() {
        return entityData.get(COCOONS);
    }

    private void setCocoons(int n) {
        entityData.set(COCOONS, Math.max(0, n));
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        ServerLevel sl = p.level();
        int n = getCocoons();
        if (n > 0) {
            Facility.addEnergy(sl.getServer(), n);
            p.sendSystemMessage(Component.literal("  고치들이 희미하게 맥동한다. (추가 PE +" + n + ")")
                    .withStyle(st -> st.withColor(0xE8E4DA).withItalic(true)));
            sl.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() - 0.5, getZ(), 8 + n * 2, 1.2, 0.8, 1.2, 0.01);
        }

        EmployeeData d = Employees.get(p);
        if (d.prudence < 30 || s.type == WorkType.INSIGHT) {
            cocoon(p);
            return;
        }

        String text = switch (result) {
            case GOOD -> "  거미줄이 아주 조금 흔들렸다. 어미는 다시 조용해졌다.";
            case NORMAL -> "  바닥을 기어다니던 새끼들이 당신의 발치에서 멈춘다.";
            case BAD -> "  천장에서 무언가가 떨어지는 소리가 났다. 바닥에는 아무것도 없다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    private void cocoon(ServerPlayer p) {
        ServerLevel sl = p.level();
        setCocoons(getCocoons() + 1);
        sl.broadcastEntityEvent(this, (byte) 5);
        sl.playSound(null, blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.6f, 0.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 1, p.getZ(), 80, 0.5, 0.9, 0.5, 0.02);
        sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.7, 0.4, 0.01);
        BlockPos feet = p.blockPosition();
        if (sl.getBlockState(feet).isAir()) {
            sl.setBlock(feet, Blocks.COBWEB.defaultBlockState(), 3);
        }
        p.sendSystemMessage(Component.literal("  여덟 개의 붉은 점이 어둠을 가르고 내려온다. 실이 사방에서 감겨 온다.")
                .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 T-02-43의 고치가 되었습니다.").withStyle(st -> st.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    // ───────────── 어둠 / 연출 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 40 == 0) {
            BlockPos c = home != null ? home : blockPosition();
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) <= 25) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                }
            }
        }
        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() - 0.4, getZ(), 1, 0.8, 0.3, 0.8, 0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 5) {
            // 클라이언트: 고치가 하나 늘었을 때의 흔들림 (모델은 synced COCOONS를 그대로 사용)
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_cocoons", getCocoons());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setCocoons(in.getIntOr("lc_cocoons", 0));
    }
}
