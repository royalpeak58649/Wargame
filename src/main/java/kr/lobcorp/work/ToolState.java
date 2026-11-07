package kr.lobcorp.work;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** 도구형 환상체의 시설 단위 상태 */
public final class ToolState {
    private ToolState() {}

    /** 거대한 나무 수액: 항아리가 터질 확률 (%) */
    public static int sapChance = 0;

    /** 3월 27일의 대피소: 안에 있는 직원 */
    public static UUID shelterUser = null;
    public static int shelterTicks = 0;
    public static int shelterEntityId = -1;

    /** 대피소 안에 있는가 (피해 무효) */
    public static boolean isSheltered(ServerPlayer p) {
        return p != null && shelterUser != null && shelterUser.equals(p.getUUID());
    }

    // ───────────── 다른 세계의 초상 (O-09-91) ─────────────

    /** 새겨진 자: 받을 피해가 대신하는 자에게 옮겨 간다 */
    public static UUID etched = null;
    /** 대신하는 자 */
    public static UUID victim = null;
    /** 지금까지 옮겨 간 피해 누적 */
    public static float portraitTotal = 0;

    public static boolean isEtched(ServerPlayer p) {
        return p != null && etched != null && etched.equals(p.getUUID());
    }

    public static boolean isVictim(ServerPlayer p) {
        return p != null && victim != null && victim.equals(p.getUUID());
    }

    /** 누적 피해에 따른 피해 배율 */
    public static float portraitMult() {
        if (portraitTotal >= 200f) return 2.5f;
        if (portraitTotal >= 100f) return 2.0f;
        return 1.5f;
    }

    public static void resetPortrait() {
        etched = null;
        victim = null;
        portraitTotal = 0;
    }

    // ───────────── 미친 연구원의 노트 (T-09-78) ─────────────

    /** 노트를 들고 있는 직원 */
    public static UUID notesHolder = null;
    /** 노트를 들고 있은 지 지난 틱 */
    public static int notesTicks = 0;
    /** 노트를 든 채로 작업을 한 번이라도 마쳤는가 */
    public static boolean notesWorked = false;
    /** 노트를 든 채로 누적된 HP·SP 피해 */
    public static float notesDamage = 0;

    public static final int NOTES_SAFE_TICKS = 600;
    public static final float NOTES_DAMAGE_LIMIT = 60f;

    public static boolean isNotesHolder(ServerPlayer p) {
        return p != null && notesHolder != null && notesHolder.equals(p.getUUID());
    }

    public static void clearNotes() {
        notesHolder = null;
        notesTicks = 0;
        notesWorked = false;
        notesDamage = 0;
    }

    /** 노트를 든 직원이 피해를 받았다. 누적이 한계를 넘으면 터진다. */
    public static void addNotesDamage(ServerPlayer p, float amount) {
        if (!isNotesHolder(p) || amount <= 0) return;
        notesDamage += amount;
        if (notesDamage >= NOTES_DAMAGE_LIMIT) {
            kr.lobcorp.entity.NotesEntity.explode(p);
        }
    }

    /** 매 틱: 노트 보유 시간을 센다 */
    public static void tick(MinecraftServer server) {
        if (notesHolder == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(notesHolder);
        if (p == null || !p.isAlive()) {
            clearNotes();
            return;
        }
        notesTicks++;
    }

    public static void nextDay(MinecraftServer server) {
        sapChance = 0;
        shelterUser = null;
        shelterTicks = 0;
        shelterEntityId = -1;
        resetPortrait();
        // 노트는 폭발하지 않고 그냥 손에서 미끄러진다
        if (notesHolder != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(notesHolder);
            if (p != null) {
                p.sendSystemMessage(Component.literal("  손에 쥔 노트가 스르륵 미끄러져 바닥에 떨어졌다.")
                        .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
            }
        }
        clearNotes();
    }

    /** 직원이 죽었을 때 (도구형 환상체 관련 후속 처리) */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer p)) return;
        if (isNotesHolder(p)) clearNotes();
        // 따뜻한 마음의 나무꾼: 곁에서 죽은 직원의 심장을 가슴에 밀어 넣는다
        kr.lobcorp.entity.WoodsmanEntity.onPlayerDeath(p);
        // 은하수의 아이: 조약돌을 지닌 직원이 죽으면 아이가 무너진다
        if (Employees.get(p).pebble) {
            kr.lobcorp.entity.ChildGalaxyEntity child = kr.lobcorp.entity.ChildGalaxyEntity.current(p.level().getServer());
            if (child != null) child.holderDied(p);
            Employees.get(p).pebble = false;
        }
        if (isEtched(p)) {
            resetPortrait();
            if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                Facility.broadcast(sl.getServer(), Component.literal("[관제] 초상화 속 얼굴이 지워졌습니다. 초상화가 제자리로 돌아갑니다.")
                        .withStyle(s -> s.withColor(0x8A4AC0)));
            }
        } else if (isVictim(p)) {
            UUID e = etched;
            resetPortrait();
            if (e != null && p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                ServerPlayer ep = sl.getServer().getPlayerList().getPlayer(e);
                if (ep != null) {
                    ep.sendSystemMessage(Component.literal("  초상화 속 얼굴이 흐려진다. 대신 받아 줄 사람이 사라졌다.")
                            .withStyle(s -> s.withColor(0x8A4AC0).withItalic(true)));
                }
            }
        }
    }
}
