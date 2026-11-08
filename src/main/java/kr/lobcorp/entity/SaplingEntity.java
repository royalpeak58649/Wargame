package kr.lobcorp.entity;

import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 묘목 — 기생 나무의 축복을 받은 채 죽은 직원의 몸에서 자라난 어린 나무.
 * 환상체가 아니며, E.G.O 무기로만 제대로 피해가 들어간다. 움직이지 않고 주변에 독가스를 뿜는다.
 */
public class SaplingEntity extends PathfinderMob {
    public static final double GAS_RADIUS = 6.0;
    public static final int GAS_SP = 3;

    public SaplingEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    /** 직원의 몸에서 묘목이 자라난다 */
    public static void spawnFrom(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return;
        SaplingEntity sapling = kr.lobcorp.ModEntities.SAPLING.create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (sapling == null) return;
        sapling.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        sl.addFreshEntity(sapling);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원의 몸에서 묘목이 자라났습니다.").withStyle(s -> s.withColor(0xAA55FF))));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float mult = 0.5f;
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof EgoWeaponItem ego) {
                mult = switch (ego.color) {
                    case RED -> 1.2f;
                    case WHITE -> 0.6f;
                    case BLACK -> 0.8f;
                    case PALE -> 0.8f;
                };
            }
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 != 0) return;
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.8, getZ(), 8, 2.0, 0.6, 2.0, 0.01);
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            if (p.distanceToSqr(getX(), getY(), getZ()) > GAS_RADIUS * GAS_RADIUS) continue;
            Employees.damageSp(p, GAS_SP);
            p.sendSystemMessage(Component.literal("  묘목이 내뿜는 독가스가 목을 조인다.")
                    .withStyle(st -> st.withColor(0xAA55FF).withItalic(true)));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
