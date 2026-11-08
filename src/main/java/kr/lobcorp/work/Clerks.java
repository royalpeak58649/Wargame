package kr.lobcorp.work;

import kr.lobcorp.ModEntities;
import kr.lobcorp.entity.AgentEntity;
import kr.lobcorp.entity.ClerkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.ArrayList;
import java.util.List;

/** 사무직 배치/정리. 부서 중앙 홀마다 몇 명씩 상주시킨다. */
public final class Clerks {
    private Clerks() {}

    /** 부서당 사무직 수 */
    public static final int PER_DEPT = 2;

    /** 무작위로 붙일 한국어 이름 */
    public static final String[] NAMES = {
            "민준", "서연", "지호", "하은", "도윤", "지우", "예준", "수아", "시우", "지민",
            "주원", "하윤", "건우", "서윤", "우진", "지유", "현우", "채원", "지훈", "다은",
            "성민", "유진", "재원", "소율", "태윤", "예린", "동현", "나윤", "승우", "혜원"
    };

    /** 모든 부서에 사무직을 다시 배치하고 배치한 수를 돌려준다. */
    public static int spawnAll(MinecraftServer server) {
        if (server == null) return 0;
        discardAll(server);
        ServerLevel level = server.overworld();
        int n = 0;
        for (BlockPos c : Facility.centers(server)) {
            for (int i = 0; i < PER_DEPT; i++) {
                ClerkEntity clerk = ModEntities.CLERK.create(level, EntitySpawnReason.EVENT);
                if (clerk == null) continue;
                double x = c.getX() + 0.5 + (level.getRandom().nextInt(7) - 3);
                double z = c.getZ() + 0.5 + (level.getRandom().nextInt(7) - 3);
                clerk.snapTo(x, c.getY(), z, level.getRandom().nextFloat() * 360f, 0);
                clerk.setup(c, level.getRandom().nextInt(4), NAMES[level.getRandom().nextInt(NAMES.length)]);
                clerk.setPersistenceRequired();
                level.addFreshEntity(clerk);
                n++;
            }
        }
        return n;
    }

    /** 오버월드의 모든 사무직을 제거한다. */
    public static void discardAll(MinecraftServer server) {
        if (server == null) return;
        for (ClerkEntity c : all(server.overworld())) c.discard();
    }

    /** 해당 레벨의 살아 있는 사무직 */
    public static List<ClerkEntity> all(ServerLevel level) {
        List<ClerkEntity> out = new ArrayList<>();
        if (level == null) return out;
        for (Entity e : level.getAllEntities()) {
            // 관리직(AgentEntity)은 사무직이 아니므로 하루 교체 대상에서 제외한다
            if (e instanceof ClerkEntity c && !(e instanceof AgentEntity) && c.isAlive()) out.add(c);
        }
        return out;
    }

    /** 오버월드의 살아 있는 사무직 수 */
    public static int count(MinecraftServer server) {
        if (server == null) return 0;
        return all(server.overworld()).size();
    }
}
