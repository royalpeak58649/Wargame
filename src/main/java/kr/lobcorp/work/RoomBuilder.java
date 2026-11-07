package kr.lobcorp.work;

import kr.lobcorp.entity.AbnormalityEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** 플레이어 기준 동쪽(+X)에 9x6x9 격리실을 짓는다. 문은 서쪽 벽 가운데. */
public final class RoomBuilder {
    private RoomBuilder() {}

    public static <T extends AbnormalityEntity> T build(ServerLevel level, BlockPos o, EntityType<T> type, boolean corner) {
        shell(level, o);
        return spawn(level, o, type, corner);
    }

    /** 빈 격리실(벽·창·문·조명)만 짓는다. */
    public static void shell(ServerLevel level, BlockPos o) {
        // 외벽 (속은 비움)
        for (int x = 2; x <= 10; x++)
            for (int y = -1; y <= 4; y++)
                for (int z = -4; z <= 4; z++) {
                    boolean shell = x == 2 || x == 10 || y == -1 || y == 4 || z == -4 || z == 4;
                    set(level, o.offset(x, y, z), shell ? Blocks.CONCRETE.lightGray().defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
        // 바닥 / 천장 / 조명
        for (int x = 3; x <= 9; x++)
            for (int z = -3; z <= 3; z++) {
                set(level, o.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                set(level, o.offset(x, 4, z), Blocks.SMOOTH_QUARTZ.defaultBlockState());
            }
        for (int x = 4; x <= 8; x++) set(level, o.offset(x, 4, 0), Blocks.SEA_LANTERN.defaultBlockState());
        set(level, o.offset(6, 4, -2), Blocks.SEA_LANTERN.defaultBlockState());
        set(level, o.offset(6, 4, 2), Blocks.SEA_LANTERN.defaultBlockState());

        // 정면 벽 + 관찰창
        for (int y = 0; y <= 3; y++)
            for (int z = -4; z <= 4; z++) set(level, o.offset(2, y, z), Blocks.CONCRETE.white().defaultBlockState());
        for (int y = 1; y <= 2; y++) {
            for (int z : new int[]{-3, -2, 2, 3}) set(level, o.offset(2, y, z), Blocks.GLASS.defaultBlockState());
        }
        // 철문 + 버튼
        BlockState door = Blocks.IRON_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        set(level, o.offset(2, 0, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, o.offset(2, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockState button = Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL);
        set(level, o.offset(1, 1, 1), button.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        set(level, o.offset(3, 1, -1), button.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
    }

    /** shell()로 지은 격리실 o에 환상체를 들인다. */
    public static <T extends AbnormalityEntity> T spawn(ServerLevel level, BlockPos o, EntityType<T> type, boolean corner) {
        T abno = type.create(level, EntitySpawnReason.COMMAND);
        if (abno == null) return null;
        // 중앙(정면=서쪽, yaw 90) 또는 안쪽 구석(벽을 보고 섬)
        BlockPos home = corner ? o.offset(9, 0, 3) : o.offset(6, 0, 0);
        float yaw = corner ? -45f : 90f;
        abno.setHome(home, yaw);
        abno.snapTo(home.getX() + 0.5, home.getY() + abno.homeYOffset(), home.getZ() + 0.5, yaw, 0);
        level.addFreshEntity(abno);
        return abno;
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }
}
