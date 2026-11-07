package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

import java.util.UUID;

/** T-04-06 행복한 테디베어 — 같은 직원이 연달아 작업하면 끌어안아 질식시킨다. 탈출 없음. */
public class HappyTeddyBearEntity extends AbnormalityEntity {
    private UUID lastWorker = null;
    /** 클라이언트 전용: 포옹 애니메이션 */
    public int hugTicks = 0;

    public HappyTeddyBearEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.HAPPY_TEDDY;
    }

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        if (lastWorker == null || !lastWorker.equals(p.getUUID())) return true;
        hug(p);
        return false;
    }

    private void hug(ServerPlayer p) {
        ServerLevel sl = p.level();
        lastWorker = null;
        sl.broadcastEntityEvent(this, (byte) 4);
        // 테디베어 앞으로 끌어당긴다
        double yaw = Math.toRadians(getYRot());
        p.teleportTo(getX() - Math.sin(yaw) * 0.9, getY(), getZ() + Math.cos(yaw) * 0.9);
        sl.playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1.5f, 0.6f);
        sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.4, 0.3, 0.01);
        p.sendSystemMessage(Component.literal("  테디베어가 두 팔을 벌려 당신을 꼭 끌어안는다. 숨을 쉴 수가 없다.")
                .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 T-04-06에게 붙잡혔습니다. 같은 직원을 연달아 보내지 마십시오.").withStyle(st -> st.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    /** 작업을 "시작"한 직원을 기억한다 — 패닉 등으로 중단돼도 한 번 들어온 것으로 친다 */
    @Override
    public void onWorkStart(ServerPlayer p, WorkType type) {
        lastWorker = p.getUUID();
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  테디베어는 앞에 놓인 액자만 가만히 바라보고 있다. 풀린 리본을 다시 매어 주었다.";
            case NORMAL -> "  테디베어의 단추 눈 하나가 실 한 가닥에 매달려 흔들린다.";
            case BAD -> "  테디베어의 팔이 아주 조금 움직인 것 같다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) hugTicks = 40;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (hugTicks > 0) hugTicks--;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (lastWorker != null) out.store("lc_last_worker", UUIDUtil.CODEC, lastWorker);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        lastWorker = in.read("lc_last_worker", UUIDUtil.CODEC).orElse(null);
    }
}
