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
            Employees.apply(newP);
            Employees.restoreSp(newP);
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
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> LobCorpCommands.register(dispatcher));
        LOG.info("LobCorp loaded — 오늘도 좋은 하루 되세요.");
    }
}
