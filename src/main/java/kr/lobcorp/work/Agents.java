package kr.lobcorp.work;

import kr.lobcorp.ModEntities;
import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.AgentEntity;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 관리직(플레이어가 명령하는 직원)의 고용·선택·명령을 모아 둔 곳.
 * 선택 상태는 서버 메모리에만 남는다(재접속 시 초기화).
 */
public final class Agents {
    private Agents() {}

    /** 플레이어 한 명이 고용할 수 있는 최대 관리직 수 */
    public static final int MAX_PER_PLAYER = 5;

    /** 플레이어 → 선택된 관리직 UUID */
    public static final Map<UUID, Set<UUID>> SELECTED = new HashMap<>();

    /** 플레이어가 고용한 살아 있는 관리직 */
    public static List<AgentEntity> owned(ServerPlayer p) {
        List<AgentEntity> out = new ArrayList<>();
        if (p == null) return out;
        AABB box = p.getBoundingBox().inflate(256);
        for (AgentEntity a : p.level().getEntitiesOfClass(AgentEntity.class, box)) {
            if (a.isAlive() && p.getUUID().equals(a.owner())) out.add(a);
        }
        return out;
    }

    /** 관리직을 한 명 고용한다. 실패하면 오류 문구를 돌려준다. */
    public static String hire(ServerPlayer p) {
        if (p == null) return "플레이어만 고용할 수 있습니다.";
        if (owned(p).size() >= MAX_PER_PLAYER) return "관리직은 최대 5명까지 고용할 수 있습니다.";
        if (!(p.level() instanceof ServerLevel sl)) return "이곳에서는 고용할 수 없습니다.";
        AgentEntity a = ModEntities.AGENT.create(sl, EntitySpawnReason.EVENT);
        if (a == null) return "관리직을 고용하지 못했습니다.";
        a.snapTo(p.getX() + 1.0, p.getY(), p.getZ() + 1.0, p.getYRot(), 0f);
        String name = Clerks.NAMES[sl.getRandom().nextInt(Clerks.NAMES.length)];
        a.initAgent(p, name, sl.getRandom().nextInt(4));
        a.setPersistenceRequired();
        sl.addFreshEntity(a);
        p.sendSystemMessage(Component.literal("[관제] 관리직 " + a.agentName() + "을(를) 고용했습니다. ("
                + owned(p).size() + "/" + MAX_PER_PLAYER + ")").withStyle(s -> s.withColor(0x55FF55)));
        p.sendSystemMessage(describe(a));
        return null;
    }

    /** 선택된 관리직 (죽었거나 사라진 것은 목록에서 지운다) */
    public static List<AgentEntity> selected(ServerPlayer p) {
        List<AgentEntity> out = new ArrayList<>();
        if (p == null) return out;
        Set<UUID> ids = SELECTED.get(p.getUUID());
        if (ids == null || ids.isEmpty()) return out;
        if (!(p.level() instanceof ServerLevel sl)) return out;
        ids.removeIf(id -> {
            Entity e = sl.getEntity(id);
            return !(e instanceof AgentEntity a) || !a.isAlive();
        });
        for (UUID id : ids) {
            Entity e = sl.getEntity(id);
            if (e instanceof AgentEntity a && a.isAlive()) out.add(a);
        }
        return out;
    }

