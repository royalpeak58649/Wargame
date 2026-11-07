package kr.lobcorp.item;

import kr.lobcorp.DamageColor;
import net.minecraft.world.item.Item;

/** E.G.O 무기. 환상체에게 주는 피해 배율은 이 색상과 환상체 내성으로 결정된다. */
public class EgoWeaponItem extends Item {
    public final DamageColor color;

    public EgoWeaponItem(Properties properties, DamageColor color) {
        super(properties);
        this.color = color;
    }
}
