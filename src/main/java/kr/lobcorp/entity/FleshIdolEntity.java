package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

import java.util.UUID;

/**
 * T-09-79 살점 우상 — 도구형.
 * 날고기와 뼈를 아무렇게나 쌓아 올린 조잡한 우상. 앞에는 작은 제단이 놓여 있다.
 * 누군가 제단 앞에 무릎을 꿇고 기도하는 동안, 시설의 모든 직원이 조금씩 상처를 회복한다.
 * 대신 그 값을 치르는 것은 기도하는 자 자신이다. 오래 기도할수록 우상은 더 많은 살점을 요구한다.
 */
public class FleshIdolEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Boolean> PRAYING =
            SynchedEntityData.defineId(FleshIdolEntity.class, EntityDataSerializers.BOOLEAN);

    /** 기도 한 주기 (틱) */
    private static final int CYCLE_TICKS = 200;
    /** 이 거리보다 멀어지면 기도가 끊긴다 */
    private static final double PRAY_RANGE = 3.0;
    /** 이 주기마다 우상이 굶주린다 */
    private static final int HUNGER_CYCLE = 6;

    /** 기도하는 직원 (저장하지 않는다) */
    private UUID prayer = null;
    private int prayTicks = 0;
    private int cycles = 0;

    public FleshIdolEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.FLESH_IDOL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PRAYING, false);
    }

    /** 누군가 기도하고 있는가 (렌더러가 제단의 빛을 켠다) */
    public boolean isPraying() {
        return entityData.get(PRAYING);
    }

    private void setPraying(boolean b) {
        entityData.set(PRAYING, b);
    }

    // ───────────── 사용 ─────────────

    @Override
    public void useTool(ServerPlayer p) {
        if (prayer == null) {
            prayer = p.getUUID();
            prayTicks = 0;
            cycles = 0;
            setPraying(true);
            if (p.level() instanceof ServerLevel sl) {
                sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.2, getZ(), 10, 0.4, 0.4, 0.4, 0.02);
            }
            p.sendSystemMessage(Component.literal("  우상 앞에 무릎을 꿇었다…")
                    .withStyle(s -> s.withColor(0xAA55FF).withItalic(true)));
            return;
        }
        if (prayer.equals(p.getUUID())) {
            stopPrayer(p, "  기도를 멈추고 자리에서 일어섰다.");
            return;
        }
        p.sendSystemMessage(Component.literal("  누군가 이미 우상 앞에 무릎을 꿇고 있다.")
                .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
    }

    /** 기도를 끝낸다. p가 null이면 조용히 정리한다 */
    private void stopPrayer(ServerPlayer p, String message) {
        prayer = null;
        prayTicks = 0;
        setPraying(false);
        if (p != null && message != null) {
            p.sendSystemMessage(Component.literal(message)
                    .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
        }
    }

    /** 하루가 지나면 우상은 다시 조용해진다 */
    public void clearDaily() {
        prayer = null;
        setPraying(false);
        cycles = 0;
        prayTicks = 0;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (prayer == null) return;

        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(prayer);
        if (p == null) {
            stopPrayer(null, null);
            return;
        }
        if (!p.isAlive() || p.isSpectator()) {
            stopPrayer(p, "  기도가 끊겼다.");
            return;
        }
        if (p.distanceToSqr(this) > PRAY_RANGE * PRAY_RANGE) {
            stopPrayer(p, "  우상에게서 너무 멀어졌다. 기도가 끊겼다.");
            return;
        }

        prayTicks++;
        if (prayTicks % 20 == 0) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.4, getZ(), 4, 0.4, 0.3, 0.4, 0.01);
        }
        if (prayTicks < CYCLE_TICKS) return;
        prayTicks = 0;
        cycle(sl, p);
    }

    /** 한 주기: 모두가 회복하고, 기도하는 자가 값을 치른다 */
    private void cycle(ServerLevel sl, ServerPlayer p) {
        for (ServerPlayer o : sl.getServer().getPlayerList().getPlayers()) {
            if (!o.isAlive() || o.isSpectator()) continue;
            o.heal(2f);
            EmployeeData d = Employees.get(o);
            if (d.sp < d.maxSp()) {
                d.sp = Math.min(d.maxSp(), d.sp + 1);
                Employees.sync(o);
            }
        }
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 24, 0.8, 0.6, 0.8, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 0.8f);

        p.hurtServer(sl, sl.damageSources().magic(), 2f);
        Employees.damageSp(p, 2);
        cycles++;

        if (cycles % HUNGER_CYCLE == 0) {
            p.hurtServer(sl, sl.damageSources().magic(), p.getMaxHealth() * 0.25f);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2f, 0.5f);
            Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                    .append(p.getDisplayName())
                    .append(Component.literal(" 직원의 살점이 우상에게 먹혔습니다. 우상이 더 많은 것을 요구합니다.")
                            .withStyle(st -> st.withColor(0xFF5555))));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_idol_cycles", cycles);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        cycles = in.getIntOr("lc_idol_cycles", 0);
    }
}
