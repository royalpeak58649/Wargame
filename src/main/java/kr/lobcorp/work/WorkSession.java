package kr.lobcorp.work;

import kr.lobcorp.entity.AbnormalityEntity;

public class WorkSession {
    public final AbnormalityEntity abno;
    public final WorkType type;
    public final int maxBox;
    public final int chance;
    public final int interval;
    public int box = 0;
    public int good = 0;
    public int tick = 0;

    public WorkSession(AbnormalityEntity abno, WorkType type, int maxBox, int chance, int interval) {
        this.abno = abno;
        this.type = type;
        this.maxBox = maxBox;
        this.chance = chance;
        this.interval = interval;
    }
}
