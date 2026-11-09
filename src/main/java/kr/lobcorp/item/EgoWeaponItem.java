package kr.lobcorp.item;

import kr.lobcorp.DamageColor;
import kr.lobcorp.Grades;
import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** E.G.O 무기. 환상체에게 주는 피해 배율은 이 색상과 환상체 내성으로 결정된다. */
public class EgoWeaponItem extends Item {
    public final DamageColor color;

    /** 플레이어 UUID + 아이템 id → 마지막으로 발사한 gameTime */
    private static final Map<String, Long> LAST_SHOT = new HashMap<>();

    public EgoWeaponItem(Properties properties, DamageColor color) {
        super(properties);
        this.color = color;
    }

    public WeaponSpecs.Spec spec() {
        return WeaponSpecs.of(BuiltInRegistries.ITEM.getKey(this).getPath());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        WeaponSpecs.Spec spec = spec();
        if (!spec.kind().ranged()) return super.use(level, player, hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.FAIL;

        String key = player.getUUID() + ":" + BuiltInRegistries.ITEM.getKey(this).getPath();
        long now = sl.getGameTime();
        Long last = LAST_SHOT.get(key);
        if (last != null && now - last < spec.cooldown()) {
            sp.sendOverlayMessage(Component.literal("재장전 중…").withStyle(s -> s.withColor(0xAAAAAA)));
            return InteractionResult.FAIL;
        }
        LAST_SHOT.put(key, now);

        shoot(sl, sp, spec);
        return InteractionResult.SUCCESS;
    }

    // ───────────── 발사 ─────────────

    private void shoot(ServerLevel sl, ServerPlayer player, WeaponSpecs.Spec spec) {
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        WeaponKind kind = spec.kind();

        if (kind == WeaponKind.THROWN) {
            double a = Math.toRadians(8);
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            fireLine(sl, player, spec, from, dir);
            fireLine(sl, player, spec, from, new Vec3(dir.x * cos - dir.z * sin, dir.y, dir.x * sin + dir.z * cos));
            fireLine(sl, player, spec, from, new Vec3(dir.x * cos + dir.z * sin, dir.y, -dir.x * sin + dir.z * cos));
        } else {
            fireLine(sl, player, spec, from, dir);
        }

        if (kind == WeaponKind.GUN || kind == WeaponKind.PIERCE || kind == WeaponKind.CANNON) {
            sl.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1f, 1.2f);
        } else {
            sl.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6f, 1.8f);
        }
    }

    /** from에서 dir 방향으로 한 줄. 벽에서 멈추고, 종류에 따라 관통/폭발한다. */
    private void fireLine(ServerLevel sl, ServerPlayer player, WeaponSpecs.Spec spec, Vec3 from, Vec3 dir) {
        WeaponKind kind = spec.kind();
        boolean friendlyFire = "magic_bullet".equals(BuiltInRegistries.ITEM.getKey(this).getPath());
        Vec3 d = dir.normalize();
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 point = from;
        boolean stopped = false;

        for (double t = 0.5; t <= spec.range(); t += 0.5) {
            point = from.add(d.scale(t));
            if (sl.getBlockState(BlockPos.containing(point)).isSolid()) {
                stopped = true;
                break;
            }
            trail(sl, kind, point, t);
            AABB box = new AABB(point.x - 0.3, point.y - 0.3, point.z - 0.3,
                    point.x + 0.3, point.y + 0.3, point.z + 0.3);
            boolean found = false;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                if (!hits(e, player, friendlyFire) || hit.contains(e)) continue;
                hit.add(e);
                found = true;
            }
            if (found && kind != WeaponKind.PIERCE) {
                stopped = true;
                break;
            }
        }

        if (kind == WeaponKind.CANNON) {
            AABB blast = new AABB(point.x - 3, point.y - 3, point.z - 3, point.x + 3, point.y + 3, point.z + 3);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, blast)) {
                if (!hits(e, player, friendlyFire) || hit.contains(e)) continue;
                hit.add(e);
            }
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, point.x, point.y, point.z, 40, 1.2, 1.2, 1.2, 0.05);
            sl.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 20, 0.8, 0.8, 0.8, 0.05);
            sl.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1f, 1f);
        } else {
            impact(sl, kind, point);
        }
        if (stopped && kind != WeaponKind.CANNON) {
            sl.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 6, 0.15, 0.15, 0.15, 0.01);
        }

        for (LivingEntity e : hit) {
            // 사격 피해도 무기 위험 등급에 따라 증가한다 (한 발당 한 번만 계산)
            float shotDamage = spec.damage() * Grades.damageScale(Grades.ofItem(this));
            if (e instanceof ServerPlayer sp && sp != player) {
                sp.hurtServer(sl, sl.damageSources().magic(), shotDamage);
                sp.sendSystemMessage(Component.literal("  어디선가 날아온 마탄이 몸을 꿰뚫었다.")
                        .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
                player.sendSystemMessage(Component.literal("  마탄이 아군을 꿰뚫었다…")
                        .withStyle(s -> s.withColor(0xFF5555)));
            } else {
                e.hurtServer(sl, sl.damageSources().playerAttack(player), shotDamage);
            }
        }
    }

    /** 마탄은 아군(다른 생존 플레이어)도 맞는다. */
    private boolean hits(LivingEntity e, ServerPlayer shooter, boolean friendlyFire) {
        return validTarget(e, shooter) || (friendlyFire && e instanceof ServerPlayer sp && sp != shooter
                && sp.isAlive() && !sp.isCreative() && !sp.isSpectator());
    }

    public static boolean validTarget(LivingEntity e, ServerPlayer shooter) {
        if (e == shooter || !e.isAlive()) return false;
        if (e instanceof Player) return false;
        if (e instanceof AbnormalityEntity a && !a.isBreached()) return false;
        if (e instanceof BreachingAbnormality b && b.isFriendly()) return false;
        return true;
    }

    private static void trail(ServerLevel sl, WeaponKind kind, Vec3 p, double t) {
        switch (kind) {
            case GUN -> {
                sl.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                if (((int) (t * 2)) % 4 == 0) {
                    sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
            case PIERCE -> {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                if (((int) (t * 2)) % 4 == 0) {
                    sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
            case BOW -> sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            case THROWN -> sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            case CANNON -> {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
                if (((int) (t * 2)) % 3 == 0) {
                    sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
            default -> {
            }
        }
    }

    private static void impact(ServerLevel sl, WeaponKind kind, Vec3 p) {
        switch (kind) {
            case GUN -> sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 8, 0.2, 0.2, 0.2, 0.05);
            case PIERCE -> sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 12, 0.3, 0.3, 0.3, 0.02);
            case BOW -> sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 6, 0.2, 0.2, 0.2, 0.05);
            case THROWN -> sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 6, 0.2, 0.2, 0.2, 0.02);
            default -> sl.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 6, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
