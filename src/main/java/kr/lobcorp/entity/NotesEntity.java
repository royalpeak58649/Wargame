package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.ToolState;
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

/**
 * T-09-78 미친 연구원의 노트 — 도구형.
 * 노트를 들면 작업 성공률 +20, 작업 속도가 빨라진다.
 * 30초가 지나고 한 번이라도 작업을 마친 뒤에만 안전하게 내려놓을 수 있다.
 * 그 전에 반납하거나, 든 채로 HP·SP 피해를 60 이상 받으면 머리가 터진다.
 */
public class NotesEntity extends AbnormalityEntity {
    private static final EntityDataAccessor<Boolean> TAKEN =
            SynchedEntityData.defineId(NotesEntity.class, EntityDataSerializers.BOOLEAN);

    public NotesEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        return AbnoProfile.NOTES;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAKEN, false);
    }

    /** 누군가 노트를 들고 있으면 책 모델을 감춘다 */
    public boolean isTaken() {
        return entityData.get(TAKEN);
    }

    private void setTaken(boolean b) {
        entityData.set(TAKEN, b);
    }

    // ───────────── 사용 ─────────────

    @Override
    public void useTool(ServerPlayer p) {
        if (ToolState.notesHolder == null) {
            take(p);
            return;
        }
        if (ToolState.isNotesHolder(p)) {
            giveBack(p);
            return;
        }
        p.sendOverlayMessage(Component.literal("누군가 노트를 가지고 있습니다.").withStyle(s -> s.withColor(0xFF5555)));
    }

    private void take(ServerPlayer p) {
        ServerLevel sl = p.level();
        ToolState.notesHolder = p.getUUID();
        ToolState.notesTicks = 0;
        ToolState.notesWorked = false;
        ToolState.notesDamage = 0;
        setTaken(true);
        sl.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, 0.9f);
        sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.1, getZ(), 12, 0.3, 0.2, 0.3, 0.05);
        p.sendSystemMessage(Component.literal("  노트를 펼쳤다. 글자들이 머릿속으로 기어든다.")
                .withStyle(s -> s.withColor(0xFFFFAA00).withItalic(true)));
        p.sendSystemMessage(Component.literal("  (작업 성공률 +20, 작업 속도 증가 · 30초가 지나고 한 번 작업을 마쳐야 안전하게 내려놓을 수 있습니다)")
                .withStyle(s -> s.withColor(0xAAAAAA)));
        Employees.sync(p);
    }

    private void giveBack(ServerPlayer p) {
        if (ToolState.notesTicks < ToolState.NOTES_SAFE_TICKS || !ToolState.notesWorked) {
            explode(p);
            return;
        }
        ServerLevel sl = p.level();
        ToolState.clearNotes();
        setTaken(false);
        sl.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, 0.7f);
        p.sendSystemMessage(Component.literal("  노트를 내려놓았다. 머리가 맑아진다.")
                .withStyle(s -> s.withColor(0x55FF55).withItalic(true)));
        Employees.sync(p);
    }

    /** 노트를 감당하지 못하고 터진다 */
    public static void explode(ServerPlayer p) {
        ServerLevel sl = p.level();
        ToolState.clearNotes();
        sl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 3, 0.4, 0.4, 0.4, 0);
        sl.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.8f);
        for (ServerPlayer o : sl.players()) {
            if (o != p && o.distanceTo(p) <= 5.0) {
                o.hurtServer(sl, sl.damageSources().magic(), 20f);
            }
        }
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 노트의 내용을 감당하지 못했습니다.").withStyle(s -> s.withColor(0xFF5555))));
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.1, getZ(), 1, 0.3, 0.1, 0.3, 0);
        }
        // 보유자가 사라졌으면 노트는 제자리로 돌아온다
        if (ToolState.notesHolder != null && tickCount % 20 == 0) {
            ServerPlayer h = sl.getServer().getPlayerList().getPlayer(ToolState.notesHolder);
            if (h == null || !h.isAlive()) {
                ToolState.clearNotes();
                setTaken(false);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("lc_taken", isTaken());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setTaken(in.getBooleanOr("lc_taken", false));
    }
}
