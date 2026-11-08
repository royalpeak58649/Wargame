package kr.lobcorp.work;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.DamageColor;
import kr.lobcorp.ModItems;
import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.CrumblingArmorEntity;
import kr.lobcorp.entity.FairyFestivalEntity;
import kr.lobcorp.entity.LaetitiaEntity;
import kr.lobcorp.entity.PunishingBirdEntity;
import kr.lobcorp.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/** 작업 진행 (PE-BOX 판정) */
public final class WorkManager {
    private WorkManager() {}

    public enum Result { BAD, NORMAL, GOOD }

    public static final double WORK_RANGE = 7.0;
    private static final Map<UUID, WorkSession> SESSIONS = new HashMap<>();

    public static WorkSession session(ServerPlayer p) {
        return SESSIONS.get(p.getUUID());
    }

    public static boolean isAbnoBusy(AbnormalityEntity a) {
        for (WorkSession s : SESSIONS.values()) if (s.abno == a) return true;
        return false;
    }

    // ───────────── 화면 열기 / 버튼 처리 ─────────────

    public static void openScreen(ServerPlayer p, AbnormalityEntity a) {
        if (ServerPlayNetworking.canSend(p, Payloads.OpenWork.TYPE)) {
            ServerPlayNetworking.send(p, new Payloads.OpenWork(a.getId(), a.profile().id(), a.getPe(), a.getQliphoth(), isAbnoBusy(a)));
        } else {
            p.sendSystemMessage(Component.literal("LobCorp 모드가 클라이언트에 설치되어 있지 않습니다.").withStyle(s -> s.withColor(0xFF5555)));
        }
    }

    public static void handleAction(ServerPlayer p, int entityId, int action) {
        Entity e = p.level().getEntity(entityId);
        if (!(e instanceof AbnormalityEntity a) || !a.isAlive()) return;
        if (p.distanceTo(a) > WORK_RANGE + 2) {
            actionbar(p, Component.literal("환상체와 너무 멀리 떨어져 있습니다.").withStyle(s -> s.withColor(0xFF5555)));
            return;
        }
        if (a.profile().isTool()) {
            if (action == 0) a.useTool(p);
            return;
        }
        if (action >= 0 && action <= 3) start(p, a, WorkType.byIndex(action));
        else if (action == 11 || action == 12) extractEgo(p, a, action == 11);
    }

