package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * O-01-55 은하수의 아이 — 탈출하지 않는다.
 * 작업을 마칠 때마다 클리포트 카운터가 오르고, 작업자는 우정의 조약돌을 받는다.
 * 조약돌을 지닌 이가 배신하거나 죽으면 카운터가 내려가고, 0이 되면 조약돌을 지닌 모두가 함께 무너진다.
 */
public class ChildGalaxyEntity extends AbnormalityEntity {
    public static final int MAX_COUNTER = 5;

    public ChildGalaxyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.qliphoth = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    public AbnoProfile profile() {
        return AbnoProfile.CHILD_GALAXY;
    }

    @Override
    public void resetQliphoth() {
        qliphoth = 1;
    }

    /** 현재 로드된 은하수의 아이 (오버월드 우선) */
    public static ChildGalaxyEntity current(MinecraftServer server) {
        if (server == null) return null;
        ChildGalaxyEntity found = findIn(server.overworld());
        if (found != null) return found;
        for (ServerLevel l : server.getAllLevels()) {
            found = findIn(l);
            if (found != null) return found;
        }
        return null;
    }

    private static ChildGalaxyEntity findIn(ServerLevel level) {
        if (level == null) return null;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof ChildGalaxyEntity c && c.isAlive()) return c;
        }
        return null;
    }

    // ───────────── 작업 ─────────────

    @Override
    public void onWorkFinished(ServerPlayer p, WorkManager.Result result, WorkSession s) {
        qliphoth = Math.min(MAX_COUNTER, qliphoth + 1);
        EmployeeData d = Employees.get(p);
        if (!d.pebble) {
            d.pebble = true;
            p.sendSystemMessage(Component.literal("  아이가 반짝이는 조약돌 하나를 쥐여 주었다.")
                    .withStyle(st -> st.withColor(0x9AD0FF).withItalic(true)));
            p.sendSystemMessage(Component.literal("  조약돌은 지닌 이의 상처를 조금씩 아물게 한다. 다만 다른 환상체에게 가면 아이가 슬퍼한다.")
                    .withStyle(st -> st.withColor(0xAAAAAA)));
        } else {
            p.sendSystemMessage(Component.literal("  아이가 당신의 주머니를 가만히 들여다본다. 이미 조약돌을 받았기 때문이다.")
                    .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        }
        p.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + MAX_COUNTER)
                .withStyle(st -> st.withColor(0x9AD0FF)));
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 12, 0.4, 0.5, 0.4, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8f, 1.8f);
        }
        Employees.sync(p);
    }

    // ───────────── 배신 / 죽음 ─────────────

    /** 조약돌을 지닌 직원이 다른 환상체를 작업했다 */
    public void holderBetrayed(ServerPlayer p) {
        qliphoth = Math.max(0, qliphoth - 1);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 8, 0.3, 0.4, 0.3, 0.01);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 0.8f, 0.6f);
        }
        p.sendSystemMessage(Component.literal("  주머니 속 조약돌이 차갑게 식는다.")
                .withStyle(st -> st.withColor(0xAAAAAA).withItalic(true)));
        p.sendSystemMessage(Component.literal("  클리포트 카운터 " + qliphoth + "/" + MAX_COUNTER)
                .withStyle(st -> st.withColor(0x9AD0FF)));
        ServerLevel sl = p.level();
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        p.hurtServer(sl, sl.damageSources().magic(), 3f);
        Employees.damageSp(p, 3);
        if (qliphoth <= 0) collapse();
    }

    /** 조약돌을 지닌 직원이 죽었다 */
    public void holderDied(ServerPlayer p) {
        qliphoth = Math.max(0, qliphoth - 4);
        if (level() instanceof ServerLevel sl) {
            Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                    .append(p.getDisplayName())
                    .append(Component.literal(" 직원이 쓰러지자 조약돌 하나가 빛을 잃었습니다. (카운터 " + qliphoth + "/" + MAX_COUNTER + ")")
                            .withStyle(st -> st.withColor(0x9AD0FF))));
        }
        if (qliphoth <= 0) collapse();
    }

    /** 카운터 0: 조약돌을 지닌 모두가 함께 무너진다 */
    public void collapse() {
        if (!(level() instanceof ServerLevel sl)) return;
        Facility.broadcast(sl.getServer(), Component.literal("[관제] O-01-55가 울음을 터뜨렸습니다.")
                .withStyle(st -> st.withColor(0x8A0000).withBold(true)));
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 60, 0.8, 1.0, 0.8, 0.08);
        sl.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.2f, 0.7f);
        for (ServerPlayer o : sl.getServer().getPlayerList().getPlayers()) {
            EmployeeData d = Employees.get(o);
            if (!d.pebble) continue;
            d.pebble = false;
            ServerLevel ol = o.level();
            ol.sendParticles(ParticleTypes.END_ROD, o.getX(), o.getY() + 1.2, o.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            ol.playSound(null, o.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
            o.sendSystemMessage(Component.literal("  손안의 조약돌이 산산이 부서졌다. 별빛이 손가락 사이로 흘러내린다.")
                    .withStyle(st -> st.withColor(0x9AD0FF).withItalic(true)));
            Employees.sync(o);
            o.hurtServer(ol, ol.damageSources().magic(), 1000f);
        }
        qliphoth = 1;
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 1, 0.5, 0.5, 0.5, 0.005);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_counter", qliphoth);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        qliphoth = Math.max(1, Math.min(MAX_COUNTER, in.getIntOr("lc_counter", 1)));
    }
}
