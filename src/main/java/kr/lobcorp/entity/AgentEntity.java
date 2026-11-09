package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.Grades;
import kr.lobcorp.item.EgoWeaponItem;
import kr.lobcorp.item.SuitResist;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 관리직 — 플레이어가 명령해 환상체 작업·전투·대기를 수행하는 직원.
 * 사무직(ClerkEntity)의 정신력/공황 체계를 그대로 물려받되, 스탯·작업 시뮬레이션은 플레이어와 같은 규칙을 쓴다.
 */
public class AgentEntity extends ClerkEntity {
    /** 명령 모드 */
    public enum Mode { FOLLOW, HOLD, WORK, FIGHT }

    /** 관리직이 작업 중인 환상체 UUID (다른 직원의 작업을 막는다) */
    private static final Set<UUID> BUSY = new HashSet<>();

    private UUID owner = null;
    private String agentName = "?";
    private int fortitude = EmployeeData.BASE_STAT;
    private int prudence = EmployeeData.BASE_STAT;
    private int temperance = EmployeeData.BASE_STAT;
    private int justice = EmployeeData.BASE_STAT;
    private Mode mode = Mode.FOLLOW;
    private BlockPos holdPos = null;
    private UUID workAbno = null;
    private WorkType workType = WorkType.INSTINCT;
    private boolean working = false;
    private int box = 0;
    private int good = 0;
    private int maxBox = 0;
    private int interval = 30;
    private int chance = 50;
    private int tickInBox = 0;
    private int goTicks = 0;
    private UUID fightTarget = null;

