package kr.lobcorp.apocalypse;

import kr.lobcorp.DamageColor;
import kr.lobcorp.item.EgoWeaponItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * 종말새 사건의 알. 세 개 중 하나라도 남아 있으면 종말새는 쓰러지지 않는다.
 * 0 = 작은 부리, 1 = 긴 팔, 2 = 큰 눈. E.G.O 무기로만 부술 수 있고, 속성에 따라 반응이 다르다.
 */
public class ApocalypseEggEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(ApocalypseEggEntity.class, EntityDataSerializers.INT);

    private int particleTicks = 0;

    public ApocalypseEggEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 640)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    /** 알의 종류를 정하고 최대 체력을 그에 맞춘다 (0 부리 640 / 1 팔 990 / 2 눈 640) */
    public void setKind(int k) {
        int v = Math.max(0, Math.min(2, k));
        entityData.set(KIND, v);
        float hp = v == 1 ? 990f : 640f;
        var attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(hp);
        setHealth(hp);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean ego = source.getEntity() instanceof LivingEntity le
                && le.getMainHandItem().getItem() instanceof EgoWeaponItem;
        if (!ego) return false;
        DamageColor color = source.getEntity() instanceof LivingEntity le2
                && le2.getMainHandItem().getItem() instanceof EgoWeaponItem w ? w.color : DamageColor.RED;
        float mult = 1f;
        boolean heal = false;
        switch (kind()) {
            case 0 -> { // 작은 부리: RED 회복, WHITE 감소, BLACK 증가, PALE 약간 감소
                switch (color) {
                    case RED -> heal = true;
                    case WHITE -> mult = .5f;
                    case BLACK -> mult = 1.2f;
                    case PALE -> mult = .8f;
                }
            }
            case 1 -> { // 긴 팔: PALE 회복, 나머지 감소
                switch (color) {
                    case PALE -> heal = true;
                    default -> mult = .5f;
                }
            }
            default -> { // 큰 눈: BLACK 회복, WHITE 증가, 나머지 감소
                switch (color) {
                    case BLACK -> heal = true;
                    case WHITE -> mult = 1.2f;
                    default -> mult = .5f;
                }
            }
        }
        if (heal) {
            setHealth(Math.min(getMaxHealth(), getHealth() + amount * 2f));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 8, 0.4, 0.5, 0.4, 0.02);
            return false;
        }
        return super.hurtServer(level, source, amount * mult);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl && sl.getServer() != null) {
            ApocalypseManager.eggDestroyed(sl.getServer(), kind());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (++particleTicks % 20 != 0) return;
        switch (kind()) {
            case 0 -> sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1, getZ(), 8, 0.5, 0.8, 0.5, 0.01);
            case 1 -> sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 6, 0.5, 0.8, 0.5, 0.02);
            default -> sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 6, 0.5, 0.8, 0.5, 0.02);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_egg_kind", kind());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setKind(in.getIntOr("lc_egg_kind", 0));
    }
}
