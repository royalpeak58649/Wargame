package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
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

import java.util.ArrayList;
import java.util.List;

/**
 * T-04-50 여왕벌 — 밀랍과 살로 빚은 벌집 위에 앉아 부푼 배를 느리게 움직인다.
 * 클리포트 카운터가 0이 되면 3초 뒤 포자가 퍼져 나가 주변 직원을 감염시킨다.
 * 탈출하지 않으며, 포자를 뿌린 뒤 카운터는 1로 돌아온다.
 */
public class QueenBeeEntity extends AbnormalityEntity {
    public static final int SPORE_RADIUS = 32;
    public static final int SPORE_TICKS = 400;
    public static final int MAX_INFECTED = 3;

    private int sporeDelay = -1;

    public QueenBeeEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.QUEEN_BEE;
    }

    // ───────────── 클리포트 카운터 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        int before = qliphoth;
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal("  여왕벌이 더듬이를 천천히 흔든다. 벌집이 낮게 울린다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                if (p.getRandom().nextInt(100) < 50) qliphoth = Math.max(0, qliphoth - 1);
                p.sendSystemMessage(Component.literal("  밀랍 벽 너머에서 날개 소리가 들린다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
            case BAD -> {
                if (p.getRandom().nextInt(100) < 70) qliphoth = Math.max(0, qliphoth - 1);
                p.sendSystemMessage(Component.literal("  벌집의 벌집무늬가 한 칸씩 검게 물든다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            }
        }
        if (qliphoth != before) {
            p.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xAAAAAA)));
        }
        if (qliphoth <= 0 && sporeDelay < 0) {
            sporeDelay = 60;
            if (level() instanceof ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] T-04-50 주변에 포자가 퍼지고 있습니다.")
                        .withStyle(st -> st.withColor(0xFFC040)));
                sl.playSound(null, blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
        }
    }

    // ───────────── 포자 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, getX(), getY() + 1.4, getZ(), 2, 0.8, 0.4, 0.8, 0.005);
        }
        if (sporeDelay > 0) {
            if (sporeDelay % 10 == 0) {
                sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, getX(), getY() + 1.6, getZ(), 8, 1.0, 0.6, 1.0, 0.02);
                sl.playSound(null, blockPosition(), SoundEvents.BEE_LOOP, SoundSource.HOSTILE, 0.8f, 0.7f);
            }
            if (--sporeDelay == 0) {
                releaseSpores(sl);
                sporeDelay = -1;
            }
        }
    }

    private void releaseSpores(ServerLevel sl) {
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            if (kr.lobcorp.work.ToolState.isSheltered(p)) continue;
            if (p.distanceTo(this) > SPORE_RADIUS) continue;
            candidates.add(p);
        }
        java.util.Collections.shuffle(candidates, new java.util.Random(getRandom().nextLong()));
        int n = Math.min(MAX_INFECTED, candidates.size());
        for (int i = 0; i < n; i++) {
            ServerPlayer p = candidates.get(i);
            EmployeeData d = Employees.get(p);
            d.sporeTicks = SPORE_TICKS;
            sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.8, 0.5, 0.03);
            p.sendSystemMessage(Component.literal("  숨을 들이쉬자 목구멍 안쪽에서 무언가가 꿈틀거린다.")
                    .withStyle(st -> st.withColor(0xFFC040).withItalic(true)));
            Employees.sync(p);
        }
        sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, getX(), getY() + 1.6, getZ(), 60, 2.0, 1.0, 2.0, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.BEE_HURT, SoundSource.HOSTILE, 1.5f, 0.5f);
        if (n > 0) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] T-04-50의 포자가 " + n + "명의 직원에게 스며들었습니다.")
                    .withStyle(st -> st.withColor(0xFFC040)));
        } else {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] T-04-50의 포자가 허공으로 흩어졌습니다.")
                    .withStyle(st -> st.withColor(0xFFC040)));
        }
        qliphoth = profile().qliphothMax();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_spore_delay", sporeDelay);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        sporeDelay = in.getIntOr("lc_spore_delay", -1);
    }
}
