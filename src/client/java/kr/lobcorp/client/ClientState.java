package kr.lobcorp.client;

/** 서버에서 동기화된 내 직원 상태 (HUD용) */
public final class ClientState {
    private ClientState() {}

    public static int[] v = new int[22];
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
    public static int possess() { return v.length > 14 ? v[14] : 0; }
    public static boolean heart() { return v.length > 15 && v[15] == 1; }
    public static boolean sheltered() { return v.length > 16 && v[16] == 1; }
    public static int portrait() { return v.length > 17 ? v[17] : 0; }
    public static int notes() { return v.length > 18 ? v[18] : 0; }
    public static boolean pebble() { return v.length > 19 && v[19] == 1; }
    public static int frost() { return v.length > 20 ? v[20] : 0; }
    public static boolean blessed() { return v.length > 21 && v[21] == 1; }
}
