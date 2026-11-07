package kr.lobcorp.work;

import kr.lobcorp.ModAttachments;
import kr.lobcorp.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

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
        v[14] = d.possessTicks;
        v[15] = d.laetitiaHeart ? 1 : 0;
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

    // ───────────── 홀림 (강제 조종) ─────────────

    /**
     * 직원을 홀리게 한다. 홀린 직원은 조종권을 잃고 가장 가까운 동료를 쫓아가 때린다.
     * 동료에게 세 번 맞으면 풀린다.
     */
    public static void possess(ServerPlayer p, int ticks, String label, int healthBoostAmp) {
        if (p.isCreative() || p.isSpectator()) return;
        EmployeeData d = get(p);
        if (d.possessTicks > 0) return;
        WorkManager.abort(p, label);
        d.possessTicks = ticks;
        d.possessHits = 0;
        d.possessLabel = label;
        d.possessHeal = true;
        if (healthBoostAmp >= 0) {
            p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, ticks, healthBoostAmp, false, false));
        }
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
        p.setHealth(p.getMaxHealth());
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(label).withStyle(s -> s.withColor(0xFFAAAA))));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("홀림").withStyle(s -> s.withColor(0xFF3333).withBold(true))));
        p.sendSystemMessage(Component.literal("  " + label + "이(가) 당신의 발을 붙잡았다. 몸이 제멋대로 움직인다.")
                .withStyle(s -> s.withColor(0xFF7777).withItalic(true)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.HOSTILE, 1.2f, 0.5f);
        Facility.broadcast(p.level().getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 " + label + "에 홀렸습니다! 세 번 때려 정신 차리게 하십시오.")
                        .withStyle(s -> s.withColor(0xFF5555))));
        sync(p);
    }

    /** 홀림을 푼다. */
    public static void release(ServerPlayer p, String why) {
        EmployeeData d = get(p);
        if (d.possessLabel.isEmpty() && d.possessTicks <= 0) return;
        d.possessTicks = 0;
        d.possessHits = 0;
        d.possessLabel = "";
        d.possessHeal = false;
        p.removeEffect(MobEffects.HEALTH_BOOST);
        p.removeEffect(MobEffects.STRENGTH);
        p.removeEffect(MobEffects.SPEED);
        p.removeEffect(MobEffects.GLOWING);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        p.sendSystemMessage(Component.literal(why).withStyle(s -> s.withColor(0x55FF55)));
        sync(p);
    }

    /** 홀린 직원이 동료에게 맞았을 때. 세 번이면 풀린다. */
    public static void onPossessedHit(ServerPlayer p) {
        EmployeeData d = get(p);
        if (d.possessTicks <= 0) return;
        d.possessHits++;
        if (d.possessHits >= 3) {
            release(p, "  정신을 차렸다.");
        } else {
            WorkManager.actionbar(p, Component.literal("정신이 흔들린다 (" + d.possessHits + "/3)")
                    .withStyle(s -> s.withColor(0xFFAAAA)));
        }
    }

    /** 매 틱, 홀린 직원을 조종한다. */
    private static void tickPossession(ServerPlayer p, EmployeeData d) {
        ServerLevel sl = p.level();
        if (--d.possessTicks <= 0) {
            release(p, "  홀림이 풀렸다.");
            return;
        }
        if (p.tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.3, 0.1, 0.3, 0.01);
        }
        ServerPlayer target = null;
        double best = 32 * 32;
        for (ServerPlayer o : sl.players()) {
            if (o == p || !o.isAlive() || o.isCreative() || o.isSpectator()) continue;
            if (Employees.get(o).possessTicks > 0) continue;
            double dist = o.distanceToSqr(p);
            if (dist < best) {
                best = dist;
                target = o;
            }
        }
        if (target == null) return;
        Vec3 diff = target.position().subtract(p.position());
        Vec3 horiz = new Vec3(diff.x, 0, diff.z);
        if (horiz.lengthSqr() > 1.0E-4) {
            horiz = horiz.normalize().scale(0.30);
        }
        double vy = p.getDeltaMovement().y;
        if (p.onGround() && target.getY() - p.getY() > 1.0) vy = 0.42;
        p.setDeltaMovement(horiz.x, vy, horiz.z);
        if (p.distanceTo(target) < 2.6 && p.tickCount % 20 == 0) {
            target.hurtServer(sl, sl.damageSources().playerAttack(p), 4 + p.getRandom().nextInt(3));
            p.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1f);
            if (!target.isAlive() && d.possessHeal) {
                p.setHealth(p.getMaxHealth());
                Facility.broadcast(sl.getServer(), Component.literal("  … ").withStyle(s -> s.withColor(0xAA0000))
                        .append(p.getDisplayName())
                        .append(Component.literal("이(가) 쓰러진 동료를 짓밟고 기운을 되찾았다.").withStyle(s -> s.withColor(0xAA0000))));
            }
        }
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
            if (d.possessTicks > 0) {
                tickPossession(p, d);
            }
            if (d.laetitiaHeart && p.tickCount % 20 == 0) {
                p.level().sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 2.2, p.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
            if (d.sapTicks > 0) {
                d.sapTicks--;
                if (d.sapTicks == 100) {
                    p.level().sendParticles(ParticleTypes.FALLING_HONEY, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
                    WorkManager.actionbar(p, Component.literal("몸속에서 무언가 끓어오른다…").withStyle(s -> s.withColor(0xFFC040)));
                }
                if (d.sapTicks == 0) {
                    explodeSap(p);
                }
            }
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

    /** 거대한 나무 수액: 몸속의 수액이 터진다 */
    private static void explodeSap(ServerPlayer p) {
        ServerLevel sl = p.level();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.DRIPPING_HONEY, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.7f);
        for (ServerPlayer o : sl.players()) {
            if (o != p && o.distanceTo(p) <= 8) {
                damageSp(o, 15);
            }
        }
        Facility.broadcast(sl.getServer(), Component.literal("[관제] ").withStyle(s -> s.withColor(0xAA0000))
                .append(p.getDisplayName())
                .append(Component.literal(" 직원이 수액에 삼켜졌습니다.").withStyle(s -> s.withColor(0xFF5555))));
        ToolState.sapChance = 0;
        p.hurtServer(sl, sl.damageSources().magic(), 1000f);
    }

    public static Component statsText(EmployeeData d) {
        return Component.literal("")
                .append(Component.literal("용기 " + d.fortitude + " (" + EmployeeData.grade(d.fortitude) + ")  ").withStyle(s -> s.withColor(0xFF5555)))
                .append(Component.literal("신중 " + d.prudence + " (" + EmployeeData.grade(d.prudence) + ")  ").withStyle(s -> s.withColor(0xFFFFFF)))
                .append(Component.literal("자제 " + d.temperance + " (" + EmployeeData.grade(d.temperance) + ")  ").withStyle(s -> s.withColor(0xB46CFF)))
                .append(Component.literal("정의 " + d.justice + " (" + EmployeeData.grade(d.justice) + ")").withStyle(s -> s.withColor(0x55FFFF)));
    }
}
