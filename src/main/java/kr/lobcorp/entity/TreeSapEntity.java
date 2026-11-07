package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

/**
 * T-09-80 거대한 나무 수액 — 도구형. 국자로 떠 마시면 HP가 모두 회복되고 재생된다.
 * 마실 때마다 항아리가 터질 확률이 오르고, 터지면 20초 뒤 마신 직원이 터진다.
 */
public class TreeSapEntity extends AbnormalityEntity {
    public static final int SAP_DELAY = 400; // 20초

    public TreeSapEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.TREE_SAP;
    }

    @Override
    public void useTool(ServerPlayer p) {
        ServerLevel sl = p.level();
        p.setHealth(p.getMaxHealth());
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2, false, false));
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1f, 0.8f);
        sl.sendParticles(ParticleTypes.DRIPPING_HONEY, p.getX(), p.getY() + 1.4, p.getZ(), 12, 0.3, 0.3, 0.3, 0.01);
        p.sendSystemMessage(Component.literal("  달콤하고 묵직한 수액이 목을 타고 넘어간다. 몸이 가볍다.")
                .withStyle(st -> st.withColor(0xE09A20).withItalic(true)));

        int chance = ToolState.sapChance;
        if (chance > 0 && p.getRandom().nextInt(100) < chance) {
            EmployeeData d = Employees.get(p);
            d.sapTicks = SAP_DELAY;
            Employees.sync(p);
        }
        ToolState.sapChance = Math.min(60, ToolState.sapChance + 15);
        if (ToolState.sapChance > 0) {
            p.sendSystemMessage(Component.literal("  항아리 바닥에서 무언가 부글거린다…")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.DRIPPING_HONEY, getX(), getY() + 1.2, getZ(), 1, 0.2, 0.1, 0.2, 0);
        }
    }
}