    private static void start(ServerPlayer p, AbnormalityEntity a, WorkType type) {
        EmployeeData d = Employees.get(p);
        if (session(p) != null) { actionbar(p, red("이미 작업 중입니다.")); return; }
        if (d.isPanicking()) { actionbar(p, red("패닉 상태에서는 작업할 수 없습니다.")); return; }
        if (d.possessTicks > 0) { actionbar(p, red("홀린 상태로는 작업할 수 없습니다.")); return; }
        if (d.frost == 2) { actionbar(p, red("얼어붙어 움직일 수 없습니다.")); return; }
        if (d.devoured) { actionbar(p, red("늑대의 뱃속에서는 아무것도 할 수 없습니다.")); return; }
        if (d.blessed) { actionbar(p, red("가호를 받은 몸으로는 작업할 수 없습니다.")); return; }
        if (a.isBreached()) { actionbar(p, red("탈출한 환상체에게는 작업할 수 없습니다. 제압하세요!")); return; }
        if (isAbnoBusy(a)) { actionbar(p, red("다른 직원이 작업 중인 환상체입니다.")); return; }

        // 요정의 축제: 축복받은 채 다른 환상체에 들어가면 잡아먹힌다
        if (d.fairyTicks > 0 && !(a instanceof FairyFestivalEntity)) {
            FairyFestivalEntity.devour(p, a);
            return;
        }
        // 부서지는 갑옷: 「용기」를 지닌 직원이 애착 작업을 시작하면 목이 베인다
        if (d.hasGift("courage") && type == WorkType.ATTACHMENT) {
            CrumblingArmorEntity.decapitate(p, "용기를 지닌 채 애착 작업");
            return;
        }
        // 환상체별 사전 판정 (행복한 테디베어의 포옹 등)
        if (!a.beforeWorkStart(p, type)) return;

        AbnoProfile prof = a.profile();
        boolean notes = ToolState.isNotesHolder(p);
        int chance = prof.chances()[type.ordinal()] + d.temperance / 5 + d.giftChanceBonus()
                + a.workChanceBonus(p, type) + (notes ? 20 : 0);
        chance = Math.max(5, Math.min(95, chance));
        int interval = Math.max(10, 30 - d.temperance / 4);
        if (notes) interval = Math.max(6, interval * 3 / 4);
        if (d.firebirdBlind && !(a instanceof kr.lobcorp.entity.FirebirdEntity)) interval *= 2;
        SESSIONS.put(p.getUUID(), new WorkSession(a, type, prof.maxBox(), chance, interval));
        PunishingBirdEntity.onAnyWorkStarted(p.level(), a, p);
        // 은하수의 아이: 조약돌을 지닌 채 다른 환상체를 작업하면 아이가 배신을 알아챈다
        if (d.pebble && !(a instanceof kr.lobcorp.entity.ChildGalaxyEntity)) {
            kr.lobcorp.entity.ChildGalaxyEntity child = kr.lobcorp.entity.ChildGalaxyEntity.current(p.level().getServer());
            if (child != null) child.holderBetrayed(p);
        }

        p.sendSystemMessage(Component.literal("▶ ").withStyle(s -> s.withColor(0x555555))
                .append(Component.literal(type.ko).withStyle(s -> s.withColor(type.rgb)))
                .append(Component.literal(" 작업을 시작합니다. (환상체 곁을 " + (int) WORK_RANGE + "블록 이상 벗어나면 남은 박스는 실패)").withStyle(s -> s.withColor(0xAAAAAA))));
        a.level().playSound(null, a.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1f, 0.6f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.4f);
        a.onWorkStart(p, type);
        if (d.laetitiaHeart && !(a instanceof LaetitiaEntity)) {
            actionbar(p, Component.literal("선물이 금 가기 시작했다…").withStyle(s -> s.withColor(0xFF9AD0)));
        }
        Employees.sync(p);
    }

    // ───────────── 틱 ─────────────

    public static final double ABANDON_RANGE = 16.0;

