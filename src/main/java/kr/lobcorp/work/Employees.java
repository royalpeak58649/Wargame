package kr.lobcorp.work;

import kr.lobcorp.ModAttachments;
import kr.lobcorp.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** 직원 스탯 / SP / 패닉 처리 */
public final class Employees {
    private Employees() {}

    public static final int PANIC_TICKS = 400;
    public static final int REGEN_INTERVAL = 300; // 15초마다 SP 1

    public static EmployeeData get(ServerPlayer p) {
        EmployeeData d = p.getAttachedOrCreate(ModAttachments.EMPLOYEE);
        if (d.sp < 0) d.sp = d.maxSp();
        return d;
    }

    /** 스탯을 실제 속성(최대 HP, 이동 속도)에 반영 */
    public static void apply(ServerPlayer p) {
        EmployeeData d = get(p);
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getBaseValue() != d.maxHp()) {
            hp.setBaseValue(d.maxHp());
            if (p.getHealth() > d.maxHp()) p.setHealth(d.maxHp());
        }
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            double mult = Math.min(1.4, (1.0 + (d.justice - EmployeeData.BASE_STAT) * 0.004)
                    * (1.0 + d.giftSpeedBonus() / 100.0));
            speed.setBaseValue(0.1 * mult);
        }
        if (d.sp > d.maxSp()) d.sp = d.maxSp();
    }

    public static void sync(ServerPlayer p) {
        EmployeeData d = get(p);
        WorkSession s = WorkManager.session(p);
        int[] v = new int[Payloads.EmployeeSync.SIZE];
        v[0] = d.sp;
        v[1] = d.maxSp();
        v[2] = d.fortitude;
        v[3] = d.prudence;
        v[4] = d.temperance;
        v[5] = d.justice;
        v[6] = d.panicTicks;
        v[12] = d.fairyTicks;
        v[13] = d.sleepTicks;
        if (s != null) {
            v[7] = 1;
            v[8] = s.box;
            v[9] = s.maxBox;
            v[10] = s.good;
            v[11] = s.type.ordinal();
        }
        if (ServerPlayNetworking.canSend(p, Payloads.EmployeeSync.TYPE)) {
            ServerPlayNetworking.send(p, new Payloads.EmployeeSync(v));
        }
    }

    public static void restoreSp(ServerPlayer p) {
        EmployeeData d = get(p);
        d.sp = d.maxSp();
    }

    // ───────────── 수면 (공허한 꿈) ─────────────

    /** 잠에 빠뜨린다. 진행 중인 작업은 중단된다. */
    public static void sleep(ServerPlayer p, int ticks) {
        EmployeeData d = get(p);
        if (d.isPanicking()) return;
        d.sleepTicks = Math.max(d.sleepTicks, ticks);
        WorkManager.abort(p, "수면");
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("공허한 꿈이 스며든다")
                .withStyle(s -> s.withColor(0xC9C3E6))));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("잠듦").withStyle(s -> s.withColor(0xECEAF4).withBold(true))));
        p.sendSystemMessage(Component.literal("  눈꺼풀이 무거워진다. 빛무리 하나가 목덜미에 스쳤다.")
                .withStyle(s -> s.withColor(0xC9C3E6).withItalic(true)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
        sync(p);
    }

    /** 잠에서 깬다. panic이면 곧바로 패닉 상태가 된다. */
    public static void wake(ServerPlayer p, boolean panic) {
        EmployeeData d = get(p);
        if (d.sleepTicks <= 0 && !panic) return;
        d.sleepTicks = 0;
        p.removeEffect(MobEffects.SLOWNESS);
        p.removeEffect(MobEffects.BLINDNESS);
        if (panic) {
            panic(p);
        } else {
            p.sendSystemMessage(Component.literal("  잠에서 깼다.").withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
        }
        sync(p);
    }

    /** SP 피해. 0 이하가 되면 패닉 */
    public static void damageSp(ServerPlayer p, int amount) {
        EmployeeData d = get(p);
        d.sp -= amount;
        if (d.sp <= 0) panic(p);
    }

    public static void panic(ServerPlayer p) {
        EmployeeData d = get(p);
        d.sp = 0;
        WorkManager.abort(p, "패닉");
        d.panicTicks = PANIC_TICKS;
        p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, PANIC_TICKS, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, PANIC_TICKS, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PANIC_TICKS, 1, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PANIC_TICKS, 1, false, false));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("정신력이 바닥났습니다").withStyle(s -> s.withColor(0xAAAAAA))));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("패닉").withStyle(s -> s.withColor(0xFFFFFF).withBold(true))));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.7f, 1.3f);
        Facility.broadcast(p.level().getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 패닉 상태에 빠졌습니다!").withStyle(s -> s.withColor(0xFF5555))));
        kr.lobcorp.entity.PunishingBirdEntity.onAnyPanic(p.level());
    }

    /** 매 틱 호출 */
    public static void tick(MinecraftServer server) {
        boolean second = server.getTickCount() % 20 == 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            EmployeeData d = get(p);
            kr.lobcorp.entity.FairyFestivalEntity.tickBlessing(p, d);
            if (d.sleepTicks > 0) {
                d.sleepTicks--;
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 10, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                if (d.sleepTicks % 20 == 0) {
                    p.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 2.1, p.getZ(), 2, 0.25, 0.1, 0.25, 0);
                }
                if (d.sleepTicks == 0) {
                    p.removeEffect(MobEffects.SLOWNESS);
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.sendSystemMessage(Component.literal("  잠에서 깼다.").withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
                }
            }
            if (d.panicTicks > 0) {
                d.panicTicks--;
                if (d.panicTicks % 5 == 0) {
                    p.level().sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 1, p.getZ(), 3, 0.3, 0.5, 0.3, 0);
                }
                if (d.panicTicks == 0) {
                    d.sp = Math.max(1, d.maxSp() / 2);
                    p.sendSystemMessage(Component.literal("[관제] 정신을 차렸습니다. SP가 절반 회복되었습니다.").withStyle(s -> s.withColor(0xAAAAAA)));
                }
            } else if (WorkManager.session(p) == null && d.sp < d.maxSp()) {
                if (++d.regenTicks >= REGEN_INTERVAL) {
                    d.regenTicks = 0;
                    d.sp++;
                }
            }
            boolean working = WorkManager.session(p) != null;
            if (second || (working && server.getTickCount() % 4 == 0)) {
                apply(p);
                sync(p);
            }
        }
    }

    public static Component statsText(EmployeeData d) {
        return Component.literal("")
                .append(Component.literal("용기 " + d.fortitude + " (" + EmployeeData.grade(d.fortitude) + ")  ").withStyle(s -> s.withColor(0xFF5555)))
                .append(Component.literal("신중 " + d.prudence + " (" + EmployeeData.grade(d.prudence) + ")  ").withStyle(s -> s.withColor(0xFFFFFF)))
                .append(Component.literal("자제 " + d.temperance + " (" + EmployeeData.grade(d.temperance) + ")  ").withStyle(s -> s.withColor(0xB46CFF)))
                .append(Component.literal("정의 " + d.justice + " (" + EmployeeData.grade(d.justice) + ")").withStyle(s -> s.withColor(0x55FFFF)));
    }
}
