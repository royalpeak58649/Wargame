package kr.lobcorp.entity;

import kr.lobcorp.AbnoProfile;
import kr.lobcorp.work.WorkManager;
import kr.lobcorp.work.WorkSession;
import kr.lobcorp.work.WorkType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 모든 환상체의 공통 기반: 작업 대상, PE 저장, 격리 위치 고정 */
public abstract class AbnormalityEntity extends PathfinderMob {
    protected int pe = 0;
    protected int qliphoth;
    protected BlockPos home = null;
    protected float homeYaw = 0;

    protected AbnormalityEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.qliphoth = profile().qliphothMax();
        this.setPersistenceRequired();
    }

    public abstract AbnoProfile profile();

    public boolean isBreached() {
        return false;
    }

    /** 적대하지 않는 탈출 상태 (고용된 용병 등). 기본은 false. */
    public boolean isFriendly() {
        return false;
    }

    public int getPe() {
        return pe;
    }

    public void addPe(int amount) {
        pe = Math.max(0, pe + amount);
    }

    public int getQliphoth() {
        return qliphoth;
    }

    public void resetQliphoth() {
        qliphoth = profile().qliphothMax();
    }

    public void setHome(BlockPos pos, float yaw) {
        this.home = pos;
        this.homeYaw = yaw;
    }

    /** 격리 위치의 Y 보정. 천장에 매달리는 환상체는 양수를 돌려준다. */
    public double homeYOffset() {
        return 0;
    }

    /** 작업 시작 직전. false면 작업이 시작되지 않는다. */
    public boolean beforeWorkStart(ServerPlayer worker, WorkType type) {
        return true;
    }

    /** 도구형 환상체를 사용했을 때 (작업 대신) */
    public void useTool(ServerPlayer p) {}

    public void onWorkStart(ServerPlayer worker, WorkType type) {}

    public void onWorkFinished(ServerPlayer worker, WorkManager.Result result, WorkSession session) {}

    /** 작업이 중단되었을 때 (패닉, 수면, 거리 이탈 등). 완료 판정은 일어나지 않는다. */
    public void onWorkAborted(ServerPlayer worker, WorkSession session, String reason) {}

    /** 다른 환상체의 작업이 끝났을 때 (자신이 대상이 아닌 경우) */
    public void onOtherWorkFinished(ServerPlayer worker, AbnormalityEntity other, WorkManager.Result result) {}

    /** 작업 성공률 보정 (퍼센트 포인트). 오늘의 수줍은 표정 등 */
    public int workChanceBonus(ServerPlayer worker, WorkType type) {
        return 0;
    }

    /** 작업 실패 피해 배율 */
    public float workDamageMultiplier(ServerPlayer worker) {
        return 1f;
    }

    /** PE-BOX 하나가 판정된 직후 */
    public void onBoxResult(ServerPlayer worker, boolean success) {}

    /** 다른 환상체가 탈출했을 때 (자신은 제외) */
    public void onAnyBreach(BreachingAbnormality who) {}

    /** 시설의 직원이 죽었을 때 */
    public void onAnyEmployeeDeath(ServerPlayer p) {}

    /** 시설의 직원이 패닉에 빠졌을 때 */
    public void onAnyPanic(ServerPlayer p) {}

    /** 시설의 사무직이 죽었을 때 (시체를 쓰는 환상체 등) */
    public void onAnyClerkDeath(kr.lobcorp.entity.ClerkEntity c) {}

    /** 시설의 사무직이 공황에 빠졌을 때 */
    public void onAnyClerkPanic(kr.lobcorp.entity.ClerkEntity c) {}

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isBreached()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) WorkManager.openScreen(sp, this);
        return InteractionResult.SUCCESS;
    }

    /** 격리 중인 환상체는 피해를 받지 않는다 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isBreached() && !source.isCreativePlayer()) return false;
        return super.hurtServer(level, source, amount);
    }

    private String panelKey = null;

    @Override
    public void tick() {
        super.tick();
        // 복도에서 보이는 클리포트 카운터 글자판
        if (!level().isClientSide() && home != null && tickCount % 20 == 7) {
            String k = kr.lobcorp.work.QliphothPanel.key(this);
            boolean changed = !k.equals(panelKey);
            panelKey = k;
            kr.lobcorp.work.QliphothPanel.update((ServerLevel) level(), this, home, changed);
        }
        if (!level().isClientSide() && home != null && !isBreached() && !isFriendly() && tickCount % 10 == 0) {
            double hx = home.getX() + 0.5, hz = home.getZ() + 0.5, hy = home.getY() + homeYOffset();
            if (distanceToSqr(hx, hy, hz) > 0.3) {
                snapTo(hx, hy, hz, homeYaw, 0);
                getNavigation().stop();
            }
            setYRot(homeYaw);
            setYBodyRot(homeYaw);
            setYHeadRot(homeYaw);
        }
    }

    @Override
    public boolean isPushable() {
        return isBreached();
    }

    @Override
    public void push(Entity entity) {
        if (isBreached()) super.push(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("lc_pe", pe);
        out.putInt("lc_qliphoth", qliphoth);
        if (home != null) {
            out.putBoolean("lc_has_home", true);
            out.putInt("lc_hx", home.getX());
            out.putInt("lc_hy", home.getY());
            out.putInt("lc_hz", home.getZ());
            out.putInt("lc_hyaw", (int) homeYaw);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        pe = in.getIntOr("lc_pe", 0);
        qliphoth = in.getIntOr("lc_qliphoth", profile().qliphothMax());
        if (in.getBooleanOr("lc_has_home", false)) {
            home = new BlockPos(in.getIntOr("lc_hx", 0), in.getIntOr("lc_hy", 0), in.getIntOr("lc_hz", 0));
            homeYaw = in.getIntOr("lc_hyaw", 0);
        }
    }
}