    /** 관리직 선택/해제 */
    public static void toggle(ServerPlayer p, AgentEntity a) {
        if (p == null || a == null) return;
        Set<UUID> ids = SELECTED.computeIfAbsent(p.getUUID(), k -> new HashSet<>());
        if (ids.remove(a.getUUID())) {
            a.removeEffect(MobEffects.GLOWING);
            p.sendSystemMessage(Component.literal("관리직 " + a.agentName() + " 선택 해제 (선택 "
                    + ids.size() + "명)").withStyle(s -> s.withColor(0xAAAAAA)));
            return;
        }
        ids.add(a.getUUID());
        a.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 30, 0, false, false));
        p.sendSystemMessage(Component.literal("관리직 " + a.agentName() + " 선택됨 (선택 "
                + ids.size() + "명)").withStyle(s -> s.withColor(0x55FFFF)));
    }

    /** 관리직 한 명의 상태 한 줄 */
    public static Component describe(AgentEntity a) {
        String mode = switch (a.mode()) {
            case FOLLOW -> "추종";
            case HOLD -> "대기";
            case WORK -> "작업";
            case FIGHT -> "전투";
        };
        return Component.literal("관리직 " + a.agentName()
                + "  용기 " + EmployeeData.grade(a.fortitude()) + "(" + a.fortitude() + ")"
                + " 신중 " + EmployeeData.grade(a.prudence()) + "(" + a.prudence() + ")"
                + " 자제 " + EmployeeData.grade(a.temperance()) + "(" + a.temperance() + ")"
                + " 정의 " + EmployeeData.grade(a.justice()) + "(" + a.justice() + ")"
                + "  HP " + Math.round(a.getHealth()) + "/" + Math.round(a.getMaxHealth())
                + "  SP " + a.sp() + "/" + a.maxSp()
                + "  [" + mode + "]").withStyle(s -> s.withColor(0xCCCCCC));
    }

    /** 선택된 관리직 중 명령을 받을 수 있는 첫 번째 */
    private static AgentEntity freeAgent(ServerPlayer p) {
        for (AgentEntity a : selected(p)) {
            if (a.mode() != AgentEntity.Mode.WORK && !a.isPanicking()) return a;
        }
        return null;
    }

    /** 환상체 작업 메뉴 — 작업 종류별 클릭 버튼을 한 줄로 보낸다. */
    public static void showWorkMenu(ServerPlayer p, AbnormalityEntity a) {
        if (p == null || a == null) return;
        if (selected(p).isEmpty()) {
            p.sendSystemMessage(Component.literal("먼저 태블릿으로 관리직을 선택하세요.")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return;
        }
        AgentEntity agent = freeAgent(p);
        if (agent == null) {
            p.sendSystemMessage(Component.literal("지금은 명령을 받을 수 있는 관리직이 없습니다.")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return;
        }
        MutableComponent row = Component.literal("▶ " + agent.agentName() + " → " + a.profile().name() + ": ")
                .withStyle(s -> s.withColor(0xFFFFFF));
        for (WorkType t : WorkType.values()) {
            int chance = Math.max(5, Math.min(95, a.profile().chances()[t.ordinal()] + agent.temperance() / 5));
            row.append(Component.literal("[" + t.ko + " " + chance + "%]").withStyle(s -> s.withColor(t.rgb)
                    .withClickEvent(new ClickEvent.RunCommand("/lobcorp agent work " + a.getId() + " " + t.ordinal()))));
            row.append(Component.literal(" "));
        }
        p.sendSystemMessage(row);
    }

    /** 클릭한 작업 명령을 선택된 관리직에게 내린다. */
    public static void orderWork(ServerPlayer p, int entityId, int typeIdx) {
        if (p == null) return;
        Entity e = p.level().getEntity(entityId);
        if (!(e instanceof AbnormalityEntity a) || a.distanceTo(p) > 64) {
            p.sendSystemMessage(Component.literal("작업할 환상체를 찾을 수 없습니다.")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return;
        }
        if (selected(p).isEmpty()) {
            p.sendSystemMessage(Component.literal("먼저 태블릿으로 관리직을 선택하세요.")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return;
        }
        AgentEntity agent = freeAgent(p);
        if (agent == null) {
            p.sendSystemMessage(Component.literal("지금은 명령을 받을 수 있는 관리직이 없습니다.")
                    .withStyle(s -> s.withColor(0xFFAA00)));
            return;
        }
        String err = agent.orderWork(a, WorkType.byIndex(typeIdx));
        if (err != null) {
            p.sendSystemMessage(Component.literal("관리직 " + agent.agentName() + ": " + err)
                    .withStyle(s -> s.withColor(0xFF5555)));
            return;
        }
        p.sendSystemMessage(Component.literal("관리직 " + agent.agentName() + "이(가) 작업하러 갑니다.")
                .withStyle(s -> s.withColor(0x55FF55)));
    }
}
