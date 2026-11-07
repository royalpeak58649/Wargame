package kr.lobcorp;

/** 원작의 4가지 피해 유형. RED=HP, WHITE=SP, BLACK=HP+SP, PALE=최대 HP 비율 */
public enum DamageColor {
    RED("RED", 0xFF5555),
    WHITE("WHITE", 0xFFFFFF),
    BLACK("BLACK", 0xAA55FF),
    PALE("PALE", 0x55FFFF);

    public final String label;
    public final int rgb;

    DamageColor(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
    }
}
