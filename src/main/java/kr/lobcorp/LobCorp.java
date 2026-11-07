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
