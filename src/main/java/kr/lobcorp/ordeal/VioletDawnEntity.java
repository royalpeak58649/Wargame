package kr.lobcorp.ordeal;

import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 보라색 여명 「이해의 열매」 — 홀을 느리게 떠다니는 열매.
 * 맞기 전에는 아무도 쫓지 않지만, 공격당하면 300틱 동안 공격자를 쫓는다.
 * 일정 시간이 지나면 스스로 터져 주변 직원의 정신력을 무너뜨리고, 주변 이상의 클리포트 카운터를 0으로 만든다.
 */
public class VioletDawnEntity extends OrdealEntity {
    /** 공격당한 뒤 추격을 유지하는 틱 */
    private int angry = 0;
    /** 자폭까지 남은 틱 */
    private int fuse = 1200 + getRandom().nextInt(200);

    public VioletDawnEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public OrdealType ordealType() {
        return OrdealType.VIOLET_DAWN;
    }

    @Override
    protected float[] resist() {
        return new float[]{1.0f, 1.5f, 1.0f, 1.0f};
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hit = super.hurtServer(level, source, amount);
        if (hit && source.getEntity() instanceof ServerPlayer sp) {
            angry = 300;
            setTarget(sp);
        }
        return hit;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        boolean hit = le.hurtServer(level, level.damageSources().magic(), 2f);
        if (!hit) return false;
        level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.3f, 0.7f);
        if (le instanceof ServerPlayer sp) Employees.damageSp(sp, 2);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (angry > 0) angry--;
        if (angry <= 0) setTarget(null);
        if (--fuse <= 0) {
            explode(sl);
            return;
        }
        if (fuse < 400 && fuse % 100 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 0.6f, 1.6f);
            sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.6, getZ(), 10, 0.4, 0.4, 0.4, 0.02);
        }
    }

    /** 자폭. die()가 아니라 discard()로 끝내 처치로 집계되지 않게 한다. */
    private void explode(ServerLevel sl) {
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 이해의 열매가 터졌습니다. 부서의 클리포트 카운터가 무너집니다!")
                .withStyle(s -> s.withColor(0x9B59D0)));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 60, 1.2, 1.0, 1.2, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.8f);
        for (ServerPlayer p : playersNear(sl, position(), 16.0)) Employees.damageSp(p, 12);
        for (Entity e : sl.getAllEntities()) {
            if (e instanceof AbnormalityEntity a && a.isAlive() && a.distanceTo(this) <= 40
                    && a instanceof BreachingAbnormality b && !b.isBreached()) {
                b.lowerQliphoth(null, b.getQliphoth());
            }
        }
        discard();
    }
}
