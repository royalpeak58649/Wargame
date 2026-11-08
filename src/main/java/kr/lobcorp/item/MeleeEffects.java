package kr.lobcorp.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 근접 E.G.O 무기의 종류별 부가 효과(관통·광역·연격). */
public final class MeleeEffects {
    /** 재진입 방지 — 부가 피해가 다시 AFTER_DAMAGE로 들어오는 것을 막는다. */
    private static boolean busy = false;

    /** 플레이어 UUID → {연격 수, 마지막 타격 tick} */
    private static final Map<UUID, int[]> COMBO = new HashMap<>();

    private MeleeEffects() {
    }

    /** 속도형 무기 연격 배율. 3타마다 1.5배. */
    public static float comboMultiplier(ServerPlayer attacker, ServerLevel sl) {
        int now = (int) sl.getGameTime();
        int[] state = COMBO.computeIfAbsent(attacker.getUUID(), k -> new int[]{0, now});
        if (now - state[1] > 40) state[0] = 0;
        state[1] = now;
        state[0]++;
        if (state[0] >= 3) {
            state[0] = 0;
            Vec3 p = attacker.getEyePosition().add(attacker.getLookAngle().normalize().scale(1.5));
            sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 10, 0.3, 0.3, 0.3, 0.1);
            return 1.5f;
        }
        return 1.0f;
    }

    /** 근접 타격 후 무기 종류별 부가 효과. */
    public static void afterMelee(LivingEntity target, ServerPlayer attacker, float damageTaken) {
        if (busy) return;
        if (!(attacker.getMainHandItem().getItem() instanceof EgoWeaponItem ego)) return;
        WeaponKind kind = ego.spec().kind();
        if (kind.ranged()) return;
        if (!(target.level() instanceof ServerLevel sl)) return;
        busy = true;
        try {
            // 흉내: 적중 시 준 피해의 25%를 회복한다
            if (BuiltInRegistries.ITEM.getKey(ego).getPath().equals("mimicry") && damageTaken > 0) {
                attacker.heal(damageTaken * 0.25f);
                Vec3 hp = attacker.position().add(0, 1.2, 0);
                sl.sendParticles(ParticleTypes.HEART, hp.x, hp.y, hp.z, 3, 0.3, 0.3, 0.3, 0.01);
            }
            switch (kind) {
                case SPEAR -> {
                    Vec3 dir = target.position().subtract(attacker.position());
                    if (dir.lengthSqr() < 1.0E-4) return;
                    Vec3 d = dir.normalize();
                    Vec3 center = target.position().add(d.scale(1.5));
                    AABB box = new AABB(center.x - 1, center.y - 1, center.z - 1,
                            center.x + 1, center.y + 1, center.z + 1);
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                        if (e == target || !EgoWeaponItem.validTarget(e, attacker)) continue;
                        e.hurtServer(sl, sl.damageSources().playerAttack(attacker), damageTaken * 0.6f);
                        break;
                    }
                    for (double t = 0.5; t <= 2.0; t += 0.5) {
                        Vec3 p = target.position().add(d.scale(t)).add(0, 1, 0);
                        sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                case HEAVY -> {
                    AABB box = target.getBoundingBox().inflate(2.5);
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                        if (e == target || !EgoWeaponItem.validTarget(e, attacker)) continue;
                        e.hurtServer(sl, sl.damageSources().playerAttack(attacker), damageTaken * 0.5f);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, false, false));
                    Vec3 c = target.position().add(0, 1, 0);
                    for (int i = 0; i < 12; i++) {
                        double a = Math.PI * 2 * i / 12.0;
                        sl.sendParticles(ParticleTypes.CRIT, c.x + Math.cos(a) * 1.2, c.y, c.z + Math.sin(a) * 1.2,
                                1, 0.05, 0.05, 0.05, 0.01);
                    }
                    sl.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.4f);
                }
                default -> {
                }
            }
        } finally {
            busy = false;
        }
    }
}
