package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.ModEntities;
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
 * O-01-67 라티샤 — 탈출하지 않는다.
 * 작업 결과가 보통이면 작업자에게 하트 모양 선물을 건넨다.
 * 선물을 지닌 직원이 다른 환상체와 작업을 시작하면 선물에 금이 가고,
 * 그 작업이 절반을 넘는 순간 배가 터져 라티샤의 친구가 기어 나온다.
 */
public class LaetitiaEntity extends AbnormalityEntity {
    public LaetitiaEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.LAETITIA;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(p);
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  라티샤가 방긋 웃으며 치마 자락을 흔들었다. 오늘은 아무것도 건네지 않는다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                if (d.laetitiaHeart) {
                    p.sendSystemMessage(Component.literal("  라티샤가 고개를 갸웃한다. 이미 받은 선물을 또 주려던 참이었나 보다.")
                            .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                    return;
                }
                d.laetitiaHeart = true;
                p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                p.sendSystemMessage(Component.literal("  라티샤가 작은 선물을 건넸다. 머리 위에 하트가 떠 있다.")
                        .withStyle(st -> st.withColor(0xFF9AD0).withItalic(true)));
                p.sendSystemMessage(Component.literal("  이 선물을 지닌 채 다른 환상체를 작업하면 위험합니다.")
                        .withStyle(st -> st.withColor(0xFF7777)));
                Employees.sync(p);
            }
            case BAD -> p.sendSystemMessage(Component.literal("  라티샤가 선물 상자를 뒤로 감추고 시무룩한 얼굴을 지었다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    /** 선물이 터진다. 직원은 죽고, 그 자리에서 라티샤의 친구가 기어 나온다. */
    public static void burst(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        EmployeeData d = Employees.get(p);
        d.laetitiaHeart = false;
        WorkManager.clear(p);
        sl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 2, 0.3, 0.4, 0.3, 0);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
        sl.playSound(null, p.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원의 선물이 터졌습니다! 무언가 기어 나옵니다.").withStyle(st -> st.withColor(0xFF5555))));
        LaetitiaFriendEntity friend = ModEntities.LAETITIA_FRIEND.create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (friend != null) {
            friend.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
            sl.addFreshEntity(friend);
        }
        Employees.sync(p);
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.4, getZ(), 1, 0.5, 0.4, 0.5, 0);
        }
    }
}