    public AgentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_DAMAGE, 2);
    }

    /** 이 환상체를 관리직이 작업 중인가 */
    public static boolean isWorkingOn(AbnormalityEntity a) {
        return a != null && BUSY.contains(a.getUUID());
    }

    // ───────────── 초기화 / 스탯 ─────────────

    /** 배치 직후 호출: 주인, 이름, 외형, 스탯을 정한다. */
    public void initAgent(ServerPlayer owner, String name, int skin) {
        this.owner = owner.getUUID();
        this.agentName = name;
        this.fortitude = EmployeeData.BASE_STAT + getRandom().nextInt(11);
        this.prudence = EmployeeData.BASE_STAT + getRandom().nextInt(11);
        this.temperance = EmployeeData.BASE_STAT + getRandom().nextInt(11);
        this.justice = EmployeeData.BASE_STAT + getRandom().nextInt(11);
        setup(null, skin, name);
        setCustomName(Component.literal("관리직 " + name));
        setCustomNameVisible(true);
        applyStats();
        setHealth(getMaxHealth());
        sp = maxSp();
    }

    /** 스탯을 실제 능력치에 반영한다. */
    public void applyStats() {
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.max(10, fortitude));
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    @Override
    public int maxSp() {
        return Math.max(1, prudence);
    }

    public UUID owner() {
        return owner;
    }

    public String agentName() {
        return agentName;
    }

    public int fortitude() {
        return fortitude;
    }

    public int prudence() {
        return prudence;
    }

    public int temperance() {
        return temperance;
    }

    public int justice() {
        return justice;
    }

    public Mode mode() {
        return mode;
    }

    public boolean working() {
        return working;
    }

    public int box() {
        return box;
    }

    public int maxBox() {
        return maxBox;
    }

    // ───────────── AI ─────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float amount = 2f + justice / 10f;
        if (getMainHandItem().getItem() instanceof EgoWeaponItem) amount += 6f;
        return target.hurtServer(level, damageSources().mobAttack(this), amount);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof Player)
                && !(attacker instanceof ClerkEntity)
                && mode != Mode.WORK) {
            setTarget(attacker);
        }
        return super.hurtServer(level, source, amount);
    }

    // ───────────── 명령 ─────────────

    /** 작업 명령. 실패하면 오류 문구를, 성공하면 null을 돌려준다. */
    public String orderWork(AbnormalityEntity a, WorkType t) {
        if (isPanicking()) return "공황 상태입니다";
        if (mode == Mode.WORK) return "이미 작업 중입니다";
        if (a.isBreached()) return "탈출한 환상체입니다";
        if (a.profile().isTool()) return "도구형 환상체는 직접 사용해야 합니다";
        if (WorkManager.isAbnoBusy(a)) return "다른 직원이 작업 중입니다";
        mode = Mode.WORK;
        workAbno = a.getUUID();
        workType = t;
        working = false;
        goTicks = 0;
        holdPos = blockPosition();
        setTarget(null);
        BUSY.add(a.getUUID());
        return null;
    }

    /** 전투 명령. 진행 중인 작업은 중단한다. */
    public void orderFight(LivingEntity e) {
        abortWork("전투 명령");
        mode = Mode.FIGHT;
        fightTarget = e != null ? e.getUUID() : null;
    }

    /** 추종 명령 */
    public void orderFollow() {
        abortWork("추종 명령");
        mode = Mode.FOLLOW;
        setTarget(null);
    }

    /** 대기 명령 */
    public void orderHold(BlockPos p) {
        abortWork("대기 명령");
        mode = Mode.HOLD;
        holdPos = p != null ? p : blockPosition();
        setTarget(null);
    }

    // ───────────── 틱 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (isPanicking()) {
            abortWork("공황");
            return;
        }
        if (tickCount % 100 == 0 && level().getServer() != null) {
            for (BlockPos c : Facility.centers(level().getServer())) {
                if (distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) <= 64.0) {
                    heal(10f);
                    sp = Math.min(maxSp(), sp + 10);
                    break;
                }
            }
        }
        switch (mode) {
            case FOLLOW -> tickFollow(sl);
            case HOLD -> tickHold();
            case FIGHT -> tickFight(sl);
            case WORK -> tickWork(sl);
        }
    }

    private void tickFollow(ServerLevel sl) {
        if (owner == null) return;
        ServerPlayer p = sl.getServer() == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
        if (p == null || p.level() != sl) return;
        double d = distanceTo(p);
        if (d > 32) {
            teleportTo(p.getX() + 1, p.getY(), p.getZ() + 1);
            return;
        }
        if (d > 4 && tickCount % 20 == 0) getNavigation().moveTo(p, 1.1);
    }

    private void tickHold() {
        if (holdPos == null) return;
        double d = distanceToSqr(holdPos.getX() + 0.5, holdPos.getY(), holdPos.getZ() + 0.5);
        if (d > 4.0 && tickCount % 40 == 0) {
            getNavigation().moveTo(holdPos.getX() + 0.5, holdPos.getY(), holdPos.getZ() + 0.5, 1.1);
        }
    }

    private void tickFight(ServerLevel sl) {
        Entity e = fightTarget == null ? null : sl.getEntity(fightTarget);
        if (!(e instanceof LivingEntity le) || !le.isAlive()
                || (le instanceof BreachingAbnormality b && !b.isBreached())) {
            mode = Mode.FOLLOW;
            fightTarget = null;
            setTarget(null);
            return;
        }
        setTarget(le);
    }

    private void tickWork(ServerLevel sl) {
        Entity e = workAbno == null ? null : sl.getEntity(workAbno);
        if (!(e instanceof AbnormalityEntity a) || !a.isAlive() || a.isBreached()) {
            abortWork("작업 중단");
            return;
        }
        if (!working) {
            goTicks++;
            if (distanceTo(a) <= 3.5) {
                beginWork(a);
                return;
            }
            if (goTicks > 300) {
                if (a.home != null) teleportTo(a.home.getX() + 0.5, a.home.getY(), a.home.getZ() + 1.5);
                else teleportTo(a.getX() + 1, a.getY(), a.getZ());
                beginWork(a);
                return;
            }
            if (tickCount % 20 == 0) getNavigation().moveTo(a, 1.1);
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(a);
        if (++tickInBox >= interval) {
            tickInBox = 0;
            processBox(a);
            if (box >= maxBox) finishWork(a);
        }
    }

    // ───────────── 작업 시뮬레이션 ─────────────

    private void beginWork(AbnormalityEntity a) {
        working = true;
        AbnoProfile prof = a.profile();
        chance = Math.max(5, Math.min(95, prof.chances()[workType.ordinal()] + temperance / 5));
        interval = Math.max(10, 30 - temperance / 4);
        maxBox = prof.maxBox();
        box = 0;
        good = 0;
        tickInBox = 0;
        level().playSound(null, a.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1f, 0.6f);
        tell("▶ 관리직 " + agentName + ": " + workType.ko + " 작업 시작 (성공률 " + chance + "%)");
        kr.lobcorp.work.Meltdown.onWorkStarted(level().getServer(), a);
    }

    private void processBox(AbnormalityEntity a) {
        if (!(level() instanceof ServerLevel sl)) return;
        box++;
        AbnoProfile prof = a.profile();
        if (getRandom().nextInt(100) < chance) {
            good++;
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, a.getX(), a.getY() + 1.5, a.getZ(), 6, 0.4, 0.5, 0.4, 0);
            return;
        }
        int n = prof.dmgMin() + getRandom().nextInt(prof.dmgMax() - prof.dmgMin() + 1);
        float res = SuitResist.itemMult(getItemBySlot(EquipmentSlot.CHEST), prof.workDamage(), Grades.of(prof.risk()));
        switch (prof.workDamage()) {
            case RED -> hurtServer(sl, sl.damageSources().magic(), n * res);
            case WHITE -> damageSp(Math.round(n * res));
            case BLACK -> {
                hurtServer(sl, sl.damageSources().magic(), n * res);
                damageSp(Math.round(n * res));
            }
            case PALE -> hurtServer(sl, sl.damageSources().magic(), getMaxHealth() * 0.05f * n * res);
        }
        if (!isAlive()) {
            BUSY.remove(a.getUUID());
            working = false;
        }
    }

    private void finishWork(AbnormalityEntity a) {
        AbnoProfile prof = a.profile();
        WorkManager.Result r = good >= prof.goodMin() ? WorkManager.Result.GOOD
                : good >= prof.normalMin() ? WorkManager.Result.NORMAL : WorkManager.Result.BAD;
        if (level().getServer() != null) Facility.addEnergy(level().getServer(), good);

        // 플레이어와 같은 성장 규칙
        int statVal = switch (workType) {
            case INSTINCT -> fortitude;
            case INSIGHT -> prudence;
            case ATTACHMENT -> temperance;
            case REPRESSION -> justice;
        };
        int over = EmployeeData.level(statVal) - Grades.of(prof.risk());
        float mult = over <= 0 ? 1.0f : over == 1 ? 0.5f : over == 2 ? 0.25f : 0f;
        int gain = mult > 0 ? (int) Math.floor((good / 4f) * mult + getRandom().nextFloat()) : 0;
        if (gain > 0) {
            switch (workType) {
                case INSTINCT -> fortitude = Math.min(EmployeeData.MAX_STAT, fortitude + gain);
                case INSIGHT -> prudence = Math.min(EmployeeData.MAX_STAT, prudence + gain);
                case ATTACHMENT -> temperance = Math.min(EmployeeData.MAX_STAT, temperance + gain);
                case REPRESSION -> justice = Math.min(EmployeeData.MAX_STAT, justice + gain);
            }
            applyStats();
        }

        a.onAgentWorkFinished(this, r, workType);

        String res = switch (r) {
            case GOOD -> "좋음";
            case NORMAL -> "보통";
            case BAD -> "나쁨";
        };
        tell("■ 관리직 " + agentName + " — 작업 결과: " + res + "  PE-BOX +" + good
                + (gain > 0 ? "  " + workType.stat + " +" + gain : ""));

        BUSY.remove(a.getUUID());
        working = false;
        mode = Mode.HOLD;
    }

    /** 작업 중단 (공황·전투 명령·대상 소실 등) */
    private void abortWork(String why) {
        if (mode != Mode.WORK) return;
        if (workAbno != null) BUSY.remove(workAbno);
        working = false;
        mode = Mode.HOLD;
        tell("■ 관리직 " + agentName + ": " + why);
    }

    private void tell(String msg) {
        if (owner == null || !(level() instanceof ServerLevel sl) || sl.getServer() == null) return;
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(owner);
        if (p != null) p.sendSystemMessage(Component.literal(msg));
    }

    // ───────────── 상호작용 ─────────────

    /** 주인만 관리직을 직접 다룰 수 있다: 태블릿 선택, 무기/슈트 교환, 회수 */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean tablet = held.getItem() instanceof kr.lobcorp.item.AgentTabletItem;
        boolean weapon = held.getItem() instanceof EgoWeaponItem;
        boolean suit = isSuit(held);
        if (level().isClientSide()) {
            return tablet || weapon || suit || (held.isEmpty() && player.isShiftKeyDown())
                    ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
        }
        if (owner == null || !owner.equals(player.getUUID())) return super.mobInteract(player, hand);
        if (tablet) return InteractionResult.PASS;

        if (weapon) {
            swapSlot(player, hand, EquipmentSlot.MAINHAND);
            player.sendSystemMessage(Component.literal("관리직 " + agentName + "에게 무기를 건넸습니다."));
            return InteractionResult.SUCCESS;
        }
        if (suit) {
            swapSlot(player, hand, EquipmentSlot.CHEST);
            player.sendSystemMessage(Component.literal("관리직 " + agentName + "에게 슈트를 건넸습니다."));
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && player.isShiftKeyDown()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.CHEST}) {
                ItemStack stack = getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                player.getInventory().placeItemBackInInventory(stack.copy(), net.minecraft.util.Prediction.SERVER_ONLY);
                setItemSlot(slot, ItemStack.EMPTY);
            }
            player.sendSystemMessage(Component.literal("관리직 " + agentName + "의 장비를 회수했습니다."));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** 손에 든 물건과 관리직의 장비 칸을 맞바꾼다. */
    private void swapSlot(Player player, InteractionHand hand, EquipmentSlot slot) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack old = getItemBySlot(slot);
        setItemSlot(slot, held.copy());
        if (old.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        } else {
            player.setItemInHand(hand, old);
            player.getInventory().placeItemBackInInventory(held.copy(), net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    /** E.G.O 슈트(레지스트리 경로가 _suit로 끝나는 아이템)인가 */
    private static boolean isSuit(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.getPath().endsWith("_suit");
    }

    // ───────────── 사망 / 저장 ─────────────

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.CHEST}) {
                ItemStack stack = getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    spawnAtLocation(sl, stack);
                    setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        if (working && workAbno != null) BUSY.remove(workAbno);
        working = false;
        tell("[관제] 관리직 " + agentName + "이(가) 사망했습니다.");
        super.die(source);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (owner != null) out.putString("lc_agent_owner", owner.toString());
        out.putString("lc_agent_name", agentName);
        out.putInt("lc_agent_fort", fortitude);
        out.putInt("lc_agent_prud", prudence);
        out.putInt("lc_agent_temp", temperance);
        out.putInt("lc_agent_just", justice);
        out.putString("lc_agent_mode", mode.name());
        if (holdPos != null) {
            out.putBoolean("lc_agent_has_hold", true);
            out.putInt("lc_agent_hx", holdPos.getX());
            out.putInt("lc_agent_hy", holdPos.getY());
            out.putInt("lc_agent_hz", holdPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        String o = in.getStringOr("lc_agent_owner", "");
        owner = o.isEmpty() ? null : UUID.fromString(o);
        agentName = in.getStringOr("lc_agent_name", "?");
        fortitude = in.getIntOr("lc_agent_fort", EmployeeData.BASE_STAT);
        prudence = in.getIntOr("lc_agent_prud", EmployeeData.BASE_STAT);
        temperance = in.getIntOr("lc_agent_temp", EmployeeData.BASE_STAT);
        justice = in.getIntOr("lc_agent_just", EmployeeData.BASE_STAT);
        String m = in.getStringOr("lc_agent_mode", Mode.FOLLOW.name());
        mode = switch (m) {
            case "HOLD" -> Mode.HOLD;
            case "WORK", "FIGHT" -> Mode.HOLD; // 진행 중이던 작업/전투는 복원하지 않는다
            default -> Mode.FOLLOW;
        };
        if (in.getBooleanOr("lc_agent_has_hold", false)) {
            holdPos = new BlockPos(in.getIntOr("lc_agent_hx", 0), in.getIntOr("lc_agent_hy", 0), in.getIntOr("lc_agent_hz", 0));
        }
        applyStats();
    }
}
