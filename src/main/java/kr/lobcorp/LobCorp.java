package kr.lobcorp;

import kr.lobcorp.net.Payloads;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.WorkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LobCorp implements ModInitializer {
    public static final String MOD_ID = "lobcorp";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.init();
        ModEntities.init();
        ModAttachments.init();
        Payloads.init();

        ServerPlayNetworking.registerGlobalReceiver(Payloads.WorkAction.TYPE,
                (payload, ctx) -> WorkManager.handleAction(ctx.player(), payload.entityId(), payload.action()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WorkManager.tick(server);
            Employees.tick(server);
            Facility.tick(server);
            kr.lobcorp.work.ToolState.tick(server);
            kr.lobcorp.ordeal.OrdealManager.tick(server);
            kr.lobcorp.apocalypse.ApocalypseManager.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player;
            boolean fresh = !p.hasAttached(ModAttachments.EMPLOYEE);
            Employees.apply(p);
            if (fresh) {
                Employees.restoreSp(p);
                p.sendSystemMessage(Component.literal("[L사] 신입 직원으로 등록되었습니다. /lobcorp stats 로 스탯을 확인하세요.").withStyle(s -> s.withColor(0xAAAAAA)));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            WorkManager.abort(handler.player, "접속 종료");
            Facility.onLeave(handler.player);
            // 다른 세계의 초상: 새겨진 자나 대신하는 자가 나가면 초상화가 제자리로 돌아간다
            if (kr.lobcorp.work.ToolState.isEtched(handler.player)) {
                kr.lobcorp.work.ToolState.resetPortrait();
            } else if (kr.lobcorp.work.ToolState.isVictim(handler.player)) {
                java.util.UUID e = kr.lobcorp.work.ToolState.etched;
                kr.lobcorp.work.ToolState.resetPortrait();
                if (e != null) {
                    var ep = server.getPlayerList().getPlayer(e);
                    if (ep != null) {
                        ep.sendSystemMessage(Component.literal("  초상화 속 얼굴이 흐려진다. 대신 받아 줄 사람이 사라졌다.")
                                .withStyle(s -> s.withColor(0x8A4AC0).withItalic(true)));
                    }
                }
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            WorkManager.clear(oldP);
            Employees.get(newP).fairyTicks = 0;
            Employees.get(newP).sleepTicks = 0;
            Employees.get(newP).possessTicks = 0;
            Employees.get(newP).possessHits = 0;
            Employees.get(newP).possessLabel = "";
            Employees.get(newP).possessHeal = false;
            Employees.get(newP).laetitiaHeart = false;
            Employees.get(newP).sapTicks = 0;
            Employees.get(newP).pebble = false;
            Employees.get(newP).frost = 0;
            Employees.get(newP).frozenAt = null;
            Employees.get(newP).blessed = false;
            Employees.get(newP).devoured = false;
            Employees.get(newP).devouredAt = null;
            Employees.get(newP).sheepTicks = false;
            if (kr.lobcorp.work.ToolState.isNotesHolder(oldP)) kr.lobcorp.work.ToolState.clearNotes();
            if (kr.lobcorp.work.ToolState.isEtched(newP) || kr.lobcorp.work.ToolState.isVictim(newP)) {
                kr.lobcorp.work.ToolState.resetPortrait();
            }
            Employees.apply(newP);
            Employees.restoreSp(newP);
        });
        // 대피소 안의 직원은 어떤 피해도 받지 않는다
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof net.minecraft.server.level.ServerPlayer p && kr.lobcorp.work.ToolState.isSheltered(p)) {
                return false;
            }
            // E.G.O 무기로 다른 직원을 칠 때: WHITE=SP, BLACK=HP+SP, PALE=최대 HP 비율
            if (entity instanceof net.minecraft.server.level.ServerPlayer p
                    && source.getEntity() instanceof net.minecraft.server.level.ServerPlayer attacker
                    && attacker != p && source.getDirectEntity() == attacker
                    && attacker.getMainHandItem().getItem() instanceof kr.lobcorp.item.EgoWeaponItem ego
                    && ego.color != kr.lobcorp.DamageColor.RED) {
                net.minecraft.server.level.ServerLevel sl = p.level();
                switch (ego.color) {
                    case WHITE -> {
                        int sp = Math.round(amount * kr.lobcorp.item.SuitResist.mult(p, kr.lobcorp.DamageColor.WHITE));
                        Employees.damageSpRaw(p, Math.max(1, sp));
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
                        sl.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_HURT,
                                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.6f);
                        if (Employees.get(p).sleepTicks > 0) Employees.wake(p, false);
                        if (Employees.get(p).possessTicks > 0) Employees.onPossessedHit(p);
                        Employees.sync(p);
                        return false;
                    }
                    case BLACK -> {
                        // HP 피해와 같은 양의 SP 피해가 더해진다 (둘 다 BLACK 내성 적용)
                        float bm = kr.lobcorp.item.SuitResist.mult(p, kr.lobcorp.DamageColor.BLACK);
                        Employees.damageSpRaw(p, Math.max(1, Math.round(amount * bm)));
                        kr.lobcorp.item.SuitResist.hurtRaw(p, amount * bm);
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
                        Employees.sync(p);
                        return false;
                    }
                    case PALE -> {
                        // 피해 수치 1당 최대 HP의 5%
                        Employees.damagePale(p, amount * 5f);
                        if (Employees.get(p).possessTicks > 0) Employees.onPossessedHit(p);
                        return false;
                    }
                    default -> { }
                }
            }
            // 미친 연구원의 노트: 든 채로 받은 피해가 쌓인다
            if (entity instanceof net.minecraft.server.level.ServerPlayer p && kr.lobcorp.work.ToolState.isNotesHolder(p)) {
                kr.lobcorp.work.ToolState.addNotesDamage(p, amount);
            }
            // 다른 세계의 초상: 새겨진 자가 받을 피해는 대신하는 자에게 옮겨 간다
            if (entity instanceof net.minecraft.server.level.ServerPlayer p && kr.lobcorp.work.ToolState.isEtched(p)) {
                if (amount > p.getMaxHealth()) return true; // 한 번에 최대 HP를 넘는 피해는 그대로 받는다
                net.minecraft.server.level.ServerPlayer v =
                        p.level().getServer().getPlayerList().getPlayer(kr.lobcorp.work.ToolState.victim);
                if (v != null && v != p) {
                    float redirected = amount * kr.lobcorp.work.ToolState.portraitMult();
                    kr.lobcorp.work.ToolState.portraitTotal += redirected;
                    p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                            p.getX(), p.getY() + 1.2, p.getZ(), 8, 0.4, 0.6, 0.4, 0.1);
                    v.level().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                            v.getX(), v.getY() + 1.2, v.getZ(), 8, 0.4, 0.6, 0.4, 0.1);
                    v.hurtServer(v.level(), v.level().damageSources().magic(), redirected);
                }
                return false;
            }
            // 그 밖의 HP 피해는 모두 RED 피해로 취급한다
            if (entity instanceof net.minecraft.server.level.ServerPlayer p2 && !kr.lobcorp.item.SuitResist.bypass
                    && amount > 0 && amount < 500f) {
                float m = kr.lobcorp.item.SuitResist.mult(p2, kr.lobcorp.DamageColor.RED);
                if (m != 1f) {
                    kr.lobcorp.item.SuitResist.bypass = true;
                    try {
                        p2.hurtServer(p2.level(), source, amount * m);
                    } finally {
                        kr.lobcorp.item.SuitResist.bypass = false;
                    }
                    return false;
                }
            }
            return true;
        });
        // 잠든 직원은 피해를 받으면 깨고, 홀린 직원은 동료에게 세 번 맞으면 정신을 차린다
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof net.minecraft.server.level.ServerPlayer p) {
                if (Employees.get(p).sleepTicks > 0) {
                    Employees.wake(p, false);
                }
                if (Employees.get(p).possessTicks > 0 && source.getEntity() instanceof net.minecraft.world.entity.player.Player attacker
                        && attacker != p) {
                    Employees.onPossessedHit(p);
                }
            }
            if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer attacker && source.getDirectEntity() == attacker
                    && entity != attacker && damageTaken > 0) {
                kr.lobcorp.item.MeleeEffects.afterMelee(entity, attacker, damageTaken);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof net.minecraft.server.level.ServerPlayer p && Employees.get(p).possessTicks > 0) {
                Employees.release(p, "  홀림이 풀렸다.");
            }
            kr.lobcorp.work.ToolState.onDeath(entity, source);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> LobCorpCommands.register(dispatcher));
        LOG.info("LobCorp loaded — 오늘도 좋은 하루 되세요.");
    }
}
