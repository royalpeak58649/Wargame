package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * F-04-83 요정의 축제 — 작업을 마친 직원에게 축복(HP·SP 회복)을 내린다.
 * 축복을 받은 채 다른 환상체를 작업하면 요정들이 그 직원을 먹어 치운다.
 */
public class FairyFestivalEntity extends AbnormalityEntity {
    public static final int BLESS_TICKS = 1200; // 60초

    public FairyFestivalEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.FAIRY_FESTIVAL;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(p);
        d.fairyTicks = BLESS_TICKS;
        p.sendSystemMessage(Component.literal("  작은 요정들이 웃으며 당신의 어깨 위로 날아와 앉는다. ")
                .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true))
                .append(Component.literal("(축복 60초: HP·SP 회복)").withStyle(st -> st.withColor(0x7CFFE0).withItalic(false))));
        p.sendSystemMessage(Component.literal("  축복이 끝나기 전에 다른 환상체를 작업하면 위험합니다.").withStyle(st -> st.withColor(0xFF7777)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
    }

    /** 축복받은 직원이 다른 환상체에게 갔을 때 */
    public static void devour(ServerPlayer p, AbnormalityEntity other) {
        ServerLevel sl = p.level();
        EmployeeData d = Employees.get(p);
        d.fairyTicks = 0;
        sl.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1, p.getZ(), 60, 0.4, 0.7, 0.4, 0.05);
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 1.5f, 1.4f);
        p.sendSystemMessage(Component.literal("  어깨 위의 요정들이 일제히 입을 벌린다.").withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 요정의 축복을 받은 채 " + other.profile().code() + " 작업을 시도했습니다. 요정들에게 잡아먹혔습니다.")
                        .withStyle(st -> st.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    /** 매 틱, 축복받은 직원에게 */
    public static void tickBlessing(ServerPlayer p, EmployeeData d) {
        if (d.fairyTicks <= 0) return;
        d.fairyTicks--;
        if (d.fairyTicks % 40 == 0) {
            p.heal(1f);
            if (d.sp < d.maxSp()) d.sp++;
        }
        if (d.fairyTicks % 6 == 0) {
            p.level().sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1.6, p.getZ(), 1, 0.4, 0.2, 0.4, 0);
        }
        if (d.fairyTicks == 0) {
            p.sendSystemMessage(Component.literal("  요정들이 하나둘 날아가 버렸다. (축복 종료)").withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 1.2, getZ(), 2, 0.7, 0.6, 0.7, 0);
        }
    }
}
