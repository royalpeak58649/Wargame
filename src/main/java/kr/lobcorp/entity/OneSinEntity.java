package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
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

/** O-03-03 단 한 번의 악과 수백 가지의 선행 — 탈출하지 않는 ZAYIN */
public class OneSinEntity extends AbnormalityEntity {

    public OneSinEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.ONE_SIN;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.7, getZ(), 2, 0.35, 0.5, 0.35, 0.005);
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        ServerLevel level = p.level();
        if (result == WorkManager.Result.GOOD) {
            Employees.restoreSp(p);
            p.sendSystemMessage(Component.literal("  텅 빈 눈구멍이 당신의 속을 들여다본다. 무언가를 털어놓은 듯 마음이 가벼워졌다. ")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true))
                    .append(Component.literal("(SP 회복)").withStyle(st -> st.withColor(0x55FFFF).withItalic(false))));
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 40, 0.5, 0.6, 0.5, 0.05);
            level.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 0.6f, 1.2f);
        } else if (result == WorkManager.Result.NORMAL) {
            p.sendSystemMessage(Component.literal("  그것은 그저 조용히 떠 있을 뿐이었다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        } else {
            p.sendSystemMessage(Component.literal("  가시관이 삐걱이며 조여든다. 무언가를 들킨 것 같은 기분이다.").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }

        if (s.good >= s.maxBox) {
            for (ServerPlayer other : level.players()) {
                if (other.distanceTo(this) <= 32) {
                    Employees.restoreSp(other);
                    other.sendSystemMessage(Component.literal("[관제] 단 한 번의 악과 수백 가지의 선행이 조용히 빛난다. 부서 전원의 정신력이 회복되었다.")
                            .withStyle(st -> st.withColor(0xF0E6C8)));
                }
            }
        }
    }
}
