package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.DamageColor;
import kr.lobcorp.ModItems;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.FacilityData;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * F-01-69 마탄의 사수 — 어두운 코트를 걸친 마른 사냥꾼.
 * 정의가 낮은 직원이 작업을 마치거나 결과가 좋음이 아니면 카운터가 줄고, 0이 되면 시설 어딘가로 마탄이 날아간다.
 * 애착 작업은 시설 에너지를 바친 「의뢰」로 바뀐다 — 눈이 겨눈 방향으로 탄환이 벽을 뚫고 날아간다.
 */
public class FreischutzEntity extends AbnormalityEntity {
    public static final int MAX_COUNTER = 3;
    public static final int COOLDOWN_TICKS = 200;
    public static final int REQUEST_ENERGY_MIN = 10;
    public static final double BULLET_RANGE = 64.0;

    private int counter = MAX_COUNTER;
    private int requests = 0;
    private int cooldown = 0;

    public FreischutzEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.FREISCHUTZ;
    }

    @Override
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        int bonus = super.workChanceBonus(worker, type);
        if (type == WorkType.REPRESSION && Employees.get(worker).justice >= 45) bonus += 45;
        return bonus;
    }

    public int getCounter() {
        return counter;
    }

    public int getCooldown() {
        return cooldown;
    }

    // ───────────── 의뢰 (애착 작업 대체) ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        if (type != WorkType.ATTACHMENT) return true;
        request(p);
        return false;
    }

    private void request(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (cooldown > 0) {
            WorkManager.actionbar(p, Component.literal("총신이 아직 뜨겁다. (" + (cooldown / 20 + 1) + "초)")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return;
        }
        FacilityData f = Facility.get(sl.getServer());
        if (f.energy < REQUEST_ENERGY_MIN) {
            WorkManager.actionbar(p, WorkManager.red("의뢰할 에너지가 부족합니다. (필요 " + REQUEST_ENERGY_MIN + ")"));
            return;
        }
        int cost = Math.max(1, f.energy / 10);
        f.energy = Math.max(0, f.energy - cost);
        cooldown = COOLDOWN_TICKS;

        p.sendSystemMessage(Component.literal("  사냥꾼이 총구를 당신의 눈높이에 맞춘다. 무엇을 겨누고 있는지, 그는 묻지 않는다.")
                .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
        p.sendSystemMessage(Component.literal("  시설 에너지 -" + cost + "   (남은 에너지 " + f.energy + ")")
                .withStyle(s -> s.withColor(0xFFAA00)));

        p.getInventory().placeItemBackInInventory(new ItemStack(ModItems.MAGIC_BULLET_ORDER),
                net.minecraft.util.Prediction.SERVER_ONLY);
        p.sendSystemMessage(Component.literal("  사냥꾼이 탄환 한 발을 건넨다. (마탄 의뢰서: 우클릭하면 바라보는 방향으로 벽을 뚫고 발사)")
                .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
    }

    /** 마탄 의뢰서 사용 — 눈에서 바라보는 방향으로 발사한다. 일곱 번째마다 탄환이 제멋대로 날아간다. */
    public void fireOrder(ServerPlayer p) {
        requests++;
        if (requests % 7 == 0) {
            fireRandom();
            lowerCounter(p, 1);
            p.sendSystemMessage(Component.literal("  탄환이 제멋대로 날아갔다…")
                    .withStyle(s -> s.withColor(0xFF7777).withItalic(true)));
        } else {
            fire(p.getEyePosition(), p.getLookAngle(), p);
        }
    }

    // ───────────── 발사 ─────────────

    /** from에서 dir 방향으로 64블록. 벽은 무시하고 선 위의 모든 것을 관통한다. */
    public void fire(Vec3 from, Vec3 dir, ServerPlayer shooter) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 d = dir.normalize();
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 point = from;
        for (double t = 0; t <= BULLET_RANGE; t += 0.5) {
            point = from.add(d.scale(t));
            sl.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0);
            if (((int) (t * 2)) % 4 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0);
            }
            AABB box = new AABB(point.x - 0.3, point.y - 0.3, point.z - 0.3,
                    point.x + 0.3, point.y + 0.3, point.z + 0.3);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == shooter || hit.contains(e)) continue;
                if (e instanceof AbnormalityEntity a && !a.isBreached()) continue;
                hit.add(e);
            }
        }
        for (LivingEntity e : hit) {
            if (e instanceof ServerPlayer p) {
                p.hurtServer(sl, sl.damageSources().magic(), 30f);
                Employees.damageSp(p, 15);
                p.sendSystemMessage(Component.literal("  탄환이 몸을 꿰뚫었다. 어디서 날아왔는지조차 보지 못했다.")
                        .withStyle(s -> s.withColor(0xFF7777).withItalic(true)));
            } else if (e instanceof BreachingAbnormality b && b.isBreached()) {
                float mult = b.profile().resistTo(DamageColor.BLACK);
                b.hurtServer(sl, sl.damageSources().magic(), 40f * mult);
            } else {
                e.hurtServer(sl, sl.damageSources().magic(), 40f);
            }
        }
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1f, 0.5f);
        sl.playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.4f, 1.2f);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 20, 0.4, 0.4, 0.4, 0.02);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] 마탄이 발사되었습니다.")
                .withStyle(s -> s.withColor(0x555555)));
    }

    /** 시설 어딘가로 마탄이 날아간다 */
    public void fireRandom() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 point;
        List<ServerPlayer> players = sl.players();
        if (!players.isEmpty()) {
            ServerPlayer target = players.get(getRandom().nextInt(players.size()));
            point = target.position().add(
                    (getRandom().nextDouble() - 0.5) * 12.0,
                    getRandom().nextDouble() * 2.0,
                    (getRandom().nextDouble() - 0.5) * 12.0);
        } else {
            point = position().add(
                    (getRandom().nextDouble() - 0.5) * 12.0,
                    getRandom().nextDouble() * 2.0,
                    (getRandom().nextDouble() - 0.5) * 12.0);
        }
        double a = getRandom().nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
        Vec3 from = point.subtract(dir.scale(32));
        fire(from, dir, null);
    }

    // ───────────── 클리포트 카운터 ─────────────

    private void lowerCounter(ServerPlayer by, int amount) {
        counter = Math.max(0, counter - amount);
        if (by != null) {
            by.sendSystemMessage(Component.literal("  클리포트 카운터 " + counter + "/" + MAX_COUNTER)
                    .withStyle(s -> s.withColor(0xFF5555)));
        }
        if (counter <= 0) {
            fireRandom();
            counter = MAX_COUNTER;
            if (level() instanceof ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] F-01-69의 총구가 식었다. 카운터가 3으로 돌아옵니다.")
                        .withStyle(s -> s.withColor(0xAAAAAA)));
            }
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        EmployeeData d = Employees.get(p);
        int drop = 0;
        if (d.justice < 45) drop++;
        if (result != WorkManager.Result.GOOD) drop++;
        if (drop > 0) lowerCounter(p, drop);
        else p.sendSystemMessage(Component.literal("  사냥꾼이 모자를 살짝 들어 보인다. 클리포트 카운터 " + counter + "/" + MAX_COUNTER)
                .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
    }

    @Override
    public void tick() {
        super.tick();
        if (cooldown > 0) cooldown--;
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 2.1, getZ(), 1, 0.1, 0.05, 0.1, 0.005);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_counter", counter);
        out.putInt("lc_requests", requests);
        out.putInt("lc_cooldown", cooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        counter = in.getIntOr("lc_counter", MAX_COUNTER);
        requests = in.getIntOr("lc_requests", 0);
        cooldown = in.getIntOr("lc_cooldown", 0);
    }
}
