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
            () -> kr.lobcorp.ModEntities.GREEN_DAWN),
    CRIMSON_DAWN("crimson_dawn", "진홍색 여명 — 광대들의 행진", OrdealTier.DAWN, 0xDC143C, 3, 3,
            () -> kr.lobcorp.ModEntities.CRIMSON_DAWN),
    VIOLET_DAWN("violet_dawn", "보라색 여명 — 이해의 열매", OrdealTier.DAWN, 0x9B59D0, 3, 1,
            () -> kr.lobcorp.ModEntities.VIOLET_DAWN),
    GREEN_NOON("green_noon", "녹색 정오 — 이해의 과정", OrdealTier.NOON, 0x55CC55, 3, 1,
            () -> kr.lobcorp.ModEntities.GREEN_NOON),
    CRIMSON_NOON("crimson_noon", "진홍색 정오 — 흥겨운 행렬", OrdealTier.NOON, 0xDC143C, 2, 1,
            () -> kr.lobcorp.ModEntities.CRIMSON_NOON),
    VIOLET_NOON("violet_noon", "보라색 정오 — 사랑을 주소서", OrdealTier.NOON, 0x9B59D0, 3, 1,
            () -> kr.lobcorp.ModEntities.VIOLET_NOON),
    INDIGO_NOON("indigo_noon", "쪽빛 정오 — 청소부", OrdealTier.NOON, 0x3F51B5, 4, 3,
            () -> kr.lobcorp.ModEntities.INDIGO_NOON),
    AMBER_DUSK("amber_dusk", "호박색 황혼 — 먹이사슬", OrdealTier.DUSK, 0xFFB000, 2, 1,
            () -> kr.lobcorp.ModEntities.AMBER_DUSK),
    GREEN_DUSK("green_dusk", "녹색 황혼 — 우리가 도달해야 할 곳", OrdealTier.DUSK, 0x55CC55, 3, 1,
            () -> kr.lobcorp.ModEntities.GREEN_DUSK),
    CRIMSON_DUSK("crimson_dusk", "진홍색 황혼 — 막이 내리면", OrdealTier.DUSK, 0xDC143C, 2, 1,
            () -> kr.lobcorp.ModEntities.CRIMSON_DUSK);

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
