package kr.lobcorp.ordeal;

/**
 * 시련의 등급. 날짜가 지날수록 더 높은 등급이 열리고, 할당량 대비 에너지 비율이 기준을 넘으면 발생한다.
 */
public enum OrdealTier {
    DAWN(2, 0.25f, 0.10f, "여명"),
    NOON(4, 0.50f, 0.15f, "정오"),
    DUSK(7, 0.75f, 0.20f, "황혼");

    /** 이 등급이 열리는 최소 일차 */
    public final int minDay;
    /** 하루 할당량 대비 발생 기준 비율 */
    public final float threshold;
    /** 극복 보상 (할당량 대비 에너지 비율) */
    public final float reward;
    /** 한국어 이름 */
    public final String ko;

    OrdealTier(int minDay, float threshold, float reward, String ko) {
        this.minDay = minDay;
        this.threshold = threshold;
        this.reward = reward;
        this.ko = ko;
    }
}
