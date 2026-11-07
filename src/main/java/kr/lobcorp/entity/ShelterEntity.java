package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * T-09-82 3월 27일의 대피소 — 도구형. 해치를 열고 들어가면 한 번에 한 사람만 머무를 수 있다.
 * 안에 있는 동안 어떤 피해도 받지 않고 5초마다 HP·SP가 회복되며, 추격자들이 발자취를 잃는다.
 * 30초를 넘기면 5초마다 시설의 다른 환상체 하나가 격리를 뚫는다.
 */
public class ShelterEntity extends AbnormalityEntity {
    public static final int HEAL_INTERVAL = 100;   // 5초
    public static final int HEAL_AMOUNT = 8;
    public static final int SAFE_TICKS = 600;      // 30초
    public static final double LEAVE_RANGE = 3.0;

    public ShelterEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.SHELTER;
    }

    // ───────────── 사용 ─────────────

    @Override
    public void useTool(ServerPlayer p) {
        if (ToolState.shelterUser == null) {
            enter(p);
            return;
        }
        if (ToolState.shelterUser.equals(p.getUUID())) {
            leave(p, "  해치를 열고 나왔다.", SoundEvents.IRON_TRAPDOOR_OPEN);
            return;
        }
        p.sendOverlayMessage(Component.literal("이미 누군가 안에 있습니다.").withStyle(s -> s.withColor(0xFF5555)));
    }

    private void enter(ServerPlayer p) {
        ServerLevel sl = p.level();
        ToolState.shelterUser = p.getUUID();
        ToolState.shelterTicks = 0;
        ToolState.shelterEntityId = getId();
        p.teleportTo(getX(), getY(), getZ());
        p.setDeltaMovement(0, 0, 0);
        sl.playSound(null, blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.2f, 0.7f);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 12, 0.5, 0.1, 0.5, 0.01);
        p.sendSystemMessage(Component.literal("  해치를 닫았다. 바깥의 소리가 멀어진다. (다시 사용하거나 3블록 밖으로 나가면 나옵니다)")
                .withStyle(s -> s.withColor(0x7CFFE0).withItalic(true)));
        Employees.sync(p);
    }

    private void leave(ServerPlayer p, String msg, net.minecraft.sounds.SoundEvent sound) {
        ServerLevel sl = p.level();
        ToolState.shelterUser = null;
        ToolState.shelterTicks = 0;
        ToolState.shelterEntityId = -1;
        sl.playSound(null, blockPosition(), sound, SoundSource.BLOCKS, 1.2f, 0.8f);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 12, 0.5, 0.1, 0.5, 0.01);
        p.sendSystemMessage(Component.literal(msg).withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
        Employees.sync(p);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.4, getZ(), 1, 0.1, 0.05, 0.1, 0);
        }
        UUID user = ToolState.shelterUser;
        if (user == null) return;
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(user);
        if (p == null || !p.isAlive() || p.level() != sl || p.distanceTo(this) > LEAVE_RANGE) {
            if (p != null) leave(p, "  대피소에서 멀어졌다.", SoundEvents.IRON_TRAPDOOR_OPEN);
            else {
                ToolState.shelterUser = null;
                ToolState.shelterTicks = 0;
                ToolState.shelterEntityId = -1;
            }
            return;
        }
        ToolState.shelterTicks++;
        int t = ToolState.shelterTicks;
        if (t % HEAL_INTERVAL == 0) {
            p.heal(HEAL_AMOUNT);
            EmployeeData d = Employees.get(p);
            d.sp = Math.min(d.maxSp(), d.sp + HEAL_AMOUNT);
            Employees.sync(p);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 0.6, p.getZ(), 4, 0.4, 0.2, 0.4, 0);
        }
        if (t > SAFE_TICKS && t % HEAL_INTERVAL == 0) {
            breakRandomContainment(sl);
        }
    }

    /** 시설의 무작위 탈출형 환상체 하나의 클리포트 카운터를 0으로 만든다 */
    private void breakRandomContainment(ServerLevel sl) {
        List<BreachingAbnormality> candidates = new ArrayList<>();
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof BreachingAbnormality b && !b.isBreached() && b.isAlive()) candidates.add(b);
        }
        if (candidates.isEmpty()) return;
        BreachingAbnormality target = candidates.get(getRandom().nextInt(candidates.size()));
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 대피소 주변 공간이 일그러집니다.")
                .withStyle(s -> s.withColor(0x8A4AC0)));
        target.lowerQliphoth(null, target.getQliphoth());
    }
}
