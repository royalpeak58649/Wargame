package kr.lobcorp.apocalypse;

import kr.lobcorp.entity.BigBirdEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.entity.JudgementBirdEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 종말새 — 세 마리 새가 하나가 된 ALEPH. 무적이며, 세 개의 알이 모두 부서져야 사라진다.
 * 제자리에 떠서 주기적으로 순간이동하고, 정면의 직원에게 BLACK 피해를 준다.
 * 남아 있는 알에 따라 특수 패턴(현혹·빛줄기·황금 저울·거대한 아가리)이 달라진다.
 */
public class ApocalypseBirdEntity extends PathfinderMob {
    /** 특수 패턴 쿨다운 */
    protected int specialCooldown = 0;
    private int teleportIn = 300;
    private int attackTicks = 0;
    /** 어둠 부여 주기 */
    private int darknessTicks = 0;
    /** 다음 멜트다운까지 남은 틱 */
    private int meltdownIn = 1200;

    public ApocalypseBirdEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.FOLLOW_RANGE, 64);
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
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (--teleportIn <= 0) {
            teleportIn = 300 + getRandom().nextInt(201);
            teleport(sl);
        }
        if (specialCooldown > 0) specialCooldown--;
        if (++attackTicks % 100 == 0) {
            attack(sl);
            if (specialCooldown <= 0) special(sl);
        }
        if (++darknessTicks % 100 == 0) darkness(sl);
        if (--meltdownIn <= 0) {
            meltdownIn = 1200 + getRandom().nextInt(1201);
            meltdown(sl);
        }
    }

    /** 무작위 부서 중앙 홀로 순간이동한다 */
    private void teleport(ServerLevel sl) {
        List<BlockPos> centers = Facility.centers(sl.getServer());
        if (centers.isEmpty()) return;
        BlockPos c = centers.get(getRandom().nextInt(centers.size()));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.6, 1.0, 0.6, 0.02);
        snapTo(c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5, getYRot(), 0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.6, 1.0, 0.6, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    /** 정면 6블록 안의 직원에게 BLACK 피해 */
    private void attack(ServerLevel sl) {
        Vec3 look = getLookAngle();
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (distanceTo(p) > 6) continue;
            Vec3 dir = p.position().subtract(position());
            if (dir.lengthSqr() > 1.0E-4 && look.dot(dir.normalize()) <= 0.3) continue;
            float dmg = 14 + getRandom().nextInt(13);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            Employees.damageSp(p, Math.round(dmg));
            sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    /** 특수 패턴. 남아 있는 알에 따라 달라진다. */
    protected void special(ServerLevel sl) {
        int r = getRandom().nextInt(100);
        if (r < 30 && ApocalypseManager.eggAlive(2)) {
            charm(sl);
        } else if (r < 65 && ApocalypseManager.eggAlive(2)) {
            lightBeam(sl);
        } else if (r < 90 && ApocalypseManager.eggAlive(1)) {
            goldenScale(sl);
        } else if (ApocalypseManager.eggAlive(0)) {
            greatMaw(sl);
        } else {
            attack(sl);
            return;
        }
        specialCooldown = 300;
    }

    /** 현혹 — 무작위 직원을 새의 발치로 끌어온다 */
    private void charm(ServerLevel sl) {
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            targets.add(p);
        }
        if (targets.isEmpty()) return;
        int count = Math.min(3, targets.size());
        for (int i = 0; i < count; i++) {
            ServerPlayer p = targets.remove(getRandom().nextInt(targets.size()));
            double a = getRandom().nextDouble() * Math.PI * 2;
            double d = 1.0 + getRandom().nextDouble();
            double x = getX() + Math.cos(a) * d;
            double z = getZ() + Math.sin(a) * d;
            sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
            p.teleportTo(x, getY(), z);
            p.sendSystemMessage(Component.literal("  수많은 눈이 당신을 부른다. 정신을 차리니 새의 발치였다.")
                    .withStyle(s -> s.withColor(0xAA55FF).withItalic(true)));
            sl.sendParticles(ParticleTypes.END_ROD, x, getY() + 1.2, z, 20, 0.4, 0.6, 0.4, 0.03);
        }
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    /** 빛줄기 — 24블록 안의 직원에게 BLACK 피해 */
    private void lightBeam(ServerLevel sl) {
        Vec3 eye = new Vec3(getX(), getY() + getBbHeight() * 0.8, getZ());
        int hit = 0;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (distanceTo(p) > 24) continue;
            if (hit >= 8) break;
            hit++;
            float dmg = 8 + getRandom().nextInt(7);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            Employees.damageSp(p, Math.round(dmg));
            Vec3 to = new Vec3(p.getX(), p.getY() + 1.0, p.getZ());
            Vec3 dir = to.subtract(eye);
            double len = dir.length();
            if (len > 1.0E-4) {
                Vec3 step = dir.normalize().scale(0.5);
                for (double t = 0; t < len; t += 0.5) {
                    Vec3 at = eye.add(step.scale(t / 0.5));
                    sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4f, 1.2f);
    }

    /** 황금 저울 — 시설 전원에게 PALE 피해 */
    private void goldenScale(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            Employees.damagePale(p, 10f);
            sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.3, 0.5, 0.3, 0.03);
        }
        sl.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.6f, 0.6f);
    }

    /** 거대한 아가리 — 8블록 안의 직원에게 RED 피해 */
    private void greatMaw(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            if (ToolState.isSheltered(p)) continue;
            if (distanceTo(p) > 8) continue;
            float dmg = 50 + getRandom().nextInt(31);
            p.hurtServer(sl, sl.damageSources().magic(), dmg);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.6f);
    }

    /** 큰 눈의 알이 살아 있는 동안 시설을 어둠이 덮는다 */
    private void darkness(ServerLevel sl) {
        if (!ApocalypseManager.eggAlive(2)) return;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
        }
    }

    /** 멜트다운 — 무작위 격리실의 클리포트 카운터를 내린다 */
    private void meltdown(ServerLevel sl) {
        List<BreachingAbnormality> pool = new ArrayList<>();
        for (Entity e : sl.getAllEntities()) {
            if (!(e instanceof BreachingAbnormality b)) continue;
            if (b instanceof PunishingBirdEntity || b instanceof BigBirdEntity || b instanceof JudgementBirdEntity) continue;
            if (b.isBreached() || !b.isAlive()) continue;
            pool.add(b);
        }
        if (pool.isEmpty()) return;
        int count = Math.min(4, pool.size());
        for (int i = 0; i < count; i++) {
            BreachingAbnormality b = pool.remove(getRandom().nextInt(pool.size()));
            b.lowerQliphoth(null, 1);
            sl.sendParticles(ParticleTypes.SQUID_INK, b.getX(), b.getY() + 1, b.getZ(), 20, 0.5, 0.8, 0.5, 0.02);
        }
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 종말새의 그림자가 격리실을 덮칩니다.")
                .withStyle(s -> s.withColor(0xAA0000).withBold(true)));
    }
}
