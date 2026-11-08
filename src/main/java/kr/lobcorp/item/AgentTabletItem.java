package kr.lobcorp.item;

import kr.lobcorp.Grades;
import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.AgentEntity;
import kr.lobcorp.ordeal.OrdealEntity;
import kr.lobcorp.work.Agents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 관리직 지휘 태블릿.
 * 우클릭: 목록 / 웅크리고 우클릭: 추종 / 블록 우클릭: 대기 /
 * 관리직 우클릭: 선택 / 환상체 우클릭: 작업 지시 / 적 우클릭: 제압 지시.
 */
public class AgentTabletItem extends Item {
    public AgentTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        List<AgentEntity> sel = Agents.selected(sp);
        if (player.isShiftKeyDown()) {
            for (AgentEntity a : sel) a.orderFollow();
            sp.sendSystemMessage(Component.literal("선택한 관리직이 당신을 따라옵니다.")
                    .withStyle(s -> s.withColor(0x55FF55)));
            return InteractionResult.SUCCESS;
        }
        List<AgentEntity> mine = Agents.owned(sp);
        sp.sendSystemMessage(Component.literal("──── 관리직 " + mine.size() + "/" + Agents.MAX_PER_PLAYER
                + " (선택 " + sel.size() + "명) ────").withStyle(s -> s.withColor(0xAA0000)));
        if (mine.isEmpty()) {
            sp.sendSystemMessage(Component.literal("고용한 관리직이 없습니다.").withStyle(s -> s.withColor(0xAAAAAA)));
        } else {
            for (AgentEntity a : mine) sp.sendSystemMessage(Agents.describe(a));
        }
        sp.sendSystemMessage(Component.literal("고용: /lobcorp agent hire").withStyle(s -> s.withColor(0xAAAAAA)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        for (AgentEntity a : Agents.selected(sp)) a.orderHold(ctx.getClickedPos().above());
        sp.sendSystemMessage(Component.literal("선택한 관리직이 그 자리를 지킵니다.")
                .withStyle(s -> s.withColor(0x55FF55)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // 관리직 선택/해제
        if (target instanceof AgentEntity a) {
            if (sp.getUUID().equals(a.owner())) {
                Agents.toggle(sp, a);
                sp.sendSystemMessage(Agents.describe(a));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // 제압 지시
        boolean hostile = target instanceof OrdealEntity
                || (target instanceof AbnormalityEntity ab && ab.isBreached())
                || (Grades.ofEntity(target) > 0 && !(target instanceof AbnormalityEntity));
        if (hostile) {
            for (AgentEntity a : Agents.selected(sp)) a.orderFight(target);
            sp.sendSystemMessage(Component.literal("선택한 관리직이 제압에 나섭니다.")
                    .withStyle(s -> s.withColor(0xFF5555)));
            return InteractionResult.SUCCESS;
        }

        // 작업 지시
        if (target instanceof AbnormalityEntity ab) {
            Agents.showWorkMenu(sp, ab);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
