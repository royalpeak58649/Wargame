package kr.lobcorp.work;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 시설 전체 진행 상태 (오버월드에 저장) */
public class FacilityData {
    public int day = 1;
    public int energy = 0;
    public int quota = 30;
    public boolean quotaMet = false;
    /** /lobcorp facility 로 지은 격리실 자리(순서대로 채운다). abno == "" 이면 비어 있음. */
    public List<Slot> slots = new ArrayList<>();
    /** 지금 고를 수 있는 후보 환상체 id */
    public List<String> offers = new ArrayList<>();

    public static final class Slot {
        public final BlockPos origin;
        public final String dept;
        public String abno;
        public Slot(BlockPos origin, String dept, String abno) { this.origin = origin; this.dept = dept; this.abno = abno; }
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("origin").forGetter(s -> s.origin),
                Codec.STRING.fieldOf("dept").forGetter(s -> s.dept),
                Codec.STRING.fieldOf("abno").forGetter(s -> s.abno)
        ).apply(i, Slot::new));
    }

    public static final Codec<FacilityData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("day").forGetter(d -> d.day),
            Codec.INT.fieldOf("energy").forGetter(d -> d.energy),
            Codec.INT.fieldOf("quota").forGetter(d -> d.quota),
            Codec.BOOL.fieldOf("quota_met").forGetter(d -> d.quotaMet),
            Slot.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(d -> d.slots),
            Codec.STRING.listOf().optionalFieldOf("offers", List.of()).forGetter(d -> d.offers)
    ).apply(i, (day, energy, quota, met, slots, offers) -> {
        FacilityData d = new FacilityData();
        d.day = day;
        d.energy = energy;
        d.quota = quota;
        d.quotaMet = met;
        d.slots = new ArrayList<>(slots);
        d.offers = new ArrayList<>(offers);
        return d;
    }));
}
