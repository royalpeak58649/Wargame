package kr.lobcorp.work;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.entity.AbnormalityEntity;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

/**
 * 환상체 수용(추출): 원작처럼 하루가 끝날 때 후보 3종 중 하나를 골라 시설의 다음 빈 격리실에 들인다.
 * 후보 등급은 진행도에 따라 열린다 — ZAYIN·TETH는 처음부터, HE는 8종 이상 수용 후, WAW는 20종 이상 수용 후.
 */
public final class Extraction {
    private Extraction() {}

    public static final int HE_AFTER = 8;
    public static final int WAW_AFTER = 20;
    public static final int ALEPH_AFTER = 45;

    public static boolean hasFacility(MinecraftServer server) {
        return !Facility.get(server).slots.isEmpty();
    }

    public static Set<String> placed(MinecraftServer server) {
        Set<String> s = new HashSet<>();
        for (FacilityData.Slot slot : Facility.get(server).slots) if (!slot.abno.isEmpty()) s.add(slot.abno);
        return s;
    }

    /** 아직 수용하지 않은 환상체 중 지금 열린 등급만. */
    static List<String> pool(MinecraftServer server) {
        Set<String> have = placed(server);
        int n = have.size();
        List<String> open = new ArrayList<>(), all = new ArrayList<>();
        for (String id : FacilityBuilder.catalog().keySet()) {
            if (have.contains(id)) continue;
            all.add(id);
            String risk = AbnoProfile.BY_ID.get(id).risk();
            boolean ok = switch (risk) {
                case "HE" -> n >= HE_AFTER;
                case "WAW" -> n >= WAW_AFTER;
                case "ALEPH" -> n >= ALEPH_AFTER;
                default -> true;
            };
            if (ok) open.add(id);
        }
        return open.isEmpty() ? all : open;
    }

    /** 후보 3종을 새로 뽑아 모두에게 알린다. 남은 격리실이나 환상체가 없으면 아무것도 하지 않는다. */
    public static void offer(MinecraftServer server) {
        FacilityData f = Facility.get(server);
        if (!hasFacility(server)) return;
        if (f.slots.stream().noneMatch(s -> s.abno.isEmpty())) return;
        List<String> pool = pool(server);
        if (pool.isEmpty()) return;
        Collections.shuffle(pool, new Random(server.overworld().getRandom().nextLong()));
        f.offers = new ArrayList<>(pool.subList(0, Math.min(3, pool.size())));
        announce(server);
    }

    /** 현재 후보를 클릭 가능한 버튼으로 알린다. */
    public static void announce(MinecraftServer server) {
        FacilityData f = Facility.get(server);
        if (f.offers.isEmpty()) return;
        Facility.broadcast(server, Component.literal("[관제] 새 환상체 후보가 도착했습니다. 하나를 골라 수용하십시오 (OP가 클릭).")
                .withStyle(s -> s.withColor(0xFFAA00)));
        MutableComponent row = Component.literal("  ");
        for (String id : f.offers) {
            AbnoProfile p = AbnoProfile.BY_ID.get(id);
            MutableComponent hover = Component.literal(p.code() + " " + p.name() + "  (" + p.risk() + ")\n").withStyle(s -> s.withColor(p.riskColor()));
            for (String line : p.notes()) hover.append(Component.literal("\n· " + line).withStyle(s -> s.withColor(0xCCCCCC)));
            row.append(Component.literal("[" + p.risk() + " " + p.name() + "]").withStyle(s -> s.withColor(p.riskColor())
                    .withClickEvent(new ClickEvent.RunCommand("/lobcorp extract " + id))
                    .withHoverEvent(new HoverEvent.ShowText(hover))));
            row.append(Component.literal("  "));
        }
        Facility.broadcast(server, row);
    }

    public enum Outcome { OK, NO_FACILITY, FULL, UNKNOWN, ALREADY }

    /** id 환상체를 다음 빈 격리실에 들인다. */
    public static Outcome place(MinecraftServer server, String id) {
        if (!hasFacility(server)) return Outcome.NO_FACILITY;
        FacilityBuilder.Unit unit = FacilityBuilder.catalog().get(id);
        if (unit == null) return Outcome.UNKNOWN;
        if (placed(server).contains(id)) return Outcome.ALREADY;
        FacilityData f = Facility.get(server);
        FacilityData.Slot slot = null;
        for (FacilityData.Slot s : f.slots) if (s.abno.isEmpty()) { slot = s; break; }
        if (slot == null) return Outcome.FULL;
        ServerLevel level = server.overworld();
        AbnormalityEntity a = RoomBuilder.spawn(level, slot.origin, unit.type(), unit.corner());
        if (a == null) return Outcome.UNKNOWN;
        slot.abno = id;
        f.offers.clear();
        AbnoProfile p = a.profile();
        FacilityBuilder.labelSlot(level, slot.origin, p, slot.dept);
        String dept = slot.dept;
        Facility.broadcast(server, Component.literal("[관제] " + p.code() + " " + p.name() + "(" + p.risk() + ")을(를) " + dept + " 격리실에 수용했습니다.")
                .withStyle(s -> s.withColor(0x55FF55)));
        return Outcome.OK;
    }
}
