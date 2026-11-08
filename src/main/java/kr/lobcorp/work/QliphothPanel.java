package kr.lobcorp.work;

import kr.lobcorp.entity.AbnormalityEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import com.mojang.math.Transformation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import java.util.HashSet;
import java.util.Set;

/** 격리실 문 위(복도 쪽)에 떠 있는 클리포트 카운터 글자판. */
public final class QliphothPanel {
    private QliphothPanel() {}

    private static Set<String> corner;

    private static boolean isCorner(String id) {
        if (corner == null) {
            Set<String> s = new HashSet<>();
            FacilityBuilder.catalog().forEach((k, u) -> { if (u.corner()) s.add(k); });
            corner = s;
        }
        return corner.contains(id);
    }

    /** 집 위치에서 격리실 기준점(RoomBuilder의 o)을 거꾸로 구한다. */
    public static BlockPos roomOrigin(AbnormalityEntity a, BlockPos home) {
        return isCorner(a.profile().id()) ? home.offset(-9, 0, -3) : home.offset(-6, 0, 0);
    }

    /** 표시할 내용을 한 줄 키로 만든다(바뀌었을 때만 다시 쓰기 위해). */
    public static String key(AbnormalityEntity a) {
        return a.isBreached() ? "B" : a.getQliphoth() + "/" + a.profile().qliphothMax();
    }

    public static final String TAG = "lobcorp_panel";

    /** 복도 쪽 정면 벽, 문 위에 떠 있는 글자판을 찾거나 만들고 내용을 맞춘다. */
    public static void update(ServerLevel level, AbnormalityEntity a, BlockPos home, boolean force) {
        BlockPos o = roomOrigin(a, home);
        // 표준 격리실(흰 정면 벽)일 때만 붙인다
        if (!level.getBlockState(o.offset(2, 2, 0)).is(Blocks.CONCRETE.white())) return;
        double x = o.getX() + 1.97, y = o.getY() + 3.02, z = o.getZ() + 0.5;
        Display.TextDisplay disp = null;
        var found = level.getEntitiesOfClass(Display.TextDisplay.class, new AABB(x - 0.6, y - 0.6, z - 0.6, x + 0.6, y + 0.6, z + 0.6),
                e -> e.entityTags().contains(TAG));
        for (int i = 0; i < found.size(); i++) {
            if (i == 0) disp = found.get(0); else found.get(i).discard();
        }
        if (disp == null) {
            disp = net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
            if (disp == null) return;
            disp.addTag(TAG);
            disp.snapTo(x, y, z, 90f, 0f);
            disp.setBillboardConstraints(Display.BillboardConstraints.FIXED);
            disp.setLineWidth(200);
            disp.setBackgroundColor(0xC0101010);
            disp.setBrightnessOverride(new Brightness(15, 15));
            disp.setViewRange(1.5f);
            disp.setTransformation(new Transformation(null, null, new Vector3f(0.85f, 0.85f, 0.85f), null));
            level.addFreshEntity(disp);
            force = true;
        }
        if (force) disp.setText(text(a));
    }

    static Component text(AbnormalityEntity a) {
        var prof = a.profile();
        int max = prof.qliphothMax(), q = Math.max(0, Math.min(max, a.getQliphoth()));
        MutableComponent t = Component.literal(prof.code() + "  ").withStyle(s -> s.withColor(0xAAAAAA))
                .append(Component.literal(prof.name()).withStyle(s -> s.withColor(prof.riskColor())))
                .append(Component.literal("\n"));
        if (a.isBreached()) {
            t.append(Component.literal("탈출 중!").withStyle(s -> s.withColor(0xFF3333).withBold(true)));
        } else if (max <= 0) {
            t.append(Component.literal("클리포트 카운터 없음").withStyle(s -> s.withColor(0x888888)));
        } else {
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < max; i++) dots.append(i < q ? '●' : '○');
            int color = q == 0 ? 0xFF3333 : q * 2 <= max ? 0xFFAA00 : 0x55FF55;
            t.append(Component.literal("클리포트 ").withStyle(s -> s.withColor(0xFFFFFF)))
             .append(Component.literal(dots + "  " + q + "/" + max).withStyle(s -> s.withColor(color).withBold(true)));
        }
        return t;
    }

    /** 시설 전체의 글자판 제거 (/lobcorp clear 용) */
    public static void discardAll(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel l : server.getAllLevels())
            for (var e : l.getAllEntities())
                if (e instanceof Display.TextDisplay d && d.entityTags().contains(TAG)) d.discard();
    }
}
