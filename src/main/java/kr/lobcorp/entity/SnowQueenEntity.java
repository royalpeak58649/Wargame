package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * F-01-37 눈의 여왕 — 탈출하지 않는다.
 * 작업 결과가 보통이면 50%, 나쁨이면 반드시 저주가 진행된다.
 * 1단계: 심장에 얼음 조각이 박힌다. 2단계: 그 자리에서 얼어붙는다.
 * 얼어붙은 직원이 있는 동안 누군가 그녀와 작업을 시작하면, 그 직원은 대신 결투를 벌인다.
 */
public class SnowQueenEntity extends AbnormalityEntity {
    public static final int DUEL_TICKS = 60;

    private UUID duelist = null;
    private UUID frozenTarget = null;
    private int duelTicks = 0;

    public SnowQueenEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.SNOW_QUEEN;
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        boolean trigger = result == WorkManager.Result.BAD
                || (result == WorkManager.Result.NORMAL && p.getRandom().nextInt(100) < 50);
        if (!trigger) {
            p.sendSystemMessage(Component.literal("  여왕은 얼음 왕좌에 앉은 채 미동도 하지 않는다. 숨결만이 하얗게 얼어붙는다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            return;
        }
        EmployeeData d = Employees.get(p);
        if (d.frost <= 0) {
            d.frost = 1;
            p.sendSystemMessage(Component.literal("  차가운 무언가가 가슴 깊이 박혔다.")
                    .withStyle(st -> st.withColor(0x7CFFE0).withItalic(true)));
            p.level().playSound(null, p.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1f, 0.6f);
            Employees.sync(p);
            return;
        }
        if (d.frost == 1) {
            d.frost = 2;
            d.frozenAt = p.position();
            p.sendSystemMessage(Component.literal("  몸이 발끝부터 얼어붙는다…")
                    .withStyle(st -> st.withColor(0x7CFFE0).withItalic(true)));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 0.9, 0.5, 0.02);
                sl.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
                Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                        .append(p.getDisplayName())
                        .append(Component.literal(" 직원이 얼어붙었습니다. 눈의 여왕과 결투해 구출하십시오.")
                                .withStyle(st -> st.withColor(0x7CFFE0))));
            }
            Employees.sync(p);
        }
    }

    // ───────────── 결투 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer p, kr.lobcorp.work.WorkType type) {
        if (duelTicks > 0) {
            WorkManager.actionbar(p, WorkManager.red("눈의 여왕이 이미 누군가와 겨루고 있습니다."));
            return false;
        }
        ServerPlayer frozen = findFrozen(p);
        if (frozen == null) return true;
        duelist = p.getUUID();
        frozenTarget = frozen.getUUID();
        duelTicks = DUEL_TICKS;
        double yaw = Math.toRadians(getYRot());
        p.teleportTo(getX() - Math.sin(yaw) * 1.6, getY(), getZ() + Math.cos(yaw) * 1.6);
        p.sendSystemMessage(Component.literal("  눈의 여왕이 천천히 일어선다.")
                .withStyle(st -> st.withColor(0x7CFFE0).withItalic(true)));
        p.sendSystemMessage(Component.literal("  얼어붙은 동료를 대신해 그녀와 겨룹니다. 용기가 높을수록 이길 확률이 오릅니다.")
                .withStyle(st -> st.withColor(0xAAAAAA)));
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
        }
        return false;
    }

    private ServerPlayer findFrozen(ServerPlayer except) {
        if (!(level() instanceof ServerLevel sl)) return null;
        for (ServerPlayer o : sl.getServer().getPlayerList().getPlayers()) {
            if (o == except || !o.isAlive()) continue;
            if (Employees.get(o).frost == 2) return o;
        }
        return null;
    }

    private void tickDuel(ServerLevel sl) {
        ServerPlayer d = duelist == null ? null : sl.getServer().getPlayerList().getPlayer(duelist);
        ServerPlayer f = frozenTarget == null ? null : sl.getServer().getPlayerList().getPlayer(frozenTarget);
        if (d == null || f == null || !d.isAlive() || !f.isAlive()) {
            cancelDuel();
            return;
        }
        duelTicks--;
        if (duelTicks % 5 == 0) {
            Vec3 mid = d.position().add(f.position()).scale(0.5);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, mid.x, mid.y + 1.2, mid.z, 10, 0.6, 0.6, 0.6, 0.02);
            sl.sendParticles(ParticleTypes.CRIT, mid.x, mid.y + 1.2, mid.z, 8, 0.5, 0.5, 0.5, 0.05);
            sl.playSound(null, d.blockPosition(), (duelTicks / 5) % 2 == 0
                    ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 1f, 1.2f);
        }
        if (duelTicks <= 0) resolveDuel(sl, d, f);
    }

    private void resolveDuel(ServerLevel sl, ServerPlayer d, ServerPlayer f) {
        EmployeeData dd = Employees.get(d);
        EmployeeData fd = Employees.get(f);
        int chance = switch (EmployeeData.grade(dd.fortitude)) {
            case "V" -> 100;
            case "IV" -> 80;
            case "III" -> 50;
            case "II" -> 30;
            default -> 10;
        };
        boolean win = d.getRandom().nextInt(100) < chance;
        duelist = null;
        frozenTarget = null;
        duelTicks = 0;
        if (win) {
            dd.frost = 0;
            dd.frozenAt = null;
            fd.frost = 0;
            fd.frozenAt = null;
            dd.fortitude += 6;
            dd.prudence += 6;
            fd.fortitude += 6;
            fd.prudence += 6;
            Employees.apply(d);
            Employees.apply(f);
            Employees.sync(d);
            Employees.sync(f);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, f.getX(), f.getY() + 1, f.getZ(), 60, 0.6, 0.9, 0.6, 0.03);
            sl.playSound(null, f.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.6f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                    .append(d.getDisplayName())
                    .append(Component.literal(" 직원이 결투에서 이겼습니다. 얼음이 녹았습니다.")
                            .withStyle(st -> st.withColor(0x55FF55))));
        } else {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] 결투에서 졌습니다.")
                    .withStyle(st -> st.withColor(0x8A0000)));
            d.hurtServer(sl, sl.damageSources().magic(), 1000f);
            f.hurtServer(sl, sl.damageSources().magic(), 1000f);
        }
    }

    private void cancelDuel() {
        duelist = null;
        frozenTarget = null;
        duelTicks = 0;
    }

    // ───────────── 연출 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (duelTicks > 0) tickDuel(sl);
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.6, getZ(), 2, 0.7, 0.6, 0.7, 0);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (duelist != null) out.store("lc_duelist", UUIDUtil.CODEC, duelist);
        if (frozenTarget != null) out.store("lc_frozen", UUIDUtil.CODEC, frozenTarget);
        out.putInt("lc_duel_ticks", duelTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        duelist = in.read("lc_duelist", UUIDUtil.CODEC).orElse(null);
        frozenTarget = in.read("lc_frozen", UUIDUtil.CODEC).orElse(null);
        duelTicks = in.getIntOr("lc_duel_ticks", 0);
    }
}
