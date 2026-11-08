package kr.lobcorp.ordeal;

import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 시련의 종류. 등급·색·발생 규모·소환할 엔티티를 담는다.
 */
public enum OrdealType {
    AMBER_DAWN("amber_dawn", "호박색 여명 — 완벽한 음식", OrdealTier.DAWN, 0xFFB000, 3, 4,
            () -> kr.lobcorp.ModEntities.AMBER_DAWN),
    GREEN_DAWN("green_dawn", "녹색 여명 — 의심", OrdealTier.DAWN, 0x55CC55, 4, 1,
            () -> kr.lobcorp.ModEntities.GREEN_DAWN);

    public final String id;
    public final String ko;
    public final OrdealTier tier;
    public final int color;
    /** 발생하는 부서 수 */
    public final int sites;
    /** 부서당 개체 수 */
    public final int group;
    public final Supplier<EntityType<? extends OrdealEntity>> entity;

    OrdealType(String id, String ko, OrdealTier tier, int color, int sites, int group,
               Supplier<EntityType<? extends OrdealEntity>> entity) {
        this.id = id;
        this.ko = ko;
        this.tier = tier;
        this.color = color;
        this.sites = sites;
        this.group = group;
        this.entity = entity;
    }

    /** id로 찾는다. 없으면 null */
    public static OrdealType byId(String id) {
        for (OrdealType t : values()) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    /** 해당 등급의 시련 목록 */
    public static List<OrdealType> ofTier(OrdealTier t) {
        List<OrdealType> out = new ArrayList<>();
        for (OrdealType type : values()) {
            if (type.tier == t) out.add(type);
        }
        return out;
    }
}
