package kr.lobcorp.item;

import kr.lobcorp.entity.FreischutzEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** 마탄 의뢰서. 우클릭하면 사냥꾼이 바라보는 방향으로 탄환을 쏜다. */
public class MagicBulletOrderItem extends Item {
    public MagicBulletOrderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.FAIL;

        FreischutzEntity freischutz = null;
        for (ServerLevel other : sl.getServer().getAllLevels()) {
            for (Entity e : other.getAllEntities()) {
                if (e instanceof FreischutzEntity f) {
                    freischutz = f;
                    break;
                }
            }
            if (freischutz != null) break;
        }
        if (freischutz == null) {
            sp.sendOverlayMessage(Component.literal("사냥꾼이 시설에 없습니다.").withStyle(s -> s.withColor(0xFFAA00)));
            return InteractionResult.FAIL;
        }

        freischutz.fireOrder(sp);
        player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS;
    }
}
