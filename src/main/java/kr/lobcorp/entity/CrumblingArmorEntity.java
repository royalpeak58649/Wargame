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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * O-05-61 부서지는 갑옷 — 텅 빈 갑옷 안에서 푸른 빛이 새어 나온다.
 * 용기가 부족한 직원이 다가가면 그 자리에서 목이 날아간다. 탈출하지 않는다.
 */
public class CrumblingArmorEntity extends AbnormalityEntity {
    public CrumblingArmorEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.CRUMBLING_ARMOR;
    }

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        EmployeeData d = Employees.get(p);
        if (d.fortitude < 30) {
            decapitate(p, "용기 " + EmployeeData.grade(d.fortitude) + "등급");
            return false;
        }
        return true;
    }

    /** 목이 베인다. 어디서든 호출 가능. */
    public static void decapitate(ServerPlayer p, String reason) {
        ServerLevel sl = p.level();
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2f, 0.5f);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1.5, p.getZ(), 3, 0.2, 0.1, 0.2, 0);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.6, p.getZ(), 12, 0.25, 0.15, 0.25, 0.02);
        p.sendSystemMessage(Component.literal("  갑옷 안쪽에서 푸른 빛이 번뜩인다. 목덜미에 서늘한 바람이 스친다.")
                .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 O-05-61에게 목이 베였습니다. (" + reason + ")")
                        .withStyle(st -> st.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  갑옷의 틈으로 푸른 빛이 잦아든다. 누군가의 숨소리 같은 것이 들렸다.";
            case NORMAL -> "  투구가 아주 조금 기울어져 있다. 처음부터 그랬던 것 같기도 하다.";
            case BAD -> "  갑옷 조각 하나가 바닥에 떨어져 부서진다. 녹슨 냄새가 진동한다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));

        if (s.type != WorkType.REPRESSION) return;
        EmployeeData d = Employees.get(p);
        if (d.hasGift("courage")) return;
        d.gifts.add("courage");
        Employees.apply(p);
        Employees.sync(p);
        Facility.broadcast(p.level().getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 E.G.O 기프트 「용기」를 받았습니다. (이동 속도 +10%)")
                        .withStyle(st -> st.withColor(0xF0E6C8))));
        p.sendSystemMessage(Component.literal("  갑옷이 스스로 무릎을 꿇는다. 무언가가 당신의 걸음에 깃들었다.")
                .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        p.sendSystemMessage(Component.literal("  경고: 「용기」를 지닌 채 애착 작업을 하면 목숨을 잃습니다.")
                .withStyle(st -> st.withColor(0xFF5555)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.7, getZ(), 1, 0.15, 0.1, 0.15, 0);
        }
    }
}
