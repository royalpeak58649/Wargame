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

import java.util.ArrayList;
import java.util.List;

/**
 * O-04-08 붉은 구두 — 받침대 위에 놓인, 윤이 흐르는 붉은 구두 한 켤레.
 * 탈출하지 않는다. 대신 작업 결과가 좋지 않으면 자제가 낮은 직원을 불러 구두를 신게 하고,
 * 홀린 직원은 동료를 덮친다. 동료에게 세 번 맞으면 정신을 차린다.
 */
public class RedShoesEntity extends AbnormalityEntity {
    /** 자제 등급 I~II 상한 (30~44) */
    public static final int LOW_TEMPERANCE = 45;
    public static final int POSSESS_TICKS = 1800; // 90초
    public static final int HEALTH_BOOST_AMP = 4; // +20 HP

    public RedShoesEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.RED_SHOES;
    }

    /** 자제가 낮은 직원은 애착 작업이 아주 쉽다 — 그 쉬움이 함정이다. */
    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        if (type == WorkType.ATTACHMENT && Employees.get(worker).temperance < LOW_TEMPERANCE) return 45;
        return 0;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        String text = switch (result) {
            case GOOD -> "  구두는 얌전히 받침대 위에 놓여 있다. 광택만이 희미하게 흔들린다.";
            case NORMAL -> "  구두 한 짝이 아주 조금, 발끝을 돌린 것처럼 보였다.";
            case BAD -> "  구두가 당신의 발치로 미끄러져 왔다. 신어 보라는 듯이.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));

        int roll = p.getRandom().nextInt(100);
        boolean call = switch (result) {
            case NORMAL -> roll < 30;
            case BAD -> roll < 60;
            case GOOD -> false;
        };
        if (!call) return;
        ServerPlayer target = pickTarget(p);
        if (target == null) return;
        call(target);
    }

    /** 자제가 낮은 직원을 고른다. 가능하면 작업자가 아닌 다른 직원을 우선한다. */
    private ServerPlayer pickTarget(ServerPlayer worker) {
        if (!(level() instanceof ServerLevel sl)) return null;
        List<ServerPlayer> others = new ArrayList<>();
        List<ServerPlayer> all = new ArrayList<>();
        for (ServerPlayer o : sl.players()) {
            if (!o.isAlive() || o.isCreative() || o.isSpectator()) continue;
            EmployeeData d = Employees.get(o);
            if (d.temperance >= LOW_TEMPERANCE) continue;
            if (d.possessTicks > 0) continue;
            all.add(o);
            if (o != worker) others.add(o);
        }
        List<ServerPlayer> pool = !others.isEmpty() ? others : all;
        if (pool.isEmpty()) return null;
        return pool.get(getRandom().nextInt(pool.size()));
    }

    private void call(ServerPlayer target) {
        if (!(level() instanceof ServerLevel sl)) return;
        double yaw = Math.toRadians(homeYaw);
        double tx = getX() + Math.cos(yaw) * 1.2;
        double tz = getZ() + Math.sin(yaw) * 1.2;
        target.teleportTo(tx, getY(), tz);
        target.setYRot(homeYaw + 180f);
        target.setYBodyRot(homeYaw + 180f);
        target.setYHeadRot(homeYaw + 180f);
        sl.playSound(null, blockPosition(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.HOSTILE, 1.4f, 0.5f);
        sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 0.4, getZ(), 40, 0.6, 0.4, 0.6, 0.02);
        target.sendSystemMessage(Component.literal("  구두가 발에 딱 맞는다. 아주 편안하다. 너무 편안하다.")
                .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
        Employees.possess(target, POSSESS_TICKS, "붉은 구두", HEALTH_BOOST_AMP);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-04-08이 ").withStyle(st -> st.withColor(0xAA0000))
                .append(target.getDisplayName())
                .append(Component.literal(" 직원을 불렀습니다. 구두를 신긴 채 동료를 덮치고 있습니다.")
                        .withStyle(st -> st.withColor(0xFF5555))));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 0.3, getZ(), 1, 0.3, 0.1, 0.3, 0);
        }
    }
}
