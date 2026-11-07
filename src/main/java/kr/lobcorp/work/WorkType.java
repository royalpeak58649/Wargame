package kr.lobcorp.work;

public enum WorkType {
    INSTINCT("본능", "용기", 0xFF5555),
    INSIGHT("통찰", "신중", 0xFFFFFF),
    ATTACHMENT("애착", "자제", 0xB46CFF),
    REPRESSION("억압", "정의", 0x55FFFF);

    public final String ko;
    public final String stat;
    public final int rgb;

    WorkType(String ko, String stat, int rgb) {
        this.ko = ko;
        this.stat = stat;
        this.rgb = rgb;
    }

    public static WorkType byIndex(int i) {
        return values()[Math.max(0, Math.min(3, i))];
    }
}
