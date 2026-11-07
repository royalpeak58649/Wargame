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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * F-01-18 벽을 보는 여자 — 언제나 벽을 향해 서서 흐느적거린다. 얼굴을 본 사람은 없다.
 * 애착 작업이 카운터를 깎고, 0이 되면 부서 전체가 그 흐느낌을 듣는다. 탈출 없음.
 */
public class LadyFacingTheWallEntity extends AbnormalityEntity {
    public static final double WAIL_RANGE = 32.0;
    /** 자제 등급 I 기준 (EmployeeData.BASE_STAT = 15, II = 30) */
    public static final int TEMPERANCE_GRADE_1 = 30;

    public LadyFacingTheWallEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.LADY_FACING_THE_WALL;
    }

    // ───────────── 클리포트 카운터 ─────────────

    private void lower(ServerPlayer by) {
        qliphoth = Math.max(0, qliphoth - 1);
        if (by != null) {
            by.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + profile().qliphothMax())
                    .withStyle(st -> st.withColor(0xFF5555)));
        }
        if (qliphoth <= 0) {
            wail();
            resetQliphoth();
        }
    }

    /** 카운터 0 — 부서 전체가 흐느낌을 듣는다 */
    private void wail() {
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) > WAIL_RANGE) continue;
            Employees.damageSp(p, 3 + getRandom().nextInt(3));
            p.sendSystemMessage(Component.literal("  벽에서 돌아선 여자가 무언가를 중얼거린다. 머릿속이 하얗게 지워진다.")
                    .withStyle(st -> st.withColor(0xFFFFFF).withItalic(true)));
        }
        sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.2, getZ(), 40, 0.6, 1.0, 0.6, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.2f, 0.5f);
        Facility.broadcast(sl.getServer(), Component.literal("[관제] F-01-18의 흐느낌이 부서 전체에 울려 퍼졌습니다.")
                .withStyle(st -> st.withColor(0xFF5555)));
    }

    // ───────────── 작업 훅 ─────────────

    @Override
    public void onWorkStart(ServerPlayer p, WorkType type) {
        if (type == WorkType.ATTACHMENT) lower(p);
        // 자제 I 직원은 그 소리에 무심코 뒤를 돌아본다
        EmployeeData d = Employees.get(p);
        if (d.temperance < TEMPERANCE_GRADE_1 && getRandom().nextInt(100) < 20) {
            p.sendSystemMessage(Component.literal("  등 뒤에서 누군가 당신의 이름을 부른 것 같다. 무심코 뒤를 돌아보았다.")
                    .withStyle(st -> st.withColor(0xFFFFFF).withItalic(true)));
            Employees.panic(p);
        }
    }

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        switch (result) {
            case GOOD -> p.sendSystemMessage(Component.literal(
                    "  여자는 벽에 손바닥을 대고 아주 작게 웃는 것 같다. 벽지가 조금 젖어 있다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
            case NORMAL -> {
                p.sendSystemMessage(Component.literal(
                        "  긴 머리카락이 바닥을 쓸며 흔들린다. 무슨 말인지 알아들을 수 없다.")
                        .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
                if (getRandom().nextInt(100) < 50) lower(p);
            }
            case BAD -> {
                p.sendSystemMessage(Component.literal(
                        "  흐느낌이 갑자기 멈췄다. 여자의 어깨가 천천히 들썩인다.")
                        .withStyle(st -> st.withColor(0xFF7777).withItalic(true)));
                if (getRandom().nextInt(100) < 75) lower(p);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.4, getZ(), 1, 0.3, 0.5, 0.3, 0);
        }
    }
}
