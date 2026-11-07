package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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

import java.util.ArrayList;
import java.util.List;

/**
 * O-05-30 노래하는 기계 — 뚜껑이 달린 낡은 나무 오르골 상자. 안에서는 갈리는 드럼이 돌아간다.
 * 탈출하지 않는다. 대신 조건이 맞는 직원이 기계 속으로 뛰어들어 갈리고,
 * 그 소리가 아름다운 연주가 되어 다른 직원 한 명을 홀린다.
 */
public class SingingMachineEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Integer> MUSIC =
            SynchedEntityData.defineId(SingingMachineEntity.class, EntityDataSerializers.INT);

    /** 자제 III 하한 (45) — 이보다 낮으면 기계에 빨려 든다 */
    public static final int LOW_TEMPERANCE = 45;
    /** 용기 IV 하한 (65) — 이 이상이면 기계에 빨려 든다 */
    public static final int HIGH_FORTITUDE = 65;
    public static final int MUSIC_TICKS = 1200; // 60초
    public static final int POSSESS_TICKS = 1200;
    public static final String POSSESS_LABEL = "노래하는 기계의 음악";

    public SingingMachineEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.SINGING_MACHINE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MUSIC, 0);
    }

    public int getMusicTicks() {
        return entityData.get(MUSIC);
    }

    private void setMusicTicks(int t) {
        entityData.set(MUSIC, Math.max(0, t));
    }

    // ───────────── 작업 ─────────────

    @Override
    public boolean beforeWorkStart(ServerPlayer p, WorkType type) {
        if (qliphoth <= 0) {
            dive(p);
            return false;
        }
        return true;
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        if (result == WorkManager.Result.BAD) {
            qliphoth = Math.max(0, qliphoth - 1);
            p.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
        String text = switch (result) {
            case GOOD -> "  뚜껑 틈으로 고운 선율이 흘러나온다. 드럼은 천천히, 아주 천천히 돌아간다.";
            case NORMAL -> "  나무 상자 안쪽에서 톱니가 맞물리는 소리가 잠깐 새어 나왔다.";
            case BAD -> "  뚜껑이 조금 열렸다가 닫힌다. 안쪽에서 무언가 갈리는 소리가 났다.";
        };
        p.sendSystemMessage(Component.literal(text).withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));

        EmployeeData d = Employees.get(p);
        if (qliphoth <= 0 || d.temperance < LOW_TEMPERANCE || d.fortitude >= HIGH_FORTITUDE) {
            dive(p);
        }
    }

    /** 직원이 기계 속으로 뛰어들어 갈린다. 연주가 시작되고, 다른 직원 한 명이 홀린다. */
    private void dive(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) return;
        p.teleportTo(getX(), getY() + 0.8, getZ());
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.5f);
        sl.playSound(null, blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 2f, 0.7f);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 0.9, getZ(), 30, 0.5, 0.4, 0.5, 0.05);
        sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.2, getZ(), 20, 0.6, 0.5, 0.6, 0.1);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 노래하는 기계 속으로 뛰어들었습니다.").withStyle(st -> st.withColor(0xFF5555))));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("♪").withStyle(st -> st.withColor(0xFF3333).withBold(true))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);

        qliphoth = 1;
        setMusicTicks(MUSIC_TICKS);

        ServerPlayer target = pickBewitched(p);
        if (target != null) {
            Employees.possess(target, POSSESS_TICKS, POSSESS_LABEL, -1);
        }
    }

    /** 음악에 홀릴 다른 직원을 고른다. */
    private ServerPlayer pickBewitched(ServerPlayer diver) {
        if (!(level() instanceof ServerLevel sl)) return null;
        List<ServerPlayer> pool = new ArrayList<>();
        for (ServerPlayer o : sl.players()) {
            if (o == diver || !o.isAlive()) continue;
            if (o.isCreative() || o.isSpectator()) continue;
            if (Employees.get(o).possessTicks > 0) continue;
            pool.add(o);
        }
        if (pool.isEmpty()) return null;
        return pool.get(getRandom().nextInt(pool.size()));
    }

    // ───────────── 연주 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        int music = getMusicTicks();
        if (music <= 0) return;
        setMusicTicks(music - 1);
        if (music % 40 == 0) {
            sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.6, getZ(), 6, 0.8, 0.5, 0.8, 0.1);
            float pitch = 0.8f + getRandom().nextFloat() * 0.8f;
            if (getRandom().nextBoolean()) {
                sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 1.2f, pitch);
            } else {
                sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.2f, pitch);
            }
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(this) <= 24) {
                    sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.2, p.getZ(), 1, 0.3, 0.2, 0.3, 0);
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_music", getMusicTicks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setMusicTicks(in.getIntOr("lc_music", 0));
    }
}
