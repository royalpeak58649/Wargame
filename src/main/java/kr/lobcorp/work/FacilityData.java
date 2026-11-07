package kr.lobcorp.work;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 시설 전체 진행 상태 (오버월드에 저장) */
public class FacilityData {
    public int day = 1;
    public int energy = 0;
    public int quota = 30;
    public boolean quotaMet = false;

    public static final Codec<FacilityData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("day").forGetter(d -> d.day),
            Codec.INT.fieldOf("energy").forGetter(d -> d.energy),
            Codec.INT.fieldOf("quota").forGetter(d -> d.quota),
            Codec.BOOL.fieldOf("quota_met").forGetter(d -> d.quotaMet)
    ).apply(i, (day, energy, quota, met) -> {
        FacilityData d = new FacilityData();
        d.day = day;
        d.energy = energy;
        d.quota = quota;
        d.quotaMet = met;
        return d;
    }));
}