    public static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, WorkSession> en : new ArrayList<>(SESSIONS.entrySet())) {
            UUID id = en.getKey();
            WorkSession s = en.getValue();
            if (SESSIONS.get(id) != s) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive() || !s.abno.isAlive() || s.abno.isBreached()) {
                SESSIONS.remove(id);
                if (p != null) p.sendSystemMessage(Component.literal("■ 작업 중단").withStyle(st -> st.withColor(0xAA0000)));
                continue;
            }
            // 격리실을 크게 벗어나면 작업을 포기한 것으로 본다 (라티샤의 선물을 지키는 유일한 방법)
            if (p.distanceTo(s.abno) > ABANDON_RANGE) {
                abort(p, "작업 포기");
                continue;
            }
            if (++s.tick >= s.interval) {
                s.tick = 0;
                processBox(p, s);
                if (SESSIONS.get(id) != s) continue; // 패닉 등으로 중단됨
                if (s.box >= s.maxBox) {
                    SESSIONS.remove(id);
                    finish(p, s);
                }
            }
        }
    }

    private static void processBox(ServerPlayer p, WorkSession s) {
        s.box++;
        ServerLevel level = p.level();
        // 라티샤의 선물: 다른 환상체와의 작업이 절반을 넘으면 배가 터진다
        EmployeeData heartData = Employees.get(p);
        if (heartData.laetitiaHeart && !(s.abno instanceof LaetitiaEntity)) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 2.2, p.getZ(), 2, 0.2, 0.1, 0.2, 0);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 2.2, p.getZ(), 6, 0.3, 0.2, 0.3, 0.01);
            if (s.box * 2 >= s.maxBox) {
                LaetitiaEntity.burst(p);
                return;
            }
        }
        boolean near = p.distanceTo(s.abno) <= WORK_RANGE;
        int roll = 1 + level.getRandom().nextInt(100);
        if (near && roll <= s.chance) {
            s.good++;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.abno.getX(), s.abno.getY() + 1.5, s.abno.getZ(), 6, 0.4, 0.5, 0.4, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 1.7f);
            s.abno.onBoxResult(p, true);
            return;
        }
        // 실패 → 환상체 피해
        AbnoProfile prof = s.abno.profile();
        int dmg = prof.dmgMin() + level.getRandom().nextInt(prof.dmgMax() - prof.dmgMin() + 1);
        dmg = Math.max(1, Math.round(dmg * s.abno.workDamageMultiplier(p)));
        if (!near) actionbar(p, red("환상체와 너무 멀리 떨어져 있습니다!"));
        switch (prof.workDamage()) {
            case WHITE -> {
                if (wearsAny(p, WHITE_SUITS)) dmg--;
                if (ToolState.isYangHolder(p)) dmg--;
                level.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.4, 0.6, 0.4, 0);
                level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 0.5f);
                if (dmg > 0) Employees.damageSp(p, dmg);
            }
            case BLACK -> {
                if (blackSuit(p)) dmg--;
                // HP를 먼저 깎는다 — damageSp가 패닉을 일으켜 세션을 중단시킬 수 있기 때문
                level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.6, 0.4, 0);
                level.playSound(null, p.blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 0.8f, 0.6f);
                if (dmg > 0) p.hurtServer(level, level.damageSources().magic(), dmg);
                if (dmg > 0) Employees.damageSp(p, dmg);
            }
            case PALE -> {
                // PALE: 피해 수치 1당 최대 HP의 5%
                level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.6f);
                Employees.damagePale(p, dmg * 5f);
            }
            default -> {
                if (wearsAny(p, RED_SUITS)) dmg--;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 4, 0.3, 0.3, 0.3, 0);
                if (dmg > 0) p.hurtServer(level, level.damageSources().magic(), dmg);
            }
        }
        s.abno.onBoxResult(p, false);
    }

    private static void finish(ServerPlayer p, WorkSession s) {
        AbnormalityEntity a = s.abno;
        AbnoProfile prof = a.profile();
        EmployeeData d = Employees.get(p);

        Facility.addEnergy(p.level().getServer(), s.good);
        a.addPe(s.good);

        Result r = s.good >= prof.goodMin() ? Result.GOOD : s.good >= prof.normalMin() ? Result.NORMAL : Result.BAD;
        int gain = s.good / 4;
        d.addStat(s.type, gain);
        Employees.apply(p);

        MutableComponent line = Component.literal("■ 작업 결과: ").withStyle(st -> st.withColor(0xAAAAAA));
        switch (r) {
            case GOOD -> line.append(Component.literal("좋음").withStyle(st -> st.withColor(0x55FF55).withBold(true)));
            case NORMAL -> line.append(Component.literal("보통").withStyle(st -> st.withColor(0xFFFF55).withBold(true)));
            case BAD -> line.append(Component.literal("나쁨").withStyle(st -> st.withColor(0xFF5555).withBold(true)));
        }
        line.append(Component.literal("   PE-BOX +" + s.good).withStyle(st -> st.withColor(0x55FFFF)));
        if (gain > 0) line.append(Component.literal("   " + s.type.stat + " +" + gain).withStyle(st -> st.withColor(s.type.rgb)));
        p.sendSystemMessage(line);

        a.onWorkFinished(p, r, s);
        d.lastWorked = prof.id();
        if (ToolState.isNotesHolder(p)) ToolState.notesWorked = true;

        // E.G.O 기프트
        if (r != Result.BAD && !d.hasGift(prof.giftId()) && p.getRandom().nextInt(100) < prof.giftChance()) {
            d.gifts.add(prof.giftId());
            Employees.apply(p);
            Facility.broadcast(p.level().getServer(), Component.literal("[관제] ").withStyle(st -> st.withColor(0xAA0000))
                    .append(p.getDisplayName())
                    .append(Component.literal(" 직원이 E.G.O 기프트 「" + prof.giftName() + "」를 받았습니다. (" + prof.giftDesc() + ")").withStyle(st -> st.withColor(0xF0E6C8))));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.4f);
        Employees.sync(p);

        // 다른 환상체들에게 작업 완료를 알린다 (목록을 먼저 모은 뒤 호출)
        java.util.List<AbnormalityEntity> others = new ArrayList<>();
        for (Entity e : p.level().getAllEntities()) {
            if (e instanceof AbnormalityEntity other && other != a) others.add(other);
        }
        for (AbnormalityEntity other : others) {
            if (other.isAlive()) other.onOtherWorkFinished(p, a, r);
        }
    }

    /** 패닉/사망 등으로 작업 중단. 이미 생산한 PE는 반영 */
    public static void abort(ServerPlayer p, String reason) {
        WorkSession s = SESSIONS.remove(p.getUUID());
        if (s == null) return;
        Facility.addEnergy(p.level().getServer(), s.good);
        s.abno.addPe(s.good);
        p.sendSystemMessage(Component.literal("■ 작업 중단 (" + reason + ")  PE-BOX +" + s.good).withStyle(st -> st.withColor(0xAA0000)));
        s.abno.onWorkAborted(p, s, reason);
    }

    public static void clear(ServerPlayer p) {
        SESSIONS.remove(p.getUUID());
    }

    // ───────────── E.G.O 추출 ─────────────

    private static void extractEgo(ServerPlayer p, AbnormalityEntity a, boolean weapon) {
        AbnoProfile prof = a.profile();
        int cost = weapon ? prof.weaponCost() : prof.suitCost();
        Item item = weapon ? prof.weapon().get() : prof.suit().get();
        if (a.getPe() < cost) {
            p.sendSystemMessage(red("PE가 부족합니다. (" + a.getPe() + " / " + cost + ")"));
            return;
        }
        a.addPe(-cost);
        ItemStack stack = new ItemStack(item);
        p.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
        p.sendSystemMessage(Component.literal("[E.G.O] ").withStyle(st -> st.withColor(0xF0E6C8))
                .append(Component.literal("「").append(new ItemStack(item).getHoverName()).append("」를 추출했습니다.").withStyle(st -> st.withColor(0xAAAAAA))));
    }

    // ───────────── 유틸 ─────────────

    public static boolean wearing(ServerPlayer p, Item item) {
        return p.getItemBySlot(EquipmentSlot.CHEST).is(item);
    }

    /** BLACK 저항 방호복 착용 여부 (작업 실패 시 HP·SP 피해 -1) */

    // ── 방호복 저항 목록: 새 E.G.O 방호복은 해당 목록에 한 줄 추가 ──
    /** WHITE 작업 피해 -1 */
    public static final List<java.util.function.Supplier<Item>> WHITE_SUITS = List.of(
            () -> ModItems.CRIMSON_SCAR_SUIT,
            () -> ModItems.COBALT_SCAR_SUIT,
            () -> ModItems.FAINT_AROMA_SUIT,
            () -> ModItems.HORNET_SUIT,
            () -> ModItems.PENITENCE_SUIT,
            () -> ModItems.BEAR_PAWS_SUIT,
            () -> ModItems.SCREAMING_WEDGE_SUIT,
            () -> ModItems.SOLITUDE_SUIT,
            () -> ModItems.HORN_SUIT,
            () -> ModItems.LANTERN_SUIT,
            () -> ModItems.NOISE_SUIT,
            () -> ModItems.HARMONY_SUIT,
            () -> ModItems.PLEASURE_SUIT,
            () -> ModItems.LAETITIA_SUIT,
            () -> ModItems.OUR_GALAXY_SUIT,
            () -> ModItems.MAGIC_BULLET_SUIT,
            () -> ModItems.SYRINX_SUIT,
            () -> ModItems.FROST_SPLINTER_SUIT,
            () -> ModItems.LOGGING_SUIT,
            () -> ModItems.HARVEST_SUIT,
            () -> ModItems.CHRISTMAS_SUIT,
            () -> ModItems.SOLEMN_LAMENT_SUIT,
            () -> ModItems.GAZE_SUIT,
            () -> ModItems.LAMP_SUIT,
            () -> ModItems.JUSTITIA_SUIT,
            () -> ModItems.LOVE_AND_HATE_SUIT,
            () -> ModItems.TEARS_SWORD_SUIT,
            () -> ModItems.GOLD_RUSH_SUIT,
            () -> ModItems.BLACK_SWAN_SUIT,
            () -> ModItems.DISCORD_SUIT,
            () -> ModItems.GREEN_STEM_SUIT,
            () -> ModItems.SPORE_SUIT,
            () -> ModItems.HYPOCRISY_SUIT,
            () -> ModItems.ECSTASY_SUIT,
            () -> ModItems.HEAVEN_SUIT,
            () -> ModItems.FEATHER_OF_HONOR_SUIT);
    /** RED·PALE 작업 피해 -1 */
    public static final List<java.util.function.Supplier<Item>> RED_SUITS = List.of(
            () -> ModItems.CRIMSON_SCAR_SUIT,
            () -> ModItems.COBALT_SCAR_SUIT,
            () -> ModItems.HORNET_SUIT,
            () -> ModItems.REGRET_SUIT,
            () -> ModItems.MATCH_FLAME_SUIT,
            () -> ModItems.WINGBEAT_SUIT,
            () -> ModItems.BEAK_SUIT,
            () -> ModItems.DAREDEVIL_SUIT,
            () -> ModItems.RED_EYES_SUIT,
            () -> ModItems.HORN_SUIT,
            () -> ModItems.LANTERN_SUIT,
            () -> ModItems.SANGUINE_DESIRE_SUIT,
            () -> ModItems.LAETITIA_SUIT,
            () -> ModItems.GRINDER_SUIT,
            () -> ModItems.OUR_GALAXY_SUIT,
            () -> ModItems.MAGIC_BULLET_SUIT,
            () -> ModItems.LOGGING_SUIT,
            () -> ModItems.HARVEST_SUIT,
            () -> ModItems.CHRISTMAS_SUIT,
            () -> ModItems.GAZE_SUIT,
            () -> ModItems.LAMP_SUIT,
            () -> ModItems.JUSTITIA_SUIT,
            () -> ModItems.LOVE_AND_HATE_SUIT,
            () -> ModItems.TEARS_SWORD_SUIT,
            () -> ModItems.GOLD_RUSH_SUIT,
            () -> ModItems.BLACK_SWAN_SUIT,
            () -> ModItems.HYPOCRISY_SUIT,
            () -> ModItems.EXUVIAE_SUIT,
            () -> ModItems.FEATHER_OF_HONOR_SUIT);
    /** BLACK 작업 피해 -1 */
    public static final List<java.util.function.Supplier<Item>> BLACK_SUITS = List.of(
            () -> ModItems.CRIMSON_SCAR_SUIT,
            () -> ModItems.COBALT_SCAR_SUIT,
            () -> ModItems.FAINT_AROMA_SUIT,
            () -> ModItems.HORNET_SUIT,
            () -> ModItems.FROST_SPLINTER_SUIT,
            () -> ModItems.FRAGMENTS_SUIT,
            () -> ModItems.NOISE_SUIT,
            () -> ModItems.PLEASURE_SUIT,
            () -> ModItems.LAETITIA_SUIT,
            () -> ModItems.MAGIC_BULLET_SUIT,
            () -> ModItems.SYRINX_SUIT,
            () -> ModItems.SOLEMN_LAMENT_SUIT,
            () -> ModItems.LAMP_SUIT,
            () -> ModItems.JUSTITIA_SUIT,
            () -> ModItems.LOVE_AND_HATE_SUIT,
            () -> ModItems.TEARS_SWORD_SUIT,
            () -> ModItems.CRIMSON_SCAR_SUIT,
            () -> ModItems.BLACK_SWAN_SUIT,
            () -> ModItems.DISCORD_SUIT,
            () -> ModItems.GREEN_STEM_SUIT,
            () -> ModItems.HEAVEN_SUIT);

    public static boolean wearsAny(ServerPlayer p, List<java.util.function.Supplier<Item>> suits) {
        for (var s : suits) if (wearing(p, s.get())) return true;
        return false;
    }

    public static boolean blackSuit(ServerPlayer p) {
        return wearsAny(p, BLACK_SUITS);
    }

    public static MutableComponent red(String s) {
        return Component.literal(s).withStyle(st -> st.withColor(0xFF5555));
    }

    public static void actionbar(ServerPlayer p, Component c) {
        p.sendOverlayMessage(c);
    }
}
