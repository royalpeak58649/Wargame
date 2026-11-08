package kr.lobcorp.item;

/** E.G.O 무기 분류. GUN 이후는 모두 원거리 무기다. */
public enum WeaponKind {
    BLADE, SPEAR, HEAVY, FAST, GUN, PIERCE, BOW, CANNON, THROWN;

    public boolean ranged() {
        return ordinal() >= GUN.ordinal();
    }
}
