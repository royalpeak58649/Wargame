package kr.lobcorp.client;

/** 서버에서 동기화된 내 직원 상태 (HUD용) */
public final class ClientState {
    private ClientState() {}

    public static int[] v = new int[14];
    public static boolean received = false;

    public static int sp() { return v[0]; }
    public static int spMax() { return Math.max(1, v[1]); }
    public static int panic() { return v[6]; }
    public static boolean working() { return v[7] == 1; }
    public static int box() { return v[8]; }
    public static int maxBox() { return v[9]; }
    public static int good() { return v[10]; }
    public static int workType() { return v[11]; }
    public static int fairy() { return v.length > 12 ? v[12] : 0; }
    public static int sleep() { return v.length > 13 ? v[13] : 0; }
}
